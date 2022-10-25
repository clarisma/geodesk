package com.clarisma.common.store;

import com.clarisma.common.util.Bytes;
import org.eclipse.collections.api.map.primitive.MutableIntObjectMap;
import org.eclipse.collections.impl.map.mutable.primitive.IntObjectHashMap;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Consumer;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;

import static com.clarisma.common.store.BlobStoreConstants.*;

// TODO: We cannot mark a ticket as "completed" until the transaction has been comitted!

public class Downloader
{
    private final BlobStore store;
    private final String baseUrl;
    private final Queue<Ticket> ticketQueue;
    private final MutableIntObjectMap<Ticket> ticketMap;
    private DownloadThread thread;
    private final int maxPendingTickets = 16;
    private final int maxKeepAlive = 60_000;
    private final int retryAttempts = 3;
    private final int retryDelay = 500;
    private final boolean progressiveDelay = true;
    private static final int BUFFER_SIZE = 4096;

    public static final int METADATA_ID = -1;

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

        public int page()
        {
            return page;
        }

        // don't call this directly; must be called by Downloader.ticketCompleted()
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

        public synchronized void throwError() throws RuntimeException
        {
            assert completed;
            if(error != null)
            {
                RuntimeException ex = error instanceof RuntimeException ?
                    (RuntimeException)error :
                    new StoreException("Error while downloading", error);
                throw ex;
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

    public synchronized void shutdown()
    {
        while(thread != null)
        {
            thread.interrupt();
            try
            {
                wait();
            }
            catch(InterruptedException ex)
            {
                // TODO: do nothing?
            }
        }

        // TODO: guard against new requests submitted post-shutdown
    }

    private synchronized void ticketCompleted(Ticket ticket, int page, Throwable error)
    {
        ticketMap.remove(ticket.id);
        ticket.complete(page, error);
        notifyAll();
    }

    private synchronized void threadEnded()
    {
        thread = null;
        notifyAll();
    }

    private synchronized void cancelTickets(Throwable ex)
    {
        // make a copy because complete() modifies ticketMap
        List<Ticket> remainingTickets = new ArrayList<>(ticketMap.values());
        for(Ticket ticket: remainingTickets)
        {
            ticketCompleted(ticket, 0, ex);
        }
        assert ticketMap.size() == 0;
        ticketQueue.clear();
        thread = null;
    }

    private synchronized Ticket takeTicket(boolean wait) throws InterruptedException
    {
        Ticket ticket = ticketQueue.poll();
        if(ticket != null) return ticket;
        if(!wait) return null;
        wait(maxKeepAlive);
        ticket = ticketQueue.poll();
        if(ticket == null) thread = null;
        return ticket;
    }

    protected URL urlOf(int id) throws MalformedURLException
    {
        if(id == METADATA_ID) return new URL(baseUrl + "meta.tile");
        return new URL(String.format("%s%03X/%03X.tile",
            baseUrl, id >>> 12, id & 0xfff));
    }

    // TODO: don't complete tickets here
    protected int download(Ticket ticket) throws IOException
    {
        int id = ticket.id;
        if(id == METADATA_ID)
        {
            if(!store.isEmpty())
            {
                return 0;
            }
        }
        else
        {
            int page = store.getIndexEntry(id);
            if(page != 0) return page;
        }

        URL url = urlOf(id);

        // TODO: retry

        int page = download(id, url);
        if(id != METADATA_ID) store.setIndexEntry(id, page);
        return page;
    }

    private void invalidTileFile(String reason)
    {
        throw new RuntimeException("Invalid tile file: " + reason); // TODO
    }

    protected int download(int id, URL url) throws IOException
    {
        // TODO: checksums

        byte[] buf = new byte[BUFFER_SIZE];
        final int firstPage;

        URLConnection connection = url.openConnection();
        connection.connect();
        InputStream in = connection.getInputStream();
        try
        {
            if (in.read(buf, 0, EXPORTED_HEADER_LEN) != EXPORTED_HEADER_LEN)
            {
                invalidTileFile("Truncated header");
            }
            if (Bytes.getInt(buf, 0) != EXPORTED_MAGIC)
            {
                invalidTileFile("Wrong file type");
            }
            int uncompressedSize = Bytes.getInt(buf, EXPORTED_ORIGINAL_LEN_OFS);
            int payloadSize = uncompressedSize + 4;     // one word is the checksum
            if (payloadSize <= 0 || payloadSize > (1 << 30) - 4)
            {
                invalidTileFile("Uncompressed size invalid");
            }

            InflaterInputStream zipIn = new InflaterInputStream(in);

            if (id == METADATA_ID)
            {
                // For the metadata, we only journal the first block
                // Need to enforce requirement that metadata can only be
                // updated in an empty store

                firstPage = 0;
                int firstBlockLen = Math.min(uncompressedSize, BLOCK_LEN);
                ByteBuffer rootBlock = store.getBlockOfPage(0);
                read(zipIn, buf, rootBlock, 0, firstBlockLen);
                if (uncompressedSize > BLOCK_LEN)
                {
                    read(zipIn, buf, store.baseMapping, BLOCK_LEN, uncompressedSize - BLOCK_LEN);
                }

                // Set total number of pages based on the metadata length
                // (This cannot be included as part of the downloaded metadata itself,
                // since it varies based on the BlobStore's page size)
                int metadataSize = rootBlock.getInt(METADATA_SIZE_OFS);
                rootBlock.putInt(TOTAL_PAGES_OFS, store.bytesToPages(metadataSize));
            }
            else
            {
                // For blobs, we have to journal the first block, because we need to
                // journal the header data of a previously freed block
                // If the blob's payload overwrites the free-blob tail, we need to journal
                // the last block as well. All the other blocks in-between can be written
                // directly into the store, since these areas by definition only contain
                // garbage. We don't have to check whether the blob is allocated in the
                // "virgin" area of the store instead of in a "recycled" blob, since
                // getBlock() gives us a ByteBuffer that is an unjournaled view of the
                // raw MappedByteBuffer if the blob lies above `preTransactionFileSize`

                // TODO: must enforce that we never allocate the space of a blob that
                //  is freed within the same transaction!

                int headerLen = 8;  // blob header word + checksum word
                firstPage = store.allocateBlob(payloadSize);
                int p = store.offsetOfPage(firstPage);
                int firstBlockLen = Math.min(uncompressedSize, BLOCK_LEN - headerLen);
                read(zipIn, buf, store.getBlockOfPage(firstPage), headerLen, firstBlockLen);
                // we don't use p here, because the block buffer uses relative addressing
                if (uncompressedSize > firstBlockLen)
                {
                    // Blob is longer than one block (can still be single-page)

                    int pages = store.pagesForPayloadSize(payloadSize);

                    // We can't use store.offsetOfPage(firstPage + pages)
                    // because it returns 0 if blob sits at end of 1-GB segment;
                    // that's why we calculate explicitly
                    int pTail = p + (pages << store.pageSizeShift) - FREE_BLOB_TRAILER_LEN;
                    int pPayloadEnd = p + uncompressedSize + headerLen;
                    ByteBuffer blobBuf = store.bufferOfPage(firstPage);
                    int pUnprotectedStart = p + BLOCK_LEN;
                    if (pPayloadEnd > pTail)
                    {
                        int pUnprotectedEnd = pTail & 0xffff_f000; // TODO: assumes 4096 block len
                        int unprotectedLen = pUnprotectedEnd - pUnprotectedStart;
                        if (unprotectedLen > 0)
                        {
                            read(zipIn, buf, blobBuf, pUnprotectedStart, unprotectedLen);
                        }
                        long absoluteTailBlockPos = store.absoluteOffsetOfPage(firstPage)
                            + pUnprotectedEnd - p;
                        ByteBuffer tailBlock = store.getBlock(absoluteTailBlockPos);
                        int tailBlockLen = pPayloadEnd - pUnprotectedEnd;
                        assert tailBlockLen > 0;
                        assert tailBlockLen <= 4096;
                        read(zipIn, buf, tailBlock,0, tailBlockLen);
                    }
                    else
                    {
                        read(zipIn, buf, blobBuf, pUnprotectedStart, pPayloadEnd - pUnprotectedStart);
                    }
                }
            }
        }
        finally
        {
            in.close();
        }
        return firstPage;
    }

    /**
     * Reads compressed data from a stream and writes it into a target buffer.
     *
     * @param zipIn     the stream of compressed data
     * @param buf       the intermediate buffer to use
     * @param target    the target buffer
     * @param p         the starting position within the target buffer
     * @param len       the uncompressed length of the data to be read
     * @throws IOException
     */
    private void read(InflaterInputStream zipIn, byte[] buf, ByteBuffer target, int p, int len) throws IOException
    {
        while(len > 0)
        {
            int chunkLen = Math.min(len, buf.length);
            int bytesRead = zipIn.read(buf, 0, chunkLen);
            if(bytesRead < 0) throw new IOException("Unexpected end of compressed data");
            target.put(p, buf, 0, bytesRead);
            p += bytesRead;
            len -= bytesRead;
        }
    }

    private class DownloadThread extends Thread
    {
        @Override public void run()
        {
            try
            {
                for (; ; )
                {
                    Ticket ticket = takeTicket(true);
                    if (ticket == null) break;
                    store.beginTransaction(Store.LOCK_APPEND);
                    for (; ; )
                    {
                        int page = download(ticket);
                        store.commit();

                        // TODO: ticketCompleted should not be called until
                        //  transaction is committed!

                        ticketCompleted(ticket, page, null);
                        ticket = takeTicket(false);
                        if (ticket == null) break;
                    }
                    store.endTransaction();
                }
            }
            catch(Throwable ex)
            {
                cancelTickets(ex);
                try
                {
                    store.endTransaction();
                }
                catch(Throwable ex2)
                {
                    // doesn't matter at this point, we're shutting
                    // down because of an exception
                }
            }
            threadEnded();
        }
    }
}
