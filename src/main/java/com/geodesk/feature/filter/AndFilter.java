/*
 * Copyright (c) Clarisma / GeoDesk contributors
 *
 * This source code is licensed under the Apache 2.0 license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.geodesk.feature.filter;

import com.clarisma.common.util.Log;
import com.geodesk.core.Box;
import com.geodesk.feature.Feature;
import com.geodesk.feature.Filter;
import com.geodesk.geom.Bounds;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Polygon;

/**
 * A Filter that combined two Filters.
 */
public class AndFilter implements Filter
{
    private final Filter left;
    private final Filter right;
    private final int strategy;
    private final int acceptedTypes;
    private final Bounds bounds;

    public AndFilter(Filter left, Filter right, int strategy, Bounds bounds, int acceptedTypes)
    {
        this.left = left;
        this.right = right;
        this.strategy = strategy;
        this.acceptedTypes = acceptedTypes;
        this.bounds = bounds;
    }

    @Override public boolean accept(Feature feature)
    {
        if((strategy & FilterStrategy.NEEDS_GEOMETRY) != 0)
        {
            return accept(feature, feature.toGeometry());
        }
        return accept(feature, null);
    }

    @Override public boolean accept(Feature feature, Geometry geom)
    {
        return left.accept(feature, geom) && right.accept(feature, geom);
    }

    @Override public int strategy()
    {
        return strategy;
    }

    @Override public int acceptedTypes()
    {
        return acceptedTypes;
    }

    @Override public Bounds bounds() { return bounds; }

    @Override public Filter filterForTile(int tile, Polygon tileGeometry)
    {
        Filter newLeft = left.filterForTile(tile, tileGeometry);
        if (newLeft == FalseFilter.INSTANCE) return FalseFilter.INSTANCE;
        Filter newRight = right.filterForTile(tile, tileGeometry);
        if (newRight == FalseFilter.INSTANCE) return FalseFilter.INSTANCE;
        if (newLeft == null) return newRight;
        if (newRight == null) return newLeft;
        if (newLeft == left && newRight == right) return this;
        return create(newLeft, newRight);
            // TODO: don't need to AND types and bbox, since these are only
            //  used at beginning of filtering (not applied on a per-tile basis)
    }


    public static Filter create(Filter left, Filter right)
    {
        int leftStrategy = left.strategy();
        int rightStrategy = right.strategy();
        int leftStrictBounds = leftStrategy & FilterStrategy.STRICT_BBOX;
        int rightStrictBounds = rightStrategy & FilterStrategy.STRICT_BBOX;

        // combine all strategy flags, except strict-bbox
        // the combined filter is only strict-bbox is both are strict-bbox
        // TODO: what about needs-geometry?
        int combinedStrategy = ((leftStrategy | rightStrategy)
            & ~FilterStrategy.STRICT_BBOX) |
            (leftStrictBounds & rightStrictBounds);

        int acceptedTypes = left.acceptedTypes() & right.acceptedTypes();
        if(acceptedTypes == 0) return FalseFilter.INSTANCE;

        Bounds bounds;
        if((combinedStrategy & FilterStrategy.USES_BBOX) != 0)
        {
            // TODO: check for null bbox or enforce filter.bounds() returning World bbox
            //  if bbox not in use

            Bounds leftBounds = left.bounds();
            Bounds rightBounds = right.bounds();

            if ((leftStrictBounds | rightStrictBounds) != 0)
            {
                bounds = Box.intersection(leftBounds, rightBounds);
                if (Box.isNull(bounds)) return FalseFilter.INSTANCE;
            }
            else
            {
                bounds = Box.smaller(leftBounds, rightBounds);
            }
        }
        else
        {
            bounds = Box.ofWorld();
        }
        // Log.debug("Combining %s and %s", left, right);
        return new AndFilter(left, right, combinedStrategy, bounds, acceptedTypes);
    }
}
