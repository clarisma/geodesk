package com.clarisma.common.store;

public class BlobStoreConstants
{
    // === GENERAL ===

    /**
     * Block length (4K).
     * A Block is the smallest unit of data that is journaled.
     * The Page Size is always a power-of-2 multiple of the Block Size
     */
    public static final int BLOCK_LEN = 4096;

    /**
     * Size of each free table (in bytes). Each free table has 512 slots
     * (4 bytes each).
     */
    public static final int FREE_TABLE_LEN = 2048;

    // === HEADER ===

    public static int MAGIC = 0x7ADA0BB1;  // B10BDA7A = "BlobData"
    public static int VERSION = 1_000_000;

    /**
     * Offset where the file format version is stored.
     */
    public static final int VERSION_OFS = 4;
    public static final int TIMESTAMP_OFS = 8;
    public static final int INDEX_PTR_OFS = 44;

    // TODO: GUID_OFS
    //  PROPERTIES_PTR_OFS
    //  INDEX_PTR_OFS
    //  SUBTYPE_MAGIC_OFS
    //  SUBTYPE_VERSION_OFS

    /**
     * Offset where the total size of the Blob Store (in pages) is stored.
     */
    public static final int TOTAL_PAGES_OFS = 16;

    /**
     * Offset where the page size is stored.
     * Valid page size values are from 0 (4K) to 15 (128 MB)
     */
    public static final int PAGE_SIZE_OFS = 20;

    /**
     * Offset where the length of the metadata section is stored.
     * The length is in bytes and includes all header fields.
     */
    public static final int METADATA_SIZE_OFS = 24;

    /**
     * Offset of the bitmask that stores which ranges of the trunk free-table
     * are in use. Each range covers 16 slots.
     */
    public static final int TRUNK_FT_RANGE_BITS_OFS = 28;

    /**
     * Offset of the trunk free-table.
     * (This offset must be evenly divisible by 64)
     */
    public static final int TRUNK_FREE_TABLE_OFS = 64;     // must be divisible by 64

    public static final int DEFAULT_METADATA_SIZE = TRUNK_FREE_TABLE_OFS + FREE_TABLE_LEN;

    // === BLOB ===

    /**
     * Flag to indicate that a Blob is free. Stored in the header word
     * of a Blob.
     */
    public static final int FREE_BLOB_FLAG = 1 << 31;

    /**
     * Flag to indicate that the Blob immediately preceding this
     * Blob is free. Stored in the header word.
     */
    public static final int PRECEDING_BLOB_FREE_FLAG = 1 << 30;

    /**
     * A bit mask that, when applied to a Blob's header word, yields
     * the size of the Blob's payload (max. 1 GB - 4).
     */
    public static final int PAYLOAD_SIZE_MASK = 0x3fff_ffff;

    // === FREE BLOB ===

    /**
     * Offset where the page number of the previous free blob is stored.
     */
    public static final int PREV_FREE_BLOB_OFS = 4;

    /**
     * Offset where the page number of the next free blob is stored.
     */
    public static final int NEXT_FREE_BLOB_OFS = 8;

    /**
     * Offset of the bitmask that stores which ranges of the leaf free-table
     * are in use. Each range covers 16 slots.
     */
    public static final int LEAF_FT_RANGE_BITS_OFS = 12;

    /**
     * Offset of the leaf free-table.
     * (This offset must be evenly divisible by 64)
     */
    public static final int LEAF_FREE_TABLE_OFS = 64;      // must be divisible by 64

    /**
     * Offset where the free-blob trailer is stored (relative to the start of
     * the free blob's last block). The trailer is a single word that contains
     * the length of the free blob (in pages).
     */
    public static final int TRAILER_OFS = BLOCK_LEN - 4;

    public static final int FREE_BLOB_TRAILER_LEN = 4;


    // === EXPORTED BLOB ===

    public static int EXPORTED_MAGIC = 0x0BB1DAAD;  // ADDAB10B = "Add a Blob"
                                                         // C01DB10B = "Cold blob"
    public static int EXPORTED_HEADER_LEN = 16;  // TODO
    public static int EXPORTED_ORIGINAL_LEN_OFS = 12;  // TODO

}
