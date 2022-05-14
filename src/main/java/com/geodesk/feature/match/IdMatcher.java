package com.geodesk.feature.match;

import java.nio.ByteBuffer;

public class IdMatcher extends Matcher
{
    private final long idBits;
    private static final long TYPE_ID_MASK = 0xffff_ffff_ffff_ff18l;

    public IdMatcher(int typeCode, long id)
    {
        idBits = (id << 32) | ((id >> 24) & 0xffff_ff00L) | (typeCode << 3);
    }

    @Override public boolean accept(ByteBuffer buf, int pos)
    {
        return (buf.getLong(pos) & TYPE_ID_MASK) == idBits;
    }
}
