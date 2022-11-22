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

public class WithinFilter extends AbstractRelateFilter
{
    public WithinFilter(Feature feature)
    {
        this(feature.toGeometry());
    }

    public WithinFilter(Geometry geom)
    {
        this(PreparedGeometryFactory.prepare(geom));
    }

    public WithinFilter(PreparedGeometry prepared)
    {
        super(prepared, acceptedType(prepared));
    }

    private static int acceptedType(PreparedGeometry prepared)
    {
        Geometry geom = prepared.getGeometry();
        if(geom instanceof Polygonal) return TypeBits.ALL;
        if(geom instanceof Lineal) return TypeBits.ALL & ~TypeBits.AREAS;
        if(geom instanceof Puntal) return TypeBits.NODES | TypeBits.NONAREA_RELATIONS;
        return 0;   // don't accept generic GeometryCollection
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
        if(prepared.disjoint(tileGeometry))
        {
            return FalseFilter.INSTANCE;
        }
        if(testDimension == 2 && prepared.containsProperly(tileGeometry))
        {
            return new FastTileFilter(tile, true, this);
        }
        return this;
    }

    @Override public boolean accept(Feature feature, Geometry geom)
    {
        /*
        if(geom.getClass() == GeometryCollection.class)
        {
            int geomCount = geom.getNumGeometries();
            for(int n=0; n< geomCount; n++)
            {
                if(!prepared.contains(geom.getGeometryN(n))) return false;
            }
            return false;
        }
         */
        return prepared.contains(geom);
    }
}


