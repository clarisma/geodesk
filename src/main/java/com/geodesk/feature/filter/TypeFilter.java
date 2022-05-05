package com.geodesk.feature.filter;

import com.clarisma.common.util.Log;
import com.geodesk.feature.Filter;
import com.geodesk.feature.store.StoredFeature;
import org.locationtech.jts.geom.Geometry;

import java.nio.ByteBuffer;


public class TypeFilter implements Filter
{
    private final int acceptedTypes;
    private final Filter filter;

    public TypeFilter(int acceptedTypes, Filter filter)
    {
        this.acceptedTypes = acceptedTypes;
        this.filter = filter;
    }

    @Override public boolean accept(ByteBuffer buf, int pos)
    {
        int flags = buf.get(pos);
        int type = 1 << ((flags >>> 1) & 0x1f);
        if((type & acceptedTypes) == 0) return false;
        return filter.accept(buf, pos);
    }

    @Override public boolean acceptTyped(int types, ByteBuffer buf, int pos)
    {
        return filter.acceptTyped(types & acceptedTypes, buf, pos);
    }

    @Override public boolean acceptIndex(int keys)
    {
        return filter.acceptIndex(keys);
    }

    @Override public boolean acceptGeometry(Geometry geom)
    {
        return filter.acceptGeometry(geom);
    }
}


//public class TypeFilter extends AbstractFilter
//{
//    private int types;
//
//    public TypeFilter(int types)
//    {
//        this.types= types;
//    }
//
//    @Override public boolean accept(ByteBuffer buf, int pos)
//    {
//        int flags = buf.get(pos);
//        int type = 1 << ((flags >>> 1) & 0x1f);
//        return (types & type) != 0;
//    }
//}
