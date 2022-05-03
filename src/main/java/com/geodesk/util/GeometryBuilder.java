package com.geodesk.util;

import com.geodesk.core.Mercator;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;

public class GeometryBuilder extends GeometryFactory
{
    public Point createPointFromLonLat(double lon, double lat)
    {
        return createPoint(new Coordinate(Mercator.xFromLon(lon), Mercator.yFromLat(lat)));
    }
}
