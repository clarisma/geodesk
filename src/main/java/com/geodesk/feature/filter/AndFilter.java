package com.geodesk.feature.filter;

import com.geodesk.feature.Filter;
import org.locationtech.jts.geom.Geometry;

import java.nio.ByteBuffer;

public class AndFilter implements Filter
{
    private final Filter a;
    private final Filter b;

    public AndFilter(Filter a, Filter b)
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

    @Override public boolean acceptGeometry(Geometry geom)
    {
        return a.acceptGeometry(geom) && b.acceptGeometry(geom);
    }
}
