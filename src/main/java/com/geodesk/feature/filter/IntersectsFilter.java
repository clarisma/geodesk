/*
 * Copyright (c) Clarisma / GeoDesk contributors
 *
 * This source code is licensed under the Apache 2.0 license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.geodesk.feature.filter;

import com.geodesk.feature.Feature;
import com.geodesk.feature.Filter;
import com.geodesk.feature.match.TypeBits;
import org.locationtech.jts.geom.*;
import org.locationtech.jts.geom.prep.PreparedGeometry;
import org.locationtech.jts.geom.prep.PreparedGeometryFactory;

/**
 * A Filter that only accepts features whose geometry intersects the
 * test geometry.
 */
public class IntersectsFilter extends AbstractRelateFilter
{
    public IntersectsFilter(Feature feature)
    {
        this(feature.toGeometry());
    }

    public IntersectsFilter(Geometry geom)
    {
        this(PreparedGeometryFactory.prepare(geom));
    }

    public IntersectsFilter(PreparedGeometry prepared)
    {
        super(prepared, TypeBits.ALL);
    }

    @Override public int strategy()
    {
        return FilterStrategy.FAST_TILE_FILTER |
            FilterStrategy.NEEDS_GEOMETRY |
            FilterStrategy.USES_BBOX;
    }

    @Override public Filter filterForTile(int tile, Polygon tileGeometry)
    {
        if(prepared.disjoint(tileGeometry))
        {
            return FalseFilter.INSTANCE;
        }
        if(testDimension == 2 && prepared.containsProperly(tileGeometry))
        {
            return null;
        }
        // Log.debug("Must test: %s", Tile.toString(tile));
        return this;
    }

    @Override public boolean accept(Feature feature, Geometry geom)
    {
        return prepared.intersects(geom);
    }
}


