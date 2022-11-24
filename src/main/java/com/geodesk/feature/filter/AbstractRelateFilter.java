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
import com.geodesk.feature.match.TypeBits;
import com.geodesk.geom.Bounds;
import org.locationtech.jts.geom.*;
import org.locationtech.jts.geom.prep.PreparedGeometry;
import org.locationtech.jts.geom.prep.PreparedGeometryFactory;

/**
 * A base class for Filter classes that implement DE9-IM predicates.
 *
 * This base class provides the most common presets:
 * - strategy:
 *   - tile acceleration
 *   - uses (non-strict) bbox
 *   - needs geometry
 *   - restricts types
 * - tile acceleration:
 *   - disjoint tile: reject all
 *   - properlyContained tile: reject single-tile features, test multi-tile
 */
abstract class AbstractRelateFilter implements Filter
{
    protected final PreparedGeometry prepared;
    protected final Box bounds;
    protected final int acceptedTypes;
    protected final int testDimension;

    final static int MIXED_DIMENSION = -3;

    public AbstractRelateFilter(PreparedGeometry prepared, int acceptedTypes)
    {
        this.prepared = prepared;
        Geometry geom = prepared.getGeometry();
        bounds = Box.fromEnvelope(geom.getEnvelopeInternal());
        testDimension = (geom.getClass() == GeometryCollection.class) ? MIXED_DIMENSION :
            geom.getDimension();
        this.acceptedTypes = acceptedTypes;
    }

    @Override public int strategy()
    {
        return FilterStrategy.FAST_TILE_FILTER |
            FilterStrategy.NEEDS_GEOMETRY |
            FilterStrategy.USES_BBOX |
            FilterStrategy.RESTRICTS_TYPES;
    }

    @Override public int acceptedTypes()
    {
        return acceptedTypes;
    }

    @Override public Filter filterForTile(int tile, Polygon tileGeometry)
    {
        if(prepared.disjoint(tileGeometry))
        {
            return FalseFilter.INSTANCE;
        }
        if(testDimension == 2 && prepared.containsProperly(tileGeometry))
        {
            return new FastTileFilter(tile, false, this);
        }
        return this;
    }

    /*
    public boolean acceptGeometry(Geometry geom)
    {
        return false;
    }
     */

    /*
    // TODO: this is wrong, may need to test if true for every geometry in collection
    public boolean accept(Feature feature, Geometry geom)
    {
        if(geom.getClass() == GeometryCollection.class)
        {
            int geomCount = geom.getNumGeometries();
            for(int n=0; n< geomCount; n++)
            {
                if(acceptGeometry(geom.getGeometryN(n))) return true;
            }
            return false;
        }
        return acceptGeometry(geom);
//        catch(Exception ex)
//        {
//            Log.debug("Exception %s involving class %s: %s",
//                ex.getMessage(), geom.getClass(), geom.toString().substring(0,200));
//            throw new RuntimeException(ex);
//        }
    }
     */

    @Override public Bounds bounds() { return bounds; }
}


