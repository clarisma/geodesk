package com.geodesk.feature.filter;

import org.locationtech.jts.geom.prep.PreparedGeometry;

public class WithinFilter
{
    private final PreparedGeometry prepared;

    public WithinFilter(PreparedGeometry prepared)
    {
        this.prepared = prepared;
    }
}
