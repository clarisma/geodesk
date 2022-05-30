package com.clarisma.common.store;


import com.clarisma.common.util.Log;
import com.geodesk.core.Box;
import com.geodesk.feature.FeatureLibrary;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.*;

public class BlobStoreTest
{
    private TestBlobStore store;

    public static class TestBlobStore extends BlobStore
    {
        public TestBlobStore(String filename)
        {
            setPath(Path.of(filename));
        }

        public int alloc(int pages)
        {
            beginTransaction();
            int blob = allocateBlob((pages << pageSizeShift) - 4);
            commit();
            Log.debug("Allocated blob with %d pages at %d", pages, blob);
            return blob;
        }

        public void free(int... blobs)
        {
            beginTransaction();
            for (int i = 0; i < blobs.length; i++)
            {
                freeBlob(blobs[i]);
            }
            commit();
            Log.debug("Freed blobs at %s", blobs);
        }
    }

    @Before public void setUp() throws IOException
    {
        String filename = "c:\\geodesk\\test-blob.store";
        // TODO: delete journal
        if (Files.deleteIfExists(Path.of(filename))) ;
        store = new TestBlobStore(filename);
        store.open();
    }

    @After public void tearDown()
    {
        store.close();
    }

    @Test public void testAllocFree() throws IOException
    {
        int a = store.alloc(4);     // 1:4
        assertEquals(1, a);
        int b = store.alloc(20);    // 1:4, 5:20
        assertEquals(5, b);
        store.free(a);                    // (1:4), 5:20
        int c = store.alloc(2);     // 1:2, (3:2), 5:20
        assertEquals(1, c);
        int d = store.alloc(15);     // 1:2, (3:2), 5:20, 25:15
        assertEquals(25, d);
        store.free(b);                    // 1:2, 3:2, (3:22), 25:15
        int e = store.alloc(21);     // 1:2, 3:21, (24:1), 25:15
        assertEquals(3, e);
        store.free(d);                    // 1:2, 3:21
        int f = store.alloc(10);
        assertEquals(24, f);      // c=1:2, e=3:21, f=24:10
        int g = store.alloc(9);
        assertEquals(34, g);      // c=1:2, e=3:21, f=25:10, g=34:9
        int h = store.alloc(7);
        assertEquals(43, h);      // c=1:2, e=3:21, f=25:10, g=35:9, h=43:7
        store.free(e, g);                 // c=1:2, (3:21), f=25:10, (35:9), h=43:7
        store.free(f);                 // c=1:2, (3:40), h=43:7
        int i = store.alloc(38);
        assertEquals(3, i);      // c=1:2, i=3:38, (41:2), h=43:7
        store.free(h);                 // c=1:2, i=3:38

        int max = 1 << 18;
        int j = store.alloc(max);
        assertEquals(max, j);      // c=1:2, i=3:38, (41:2), h=43:7
        int k = store.alloc(max);
        assertEquals(max * 2, k);      // c=1:2, i=3:38, (41:2), h=43:7

        store.free(i, j, k);          // c=1:2
    }

    @Test public void testBigAllocFree() throws IOException
    {
        int max = 1 << 18;
        int a = store.alloc(max);
        check(a);
        int b = store.alloc(max);
        check(a,b);
        int c = store.alloc(max);
        check(a,b,c);
        store.free(a);
        check(b,c);
        store.free(b);
        check(c);
        store.free(c);
        check();
    }

    private void check(int... inUse)
    {
        Log.debug("---- Checking...");
        BlobStoreChecker checker = new BlobStoreChecker(store);
        for(int b: inUse) checker.useBlob(0, b);
        checker.check();
        checker.reportErrors(System.out);
        Assert.assertFalse(checker.hasErrors());
    }
}