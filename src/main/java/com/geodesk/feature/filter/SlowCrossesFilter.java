/*
 * Copyright (c) Clarisma / GeoDesk contributors
 *
 * This source code is licensed under the Apache 2.0 license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.geodesk.feature.filter;

import com.geodesk.core.Box;
import com.geodesk.feature.Feature;
import com.geodesk.feature.Relation;
import com.geodesk.geom.Bounds;
import com.geodesk.util.GeometryBuilder;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.prep.PreparedGeometry;
import org.locationtech.jts.geom.prep.PreparedGeometryFactory;

/**
 * A Filter for the `crosses` spatial predicate.
 *
 * Tile acceleration:
 * - If tile is disjoint, reject all
 * - If tile is containedProperly:
 *   - if a feature lies entirely within tile, reject it
 *     (a crossing feature must lie partially outside of test geometry)
 *   - if a feature is multi-tile, must test it
 *
 * Accepted types:
 * - dimension of intersection must be less than maximum dimension of candidate and test
 *   - if test is polygonal, don't accept areas
 *   - if test is puntal, don't accept nodes
 *
 */
public class SlowCrossesFilter extends SpatialFilter
{
    private final PreparedGeometry prepared;

    public SlowCrossesFilter(PreparedGeometry prepared)
    {
        this.prepared = prepared;
    }

    public SlowCrossesFilter(Geometry geom)
    {
        this(PreparedGeometryFactory.prepare(geom));
    }


    public SlowCrossesFilter(Feature f)
    {
        Geometry g;
        if(f instanceof Relation rel)
        {
            g = GeometryBuilder.instance.createMultiLineString(rel);
        }
        else
        {
            g = f.toGeometry();
        }
        prepared = PreparedGeometryFactory.prepare(g);
    }

    @Override public boolean acceptGeometry(Geometry geom)
    {
        return geom != null && prepared.crosses(geom);
    }

    @Override public Bounds bounds()
    {
        // TODO: if using Feature, get the bbox of feature
        //  but Envelope will be calculated anyway, so only minor savings
        return Box.fromEnvelope(prepared.getGeometry().getEnvelopeInternal());
    }
}
