package com.clarisma.common.store;

import org.eclipse.collections.api.map.primitive.MutableIntObjectMap;
import org.eclipse.collections.impl.map.mutable.primitive.IntObjectHashMap;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Consumer;

public class Downloader
{
    private final BlobStore store;
    private final String baseUrl;
    private final Queue<Ticket> ticketQueue;
    private final MutableIntObjectMap<Ticket> ticketMap;
    private DownloadThread thread;
    private final int maxPendingTickets = 16;
    private final int maxKeepAlive = 60_000;

    public Downloader(BlobStore store, String baseUrl)
    {
        this.store = store;
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl : (baseUrl + '/');
        ticketQueue = new ArrayDeque<>();
        ticketMap = new IntObjectHashMap<>();
    }


    public static class Ticket
    {
        private final int id;
        private boolean completed;
        private int page;
        private Throwable error;
        private final List<Consumer<Ticket>> consumers = new ArrayList<>();

        Ticket(int id)
        {
            this.id = id;
        }

        synchronized void complete(int page, Throwable error)
        {
            this.page = page;
            this.error = error;
            completed = true;
            for(Consumer<Ticket> c: consumers) c.accept(this);
            notifyAll();
        }

        synchronized void awaitCompletion() throws InterruptedException
        {
            for(;;)
            {
                if(completed) return;
                wait();
            }
        }
    }

    /*
    private static class Chunk
    {
        int id;
        int offset;
        int length;
        byte[] data;
        Throwable error;
    }
     */

    public synchronized Ticket request(int id, Consumer<Ticket> consumer) throws InterruptedException
    {
        Ticket ticket = ticketMap.get(id);
        if(ticket == null)
        {
            while(ticketMap.size() == maxPendingTickets)
            {
                wait();
            }
            ticket = new Ticket(id);
            ticketMap.put(id, ticket);
            ticketQueue.add(ticket);
            notifyAll();
        }
        if(consumer != null) ticket.consumers.add(consumer);
        if(thread == null)
        {
            thread = new DownloadThread();
            thread.start();
        }
        return ticket;
    }

    private synchronized void ticketCompleted(Ticket ticket, int page, Throwable error)
    {
        ticketMap.remove(ticket.id);
        ticket.complete(page, error);
    }

    private synchronized void cancelTickets(Throwable ex)
    {
        for(Ticket ticket: ticketMap.values())
        {
            ticket.complete(0, ex);
        }
        ticketMap.clear();
        ticketQueue.clear();
        thread = null;
    }

    private synchronized Ticket takeTicket()
    {
        Ticket ticket = ticketQueue.poll();
        if(ticket != null) return ticket;
        try
        {
            wait(maxKeepAlive);
        }
        catch(InterruptedException ex)
        {
            // TODO
        }
        ticket = ticketQueue.poll();
        if(ticket == null) thread = null;
        return ticket;
    }

    protected URL urlOf(int id) throws MalformedURLException
    {
        if(id == -1) return new URL(baseUrl + "meta.tile");
        return new URL(String.format("%s%03X/%03X.tile",
            baseUrl, id >>> 12, id & 0xfff));
    }



    private class DownloadThread extends Thread
    {
        @Override public void run()
        {
            int prevLockLevel;
            try
            {
                prevLockLevel = store.lock(Store.LOCK_APPEND);
            }
            catch(Throwable ex)
            {
                cancelTickets(ex);
                return;
            }
            for(;;)
            {
                Ticket ticket = takeTicket();
                if(ticket == null) break;
            }
            try
            {
                store.lock(prevLockLevel);
            }
            catch(Throwable ex)
            {
                cancelTickets(ex);
                return;
            }
        }
    }
}
