package com.geodesk.io;

import com.clarisma.common.util.Log;
import org.junit.Test;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;

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
        Log.debug("%s", geom);
    }
}