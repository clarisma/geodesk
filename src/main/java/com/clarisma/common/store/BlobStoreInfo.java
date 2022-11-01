/*
 * Copyright (c) Clarisma / GeoDesk contributors
 *
 * This source code is licensed under the Apache 2.0 license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.clarisma.common.store;

import org.eclipse.collections.api.set.primitive.MutableIntSet;
import org.eclipse.collections.impl.set.mutable.primitive.IntHashSet;

import java.nio.ByteBuffer;
import static com.clarisma.common.store.BlobStoreConstants.*;

public class BlobStoreInfo
{
    private final BlobStore store;
    private long freeSpace = -1;
    private int freeBlobCount = -1;

    public BlobStoreInfo(BlobStore store)
    {
        this.store = store;
    }

    public long freeSpace()
    {
        if(freeSpace < 0) countFreeBlobs();
        return freeSpace;
    }

    public int freeBlobCount()
    {
        if(freeBlobCount < 0) countFreeBlobs();
        return freeBlobCount;
    }

    private void countFreeBlobs()
    {
        int freeBlobCount = 0;
        int freePageCount = 0;
        MutableIntSet pagesVisited = new IntHashSet();
        ByteBuffer baseSegment = store.baseMapping;
        int totalPages = baseSegment.getInt(TOTAL_PAGES_OFS);
        for(int i=0; i<512; i++)
        {
            int freeTableBlob = baseSegment.getInt(TRUNK_FREE_TABLE_OFS + i * 4);
            if(freeTableBlob == 0) continue;
            store.checkPage(freeTableBlob);
            ByteBuffer freeTableBuf = store.bufferOfPage(freeTableBlob);
            int freeTableOfs = store.offsetOfPage(freeTableBlob + LEAF_FREE_TABLE_OFS);
            for(int i2=0; i2<512; i2++)
            {
                int freeBlob = freeTableBuf.getInt(freeTableOfs + i2*4);
                if(freeBlob == 0) continue;
                int freeBlobPages = i * 512 + i2 + 1;
                for(;;)
                {
                    store.checkPage(freeBlob);
                    freeBlobCount++;
                    freeBlobCount += freeBlobPages;
                    pagesVisited.add(freeBlob);
                    ByteBuffer blobBuf = store.bufferOfPage(freeBlob);
                    int ofs = store.offsetOfPage(freeBlob);
                    freeBlob = blobBuf.getInt(ofs + NEXT_FREE_BLOB_OFS);
                    if(freeBlob == 0) break;
                    if(pagesVisited.contains(freeBlob))
                    {
                        throw new StoreException(
                            "Circular reference for free blobs of size " + freeBlobPages,
                            store.path());
                    }
                }
            }
        }
        freeSpace = freeBlobCount * store.pageSize();
        this.freeBlobCount = freeBlobCount;
    }
}
