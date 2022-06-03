package com.clarisma.common.store;

import com.clarisma.common.util.Log;
import com.geodesk.feature.FeatureLibrary;
import org.eclipse.collections.api.list.primitive.MutableIntList;
import org.eclipse.collections.api.map.primitive.MutableIntObjectMap;
import org.eclipse.collections.api.set.primitive.MutableIntSet;
import org.eclipse.collections.impl.list.mutable.primitive.IntArrayList;
import org.eclipse.collections.impl.map.mutable.primitive.IntObjectHashMap;
import org.eclipse.collections.impl.set.mutable.primitive.IntHashSet;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.clarisma.common.store.BlobStoreConstants.*;

// TODO: Always apply journal before checking!

public class BlobStoreChecker
{
    protected final BlobStore store;

    private long fileSize;
    private int totalPages;
    private int pageSizeShift;
    private int pageSize;
    private int pagesPerSegment;
    private int metadataSize;
    private final MutableIntObjectMap<Blob> blobs = new IntObjectHashMap<>();
    private boolean valid = true;
    private final List<Error> errors = new ArrayList<>();

    protected static final int BLOB_REFERENCED_FLAG = 1;
    protected static final int FREE_BLOB_REFERENCED_FLAG = 2;
    protected static final int VALID_FREE_BLOB_SIZE = 4;
    protected static final int VALID_FREE_BLOB_TRAILER = 8;

    public BlobStoreChecker(BlobStore store)
    {
        this.store = store;
        try
        {
            fileSize = store.currentFileSize();
        }
        catch(IOException ex)
        {
            error(0, ex.getMessage());
        }
        ByteBuffer buf = store.getMapping(0);
        totalPages = buf.getInt(TOTAL_PAGES_OFS);
        metadataSize = buf.getInt(METADATA_SIZE_OFS);
        pageSizeShift = 12; // buf.get(PAGE_SIZE_OFS); //  + 12; TODO: might change
        // TODO: fix
        pageSize = 1 << pageSizeShift;
        pagesPerSegment = (1 << 30) / pageSize;
    }

    public void check()
    {
        // TODO: check page size

        checkMetadata();
        checkFreeTables();
        checkIndex();
        checkBlobs();
    }

    protected void checkMetadata()
    {
        if(metadataSize < DEFAULT_METADATA_SIZE || metadataSize > (1 << 30))
        {
            error(METADATA_SIZE_OFS, "Invalid metadata size: %d", metadataSize);
            return;
        }
    }


    protected void checkIndex()
    {
        // TODO: abstract?
    }

    protected void checkBlobs()
    {
        List<Blob> blobList = new ArrayList<>(blobs.values());
        Collections.sort(blobList);
        int metadataPages = (metadataSize + pageSize - 1) / pageSize;
        int nextPage = metadataPages;
        Blob prevBlob = null;
        for(Blob blob: blobList)
        {
            boolean gapsOrOverlaps = false;
            if(blob.firstPage != nextPage)
            {
                if(blob.firstPage < metadataPages)
                {
                    error(blob, " overlaps metadata", blob.firstPage);
                    gapsOrOverlaps = true;
                }
                else if(blob.firstPage < nextPage)
                {
                    error(blob, " overlaps Blob %d", prevBlob.firstPage);
                    gapsOrOverlaps = true;
                }
                else
                {
                    /*
                    error(prevBlob, " (%d pages) Followed by unreferenced data",
                        prevBlob.pages);
                    */
                    checkUnreferenced(nextPage, blob.firstPage);
                    gapsOrOverlaps = true;
                }
            }
            if(blob.isFree())
            {
                if (blob.hasFlags(BLOB_REFERENCED_FLAG))
                {
                    error(blob, " is marked free but is still in use");
                }
                else
                {
                    // bona-fide free blob
                    if(!blob.hasFlags(VALID_FREE_BLOB_SIZE))
                    {
                        error(blob, ": Invalid size for free blob");
                    }
                    if(!blob.hasFlags(VALID_FREE_BLOB_TRAILER))
                    {
                        error(blob, ": Invalid free-blob trailer");
                    }
                }
            }
            boolean blobStartsAtSegment = absPosOfPage(blob.firstPage) % (1 << 30) == 0;
            if(prevBlob != null && !gapsOrOverlaps)
            {
                if(prevBlob.isFree())
                {
                    if(blob.isFree() && !blobStartsAtSegment)
                    {
                        error(blob, " should have been consolidated with " +
                            "previous free blob");
                    }
                    if(!blob.hasFlags(PRECEDING_BLOB_FREE_FLAG))
                    {
                        error(blob, ": Preceding blob is free, " +
                            "but prev_blob_free flag not set");
                    }
                }
                else
                {
                    if(blob.hasFlags(PRECEDING_BLOB_FREE_FLAG))
                    {
                        error(blob, ": Preceding blob in use, " +
                            "but prev_blob_free flag set");
                    }
                }
            }
            nextPage = blob.firstPage + blob.pages;
            prevBlob = blob;
        }

        if(nextPage == totalPages) return;
        if(nextPage > totalPages)
        {
            error(TOTAL_PAGES_OFS, "total_pages should be %d instead of %d",
                nextPage, totalPages);
            return;
        }
        checkUnreferenced(nextPage, totalPages);
    }

    private void checkUnreferenced(int start, int end)
    {
        // TODO: temporarily turned off to focus on general integrity

        error(absPosOfPage(start), "%d page%s of unreferenced data",
            end-start, end-start == 1 ? "" : "s");
        // TODO: try to recover these blobs
    }

    /*
    protected void checkMetadata()
    {
        int metadataPages = (metadataSize + pageSize - 1) / pageSize;
        if(metadataPages > totalPages)
        {
            error(TOTAL_PAGES_OFS,
                "Total pages must be at least %d to accommodate metadata", metadataPages);
        }
    }
     */

    // We wrap these BlobStore methods to give us more flexibility later
    // in addressing corrupt page size settings

    protected ByteBuffer bufferOfPage(int page)
    {
        return store.getMapping(page / pagesPerSegment);
    }

    protected int offsetOfPage(int page)
    {
        return (page % pagesPerSegment) * pageSize;
    }

    protected long absPosOfPage(int page)
    {
        return (long)page * pageSize;
    }

    private void checkFreeTables()
    {
        ByteBuffer buf = bufferOfPage(0);
        int rangesUsed = 0;
        int p = TRUNK_FREE_TABLE_OFS;
        for(int slot=0; slot<512; slot++, p += 4)
        {
            int freePage = buf.getInt(p);
            if(freePage == 0) continue;
            rangesUsed |= 1 << (slot >>> 4);
            Blob freeBlob = getValidBlob(p, freePage);
            if(freeBlob == null) continue;
            if(!checkBlobIsFree(freeBlob)) continue;
            if(!freeBlob.hasFlags(VALID_FREE_BLOB_SIZE | VALID_FREE_BLOB_TRAILER)) continue;
            checkLeafFreeTable(slot, freeBlob);
        }
        checkExpectedVsActual(TRUNK_FT_RANGE_BITS_OFS, "trunk_free_range_mask",
            rangesUsed, buf.getInt(TRUNK_FT_RANGE_BITS_OFS));
    }

    private void checkExpectedVsActual(long ofs, String what, int expected, int actual)
    {
        if(expected != actual)
        {
            error(ofs, "%s should be %08X instead of %08X", what, expected, actual);
        }
    }

    private boolean checkBlobIsFree(Blob blob)
    {
        if(!blob.hasFlags(FREE_BLOB_FLAG))
        {
            error(blob, " should be free");
            return false;
        }
        return true;
    }

    private void checkLeafFreeTable(int trunkSlot, Blob blob)
    {
        int minPages = (trunkSlot * 512) + 1;
        int maxPages = minPages + 511;

        int page = blob.firstPage;
        long ofs = absPosOfPage(page);
        if(blob.pages < minPages || blob.pages > maxPages)
        {
            error(ofs, "Free blob with %d pages in wrong size range (%d to %d)",
                blob.pages, minPages, maxPages);
            return;
        }

        ByteBuffer buf = bufferOfPage(page);
        int p = offsetOfPage(page);
        int rangeMask = buf.getInt(p + LEAF_FT_RANGE_BITS_OFS);
        int rangesUsed = 0;
        p += LEAF_FREE_TABLE_OFS;
        for(int leafSlot=0; leafSlot<512; leafSlot++, p += 4)
        {
            int freePage = buf.getInt(p);
            if(freePage == 0) continue;
            rangesUsed |= 1 << (leafSlot >>> 4);
            Blob freeBlob = getValidBlob(ofs + p, freePage);
            if(freeBlob != null) checkFreeBlobChain(trunkSlot, leafSlot, freeBlob);
        }

        checkExpectedVsActual(ofs+LEAF_FREE_TABLE_OFS, "leaf_free_range_mask",
            rangesUsed, rangeMask);

        if(rangesUsed == 0) error(blob, "Leaf free-table must have at least one entry");
    }

    private void checkFreeBlobChain(int trunkSlot, int leafSlot, Blob blob)
    {
        int len = trunkSlot * 512 + leafSlot + 1;
        int prevFreePage = 0;

        for(;;)
        {
            blob.flags |= FREE_BLOB_REFERENCED_FLAG;
            if(!checkBlobIsFree(blob)) return;
            long ofs = absPosOfPage(blob.firstPage);
            if(blob.pages != len)
            {
                error(ofs, "Blob with %d pages listed in freetable for page size %d",
                    blob.pages, len);
            }
            ByteBuffer buf = bufferOfPage(blob.firstPage);
            int p = offsetOfPage(blob.firstPage);
            int pPrev = p + PREV_FREE_BLOB_OFS;
            int pNext = p + NEXT_FREE_BLOB_OFS;
            long ofsPrev = ofs + pPrev;
            long ofsNext = ofs + pNext;
            int prev = buf.getInt(pPrev);
            int next = buf.getInt(pNext);

            if(prev != prevFreePage)
            {
                error(ofsPrev, "prev_free_blob should be %d, not %d", prevFreePage, prev);
            }
            if(next == 0) return;
            prevFreePage = blob.firstPage;
            blob = getValidBlob(ofsNext, next);
            if(blob == null) return;
            if(!checkBlobIsFree(blob)) return;
            if(blob.hasFlags(FREE_BLOB_REFERENCED_FLAG))
            {
                error(ofsNext, "Circular reference in free-blob list (to %d)", next);
                return;
            }
        }
    }

    private static class Error implements Comparable<Error>
    {
        final long location;
        final String message;

        Error(long location, String message)
        {
            this.location = location;
            this.message = message;
        }

        @Override public int compareTo(Error other)
        {
            return Long.compare(this.location, other.location);
        }

        public String toString()
        {
            return String.format("%08X: %s", location, message);
        }
    }

    protected void error(long ofs, String msg, Object... args)
    {
        errors.add(new Error(ofs, String.format(msg, args)));
    }

    protected void error(Blob blob, String msg, Object... args)
    {
        errors.add(new Error(absPosOfPage(blob.firstPage),
            String.format("Blob " + blob.firstPage + msg, args)));
    }


    protected static class Blob implements Comparable<Blob>
    {
        int firstPage;
        int pages;
        int flags;

        Blob(int firstPage, int pages, int flags)
        {
            this.firstPage = firstPage;
            this.pages = pages;
            this.flags = flags;
        }

        boolean isReferenced()
        {
            return (flags & BLOB_REFERENCED_FLAG) != 0;
        }

        boolean isFree()
        {
            return (flags & FREE_BLOB_FLAG) != 0;
        }

        boolean hasFlags(int flags)
        {
            return (this.flags & flags) == flags;
        }

        @Override public int compareTo(Blob other)
        {
            return Integer.compare(this.firstPage, other.firstPage);
        }
    }

    private Blob getValidBlob(long ofs, int page)
    {
        Blob blob = getBlob(page);
        if(blob == null) error(ofs, "Bad blob reference: %d", page);
        return blob;
    }

    public Blob useBlob(long ofs, int page)
    {
        Blob blob = getValidBlob(ofs, page);
        if(blob != null) blob.flags |= BLOB_REFERENCED_FLAG;
        return blob;
    }

    private Blob getBlob(int page)
    {
        Blob blob = blobs.get(page);
        if(blob != null) return blob;
        if(page <= 0) return null;
        if((page << pageSizeShift) >= fileSize) return null;
        ByteBuffer buf = bufferOfPage(page);
        int p = offsetOfPage(page);
        int header = buf.getInt(p);
        int len = header & PAYLOAD_SIZE_MASK;
        int flags = header & ~PAYLOAD_SIZE_MASK;
        if(len == 0 || len > (1 << 30) - 4)     // can't be negative because we trimmed top 2 bits
        {
            error(absPosOfPage(page),
                "Blob at page %d has illegal payload length (%d)", page, len);
            return null;
        }
        int lenPages = (len + pageSize + 3) / pageSize;
        if((header & FREE_BLOB_FLAG) != 0)
        {
            if ((len + 4) % pageSize == 0) flags |= VALID_FREE_BLOB_SIZE;
            int lastPage = page + lenPages - 1;
            ByteBuffer lastBuf = bufferOfPage(lastPage);
            int pTrailer = offsetOfPage(lastPage) + pageSize - 4;
            if (buf.getInt(pTrailer) == lenPages) flags |= VALID_FREE_BLOB_TRAILER;
        }
        blob = new Blob(page, lenPages, flags);
        blobs.put(page, blob);
        return blob;
    }

    public void reportErrors(PrintStream out)
    {
        Collections.sort(errors);
        for(Error error: errors) out.println(error);
    }

    public boolean hasErrors()
    {
        return !errors.isEmpty();
    }
}
