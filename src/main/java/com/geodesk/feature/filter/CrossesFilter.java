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
 * A Filter that only accepts features whose geometry crosses the
 * test geometry.
 *
 * Dimension of intersection must be less than maximum dimension of candidate and test
 * - if test is polygonal, don't accept areas
 * - if test is puntal, don't accept nodes
 */

public class CrossesFilter extends AbstractRelateFilter
{
    public CrossesFilter(Feature feature)
    {
        this(feature.toGeometry());
    }

    public CrossesFilter(Geometry geom)
    {
        this(PreparedGeometryFactory.prepare(geom));
    }

    public CrossesFilter(PreparedGeometry prepared)
    {
        super(prepared, acceptedType(prepared));
    }

    private static int acceptedType(PreparedGeometry prepared)
    {
        Geometry geom = prepared.getGeometry();
        if(geom instanceof Polygonal) return TypeBits.ALL & ~TypeBits.AREAS;
        if(geom instanceof Puntal) return TypeBits.ALL & ~TypeBits.NODES;
        return TypeBits.ALL;
    }

    @Override public boolean acceptGeometry(Geometry geom)
    {
        return prepared.crosses(geom);
    }
}


