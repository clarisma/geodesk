package com.geodesk.util;

import com.geodesk.core.Mercator;
import org.locationtech.jts.geom.*;
import org.locationtech.jts.geom.prep.PreparedGeometry;
import org.locationtech.jts.geom.prep.PreparedPolygon;

public class GeometryBuilder extends GeometryFactory
{
    public Point createPointFromLonLat(double lon, double lat)
    {
        return createPoint(new Coordinate(Mercator.xFromLon(lon), Mercator.yFromLat(lat)));
    }
}
