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
 * A Filter that only accepts features whose geometry touches the
 * test geometry.
 *
 * - Test and candidate must have at least one point in common, but their
 *   interiors do not intersect.
 *
 * - if Test is puntal, do not accept nodes
 *
 * This Filter does not accept generic GeometryCollections, neither as test nor as candidate
 * (result is always `false`).
 */
public class TouchesFilter extends AbstractRelateFilter
{
    public TouchesFilter(Feature feature)
    {
        this(feature.toGeometry());
    }

    public TouchesFilter(Geometry geom)
    {
        this(PreparedGeometryFactory.prepare(geom));
    }

    public TouchesFilter(PreparedGeometry prepared)
    {
        super(prepared, acceptedType(prepared));
    }

    private static int acceptedType(PreparedGeometry prepared)
    {
        Geometry geom = prepared.getGeometry();
        if(geom instanceof Puntal) return TypeBits.ALL & ~TypeBits.NODES;
        if(geom.getClass() == GeometryCollection.class) return 0;
        return TypeBits.ALL;
    }

    @Override public boolean accept(Feature feature, Geometry geom)
    {
        if(geom.getClass() == GeometryCollection.class) return false;
        return prepared.touches(geom);
    }
}



