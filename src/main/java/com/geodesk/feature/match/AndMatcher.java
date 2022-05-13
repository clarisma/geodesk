package com.geodesk.feature.match;

import java.nio.ByteBuffer;

public class AndMatcher extends Matcher
{
    private final Matcher a;
    private final Matcher b;

    public AndMatcher(Matcher a, Matcher b)
    {
        this.a = a;
        this.b = b;
    }

    @Override public boolean accept(ByteBuffer buf, int pos)
    {
        return a.accept(buf, pos) && b.accept(buf, pos);
    }

    @Override public boolean acceptTyped(int types, ByteBuffer buf, int pos)
    {
        return a.acceptTyped(types, buf, pos) && b.acceptTyped(types, buf, pos);
    }

    @Override public boolean acceptIndex(int keys)
    {
        return a.acceptIndex(keys) && b.acceptIndex(keys);
    }
}
