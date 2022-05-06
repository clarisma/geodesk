package com.geodesk.feature.filter;

import com.geodesk.feature.Filter;
import org.locationtech.jts.geom.prep.PreparedGeometry;

import java.nio.ByteBuffer;

public class SpatialFilter implements Filter
{
    @Override public boolean accept(ByteBuffer buf, int pos)
    {
        return true;    // TODO
    }
}
