package com.clarisma.common.store;

import org.eclipse.collections.api.map.primitive.MutableIntObjectMap;
import org.eclipse.collections.impl.map.mutable.primitive.IntObjectHashMap;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Consumer;

public class Downloader
{
    private final BlobStore store;
    private final String baseUrl;
    private int maxThreads;
    private DownloadThread[] downloadThreads;
    private WriterThread writerThread;
    private BlockingQueue<Ticket> downloadQueue;
    private BlockingQueue<Chunk> dataQueue;
    private MutableIntObjectMap<Ticket> ticketMap;

    public static class Ticket
    {
        private final int id;
        private final List<Consumer<Ticket>> consumers = new ArrayList<>();

        Ticket(int id)
        {
            this.id = id;
        }

        synchronized void dispatch()
        {
            for(Consumer<Ticket> c: consumers) c.accept(this);
            notifyAll();
        }
    }

    private static class Chunk
    {
        int id;
        int offset;
        int length;
        byte[] data;
        Throwable error;
    }

    private class DownloadThread extends Thread
    {
        @Override public void run()
        {
        }
    }

    private class WriterThread extends Thread
    {
        @Override public void run()
        {
            for(;;)
            {
                try
                {
                    Chunk data = dataQueue.take();
                }
                catch(InterruptedException ex)
                {

                }
            }
        }
    }

    public Downloader(BlobStore store, String baseUrl)
    {
        this.store = store;
        this.baseUrl = baseUrl;
        downloadQueue = new LinkedBlockingQueue<>();
        dataQueue = new LinkedBlockingQueue<>();
        ticketMap = new IntObjectHashMap<>();
    }

    public synchronized Ticket request(int id, Consumer<Ticket> consumer)
    {
        Ticket ticket = ticketMap.get(id);
        if(ticket == null)
        {
            ticket = new Ticket(id);
            ticketMap.put(id, ticket);
        }
        if(consumer != null) ticket.consumers.add(consumer);
        return ticket;
    }


}
