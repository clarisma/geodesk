/*
 * Copyright (c) Clarisma / GeoDesk contributors
 *
 * This source code is licensed under the Apache 2.0 license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.geodesk.feature.filter;

import com.geodesk.feature.Feature;
import com.geodesk.feature.Filter;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.prep.PreparedGeometry;
import org.locationtech.jts.geom.prep.PreparedGeometryFactory;

/**
 * A Filter that only accepts features whose geometry is disjoint from the
 * test geometry.
 */
public class DisjointFilter implements Filter
{
    private final PreparedGeometry prepared;
    private final int testDimension;

    public DisjointFilter(Feature feature)
    {
        this(feature.toGeometry());
    }

    public DisjointFilter(Geometry geom)
    {
        this(PreparedGeometryFactory.prepare(geom));
    }

    public DisjointFilter(PreparedGeometry prepared)
    {
        this.prepared = prepared;
        Geometry geom = prepared.getGeometry();
        testDimension = (geom.getClass() == GeometryCollection.class) ?
            AbstractRelateFilter.MIXED_DIMENSION : geom.getDimension();
    }

    @Override public int strategy()
    {
        return FilterStrategy.FAST_TILE_FILTER | FilterStrategy.NEEDS_GEOMETRY;
    }

    @Override public Filter filterForTile(int tile, Polygon tileGeometry)
    {
        if(prepared.disjoint(tileGeometry))
        {
            return new FastTileFilter(tile, false, this);
        }
        if(testDimension == 2 && prepared.containsProperly(tileGeometry)) return FalseFilter.INSTANCE;
        return this;
    }

    @Override public boolean accept(Feature feature, Geometry geom)
    {
        return prepared.disjoint(geom);
    }
}
