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

/**
 * A Filter that only accepts features whose geometry overlaps the
 * test geometry.
 *
 * - The geometries of test and candidate must have the same dimension.
 *
 * - Test and candidate each have at least one point not shared by the other.
 *
 * - The intersection of their interiors has the same dimension.
 */
public class OverlapsFilter implements Filter
{
    private final PreparedGeometry prepared;
    private final Box bounds;

    public OverlapsFilter(Feature feature)
    {
        this(feature.toGeometry());
    }

    public OverlapsFilter(Geometry geom)
    {
        this(PreparedGeometryFactory.prepare(geom));
    }

    public OverlapsFilter(PreparedGeometry prepared)
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

    @Override public boolean accept(Feature feature, Geometry geom)
    {
        return prepared.overlaps(geom);
    }

    @Override public Bounds bounds() { return bounds; }
}


