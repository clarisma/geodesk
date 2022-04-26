package com.geodesk.io.osm;

public class OsmPbf
{
    public static final int BLOBHEADER_TYPE = (1 << 3) | 2;
    public static final int BLOBHEADER_DATASIZE = (3 << 3);

    public static final int BLOB_RAW_DATA = (1 << 3) | 2;
    public static final int BLOB_RAW_SIZE = (2 << 3);
    public static final int BLOB_ZLIB_DATA = (3 << 3) | 2;

    public static final int HEADER_BBOX = (1 << 3) | 2;
    public static final int HEADER_REQUIRED_FEATURES = (4 << 3) | 2;
    public static final int HEADER_OPTIONAL_FEATURES = (5 << 3) | 2;
    public static final int HEADER_WRITINGPROGRAM = (16 << 3) | 2;
    public static final int HEADER_SOURCE = (17 << 3) | 2;
    public static final int HEADER_REPLICATION_TIMESTAMP = (32 << 3);
    public static final int HEADER_REPLICATION_SEQUENCE = (33 << 3);
    public static final int HEADER_REPLICATION_URL = (34 << 3) | 2;

    public static final int BLOCK_STRINGTABLE = (1 << 3) | 2;
    public static final int BLOCK_GROUP = (2 << 3) | 2;
    public static final int BLOCK_GRANULARITY = 17 << 3;
    public static final int BLOCK_DATE_GRANULARITY = 18 << 3;
    public static final int BLOCK_LAT_OFFSET = 19 << 3;
    public static final int BLOCK_LON_OFFSET = 20 << 3;

    public static final int STRINGTABLE_ENTRY = (1 << 3) | 2;

    // Structures that appear within a PrimitiveGroup

    public static final int GROUP_NODE = (1 << 3) | 2;
    public static final int GROUP_DENSENODES = (2 << 3) | 2;
    public static final int GROUP_WAY = (3 << 3) | 2;
    public static final int GROUP_RELATION = (4 << 3) | 2;
    public static final int GROUP_CHANGESET = (5 << 3) | 2;

    public static final int DENSENODE_IDS = (1 << 3) | 2;
    public static final int DENSENODE_INFO = (5 << 3) | 2;
    public static final int DENSENODE_LATS = (8 << 3) | 2;
    public static final int DENSENODE_LONS = (9 << 3) | 2;
    public static final int DENSENODE_TAGS = (10 << 3) | 2;

    public static final int ELEMENT_ID = (1 << 3);
    public static final int ELEMENT_KEYS = (2 << 3) | 2;
    public static final int ELEMENT_VALUES = (3 << 3) | 2;
    public static final int ELEMENT_INFO = (4 << 3) | 2;

    public static final int WAY_NODES = (8 << 3) | 2;
    public static final int WAY_COORDS = (9 << 3) | 2;		// non-standard (MD)
    public static final int WAY_LATS  = (9 << 3) | 2;		// non-standard (Joto)
    public static final int WAY_LONS = (10 << 3) | 2;		// non-standard (Joto)

    public static final int RELATION_MEMBER_ROLES = (8 << 3) | 2;
    public static final int RELATION_MEMBER_IDS = (9 << 3) | 2;
    public static final int RELATION_MEMBER_TYPES = (10 << 3) | 2;
}
