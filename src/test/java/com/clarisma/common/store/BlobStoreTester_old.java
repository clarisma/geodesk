package com.clarisma.common.store;

import com.clarisma.common.text.Format;
import com.clarisma.common.util.Log;
import org.eclipse.collections.api.list.primitive.MutableLongList;
import org.eclipse.collections.impl.list.mutable.primitive.LongArrayList;
import org.locationtech.jts.util.Stopwatch;

import java.nio.file.Path;
import java.util.Random;

public class BlobStoreTester_old extends BlobStore
{
    private int runs = 10_000;
    private int maxTransactionLength = 1;
    private int maxBlobSize = 25;
    private MutableLongList blobs = new LongArrayList();
    private long totalBlobsAllocated;
    private long totalBlobsFreed;


    public BlobStoreTester_old(String filename)
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

    public void run()
    {
        Random random = new Random();

        TestBlobStoreVerifier verifier = new TestBlobStoreVerifier();

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
            beginTransaction();
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
        }
        if(!verify(verifier)) throw new RuntimeException("Store invalid");
        // must delete store before this test, or else verifier will
        // report unexpected blobs (those created outside of this test)

        close();

        long ms = timer.stop();
        Log.debug("Allocated %d and freed %d in %s", totalBlobsAllocated,
            totalBlobsFreed, Format.formatTimespan(ms));
        Log.debug("Avg. time of %d ms per alloc/free", ms /
            (totalBlobsAllocated + totalBlobsFreed));
    }

    public static void main(String[] args)
    {
        BlobStoreTester_old test = new BlobStoreTester_old("c:\\geodesk\\test.store");
        test.run();
    }

    private static class TestBlobStoreVerifier extends BlobStoreVerifier<BlobStoreTester_old>
    {
        @Override protected void doVerify()
        {
            super.doVerify();
            if(!valid) return;

            for (int i = 0; i < store.blobs.size(); i++)
            {
                long blobInfo = store.blobs.get(i);
                int pos = (int)blobInfo;
                int len = (int)(blobInfo >> 32);

                Blob blob = blobMap.get(pos);
                if(blob == null)
                {
                    error(0, "Blob %d is missing", pos);
                }
                else
                {
                    blob.flags |= BLOB_REFERENCED_FLAG;
                    if(blob.pages != len)
                    {
                        error(0, String.format("Blob %d should be %d pages, not %d",
                            pos, len, blob.pages));
                    }
                }
            }

            for(Blob blob: blobList)
            {
                if(!blob.isReferenced() && !blob.isFree())
                {
                    error(absPosOfPage(blob.firstPage),
                        "Unexpected blob %d", blob.firstPage);
                }
            }
        }
    }
}
