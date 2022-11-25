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
import com.geodesk.feature.Relation;
import com.geodesk.feature.match.QueryException;
import com.geodesk.geom.Bounds;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.TopologyException;
import org.locationtech.jts.geom.prep.PreparedGeometry;
import org.locationtech.jts.geom.prep.PreparedGeometryFactory;

/**
 * A Filter that only accepts features whose geometry contains the
 * test geometry.
 *
 * TODO: This can be accelerated; for relations (which are most likely the
 *  "containers"), we don't need to assemble the full geometry; we can
 *  walk the list of member ways and:
 *  - first, test the bbox of the way: if it does not intersect the test
 *    geometry's bbox, the way cannot intersect, no need to test
 *  - Need to make sure all vertexes of test geom lie inside candidate
 *  - Candidate edges must not cross test geom edges
 *
 * - If test geometry is Polygonal, only accept areas and relations
 * - If test geometry is Lineal, don't accept nodes
 * - Can nodes contain points?
 */
public class ContainsFilter implements Filter
{
    private final Geometry testGeom;
    private final Box bounds;

    public ContainsFilter(Feature feature)
    {
        this(feature.toGeometry());
    }

    public ContainsFilter(Geometry geom)
    {
        testGeom = geom;
        bounds = Box.fromEnvelope(geom.getEnvelopeInternal());
    }

    public ContainsFilter(PreparedGeometry prepared)
    {
        this(prepared.getGeometry());
    }

    @Override public int strategy()
    {
        return
            FilterStrategy.USES_BBOX |
            FilterStrategy.RESTRICTS_TYPES;
    }

    private boolean containedBy(Geometry g)
    {
        return g.contains(testGeom);
    }

    @Override public boolean accept(Feature feature, Geometry geom)
    {
        Box featureBounds = feature.bounds();
        if(!featureBounds.contains(bounds)) return false;
        if(geom == null) geom = feature.toGeometry();

        // TODO: for non-area relations, pre-check dimension of member
        //  (e.g. lineal way can't contain area, no need to do full test)

        try
        {
            for (int i = 0; i < geom.getNumGeometries(); i++)
            {
                Geometry g = geom.getGeometryN(i);
                if (containedBy(g)) return true;
            }
        }
        catch(Exception ex)
        {
            Log.debug("Exception (%s) while checking containedBy(%s)", ex.getMessage(), feature);
            throw new QueryException("Query failed due to topology problem");
        }
        return false;
    }

    @Override public Bounds bounds() { return bounds; }
}

