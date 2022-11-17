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
import com.geodesk.feature.match.TypeBits;
import com.geodesk.geom.Bounds;
import org.locationtech.jts.geom.*;
import org.locationtech.jts.geom.prep.PreparedGeometry;
import org.locationtech.jts.geom.prep.PreparedGeometryFactory;

/**
 * A Filter that only accepts features whose geometry is covered by the
 * test geometry.
 */
public class CoveredByFilter implements Filter
{
    private final PreparedGeometry prepared;
    private final Box bounds;
    private final int acceptedTypes;

    public CoveredByFilter(Feature feature)
    {
        this(feature.toGeometry());
    }

    public CoveredByFilter(Geometry geom)
    {
        this(PreparedGeometryFactory.prepare(geom));
    }

    public CoveredByFilter(PreparedGeometry prepared)
    {
        this.prepared = prepared;
        Geometry geom = prepared.getGeometry();
        bounds = Box.fromEnvelope(geom.getEnvelopeInternal());
        if(geom instanceof Polygonal)
        {
            acceptedTypes = TypeBits.ALL;
        }
        else if(geom instanceof Lineal)
        {
            acceptedTypes = TypeBits.ALL & ~TypeBits.AREAS;
        }
        else
        {
            assert geom instanceof Puntal;
            acceptedTypes = TypeBits.NODES | TypeBits.NONAREA_RELATIONS;
        }
    }

    @Override public int strategy()
    {
        return FilterStrategy.FAST_TILE_FILTER |
            FilterStrategy.NEEDS_GEOMETRY |
            FilterStrategy.USES_BBOX |
            FilterStrategy.STRICT_BBOX |
            FilterStrategy.RESTRICTS_TYPES;
    }

    @Override public Filter filterForTile(int tile, Polygon tileGeometry)
    {
        if(prepared.disjoint(tileGeometry)) return FalseFilter.INSTANCE;
        if(prepared.containsProperly(tileGeometry))
        {
            return new FastTileFilter(tile, true, this);
        }
        return this;
    }

    @Override public boolean accept(Feature feature, Geometry geom)
    {
        return prepared.covers(geom);
    }

    @Override public Bounds bounds() { return bounds; }

    @Override public int acceptedTypes() { return acceptedTypes; }

}

