package com.geodesk.feature.filter;

public class FilterType
{
    public static final int NODES           = 0x0050005;
    public static final int ALL_WAYS        = 0x0F000F0;
    public static final int ALL_RELATIONS   = 0xF000F00;

    public static final int AREAS           = 0xAA00AA0;
    public static final int MEMBERS         = 0xCC40CC4;

    public static int fromFlags(int flags)
    {
        return 1 << ((flags >>> 1) & 0x1f);
    }
}
