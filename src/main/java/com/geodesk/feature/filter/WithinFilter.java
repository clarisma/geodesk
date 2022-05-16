package com.geodesk.feature.filter;

import com.geodesk.core.Box;
import com.geodesk.geom.Bounds;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.prep.PreparedGeometry;

public class WithinFilter extends SpatialFilter
{
    private final PreparedGeometry prepared;

    public WithinFilter(PreparedGeometry prepared)
    {
        this.prepared = prepared;
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
