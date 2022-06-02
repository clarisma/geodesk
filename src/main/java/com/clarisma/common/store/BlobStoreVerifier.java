//package com.clarisma.common.store;
//
//import com.clarisma.common.util.Log;
//import org.eclipse.collections.api.map.primitive.MutableIntObjectMap;
//import org.eclipse.collections.impl.map.mutable.primitive.IntObjectHashMap;
//
//import java.nio.ByteBuffer;
//import java.util.ArrayList;
//import java.util.List;
//
//import static com.clarisma.common.store.BlobStore.*;
//
//import static com.clarisma.common.store.BlobStoreConstants.*;
//
//// TODO: decouple the Verifier; instead, relax method visibility of BlobStore
//public class BlobStoreVerifier<T extends BlobStore> extends Store.Verifier<T>
//{
//    protected static class Blob
//    {
//        int firstPage;
//        int pages;
//        int flags;
//
//        Blob(int firstPage, int pages, int flags)
//        {
//            this.firstPage = firstPage;
//            this.pages = pages;
//            this.flags = flags;
//        }
//
//        boolean isReferenced()
//        {
//            return (flags & BLOB_REFERENCED_FLAG) != 0;
//        }
//
//        boolean isFree()
//        {
//            return (flags & FREE_BLOB_FLAG) != 0;
//        }
//    }
//
//    protected static final int BLOB_REFERENCED_FLAG = 1;
//
//    private int totalPages;
//    private int pageSizeShift;
//    private int pageSize;
//    private int pagesPerSegment;
//    private int metadataSize;
//    protected List<Blob> blobList = new ArrayList<>();
//    protected MutableIntObjectMap<Blob> blobMap = new IntObjectHashMap<>();
//    protected boolean valid = true;
//
//    protected void error(long ofs, String msg)
//    {
//        // System.out.format("%16X  %s\n", ofs, msg);
//        Log.error("%16X  %s", ofs, msg);
//        valid = false;
//    }
//
//    protected void error(long ofs, String msg, int val)
//    {
//        // System.out.format("%16X  " + msg + "\n", ofs, val);
//        Log.error("%16X  " + msg, ofs, val);
//        valid = false;
//    }
//
//    protected boolean verifyBlockSize(ByteBuffer buf, int ofs)
//    {
//        return false;
//    }
//
//    private ByteBuffer bufferOfPage(int page)
//    {
//        return store.getMapping(page / pagesPerSegment);
//    }
//
//    private int offsetOfPage(int page)
//    {
//        return (page % pagesPerSegment) * pageSize;
//    }
//
//    protected long absPosOfPage(int page)
//    {
//        return (long)page * pageSize;
//    }
//
//    private void verifyStructure()
//    {
//        int metadataPages = (metadataSize + pageSize - 1) / pageSize;
//        if(metadataPages > totalPages)
//        {
//            error(TOTAL_PAGES_OFS,
//                "Total pages must be at least %d to accommodate metadata", metadataPages);
//            return;
//        }
//        int page = metadataPages;
//        while(page < totalPages)
//        {
//            ByteBuffer buf = bufferOfPage(page);
//            int p = offsetOfPage(page);
//            int header = buf.getInt(p);
//            int payloadSize = header & 0x3fff_ffff;
//            if(header == 0)
//            {
//                error(absPosOfPage(page),
//                    "Invalid (uninitialized) blob");
//                return;
//            }
//            if(payloadSize ==0 || payloadSize > (1 << 30) - 4)
//            {
//                error(absPosOfPage(page),
//                    "Invalid payload size: %d", payloadSize);
//                return;
//            }
//            int len = (payloadSize + pageSize + 3) / pageSize;
//            int remainingPages = ((1 << 30) - p) / pageSize;
//            if(len > remainingPages)
//            {
//                error(absPosOfPage(page), "Blob crosses segment boundary");
//                return;
//            }
//            if(page + len > totalPages)
//            {
//                error(absPosOfPage(page), "Blob exceeds total page count");
//                return;
//            }
//            Blob blob = new Blob(page, len, header &
//                (FREE_BLOB_FLAG | PRECEDING_BLOB_FREE_FLAG));
//            blobList.add(blob);
//            blobMap.put(page, blob);
//            page += len;
//        }
//    }
//
//    private void verifyLeafFreeTable(Blob leafBlob, int trunkSlot)
//    {
//        int minLen = (trunkSlot * 512) + 1;
//        int maxLen = minLen + 511;
//
//        ByteBuffer buf = bufferOfPage(leafBlob.firstPage);
//        int p = offsetOfPage(leafBlob.firstPage);
//        long ofs = absPosOfPage(leafBlob.firstPage);
//
//        if((leafBlob.flags & FREE_BLOB_FLAG) == 0)
//        {
//            error(ofs, "Leaf FT must be in a free blob");
//            return;
//        }
//
//        if(leafBlob.pages < minLen || leafBlob.pages > maxLen)
//        {
//            error(ofs, String.format("Free blob is %d pages, but contains " +
//                "leaf FT for range %d - %d", leafBlob.pages, minLen, maxLen));
//            return;
//        }
//
//        int leafRanges = buf.getInt(p + LEAF_FT_RANGE_BITS_OFS);
//        int tableStart = p + LEAF_FREE_TABLE_OFS;
//        p = tableStart;
//        for(int range=0; range<32; range++)
//        {
//            int rangeBit = leafRanges & (1 << range);
//            boolean slotsUsed = false;
//            for(int i=0; i<16; i++)
//            {
//                int freePage = buf.getInt(p);
//                if(freePage != 0)
//                {
//                    Blob freeBlob = blobMap.get(freePage);
//                    if(freeBlob == null)
//                    {
//                        error(ofs + p - tableStart + LEAF_FREE_TABLE_OFS,
//                            "Leaf FT slot references invalid page: %d", freePage);
//                    }
//                    else
//                    {
//                        slotsUsed = true;
//                        verifyFreeBlobChain(freeBlob, trunkSlot, (p - tableStart) / 4);
//                    }
//                }
//                p += 4;
//            }
//            if(rangeBit == 0 && slotsUsed)
//            {
//                error(ofs + LEAF_FT_RANGE_BITS_OFS, "Leaf range %d contains " +
//                    "free blobs, but range bit is not set", range);
//            }
//            else if(rangeBit != 0 && !slotsUsed)
//            {
//                error(ofs + LEAF_FT_RANGE_BITS_OFS, "Leaf range %d has " +
//                    "range bit set, but contains no free blobs", range);
//            }
//        }
//    }
//
//    private void verifyFreeBlobChain(Blob freeBlob, int trunkSlot, int leafSlot)
//    {
//        int len = trunkSlot * 512 + leafSlot + 1;
//        int prevFreePage = 0;
//
//        for(;;)
//        {
//            long ofs = absPosOfPage(freeBlob.firstPage);
//            if((freeBlob.flags & FREE_BLOB_FLAG) == 0)
//            {
//                error(ofs, "Allocated blob listed in free blob list for size %d", len);
//            }
//
//            if(len != freeBlob.pages)
//            {
//                error(ofs, String.format("Blob with %d pages listed in freetable for page size %d",
//                    freeBlob.pages, len));
//            }
//            freeBlob.flags |= BLOB_REFERENCED_FLAG;
//            ByteBuffer buf = bufferOfPage(freeBlob.firstPage);
//            int p = offsetOfPage(freeBlob.firstPage);
//            int prev = buf.getInt(p + PREV_FREE_BLOB_OFS);
//            int next = buf.getInt(p + NEXT_FREE_BLOB_OFS);
//
//            if(prev != prevFreePage)
//            {
//                error(ofs + PREV_FREE_BLOB_OFS, String.format(
//                    "prev_free_blob should be %d, not %d", prevFreePage, prev));
//            }
//            if(next == 0) return;
//            prevFreePage = freeBlob.firstPage;
//            freeBlob = blobMap.get(next);
//            if(freeBlob == null)
//            {
//                error(ofs + NEXT_FREE_BLOB_OFS,
//                    "next_free_blob references invalid page %d", next);
//                return;
//            }
//            if((freeBlob.flags & BLOB_REFERENCED_FLAG) != 0)
//            {
//                error(ofs + NEXT_FREE_BLOB_OFS,
//                    "Circular reference in free blob list");
//                return;
//            }
//        }
//    }
//
//    private void verifyFreeBlobs()
//    {
//        Blob prevBlob = null;
//        for(Blob blob: blobList)
//        {
//            long ofs = absPosOfPage(blob.firstPage);
//            boolean atSegmentStart = (blob.firstPage % pagesPerSegment == 0);
//            boolean prevBlobFree = false;
//            if(prevBlob != null)
//            {
//                prevBlobFree = (prevBlob.flags & FREE_BLOB_FLAG) != 0;
//            }
//
//            if((blob.flags & PRECEDING_BLOB_FREE_FLAG) != 0)
//            {
//                if(!prevBlobFree)
//                {
//                    error(ofs, "preceding_blob_free flag set, but no preceding free blob");
//                }
//            }
//            else
//            {
//                if(prevBlobFree)
//                {
//                    error(ofs, "Preceding blob is free, but preceding_blob_free flag not set");
//                }
//            }
//
//            if((blob.flags & FREE_BLOB_FLAG) != 0)
//            {
//                if((blob.flags & BLOB_REFERENCED_FLAG) == 0)
//                {
//                    error(ofs, "Free blob %d is not in any free list", blob.firstPage);
//                }
//                if(prevBlobFree && !atSegmentStart)
//                {
//                    error(ofs, "Free blob %d should have been coalesced into preceding free blob", blob.firstPage);
//                }
//
//                int lastPage = blob.firstPage + blob.pages - 1;
//                ByteBuffer buf = bufferOfPage(lastPage);
//                int p = offsetOfPage(lastPage);
//                int tailLen = buf.getInt(p + 4092);
//                if(tailLen != blob.pages)
//                {
//                    error(ofs + blob.pages * pageSize - 4, String.format(
//                        "Free blob %d: Invalid tail length %d, should be %d",
//                        blob.firstPage, tailLen, blob.pages));
//                }
//            }
//
//            prevBlob = blob;
//        }
//    }
//
//    private void verifyFreeTables()
//    {
//        ByteBuffer buf = bufferOfPage(0);
//        int trunkRanges = buf.getInt(TRUNK_FT_RANGE_BITS_OFS);
//
//        int p = TRUNK_FREE_TABLE_OFS;
//        for(int range=0; range<32; range++)
//        {
//            int rangeBit = trunkRanges & (1 << range);
//            boolean slotsUsed = false;
//            for(int i=0; i<16; i++)
//            {
//                int leafPage = buf.getInt(p);
//                if(leafPage != 0)
//                {
//                    Blob leafBlob = blobMap.get(leafPage);
//                    if(leafBlob == null)
//                    {
//                        error(p, "Trunk FT slot references invalid page: %d", leafPage);
//                    }
//                    else
//                    {
//                        slotsUsed = true;
//                        verifyLeafFreeTable(leafBlob, (p - TRUNK_FREE_TABLE_OFS) / 4);
//                    }
//                }
//                p += 4;
//            }
//            if(rangeBit == 0 && slotsUsed)
//            {
//                error(TRUNK_FT_RANGE_BITS_OFS, "Trunk range %d contains " +
//                    "leaf FT blobs, but range bit is not set", range);
//            }
//            else if(rangeBit != 0 && !slotsUsed)
//            {
//                error(TRUNK_FT_RANGE_BITS_OFS, "Trunk range %d has " +
//                    "range bit set, but contains no leaf FT blobs", range);
//            }
//        }
//    }
//
//    private void dumpBlobs()
//    {
//        int used=0;
//        int free=0;
//
//        for(Blob blob: blobList)
//        {
//            // System.out.format("%8d %8d %s%s\n",
//            Log.debug("%8d %8d %s%s",
//                blob.firstPage,
//                blob.pages,
//                blob.isFree() ? "free" : "used",
//                blob.isReferenced() ? " * " : "   ");
//            if(blob.isFree())
//            {
//                free++;
//            }
//            else
//            {
//                used++;
//            }
//        }
//        Log.debug("%d blocks (%d used, %d free)", used+free, used, free);
//    }
//
//    @Override public boolean verify()
//    {
//        blobList.clear();
//        blobMap.clear();
//        doVerify();
//        dumpBlobs();
//        return valid;
//    }
//
//    protected void doVerify()
//    {
//        ByteBuffer buf = store.getMapping(0);
//        totalPages = buf.getInt(TOTAL_PAGES_OFS);
//        metadataSize = buf.getInt(METADATA_SIZE_OFS);
//        pageSizeShift = buf.get(PAGE_SIZE_OFS) + 12;
//        pageSize = 1 << pageSizeShift;
//        pagesPerSegment = (1 << 30) / pageSize;
//
//        if(metadataSize < 32 || metadataSize > (1 << 30))
//        {
//            error(METADATA_SIZE_OFS, "Invalid metadata size: ", metadataSize);
//            return;
//        }
//
//        verifyStructure();
//        if(!valid) return;
//        verifyFreeTables();
//        verifyFreeBlobs();
//    }
//}
