package com.geodesk.feature.filter;

import com.geodesk.core.Box;
import com.geodesk.core.Mercator;
import com.geodesk.core.XY;
import com.geodesk.feature.*;
import com.geodesk.feature.store.FeatureFlags;
import com.geodesk.feature.store.StoredWay;
import com.geodesk.geom.Bounds;
import com.geodesk.geom.PointInPolygon;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.operation.distance.DistanceOp;

import java.awt.geom.Line2D;
import java.awt.geom.Point2D;

// TODO: need a way to create geometries without reference
//  to a factory

// TODO: Don't use IndexedFacetDistance, calculates only distance to edges,
//  not useful for Polygons

public class PointDistanceFilter implements Filter
{
    private final Bounds bounds;
    private final int px;
    private final int py;
    private final double distanceSquared;

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
            return PointInPolygon.testFast(way.iterXY(0), px, py) != 0;
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
        for(Feature Member: rel)
        {
            if(accept(feature)) return true;
        }
        // TODO: point in polygon test
        return false;
    }
}
