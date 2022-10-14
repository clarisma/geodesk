package com.geodesk.feature.filter;

import com.geodesk.core.Box;
import com.geodesk.feature.Feature;
import com.geodesk.feature.Relation;
import com.geodesk.feature.Way;
import com.geodesk.geom.Bounds;
import com.geodesk.util.GeometryBuilder;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.prep.PreparedGeometry;
import org.locationtech.jts.geom.prep.PreparedGeometryFactory;

import java.util.ArrayList;
import java.util.List;

public class CrossesFilter extends SpatialFilter
{
    private final PreparedGeometry prepared;

    public CrossesFilter(PreparedGeometry prepared)
    {
        this.prepared = prepared;
    }

    public CrossesFilter(Geometry geom)
    {
        this(PreparedGeometryFactory.prepare(geom));
    }


    public CrossesFilter(Feature f)
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
