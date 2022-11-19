/*
 * Copyright (c) Clarisma / GeoDesk contributors
 *
 * This source code is licensed under the Apache 2.0 license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.geodesk.feature.filter;

import com.geodesk.core.Box;
import com.geodesk.feature.*;
import com.geodesk.feature.store.FeatureFlags;
import com.geodesk.feature.store.StoredRelation;
import com.geodesk.feature.store.StoredWay;
import com.geodesk.geom.Bounds;
import com.geodesk.geom.PointInPolygon;

// TODO

/**
 * Spatial predicate that accepts only features that contain a given point.
 *
 * TODO: Are points on boundary considered "inside"?
 *  (Definition of "within" says no.)
 */
public class ContainsPointFilter implements Filter
{
    private final Bounds bounds;
    private final int px;
    private final int py;

    @Override public Bounds bounds()
    {
        return bounds;
    }

    public ContainsPointFilter(int x, int y)
    {
        this.px = x;
        this.py = y;
        bounds = Box.atXY(x,y);
    }

    @Override public boolean accept(Feature feature)
    {
        // TODO: ways and nodes can also "contain" a point!
        if(!feature.isArea()) return false; // TODO: should set as pre-filter
        if(feature instanceof StoredWay way)
        {
            return PointInPolygon.testFast(way.iterXY(FeatureFlags.AREA_FLAG), px, py) != 0;
        }
        else if(feature instanceof StoredRelation rel)
        {
            return isInsideRelation(rel);
        }
        return px == feature.x() && py == feature.y();
    }

    private boolean isInsideRelation(StoredRelation rel)
    {
        int crossings = 0;
        for(Way member: rel.memberWays())
        {
            String role = member.role();
            if(!role.equals("outer") && !role.equals("inner")) continue;
            Box memberBox = member.bounds();
            if(py < memberBox.minY() || py > memberBox.maxY()) continue;
            crossings ^= PointInPolygon.testFast(
                ((StoredWay)member).iterXY(0), px, py);
        }
        return crossings != 0;
    }
}

