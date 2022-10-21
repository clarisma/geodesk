package com.geodesk.io;

import com.clarisma.common.util.Log;
import org.junit.Assert;
import org.junit.Test;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Polygon;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import static org.junit.Assert.*;

public class PolyReaderTest
{
    @Test public void testRead() throws Exception
    {
        GeometryFactory factory = new GeometryFactory();
        BufferedReader in = new BufferedReader(
            new InputStreamReader(
                getClass().getResourceAsStream("/io/poly/bremen.poly")));
        PolyReader reader = new PolyReader(in, factory);
        Geometry geom = reader.read();

        // We should have read 2 polygons
        Assert.assertTrue(geom instanceof MultiPolygon);
        Assert.assertEquals(2, geom.getNumGeometries());

        // #1 should have 85 points (including end)
        Polygon p1 = (Polygon)geom.getGeometryN(0);
        Assert.assertEquals(85, p1.getExteriorRing().getCoordinateSequence().size());

        // #2 should have 85 points (including end)
        Polygon p2 = (Polygon)geom.getGeometryN(1);
        Assert.assertEquals(254, p2.getExteriorRing().getCoordinateSequence().size());

        in = new BufferedReader(
            new InputStreamReader(
                getClass().getResourceAsStream("/io/poly/holes.poly")));
        reader = new PolyReader(in, factory);
        geom = reader.read();

        Assert.assertTrue(geom instanceof MultiPolygon);
        Assert.assertEquals(2, geom.getNumGeometries());

        p1 = (Polygon)geom.getGeometryN(0);
        Assert.assertEquals(5, p1.getExteriorRing().getCoordinateSequence().size());
        Assert.assertEquals(2, p1.getNumInteriorRing());
        Assert.assertEquals(4, p1.getInteriorRingN(0).getCoordinateSequence().size());
        Assert.assertEquals(4, p1.getInteriorRingN(1).getCoordinateSequence().size());
        p2 = (Polygon)geom.getGeometryN(1);
        Assert.assertEquals(5, p2.getExteriorRing().getCoordinateSequence().size());
        Assert.assertEquals(1, p2.getNumInteriorRing());
        Assert.assertEquals(4, p2.getInteriorRingN(0).getCoordinateSequence().size());

    }
}