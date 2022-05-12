package com.geodesk.feature.filter;

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.prep.PreparedGeometry;

public class WithinFilter extends SpatialFilter
{
    private final PreparedGeometry prepared;

    public WithinFilter(PreparedGeometry prepared)
    {
        this.prepared = prepared;
    }

    @Override public boolean acceptGeometry(Geometry geom)
    {
        return prepared.contains(geom);
    }
}
