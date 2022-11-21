/*
 * Copyright (c) Clarisma / GeoDesk contributors
 *
 * This source code is licensed under the Apache 2.0 license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.geodesk.feature.filter;

import com.geodesk.core.Box;
import com.geodesk.geom.Bounds;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.prep.PreparedGeometry;
import org.locationtech.jts.geom.prep.PreparedGeometryFactory;

public class SlowWithinFilter extends SpatialFilter
{
    private final PreparedGeometry prepared;

    public SlowWithinFilter(PreparedGeometry prepared)
    {
        this.prepared = prepared;
    }

    public SlowWithinFilter(Geometry geom)
    {
        this(PreparedGeometryFactory.prepare(geom));
    }

    @Override public boolean acceptGeometry(Geometry geom)
    {
        return geom != null && prepared.contains(geom);
    }

    @Override public Bounds bounds()
    {
        // TODO: if using Feature, get the bbox of feature
        //  but Envelope will be calculated anyway, so only minor savings
        return Box.fromEnvelope(prepared.getGeometry().getEnvelopeInternal());
    }
}
