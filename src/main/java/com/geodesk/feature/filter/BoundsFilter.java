package com.geodesk.feature.filter;

import com.geodesk.feature.Feature;
import com.geodesk.feature.Filter;
import com.geodesk.geom.Bounds;

/**
 * A Filter that accepts only features that intersect the specified bounds.
 */
public class BoundsFilter implements Filter
{
    private final Bounds bounds;

    public BoundsFilter(Bounds bounds)
    {
        this.bounds = bounds;
    }

    @Override public boolean accept(Feature feature)
    {
        return bounds.intersects(feature.bounds());
    }

    // TODO: Geometry?
}
