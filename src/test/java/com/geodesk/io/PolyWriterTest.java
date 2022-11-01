package com.geodesk.io;

import com.clarisma.common.store.BlobStoreTest;
import com.clarisma.common.util.Log;
import com.geodesk.util.CoordinateTransformer;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.*;

public class PolyWriterTest
{
    private Path outputPath;

    @Before public void setUp() throws IOException
    {
        outputPath = Files.createTempFile("poly-test", ".poly");
    }

    @After public void tearDown() throws IOException
    {
        Files.deleteIfExists(outputPath);
    }


    /**
     * Reads a poly file, writes it and reads it again, to make sure we get
     * back the same geometry.
     *
     * @throws IOException
     */
    @Test public void testWrite() throws IOException
    {
        GeometryFactory factory = new GeometryFactory();
        CoordinateTransformer transformer = new CoordinateTransformer(6);
        BufferedReader in = new BufferedReader(
            new InputStreamReader(
                getClass().getResourceAsStream("/io/poly/bremen.poly")));
        PolyReader reader = new PolyReader(in, factory, transformer);
        Geometry g1 = reader.read();

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PrintStream out2 = new PrintStream(out);
        PolyWriter writer = new PolyWriter(out2, transformer);
        writer.write(outputPath.toString(), g1);
        String result = out.toString();
        // Log.debug(result);

        reader = new PolyReader(new BufferedReader(new StringReader(result)),
            factory, transformer);
        Geometry g2 = reader.read();
        Assert.assertEquals(g1, g2);
    }
}