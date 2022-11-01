package com.clarisma.common.store;

import org.eclipse.collections.api.iterator.IntIterator;

import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.UUID;
import java.util.zip.GZIPOutputStream;

import static com.clarisma.common.store.BlobStoreConstants.*;

/**
 * A Blob Store is a file containing a large numbers of individual binary
 * objects (blobs) that span multiple contiguous file pages. Page size is
 * configurable and must be a power-of-2 multiple of 4 KB.
 *
 * Blobs are identified by their starting page (a 32-bit integer).
 * The maximum size of a Blob Store is dependent on its page size;
 * at the 4 KB default, the file can grow to 16 TB.
 *
 * The maximum size of each blob (including its 4-byte header) is 1 GB
 * (regardless of page size). Assuming 4 KB pages, a blob can contain up
 * to 256K pages.
 *
 * There is no restriction on the structure and type of content of the blobs,
 * except for the following: The first 4 bytes contain the size of the blob
 * and two marker bits. The user must not modify this header. Apart from the
 * blobs, the Blob Store file contains metadata that maintains allocation
 * statistics and free lists. An additional user-defined metadata section
 * can be used to store an index.
 *
 * Blob Stores allow concurrent access by multiple processes, with some
 * restrictions. Write access is mediated through the use of locks.
 * A process may add blobs and alter metadata while other processes are
 * reading, but deletion or modification of blobs requires an exclusive lock.
 *
 * Blob Stores use the journaling mechanism of the Store baseclass to track
 * modifications, in order to prevent data corruption due to abnormal process
 * termination (such as power loss). Using the Journal, a Blob Store can
 * restore itself to a consistent state by either applying the failed
 * modifications, or rolling them back.
 */

public class BlobStore extends Store
{
    /**
     * The number of bits to shift left to turn number of pages into number of bytes (Page size is always a power of
     * two).
     */
    protected int pageSizeShift = 12; // 4KB default page
    protected Downloader downloader;

    protected int downloadBlob(URL url)
    {
        return 0; // TODO
    }

    public void setRepository(String url)
    {
        assert downloader == null;
        downloader = new Downloader(this, url);
    }

    /*
    protected long createFingerprint()
    {
        // A belt-and-suspender approach in case the pseudo-random
        // seed is flawed (maybe we should use a proper GUID?)

        long currentTime = System.currentTimeMillis();
        Random random = new Random();
        return currentTime ^ random.nextLong();
    }
     */

    @Override protected void createStore()
    {
        // TODO: should this be inside a transaction?
        ByteBuffer buf = baseMapping;
        buf.putInt(0, MAGIC);
        buf.putInt(VERSION_OFS, VERSION);
        buf.putLong(TIMESTAMP_OFS, System.currentTimeMillis());
        buf.putInt(METADATA_SIZE_OFS, DEFAULT_METADATA_SIZE);
        buf.putInt(TOTAL_PAGES_OFS, 1);
        // TODO: page size
    }

    @Override protected long getTimestamp()
    {
        return baseMapping.getLong(TIMESTAMP_OFS);
    }

    public UUID getGuid()
    {
        long lower64 = baseMapping.getLong(GUID_OFS);
        long upper64 = baseMapping.getLong(GUID_OFS+8);
        return new UUID(upper64, lower64); // notice order: (high, low)
    }

    @Override protected void verifyHeader()
    {
        ByteBuffer buf = baseMapping;
        if (buf.getInt(0) != MAGIC)
        {
            throw new StoreException("Not a BlobStore file", path());
        }
        // TODO: page size
        // debugCheckRootFT();
    }

    /**
     * Checks whether this BlobStore is *empty*. An empty store is valid,
     * but has no contents.
     *
     * @return
     */
    protected boolean isEmpty()
    {
        return baseMapping.getInt(INDEX_PTR_OFS) == 0;      // TODO
    }

    @Override protected void initialize() throws IOException
    {
        super.initialize();
        if(isEmpty() && downloader != null)
        {
            try
            {
                Downloader.Ticket ticket = downloader.request(Downloader.METADATA_ID, null);
                ticket.awaitCompletion();
                ticket.throwError();
            }
            catch(InterruptedException ex)
            {
                // do nothing
            }
        }
    }

    @Override protected long getTrueSize()
    {
        // assert !isInTransaction();
        // Can also be called from commit()
        return ((long) baseMapping.getInt(TOTAL_PAGES_OFS)) << pageSizeShift;
    }

    // TODO: naming
    public ByteBuffer baseMapping()
    {
        return baseMapping;
    }

    public ByteBuffer bufferOfPage(int page)
    {
        return getMapping(page >> (30 - pageSizeShift));
    }

    public int offsetOfPage(int page)
    {
        return (page << pageSizeShift) & 0x3fff_ffff;
    }

    public long absoluteOffsetOfPage(int page)
    {
        return ((long)page) << pageSizeShift;
    }

    protected ByteBuffer getBlockOfPage(int page)
    {
        assert page >= 0;       // TODO: treat page as unsigned int?
        return getBlock(((long) page) << pageSizeShift);
        // make sure to cast to long before shifting
    }

    public int pageSize()
    {
        return 1 << pageSizeShift;
    }

    public int indexPointer()
    {
        return baseMapping.getInt(INDEX_PTR_OFS) + INDEX_PTR_OFS;
    }
        // TODO: make absolute

    // TODO: should also make sure page does not lie in meta space
    protected void checkPage(int page)
    {
        if (page < 0 || page >= baseMapping.getInt(TOTAL_PAGES_OFS))
        {
            throw new StoreException("Invalid page: " + page, path());
        }
    }

    /**
     * Determines the number of pages needed to store a blob.
     *
     * @param size the size (excluding 4-byte header) of the blob
     * @return the number of pages needed to store the blob
     */
    protected int pagesForPayloadSize(int size)
    {
        assert size > 0 && size <= ((1 << 30) - 4);
        return (size + (1 << pageSizeShift) + 3) >> pageSizeShift;
    }

    /**
     * Determines the number of pages needed to store the given number
     * of bytes. (The range of bytes is assumed to start at the beginning
     * of the first page.)
     *
     * @param len  the number of bytes
     * @return the number of pages needed to store the bytes
     */
    protected int bytesToPages(int len)
    {
        assert len > 0 && len <= (1 << 30);
        return (len + (1 << pageSizeShift) - 1) >> pageSizeShift;
    }

    /**
     * Allocates a blob of a given size. If possible, the smallest existing free blob that can accommodate the requested
     * number of bytes will be reused; otherwise, a new blob will be appended to the store file.
     *
     * TODO: guard against exceeding maximum file size
     *
     * @param size the size of the blob, not including its 4-byte header
     * @return the first page of the blob
     */
    protected int allocateBlob(int size)
    {
        // debugCheckRootFT();
        assert size >= 0 && size <= ((1 << 30) - 4); // TODO: >0 ?
        int precedingBlobFreeFlag = 0;
        int requiredPages = pagesForPayloadSize(size);
        ByteBuffer rootBlock = getBlock(0);
        int trunkRanges = rootBlock.getInt(TRUNK_FT_RANGE_BITS_OFS);
        if (trunkRanges != 0)
        {
            // If there are free blobs, check if there is one large enough
            // to accommodate the requested size

            // The first slot in the trunk FT worth checking
            int trunkSlot = (requiredPages - 1) / 512;

            // The first slot in the leaf FT to check (for subsequent
            // leaf FTs, we need to start at 0
            int leafSlot = (requiredPages - 1) % 512;
            int trunkOfs = TRUNK_FREE_TABLE_OFS + trunkSlot * 4;
            int trunkEnd = (trunkOfs & 0xffff_ffc0) + 64;

            // We don't care about ranges that cover page counts that are less
            // than the number of pages we require, so shift off those bits
            trunkRanges >>>= trunkSlot / 16;

            for (; ; )
            {
                if ((trunkRanges & 1) == 0)
                {
                    // There are no free blobs in the target range, so let's
                    // check free blobs in the next-larger range; if there
                    // aren't any, we quit

                    if (trunkRanges == 0) break;

                    // If a range does not contain any free blobs, no need
                    // to check each of its 16 entries individually. The number
                    // of zero bits tells us how many ranges we can skip

                    int rangesToSkip = Integer.numberOfTrailingZeros(trunkRanges);
                    trunkEnd += rangesToSkip * 64;
                    trunkOfs = trunkEnd - 64;

                    // for any range other than the first, we check all leaf slots

                    leafSlot = 0;
                }
                assert trunkOfs < TRUNK_FREE_TABLE_OFS + FREE_TABLE_LEN;

                for (; trunkOfs < trunkEnd; trunkOfs += 4)
                {
                    int leafTableBlob = rootBlock.getInt(trunkOfs);
                    if (leafTableBlob == 0) continue;

                    // There are one or more free blobs within the
                    // 512-page size range indicated by trunkOfs

                    ByteBuffer leafBlock = getBlockOfPage(leafTableBlob);
                    int leafRanges = leafBlock.getInt(LEAF_FT_RANGE_BITS_OFS);
                    int leafOfs = LEAF_FREE_TABLE_OFS + leafSlot * 4;
                    int leafEnd = (leafOfs & 0xffff_ffc0) + 64;

                    assert (leafBlock.getInt(0) & FREE_BLOB_FLAG) != 0 :
                        String.format("Leaf FB blob %d must be a free blob",
                            leafTableBlob);

                    leafRanges >>>= leafSlot / 16;

                    for (; ; )
                    {
                        if ((leafRanges & 1) == 0)
                        {
                            if (leafRanges == 0) break;
                            int rangesToSkip = Integer.numberOfTrailingZeros(leafRanges);
                            leafEnd += rangesToSkip * 64;
                            leafOfs = leafEnd - 64;
                        }
                        for (; leafOfs < leafEnd; leafOfs += 4)
                        {
                            int freeBlob = leafBlock.getInt(leafOfs);
                            if (freeBlob == 0) continue;

                            // Found a free blob of sufficient size

                            int freePages = ((trunkOfs - TRUNK_FREE_TABLE_OFS) << 7) +
                                ((leafOfs - LEAF_FREE_TABLE_OFS) >> 2) + 1;
                            if (freeBlob == leafTableBlob)
                            {
                                // If the free blob is the same blob that holds
                                // the leaf free-table, check if there is another
                                // free blob of the same size

                                // log.debug("    Candidate free blob {} holds leaf FT", freeBlob);

                                int nextFreeBlob = leafBlock.getInt(NEXT_FREE_BLOB_OFS);
                                if (nextFreeBlob != 0)
                                {
                                    // If so, we'll use that blob instead

                                    // log.debug("    Using next free blob at {}", nextFreeBlob);
                                    freeBlob = nextFreeBlob;
                                }
                            }

                            // TODO!!!!!
                            // TODO: bug: we need to relocate ft after we
                            //  add the remaining part
                            //  free blob is last of size, remaining is in same leaf FT
                            //  won't move the FT, FT ends up in allocated portion
                            //  OR: remove entire blob first,then add remaining?
                            //  Solution: reverse sequence: remove whole blob first,
                            //  then add back the remaining part

                            ByteBuffer freeBlock = getBlockOfPage(freeBlob);
                            int header = freeBlock.getInt(0);
                            assert (header & FREE_BLOB_FLAG) != 0 :
                                String.format("Blob %d is not free", freeBlob);
                            int freeBlobPayloadSize = header & PAYLOAD_SIZE_MASK;
                            assert (freeBlobPayloadSize + 4) >> pageSizeShift == freePages :
                                String.format("Blob %d has payload size %d (%d pages), " +
                                        "but found in free-list for %d pages ",
                                    freeBlob, freeBlobPayloadSize,
                                    (freeBlobPayloadSize + 4) >> pageSizeShift, freePages);
                            assert freePages >= requiredPages;

                            precedingBlobFreeFlag = header & PRECEDING_BLOB_FREE_FLAG;
                            removeFreeBlob(freeBlock);

                            if (freeBlob == leafTableBlob)
                            {
                                // We need to move the freetable to another free blob
                                // (If it is no longer needed, this is a no-op;
                                // removeFreeBlob has already set the trunk slot to 0)
                                // TODO: consolidate with removeFreeBlob?
                                //  We only separate this step because in freeBlob
                                //  we are potentially removing preceding/following
                                //  blob of same size range, which means we'd have
                                //  to move FT twice

                                int newLeafBlob = relocateFreeTable(freeBlob, freePages);
                                if (newLeafBlob != 0)
                                {
                                    // log.debug("    Moved leaf FT to {}", newLeafBlob);
                                    assert rootBlock.getInt(trunkOfs) == newLeafBlob;
                                }
                                else
                                {
                                    // log.debug("    Leaf FT no longer needed");
                                    assert rootBlock.getInt(trunkOfs) == 0;
                                }
                            }

                            if (freePages > requiredPages)
                            {
                                // If the free blob is larger than needed, mark the
                                // remainder as free and add it to its respective free list;
                                // we always do this before we remove the reused blob, since
                                // we may needlessly remove and reallocate the free table
                                // if the reused is the last blob in the table, but the
                                // remainder is in the same 512-page range

                                // We won't need to touch the preceding-free flag of the
                                // successor blob, since it is already set

                                addFreeBlob(freeBlob + requiredPages, freePages - requiredPages, 0);
                            }
                            else
                            {
                                // Perfect fit: clear the preceding-free flag of
                                // the successor blob

                                // log.debug("      Perfect fit");

                                ByteBuffer nextBlock = getBlockOfPage(freeBlob + freePages);
                                int nextSizeAndFlags = nextBlock.getInt(0);
                                nextBlock.putInt(0, nextSizeAndFlags & ~PRECEDING_BLOB_FREE_FLAG);
                            }

                            freeBlock.putInt(0, size | precedingBlobFreeFlag);
                            // debugCheckRootFT();
                            return freeBlob;
                        }
                        leafRanges >>>= 1;
                        leafEnd += 64;
                    }
                    leafSlot = 0;
                }

                // Check the next range

                trunkRanges >>>= 1;
                trunkEnd += 64;
            }
        }

        // If we weren't able to find a suitable free blob,
        // we'll grow the store

        int totalPages = rootBlock.getInt(TOTAL_PAGES_OFS);
        int pagesPerSegment = (1 << 30) >> pageSizeShift;
        int remainingPages = pagesPerSegment - (totalPages & (pagesPerSegment - 1));
        if (remainingPages < requiredPages)
        {
            // If the blob won't fit into the current segment, we'll
            // mark the remaining space as a free blob, and allocate
            // the blob in a fresh segment

            addFreeBlob(totalPages, remainingPages, 0);
            totalPages += remainingPages;

            // In this case, we'll need to set the preceding-free flag of the
            // allocated blob

            precedingBlobFreeFlag = PRECEDING_BLOB_FREE_FLAG;
        }
        rootBlock.putInt(TOTAL_PAGES_OFS, totalPages + requiredPages);

        // TODO: no need to journal the blob's header block if it is in
        //  virgin space
        //  But: need to mark the segment as dirty, so it can be forced
        ByteBuffer newBlock = getBlockOfPage(totalPages);
        newBlock.putInt(0, size | precedingBlobFreeFlag);
        // debugCheckRootFT();
        return totalPages;
    }

    private boolean isFirstPageOfSegment(int page)
    {
        return (page & ((0x3fff_ffff) >> pageSizeShift)) == 0;
    }

    private static int getFreeTableBlob(ByteBuffer rootBlock, int pages)
    {
        int trunkSlot = (pages - 1) / 512;
        return rootBlock.getInt(TRUNK_FREE_TABLE_OFS + trunkSlot * 4);
    }

    /**
     * Deallocates a blob. Any adjacent free blobs are coalesced, provided that they are located in the same 1-GB
     * segment.
     *
     * @param firstPage the first page of the blob
     */
    protected void freeBlob(int firstPage)
    {
        // debugCheckRootFT();
        ByteBuffer rootBlock = getBlock(0);
        ByteBuffer block = getBlockOfPage(firstPage);
        int sizeAndFlags = block.getInt(0);
        int freeFlag = sizeAndFlags & FREE_BLOB_FLAG;
        int precedingBlobFree = sizeAndFlags & PRECEDING_BLOB_FREE_FLAG;

        if (freeFlag != 0)
        {
            throw new StoreException(
                "Attempt to free blob that is already marked as free", path());
        }

        int totalPages = rootBlock.getInt(TOTAL_PAGES_OFS);

        int pages = pagesForPayloadSize(sizeAndFlags & PAYLOAD_SIZE_MASK);
        int prevBlob = 0;
        int nextBlob = firstPage + pages;
        int prevPages = 0;
        int nextPages = 0;

        if (precedingBlobFree != 0 && !isFirstPageOfSegment(firstPage))
        {
            // If there is another free blob preceding this one,
            // and it is in the same segment, coalesce it

            ByteBuffer prevTailBlock = getBlockOfPage(firstPage - 1);
            prevPages = prevTailBlock.getInt(TRAILER_OFS);
            prevBlob = firstPage - prevPages;
            ByteBuffer prevBlock = getBlockOfPage(prevBlob);

            // The preceding free blob could itself have a preceding free blob
            // (not coalesced because it is located in different segment),
            // so we preserve the preceding_free_flag

            precedingBlobFree = prevBlock.getInt(0) & PRECEDING_BLOB_FREE_FLAG;
            removeFreeBlob(prevBlock);

            // log.debug("    Coalescing preceding blob at {} ({} pages})", prevBlob, prevPages);
        }

        if (nextBlob < totalPages && !isFirstPageOfSegment(nextBlob))
        {
            // There is another blob following this one,
            // and it is in the same segment

            ByteBuffer nextBlock = getBlockOfPage(nextBlob);
            int nextSizeAndFlags = nextBlock.getInt(0);
            if ((nextSizeAndFlags & FREE_BLOB_FLAG) != 0)
            {
                // The next blob is free, coalesce it

                nextPages = pagesForPayloadSize(nextSizeAndFlags & PAYLOAD_SIZE_MASK);
                removeFreeBlob(nextBlock);

                // log.debug("    Coalescing following blob at {} ({} pages})", nextBlob, nextPages);
            }
        }

        if (prevPages != 0)
        {
            int prevFreeTableBlob = getFreeTableBlob(rootBlock, prevPages);
            if (prevFreeTableBlob == prevBlob)
            {
                relocateFreeTable(prevFreeTableBlob, prevPages);
            }
        }
        if (nextPages != 0)
        {
            int nextFreeTableBlob = getFreeTableBlob(rootBlock, nextPages);
            if (nextFreeTableBlob == nextBlob)
            {
                relocateFreeTable(nextFreeTableBlob, nextPages);
            }
        }

        pages += prevPages + nextPages;
        firstPage -= prevPages;

        /*
        if(pages == 262144)
        {
            Log.debug("Freeing 1-GB blob (First page = %d @ %X)...",
                firstPage, (long)firstPage << pageSizeShift);
        }
         */

        if (firstPage + pages == totalPages)
        {
            // If the free blob is located at the end of the file, reduce
            // the total page count (effectively truncating the store)

            totalPages = firstPage;
            while (precedingBlobFree != 0)
            {
                // If the preceding blob is free, that means it
                // resides in the preceding 1-GB segment (since we cannot
                // coalesce across segment boundaries). Remove this blob
                // from its freetable and trim it off. If this blob
                // occupies an entire segment, and its preceding blob is
                // free as well, keep trimming

                // Log.debug("Trimming across segment boundary...");

                ByteBuffer prevTailBlock = getBlockOfPage(totalPages - 1);
                // TODO: this will be wrong for page size > 4KB!
                prevPages = prevTailBlock.getInt(TRAILER_OFS);
                totalPages -= prevPages;
                prevBlob = totalPages;
                ByteBuffer prevBlock = getBlockOfPage(prevBlob);
                removeFreeBlob(prevBlock);

                // Move freetable, if necessary

                int prevFreeTableBlob = getFreeTableBlob(rootBlock, prevPages);
                if (prevFreeTableBlob == prevBlob)
                {
                    // Log.debug("Relocating free table for %d pages", prevPages);
                    relocateFreeTable(prevBlob, prevPages);
                }

                if (!isFirstPageOfSegment(totalPages)) break;
                int prevSizeAndFlags = prevBlock.getInt(0);
                precedingBlobFree = prevSizeAndFlags & PRECEDING_BLOB_FREE_FLAG;
            }
            rootBlock.putInt(TOTAL_PAGES_OFS, totalPages);

            // Log.debug("Truncated store to %d pages.", totalPages);
        }
        else
        {
            // Blob is not at end of file, add it to the free table

            addFreeBlob(firstPage, pages, precedingBlobFree);
            ByteBuffer nextBlock = getBlockOfPage(firstPage + pages);
            int nextSizeAndFlags = nextBlock.getInt(0);
            nextBlock.putInt(0, nextSizeAndFlags | PRECEDING_BLOB_FREE_FLAG);

            // TODO: somewhat inefficient, since we've potentially retrieved flags
        }
        // debugCheckRootFT();
    }

    /**
     * Removes a free blob from its freetable. If this blob is the last free blob in a given size range, removes the
     * leaf freetable from the trunk freetable. If this free blob contains the leaf freetable, and this freetable is
     * still needed, it is the responsibility of the caller to copy it to another free blob in the same size range.
     *
     * This method does not affect the PRECEDING_BLOB_FREE_FLAG of the successor blob; it is the responsibility of the
     * caller to clear the flag, if necessary.
     *
     * @param freeBlock
     */
    private void removeFreeBlob(ByteBuffer freeBlock)
    {
        int prevBlob = freeBlock.getInt(PREV_FREE_BLOB_OFS);
        int nextBlob = freeBlock.getInt(NEXT_FREE_BLOB_OFS);

        // Unlink the blob from its siblings

        if (nextBlob != 0)
        {
            ByteBuffer nextBlock = getBlockOfPage(nextBlob);
            nextBlock.putInt(PREV_FREE_BLOB_OFS, prevBlob);
        }
        if (prevBlob != 0)
        {
            ByteBuffer prevBlock = getBlockOfPage(prevBlob);
            prevBlock.putInt(NEXT_FREE_BLOB_OFS, nextBlob);
            return;
        }

        // Determine the free blob that holds the leaf freetable
        // for free blobs of this size

        int payloadSize = freeBlock.getInt(0) & 0x3fff_ffff;
        assert ((payloadSize + 4) & (0xffff_ffff >>> (32 - pageSizeShift))) == 0 :
            "Payload size + 4 of a free blob must be multiple of page size";
        int pages = (payloadSize + 4) >> pageSizeShift;
        int trunkSlot = (pages - 1) / 512;
        int leafSlot = (pages - 1) % 512;

        // log.debug("     Removing blob with {} pages", pages);

        ByteBuffer rootBlock = getBlock(0);
        int trunkOfs = TRUNK_FREE_TABLE_OFS + trunkSlot * 4;
        int leafOfs = LEAF_FREE_TABLE_OFS + leafSlot * 4;
        int leafBlob = rootBlock.getInt(trunkOfs);

        // If the leaf FT has already been dropped, there's nothing
        // left to do (TODO: this feels messy)
        // (If we don't check, we risk clobbering the root FT)
        // TODO: leafBlob should never be 0!

        assert leafBlob != 0;

        // if(leafBlob == 0) return;

        // Set the next free blob as the first blob of its size

        ByteBuffer leafBlock = getBlockOfPage(leafBlob);
        leafBlock.putInt(leafOfs, nextBlob);
        if (nextBlob != 0) return;

        // Check if there are any other free blobs in the same size range

        int leafRange = leafSlot / 16;
        assert leafRange >= 0 && leafRange < 32;

        leafOfs = LEAF_FREE_TABLE_OFS + (leafRange * 64);
        int leafEnd = leafOfs + 64;
        for (; leafOfs < leafEnd; leafOfs += 4)
        {
            if (leafBlock.getInt(leafOfs) != 0) return;
        }

        // The range has no free blobs, clear its range bit

        int leafRangeBits = leafBlock.getInt(LEAF_FT_RANGE_BITS_OFS);
        leafRangeBits &= ~(1 << leafRange);
        leafBlock.putInt(LEAF_FT_RANGE_BITS_OFS, leafRangeBits);
        if (leafRangeBits != 0) return;

        // No ranges are in use, which means the leaf free table is
        // no longer required

        rootBlock.putInt(trunkOfs, 0);

        // Check if there are any other leaf tables in the same size range

        int trunkRange = trunkSlot / 16;
        assert trunkRange >= 0 && trunkRange < 32;

        trunkOfs = TRUNK_FREE_TABLE_OFS + (trunkRange * 64);
        int trunkEnd = trunkOfs + 64;
        for (; trunkOfs < trunkEnd; trunkOfs += 4)
        {
            if (rootBlock.getInt(trunkOfs) != 0) return;
        }

        // The trunk range has no leaf tables, clear its range bit

        int trunkRangeBits = rootBlock.getInt(TRUNK_FT_RANGE_BITS_OFS);
        trunkRangeBits &= ~(1 << trunkRange);
        rootBlock.putInt(TRUNK_FT_RANGE_BITS_OFS, trunkRangeBits);
    }

    /**
     * Adds a blob to the freetable, and sets its size, header flags and trailer.
     *
     * This method does not affect the PRECEDING_BLOB_FREE_FLAG of the successor blob; it is the responsibility of the
     * caller to set the flag, if necessary.
     *
     * @param firstPage the first page of the blob
     * @param pages     the number of pages of this blob
     * @param freeFlags PRECEDING_BLOB_FREE_FLAG or 0
     */
    private void addFreeBlob(int firstPage, int pages, int freeFlags)
    {
        // log.debug("      Adding free blob {} ({} pages)", firstPage, pages);

        assert freeFlags == 0 || freeFlags == PRECEDING_BLOB_FREE_FLAG;
        ByteBuffer firstBlock = getBlockOfPage(firstPage);
        int payloadSize = (pages << pageSizeShift) - 4;
        firstBlock.putInt(0, payloadSize | FREE_BLOB_FLAG | freeFlags);
        firstBlock.putInt(PREV_FREE_BLOB_OFS, 0);
        ByteBuffer lastBlock = getBlockOfPage(firstPage + pages - 1);
        // TODO: won't work if page size > 4KB
        lastBlock.putInt(TRAILER_OFS, pages);
        ByteBuffer rootBlock = getBlock(0);
        int trunkSlot = (pages - 1) / 512;
        int leafBlob = rootBlock.getInt(TRUNK_FREE_TABLE_OFS + trunkSlot * 4);
        ByteBuffer leafBlock;
        if (leafBlob == 0)
        {
            // If no local free table exists for the size range of this blob,
            // this blob becomes the local free table

            firstBlock.putInt(LEAF_FT_RANGE_BITS_OFS, 0);
            for (int i = 0; i < 2048; i += 4)
            {
                firstBlock.putInt(LEAF_FREE_TABLE_OFS + i, 0);
            }
            int trunkRanges = rootBlock.getInt(TRUNK_FT_RANGE_BITS_OFS);
            trunkRanges |= 1 << (trunkSlot / 16);
            rootBlock.putInt(TRUNK_FT_RANGE_BITS_OFS, trunkRanges);
            rootBlock.putInt(TRUNK_FREE_TABLE_OFS + trunkSlot * 4, firstPage);
            leafBlock = firstBlock;

            // Log.debug("  Free blob %d: Created leaf FT for %d pages", firstPage, pages);
        }
        else
        {
            leafBlock = getBlockOfPage(leafBlob);
        }

        // Determine the slot in the leaf freetable where this
        // free blob will be placed

        int leafSlot = (pages - 1) % 512;
        int leafOfs = LEAF_FREE_TABLE_OFS + leafSlot * 4;
        int nextBlob = leafBlock.getInt(leafOfs);
        if (nextBlob != 0)
        {
            // If a free blob of the same size exists already,
            // chain this blob as a sibling

            ByteBuffer nextBlock = getBlockOfPage(nextBlob);
            nextBlock.putInt(PREV_FREE_BLOB_OFS, firstPage);
        }
        firstBlock.putInt(NEXT_FREE_BLOB_OFS, nextBlob);

        // Enter this free blob into the size-specific slot
        // in the leaf freetable, and set the range bit

        leafBlock.putInt(leafOfs, firstPage);
        int leafRanges = leafBlock.getInt(LEAF_FT_RANGE_BITS_OFS);
        leafRanges |= 1 << (leafSlot / 16);
        leafBlock.putInt(LEAF_FT_RANGE_BITS_OFS, leafRanges);
    }

    /**
     * Copies a blob's free table to another free blob. The original blob's free table and the free-range bits must be
     * valid, all other data is allowed to have been modified at this point.
     *
     * @param page        the first page of the original blob
     * @param sizeInPages the blob's size in pages
     * @return the page of the blob to which the free table has been assigned, or 0 if the table has not been relocated.
     */

    // TODO: give this method more responsibility, see uses
    // TODO: rename preserveFreeTable
    private int relocateFreeTable(int page, int sizeInPages)
    {
        ByteBuffer block = getBlockOfPage(page);
        int ranges = block.getInt(LEAF_FT_RANGE_BITS_OFS);
        // Make a copy of ranges, because we will modify it during search
        int originalRanges = ranges;
        int p = LEAF_FREE_TABLE_OFS;
        while (ranges != 0)
        {
            if ((ranges & 1) != 0)
            {
                int pEnd = p + 64;
                for (; p < pEnd; p += 4)
                {
                    int otherPage = block.getInt(p);
                    if (otherPage != 0 && otherPage != page)
                    {
                        ByteBuffer otherBlock = getBlockOfPage(otherPage);
                        assert (otherBlock.getInt(0) & FREE_BLOB_FLAG) != 0 :
                            String.format("Found allocated blob (First page = %d) in FT",
                                otherPage);

                        for (int i = LEAF_FREE_TABLE_OFS;
                             i < LEAF_FREE_TABLE_OFS + FREE_TABLE_LEN; i += 4)
                        {
                            otherBlock.putInt(i, block.getInt(i));
                        }
                        otherBlock.putInt(LEAF_FT_RANGE_BITS_OFS, originalRanges);
                        // don't use `ranges`; search consumes the bits
                        ByteBuffer rootBlock = getBlock(0);
                        int trunkSlot = (sizeInPages - 1) / 512;
                        rootBlock.putInt(TRUNK_FREE_TABLE_OFS + trunkSlot * 4, otherPage);

                        // log.debug("      Moved free table from {} to {}", page, otherPage);
                        return otherPage;
                    }
                }
                p = pEnd;
                ranges >>>= 1;
            }
            else
            {
                int rangesToSkip = Integer.numberOfTrailingZeros(ranges);
                ranges >>>= rangesToSkip;
                p += rangesToSkip * 64;
            }
        }
        return 0;
    }

    // TODO: remove
    public void export(int page, Path path) throws IOException
    {
        final ByteBuffer buf = bufferOfPage(page);
        int p = offsetOfPage(page);
        int len = buf.getInt(p) & 0x3fff_ffff;
        final int BUF_SIZE = 64 * 1024;
        byte[] b = new byte[BUF_SIZE];
        int bytesRemaining = len;
        FileOutputStream fout = new FileOutputStream(path.toString());
        GZIPOutputStream out = new GZIPOutputStream(fout);
        byte flagMask = 0x3f;
        while (bytesRemaining > 0)
        {
            int chunkSize = Integer.min(bytesRemaining, BUF_SIZE);
            buf.get(b, 0, chunkSize);
            b[3] &= flagMask;
            flagMask = (byte) 0xff;
            out.write(b, 0, chunkSize);
            bytesRemaining -= chunkSize;
            p += chunkSize;
        }
        out.close();
        fout.close();
    }

    /*
    // TODO
    protected void debugCheck()
    {
        debugCheckRootFT();
        BlobStoreChecker checker = new BlobStoreChecker(this);
        checker.check();
        if (checker.hasErrors())
        {
            checker.reportErrors(System.out);
            throw new StoreException("BlobStore corrupted.", path());
        }
    }

    protected void debugCheckRootFT()
    {
        debugCheckRootFT(getMapping(0));
        debugCheckRootFT(getBlock(0));
    }

    protected void debugCheckRootFT(ByteBuffer buf)
    {
        debugLogRootFT(buf);
        int p = TRUNK_FREE_TABLE_OFS;
        int ranges = 0;
        for (int slot = 0; slot < 512; slot++)
        {
            if (buf.getInt(p) != 0) ranges |= 1 << (slot >>> 4);
            p += 4;
        }
        int actualRanges = buf.getInt(TRUNK_FT_RANGE_BITS_OFS);
        if (actualRanges != ranges)
        {
            throw new StoreException(
                String.format("trunk_free_range_mask should be %X instead of %X",
                    ranges, actualRanges), path());
        }
    }

    protected void debugLogRootFT(ByteBuffer buf)
    {
        Log.debug("trunk_free_range_mask (%s) = %X",
            buf == getMapping(0) ? "stored" : "uncommitted",
            buf.getInt(TRUNK_FT_RANGE_BITS_OFS));
    }

    */

    protected int getIndexEntry(int id)
    {
        // TODO: should this use journaled blocks instead of raw access?
        return baseMapping.getInt(indexPointer() + id * 4);
    }

    protected void setIndexEntry(int id, int page)
    {
        int pIndexEntry = indexPointer() + id * 4;
        ByteBuffer indexBlock = getBlock(pIndexEntry & 0xffff_f000);    // TODO: assumes block length 4096
        indexBlock.putInt(pIndexEntry % BLOCK_LEN, page);
    }

    public int fetchBlob(int id)
    {
        int page = getIndexEntry(id);
        if (page != 0) return page;
        if(downloader == null)
        {
            throw new StoreException(String.format("Cannot download %06X; " +
                "repository URL must be specified", id), path());
        }
        try
        {
            Downloader.Ticket ticket = downloader.request(id, null);
            ticket.awaitCompletion();
            ticket.throwError();
            return ticket.page();
        }
        catch(InterruptedException ex)
        {
            throw new RuntimeException(ex);     // TODO
        }
    }

    @Override public void close()
    {
        if(downloader != null) downloader.shutdown();
        super.close();
    }

    public void removeBlobs(IntIterator iter) throws IOException
    {
        beginTransaction(LOCK_EXCLUSIVE);
        while(iter.hasNext())
        {
            int id = iter.next();
            int page = getIndexEntry(id);
            freeBlob(page);
            setIndexEntry(id, 0);
        }
        commit();
        endTransaction();
    }
}