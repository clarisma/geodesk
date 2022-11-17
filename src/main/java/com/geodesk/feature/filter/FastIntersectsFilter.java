/*
 * Copyright (c) Clarisma / GeoDesk contributors
 *
 * This source code is licensed under the Apache 2.0 license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.geodesk.feature.filter;

import com.clarisma.common.util.Log;
import com.geodesk.core.Box;
import com.geodesk.core.Tile;
import com.geodesk.feature.Feature;
import com.geodesk.feature.Filter;
import com.geodesk.geom.Bounds;
import org.locationtech.jts.geom.*;
import org.locationtech.jts.geom.prep.PreparedGeometry;
import org.locationtech.jts.geom.prep.PreparedGeometryFactory;

public class FastIntersectsFilter implements Filter
{
    private final PreparedGeometry prepared;
    private final Box bounds;

    public FastIntersectsFilter(Feature feature)
    {
        this(feature.toGeometry());
    }

    public FastIntersectsFilter(Geometry geom)
    {
        this(PreparedGeometryFactory.prepare(geom));
    }

    public FastIntersectsFilter(PreparedGeometry prepared)
    {
        this.prepared = prepared;
        Geometry geom = prepared.getGeometry();
        bounds = Box.fromEnvelope(geom.getEnvelopeInternal());
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
            // Log.debug("Rejected: %s", Tile.toString(tile));
            return FalseFilter.INSTANCE;
        }
        if(prepared.containsProperly(tileGeometry))
        {
            // Log.debug("Fully inside: %s", Tile.toString(tile));
            return null;
        }
        // Log.debug("Normal test: %s", Tile.toString(tile));
        return this;
    }

    @Override public boolean accept(Feature feature, Geometry geom)
    {
        return prepared.intersects(geom);
    }

    @Override public Bounds bounds() { return bounds; }
}


