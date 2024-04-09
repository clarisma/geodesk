/*
 * Copyright (c) Clarisma / GeoDesk contributors
 *
 * This source code is licensed under the Apache 2.0 license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.geodesk.util;

import com.geodesk.geom.Mercator;
import com.geodesk.feature.Feature;
import com.geodesk.feature.Relation;
import com.geodesk.feature.store.WayCoordinateSequence;
import org.locationtech.jts.geom.*;

import java.util.ArrayList;
import java.util.List;

public class GeometryBuilder extends GeometryFactory
{
    public static final GeometryBuilder instance = new GeometryBuilder();

    public Point createPointFromLonLat(double lon, double lat)
    {
        return createPoint(new Coordinate(Mercator.xFromLon(lon), Mercator.yFromLat(lat)));
    }

    public LineString createLineString(Feature way)
    {
        return createLineString(new WayCoordinateSequence(way.toXY()));
    }

    public MultiLineString createMultiLineString(Relation rel)
    {
        List<LineString> lines = new ArrayList<>();
        for (Feature way : rel.members().ways())
        {
            lines.add(GeometryBuilder.instance.createLineString(way));
        }
        return createMultiLineString(lines.toArray(new LineString[0]));
    }

}
