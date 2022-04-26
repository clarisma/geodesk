package com.geodesk.feature.filter;

import java.nio.ByteBuffer;

public class TypeFilter extends AbstractFilter
{
    private int types;

    public TypeFilter(int types)
    {
        this.types= types;
    }

    @Override public boolean accept(ByteBuffer buf, int pos)
    {
        int flags = buf.get(pos);
        int type = 1 << ((flags >>> 1) & 0x1f);
        return (types & type) != 0;
    }
}
