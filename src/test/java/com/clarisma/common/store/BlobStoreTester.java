package com.clarisma.common.store;

import com.clarisma.common.text.Format;
import com.clarisma.common.util.Log;
import org.eclipse.collections.api.list.primitive.MutableLongList;
import org.eclipse.collections.impl.list.mutable.primitive.LongArrayList;
import org.locationtech.jts.util.Stopwatch;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Random;

public class BlobStoreTester extends BlobStore
{
    private int runs = 10;
    private int maxTransactionLength = 500;
    private int maxBlobSize = 262_144;
    private MutableLongList blobs = new LongArrayList();
    private long totalBlobsAllocated;
    private long totalBlobsFreed;


    public BlobStoreTester(String filename)
    {
        setPath(Path.of(filename));
    }

    public int alloc(int pages)
    {
        //log.debug("Attempting to allocate blob with {} pages", pages);
        int blob = allocateBlob((pages << pageSizeShift) - 4);
        //log.debug("  Allocated blob with {} pages at {}", pages, blob);
        return blob;
    }

    public void free(int blob)
    {
        //log.debug("Attempting to free blob at {}", blob);
        freeBlob(blob);
        //log.debug("  Freed blob at {}", blob);
    }

    public void check()
    {
        Checker checker = new Checker(this);
        checker.check();
        checker.reportErrors(System.out);
        if(checker.hasErrors()) throw new RuntimeException("test failed");
    }

    public void run() throws IOException
    {
        Random random = new Random();

        Stopwatch timer = new Stopwatch();
        timer.start();

        Log.debug("Opening database");
        open();
        Log.debug("Database opened");

        // alloc(4591);

        for(int run=0; run<runs; run++)
        {
            boolean deleteBlobs = random.nextBoolean();
            int numberOfBlobs = random.nextInt(maxTransactionLength) + 1;
            Log.debug("Run %d: %s %d blobs", run,
                deleteBlobs ? "Freeing" : "Allocating", numberOfBlobs);
            beginTransaction(deleteBlobs ? LOCK_EXCLUSIVE : LOCK_APPEND);
            if (deleteBlobs)
            {
                if(numberOfBlobs > blobs.size()) numberOfBlobs = blobs.size();
                for (int i = 0; i < numberOfBlobs; i++)
                {
                    int totalBlobs = blobs.size();
                    if (totalBlobs == 0) break;
                    int index = random.nextInt(totalBlobs);
                    long blobInfo = blobs.get(index);
                    if (totalBlobs > 1) blobs.set(index, blobs.get(totalBlobs - 1));
                    blobs.removeAtIndex(totalBlobs - 1);
                    int blob = (int) blobInfo;
                    free(blob);
                }
                totalBlobsFreed += numberOfBlobs;
            }
            else
            {
                for (int i = 0; i < numberOfBlobs; i++)
                {
                    int pages = random.nextInt(maxBlobSize) + 1;
                    int blob = alloc(pages);
                    blobs.add((((long) pages) << 32) | blob);
                }
                totalBlobsAllocated += numberOfBlobs;
            }
            commit();
            endTransaction();
            // check();
        }

        long ms = timer.stop();
        Log.debug("Allocated %d and freed %d in %s", totalBlobsAllocated,
            totalBlobsFreed, Format.formatTimespan(ms));
        if(totalBlobsAllocated > 0 || totalBlobsFreed > 0)
        {
            Log.debug("Avg. time of %d ms per alloc/free", ms /
                (totalBlobsAllocated + totalBlobsFreed));
        }

        check();
        close();
    }

    public static void main(String[] args) throws Exception
    {
        // Files.deleteIfExists(Path.of("c:\\geodesk\\test.store"));
        // Files.deleteIfExists(Path.of("c:\\geodesk\\test.store-journal"));
            // TODO: delete journal
        BlobStoreTester test = new BlobStoreTester("c:\\geodesk\\test.store");
        test.run();
        test.close();
    }

    private static class Checker extends BlobStoreChecker
    {
        final BlobStoreTester tester;

        Checker(BlobStoreTester tester)
        {
            super(tester);
            this.tester = tester;
        }

        @Override protected void checkIndex()
        {
            for (int i = 0; i < tester.blobs.size(); i++)
            {
                long blobInfo = tester.blobs.get(i);
                int firstPage = (int) blobInfo;
                int len = (int) (blobInfo >> 32);

                // TODO: temporarily turned off to focus on general integrity
                // useBlob(0, firstPage);
            }
        }
    }
}
