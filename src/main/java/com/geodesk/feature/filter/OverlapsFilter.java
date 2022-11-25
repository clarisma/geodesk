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
 * A Filter that only accepts features whose geometry overlaps the
 * test geometry.
 *
 * - The geometries of test and candidate must have the same dimension.
 *
 * - Test and candidate each have at least one point not shared by the other.
 *
 * - The intersection of their interiors has the same dimension.
 *
 * This Filter does not accept generic GeometryCollections, neither as test nor as candidate
 * (result is always `false`).
 */
public class OverlapsFilter extends AbstractRelateFilter
{
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
        super(prepared, acceptedType(prepared));
    }

    private static int acceptedType(PreparedGeometry prepared)
    {
        Geometry geom = prepared.getGeometry();
        if(geom instanceof Polygonal) return TypeBits.AREAS | TypeBits.NONAREA_RELATIONS;
        if(geom instanceof Lineal) return TypeBits.NONAREA_WAYS | TypeBits.NONAREA_RELATIONS;
        if(geom instanceof Puntal) return TypeBits.NODES | TypeBits.NONAREA_RELATIONS;
        return 0;   // don't accept generic GeometryCollection
    }

    @Override public boolean accept(Feature feature, Geometry geom)
    {
        if(geom.getClass() == GeometryCollection.class) return false;
        return prepared.overlaps(geom);
    }

    @Override public Bounds bounds() { return bounds; }
}


