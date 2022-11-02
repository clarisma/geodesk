/*
 * Copyright (c) Clarisma / GeoDesk contributors
 *
 * This source code is licensed under the Apache 2.0 license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.geodesk.feature.polygon;

import com.geodesk.core.XY;
import com.geodesk.feature.Feature;
import com.geodesk.feature.Relation;
import com.geodesk.feature.Way;
import com.geodesk.feature.store.WayCoordinateSequence;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.Polygon;

public class PolygonBuilder
{
    /**
     * Creates an int array with X/Y coordinate pairs that represent the given Ring. The Ring must consist of properly
     * ordered Segments which are closed.
     *
     * @param ring the Ring for which to create the coordinate array
     * @return coordinates representing the linear ring.
     */
    private static int[] getRingCoordinates(Ring ring)
    {
        int[] coords = new int[ring.coordinateCount];
        Segment segment = ring.firstSegment;
        int[] segmentCoords = segment.coords;
        int i = segment.backward ? segmentCoords.length-2 : 0;
        coords[0] = segmentCoords[i];
        coords[1] = segmentCoords[i+1];
        int pos = 2;
        do
        {
            segmentCoords = segment.coords;
            int segmentCoordinateCount = segmentCoords.length;
            if (segment.backward)
            {
                for (i = segmentCoordinateCount - 4; i >= 0; i -= 2)
                {
                    coords[pos++] = segmentCoords[i];
                    coords[pos++] = segmentCoords[i + 1];
                }
            }
            else
            {
                System.arraycopy(segmentCoords, 2, coords, pos,
                    segmentCoordinateCount - 2);
                pos += segmentCoordinateCount - 2;
            }
            segment = segment.next;
        }
        while (segment != null);
        assert pos == coords.length;
        assert XY.isClosed(coords) :
            String.format("Ring#%d: Last point should be %d,%d instead of %d,%d",
                ring.number, coords[0], coords[1],
                coords[coords.length - 2], coords[coords.length - 1]);
        return coords;
    }

    private static LinearRing createLinearRing(GeometryFactory factory, Ring ring)
    {
        return factory.createLinearRing(new WayCoordinateSequence(
            getRingCoordinates(ring)));
    }

    private static boolean overlapsFollowing(Ring inner)
    {
        Ring other = inner.next;
        while(other != null)
        {
            if(inner.bbox.intersects(other.bbox)) return true;
            other = other.next;
        }
        return false;
    }

    private static boolean ringsOverlap(Ring firstInner)
    {
        for(Ring inner=firstInner; inner != null; inner=inner.next)
        {
            if(overlapsFollowing(inner)) return true;
        }
        return false;
    }

    private static LinearRing[] createHoles(GeometryFactory factory, Ring outer)
    {
        Ring inner = outer.firstInner;
        if(inner == null) return null;

        LinearRing[] holes;
        int innerCount = 0;
        for(;inner != null; inner=inner.next)
        {
            if(inner.bbox == null) inner.calculateBounds();
            innerCount++;
        }
        if(innerCount > 4 || ringsOverlap(outer.firstInner))
        {
            // TODO: no need to merge rings that don't overlap
            // create a list, add
            Polygon[] holePolygons = new Polygon[innerCount];
            int i = 0;
            for(inner = outer.firstInner; inner != null; inner=inner.next)
            {
                holePolygons[i++] = factory.createPolygon(
                    createLinearRing(factory, inner));
            }
            Geometry g = factory.createGeometryCollection(holePolygons);
            g = g.buffer(0);
            int mergedCount = g.getNumGeometries();
            holes = new LinearRing[mergedCount];
            for(i=0; i<mergedCount; i++)
            {
                holes[i] = ((Polygon)g.getGeometryN(i)).getExteriorRing();
            }
        }
        else
        {
            holes = new LinearRing[innerCount];
            int i = 0;
            for (inner = outer.firstInner; inner != null; inner = inner.next)
            {
                holes[i++] = createLinearRing(factory, inner);
            }
        }
        return holes;
    }


    private static Polygon createPolygon(GeometryFactory factory, Ring outer)
    {
        return factory.createPolygon(
            createLinearRing(factory, outer), createHoles(factory, outer));
    }


    private static Geometry createPolygonals(GeometryFactory factory, Ring rings)
    {
        if (rings.number == 1) return createPolygon(factory, rings);
        Polygon[] polygons = new Polygon[rings.number];
        int i = 0;
        do
        {
            polygons[i++] = createPolygon(factory, rings);
            rings = rings.next;
        }
        while (rings != null);
        assert i == polygons.length;
        return factory.createMultiPolygon(polygons);
    }


    public static Geometry build(GeometryFactory factory, Relation rel)
    {
        int outerSegmentCount = 0;
        int innerSegmentCount = 0;
        Segment outerSegments = null;
        Segment innerSegments = null;

        /*
        if(rel.id() == 224457)
        {
            log.debug("!!!");
        }
         */

        // TODO: use proper member filtering

        // segments are ordered in reverse

        for (Feature member : rel)
        {
            if (member instanceof Way way)
            {
                if (way.role().equals("outer"))
                {
                    outerSegments = new Segment(++outerSegmentCount, way, outerSegments);
                }
                else if (way.role().equals("inner"))
                {
                    innerSegments = new Segment(++innerSegmentCount, way, innerSegments);
                }
            }
        }

        if (outerSegments == null) return factory.createEmpty(2);
        // Ring outerRings = null; // RingBuilder.buildFast(outerSegments);
        Ring outerRings = null; // RingBuilder.buildFast(outerSegments);
        if (outerRings == null)
        {
            // log.debug("Building outer rings slowly for {}", rel);
            outerRings = RingBuilder.build(outerSegments);
            if(outerRings == null)
            {
                // log.warn("Failed to build outer rings for {}", rel);
                return factory.createEmpty(2);
            }
        }
        if (innerSegments != null)
        {
            // Ring innerRings = null; // RingBuilder.buildFast(innerSegments);
            Ring innerRings = null; // RingBuilder.buildFast(innerSegments);
            if (innerRings == null)
            {
                // log.debug("Building inner rings slowly for {}", rel);
                innerRings = RingBuilder.build(innerSegments);
                if(innerRings == null)
                {
                    // log.warn("Failed to build inner rings for {}", rel);
                }
            }
            if (innerRings != null)
            {
                if(outerRings.next == null)
                {
                    // assign all inner rings to the one only outer ring
                    outerRings.firstInner = innerRings;
                }
                else
                {
                    RingAssigner.assignRings(outerRings, innerRings);
                }
            }
        }

        // log.debug("Attempting to build {} ...", rel);
        // return buildGeometryFast(factory, outerSegments, innerSegments);
        return createPolygonals(factory, outerRings);
    }

}
