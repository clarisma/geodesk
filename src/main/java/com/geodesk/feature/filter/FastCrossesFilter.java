/*
 * Copyright (c) Clarisma / GeoDesk contributors
 *
 * This source code is licensed under the Apache 2.0 license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.geodesk.feature.filter;

import com.geodesk.core.Box;
import com.geodesk.feature.Feature;
import com.geodesk.feature.Filter;
import com.geodesk.geom.Bounds;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.prep.PreparedGeometry;
import org.locationtech.jts.geom.prep.PreparedGeometryFactory;

public class FastCrossesFilter implements Filter
{
    private final PreparedGeometry prepared;
    private final Box bounds;

    public FastCrossesFilter(Feature feature)
    {
        this(feature.toGeometry());
    }

    public FastCrossesFilter(Geometry geom)
    {
        this(PreparedGeometryFactory.prepare(geom));
    }

    public FastCrossesFilter(PreparedGeometry prepared)
    {
        this.prepared = prepared;
        Geometry geom = prepared.getGeometry();
        bounds = Box.fromEnvelope(geom.getEnvelopeInternal());
    }

    @Override public int strategy()
    {
        return FilterStrategy.FAST_TILE_FILTER |
            FilterStrategy.NEEDS_GEOMETRY |
            FilterStrategy.USES_BBOX |
            FilterStrategy.RESTRICTS_TYPES;
    }

    @Override public Filter filterForTile(int tile, Polygon tileGeometry)
    {
        if(prepared.disjoint(tileGeometry))
        {
            return FalseFilter.INSTANCE;
        }
        if(prepared.containsProperly(tileGeometry))
        {
            return new FastTileFilter(tile, false, this);
        }
        return this;
    }

    @Override public boolean accept(Feature feature)
    {
        return accept(feature, feature.toGeometry());
    }

    @Override public boolean accept(Feature feature, Geometry geom)
    {
        return prepared.crosses(geom);
    }

    @Override public Bounds bounds() { return bounds; }
}


