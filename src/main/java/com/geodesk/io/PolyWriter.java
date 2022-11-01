/*
 * Copyright (c) Clarisma / GeoDesk contributors
 *
 * This source code is licensed under the Apache 2.0 license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.geodesk.io;

import com.geodesk.util.CoordinateTransformer;
import org.locationtech.jts.geom.CoordinateSequence;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.Polygon;

import java.io.IOException;

public class PolyWriter
{
    private final Appendable out;
    private final CoordinateTransformer transformer;
    private int shellCount;
    private int holeCount;

    public PolyWriter(Appendable out, CoordinateTransformer transformer)
    {
        this.out = out;
        this.transformer = transformer;
    }

    public void write(String name, Geometry geom) throws IOException
    {
        out.append(name);
        out.append("\n");
        write(geom);
        out.append("END");
    }


    public void write(Geometry geom) throws IOException
    {
        for (int i=0; i<geom.getNumGeometries(); i++)
        {
            if(geom.getGeometryN(i) instanceof Polygon polygon)
            {
                write(polygon);
            }
        }
    }

    public void write(Polygon polygon) throws IOException
    {
        writeRing("area", ++shellCount, polygon.getExteriorRing());
        for(int i=0; i<polygon.getNumInteriorRing(); i++)
        {
            writeRing("!hole", ++holeCount, polygon.getInteriorRingN(i));
        }
    }

    private void writeRing(String name, int count, LinearRing ring) throws IOException
    {
        out.append(name);
        assert count != 0;  // we use 1-based counting for nicer display
        if(count > 1) out.append(Integer.toString(count));
        CoordinateSequence coords = ring.getCoordinateSequence();
        for(int i=0; i<coords.size(); i++)
        {
            out.append("\n\t");
            transformer.writeX(out, coords.getX(i));
            out.append("\t");
            transformer.writeY(out, coords.getY(i));
        }
        out.append("\nEND\n");
    }
}
