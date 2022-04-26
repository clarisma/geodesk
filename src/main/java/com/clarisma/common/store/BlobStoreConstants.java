package com.clarisma.common.store;

public class BlobStoreConstants
{
    public static int MAGIC = 0x7ADA0BB1;  // B10BDA7A = "BlobData"
    public static int VERSION = 1_000_000;

    public static final int VERSION_OFS = 4;
    public static final int FINGERPRINT_OFS = 8;
    public static final int TOTAL_PAGES_OFS = 16;
    public static final int PAGE_SIZE_OFS = 20;
    public static final int METADATA_SIZE_OFS = 24;
    public static final int TRUNK_FT_RANGE_BITS_OFS = 28;
    public static final int TRUNK_FREE_TABLE_OFS = 64;     // must be divisible by 64

    public static final int PREV_FREE_BLOB_OFS = 4;
    public static final int NEXT_FREE_BLOB_OFS = 8;
    public static final int LEAF_FT_RANGE_BITS_OFS = 12;
    public static final int LEAF_FREE_TABLE_OFS = 64;      // must be divisible by 64

    public static final int BLOCK_LEN = 4096;
    public static final int TRAILER_OFS = BLOCK_LEN - 4;

    public static final int FREE_TABLE_LEN = 2048;

    public static final int DEFAULT_METADATA_SIZE = TRUNK_FREE_TABLE_OFS + FREE_TABLE_LEN;
}
