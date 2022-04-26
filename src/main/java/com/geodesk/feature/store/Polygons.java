package com.geodesk.feature.store;

import com.geodesk.feature.Feature;
import com.geodesk.feature.Way;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.operation.polygonize.Polygonizer;

import java.util.ArrayList;
import java.util.List;

// TODO: JTS 18.2 now has a GeometryFixer

public class Polygons
{
    public static final Logger log = LogManager.getLogger();
    /*
    private static class Ring
    {
        LinearRing ring;
        int assignedTo;
        int holeCount;
        LinearRing[] holes;

        Ring(LinearRing ring) { this.ring = ring; }
    }
    */

    private static Geometry asSingleGeometry(Geometry geom)
    {
        if(geom.getNumGeometries() != 1) return geom;
        /*
        if(geom.getNumGeometries() != 1)
        {
            throw new RuntimeException(String.format(
                "Expected 1 geometry instead of %d: %s",
                geom.getNumGeometries(), geom.toText()));
        }
         */
        return geom.getGeometryN(0);
    }

    // TODO: if difference() creates anything other than a polygon,
    //  we've got an invalid shape
    public static Geometry build(GeometryFactory factory, Iterable<Feature> members)
    {
        List<Geometry> outerList = new ArrayList<>();
        Polygonizer outer = new Polygonizer(false);
        Polygonizer inner = null;
        List<Geometry> innerList = null;

        for(Feature member: members)
        {
            if(!(member instanceof Way)) continue;
            // TODO: use global string table constants
            String role = member.role();
            if("outer".equals(role) || role.isEmpty())
            {
                outerList.add(member.toGeometry());
            }
            else if("inner".equals(role))
            {
                /*
                if(inner==null) inner = new Polygonizer(true);
                inner.add(member.geometry());
                 */
                if(innerList==null) innerList = new ArrayList<>();
                innerList.add(member.toGeometry());
            }
        }

        Geometry combined = factory.createGeometryCollection(
            outerList.toArray(new Geometry[0]));
        // combined = combined.buffer(0);
        combined = combined.union();
        outer.add(combined);

        // outer.add(outerList);

        Geometry polygons;
        try
        {
            polygons = outer.getGeometry();
            if(!polygons.isValid()) polygons = polygons.buffer(0);
            polygons = asSingleGeometry(polygons);
        }
        catch(Exception ex)
        {
            log.error("Failed to polygonize outer rings for {}: {}",
                members, ex.getMessage());
            return null;
        }

        if(innerList != null)
        {
            inner = new Polygonizer(false);
            combined = factory.createGeometryCollection(
                innerList.toArray(new Geometry[0]));
            // combined = combined.buffer(0);
            combined = combined.union();
            inner.add(combined);
            // inner.add(innerList);
        }

        if(!polygons.isEmpty() && inner != null)
        {
            Geometry holes;
            try
            {
                holes = inner.getGeometry();
                if(!holes.isValid()) holes = holes.buffer(0);
                holes = asSingleGeometry(holes);
            }
            catch(Exception ex)
            {
                log.error("Failed to polygonize inner rings for {}: {}",
                    members, ex.getMessage());
                ex.printStackTrace();
                log.debug("  {} inner geometries:", innerList.size());
                if(innerList.size() < 5)
                {
                    for (Geometry g : innerList)
                    {
                        log.debug("- {}", g);
                    }
                }
                return null;
            }
            int shellCount = polygons.getNumGeometries();
            if(shellCount > 1)
            {
                Geometry[] shells = new Geometry[shellCount];
                Envelope[] shellEnvelopes = new Envelope[shellCount];
                for (int i = 0; i < shellCount; i++)
                {
                    shells[i] = polygons.getGeometryN(i);
                    shellEnvelopes[i] = shells[i].getEnvelopeInternal();
                }
                for (int i = 0; i < holes.getNumGeometries(); i++)
                {
                    Geometry hole = holes.getGeometryN(i);
                    Envelope holeEnvelope = hole.getEnvelopeInternal();
                    for (int i2 = 0; i2 < shellCount; i2++)
                    {
                        if (holeEnvelope.intersects(shellEnvelopes[i2]))
                        {
                            shells[i2] = asSingleGeometry(shells[i2].difference(hole));
                        }
                    }
                }
                polygons = factory.createGeometryCollection(shells);
            }
            else
            {
                for (int i = 0; i < holes.getNumGeometries(); i++)
                {
                    try
                    {
                        polygons = asSingleGeometry(polygons.difference(
                            holes.getGeometryN(i)));
                    }
                    catch(Exception ex)
                    {
                        log.debug("Possibly bad shell: {}", polygons.toText());
                        log.debug("Possibly bad hole: {}", holes.getGeometryN(i).toText());
                        throw ex;
                    }
                }
            }
            // log.debug("{}", polygons.toText());
        }

        return polygons;
    }
}
