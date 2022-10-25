package com.geodesk.io;

import com.clarisma.common.util.Log;
import com.geodesk.util.CoordinateTransformer;
import org.junit.Assert;
import org.junit.Test;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;

import java.io.*;

import static org.junit.Assert.*;

public class PolyWriterTest
{
    /**
     * Reads a poly file, writes it and reads it again, to make sure we get
     * back the same geometry.
     *
     * @throws IOException
     */
    @Test public void testWrite() throws IOException
    {
        GeometryFactory factory = new GeometryFactory();
        BufferedReader in = new BufferedReader(
            new InputStreamReader(
                getClass().getResourceAsStream("/io/poly/bremen.poly")));
        PolyReader reader = new PolyReader(in, factory);
        Geometry g1 = reader.read();

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PrintStream out2 = new PrintStream(out);
        PolyWriter writer = new PolyWriter(out2,
            new CoordinateTransformer(6));
        writer.write("test", g1);
        String result = out.toString();
        // Log.debug(result);

        reader = new PolyReader(new BufferedReader(new StringReader(result)), factory);
        Geometry g2 = reader.read();
        Assert.assertEquals(g1, g2);
    }
}