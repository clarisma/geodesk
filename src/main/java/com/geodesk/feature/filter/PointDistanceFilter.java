/*
 * Copyright (c) Clarisma / GeoDesk contributors
 *
 * This source code is licensed under the Apache 2.0 license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.geodesk.feature.filter;

import com.geodesk.geom.Box;
import com.geodesk.geom.Mercator;
import com.geodesk.geom.XY;
import com.geodesk.feature.*;
import com.geodesk.feature.store.FeatureFlags;
import com.geodesk.feature.store.StoredWay;
import com.geodesk.geom.Bounds;
import com.geodesk.geom.PointInPolygon;

import java.awt.geom.Line2D;
import java.awt.geom.Point2D;

/**
 * Spatial predicate that accepts only features that are within a given
 * distance from a point.
 */
// TODO: check if we need to increase bbox size to account for distortion
//  intriduced by the Mercator projection
public class PointDistanceFilter implements Filter
{
    private final Bounds bounds;
    private final int px;
    private final int py;
    private final double distanceSquared;

    @Override public Bounds bounds()
    {
        return bounds;
    }

    private boolean segmentsWithinDistance(StoredWay way, int areaFlag)
    {
        StoredWay.XYIterator iter = way.iterXY(areaFlag);
        long xy = iter.nextXY();
        double x1 = XY.x(xy);
        double y1 = XY.y(xy);
        while (iter.hasNext())
        {
            xy = iter.nextXY();
            double x2 = XY.x(xy);
            double y2 = XY.y(xy);
            if (Line2D.ptSegDistSq(x1, y1, x2, y2, px, py) < distanceSquared) return true;
            x1 = x2;
            y1 = y2;
        }
        return false;
    }

    private boolean isWithinDistance(StoredWay way)
    {
        if (way.isArea())
        {
            if (segmentsWithinDistance(way, FeatureFlags.AREA_FLAG)) return true;
            // The distance of a point that lies within a polygon is zero;
            // we need to perform p-in-p check because the edges themselves
            // may be far away from the comparison point
            // TODO: check bbox first?
            return PointInPolygon.testFast(way.iterXY(FeatureFlags.AREA_FLAG), px, py) != 0;
        }
        return segmentsWithinDistance(way, 0);
    }

    public PointDistanceFilter(double distance, int x, int y)
    {
        this.px = x;
        this.py = y;
        double d = Mercator.deltaFromMeters(distance, y);
        bounds = Box.impsAroundXY((int)Math.ceil(d), x, y);
        distanceSquared = d * d;
    }

    @Override public boolean accept(Feature feature)
    {
        if(feature instanceof Way)
        {
            assert feature instanceof StoredWay;
            return isWithinDistance((StoredWay)feature);
        }
        if(feature instanceof Node)
        {
            return Point2D.distanceSq(feature.x(),feature.y(),px,py) < distanceSquared;
        }
        assert feature instanceof Relation;
        Relation rel = (Relation)feature;
        if(rel.isArea())
        {
            // measure distance to the ways that define shell and holes, and
            // also perform point in polygon test
            int odd = 0;
            for(Feature member: rel.members().ways())   // TODO: use role filter
            {
                String role = member.role();
                if(role.equals("outer") || role.equals("inner"))
                {
                    StoredWay way = (StoredWay)member;
                    int flags = way.flags();
                    if (segmentsWithinDistance(way, flags)) return true;
                    odd ^= PointInPolygon.testFast(
                        ((StoredWay)member).iterXY(flags), px, py);
                }
            }
            return odd != 0;
        }
        else
        {
            for(Feature member: rel)
            {
                if(accept(member)) return true;
            }
        }
        return false;
    }
}
