package com.geodesk.feature.store;

import com.geodesk.core.XY;
import com.geodesk.feature.Feature;
import com.geodesk.feature.Relation;
import com.geodesk.feature.Way;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.locationtech.jts.geom.*;

import java.util.HashMap;

public class PolygonBuilder_old
{
    public static final Logger log = LogManager.getLogger();

    private static class Segment
    {
        final int[] coords;
        Segment next;
        boolean backward;
        // TODO: boolean claimed

        Segment(Way way, Segment next)
        {
            coords = way.toXY();
            this.next = next;
        }
    }

    private static class Ring
    {
        int number;
        int coordinateCount;
        Segment firstSegment;
        Ring firstInner;
        Ring next;
    }

    /**
     * Turns a series of Segments into a series of Rings, which works only if
     * the Segments are ordered such that neighboring segments connect and
     * form valid linear rings.
     *
     * @param segment one or more Segments
     * @return one or more Rings, or null if the segments are not ordered properly
     */
    // TODO: stitch chain of Segments back together in case this approach fails,
    //  so the slow-path algo has the full list of Segments
    private static Ring buildRingsFast(Segment segment)
    {
        int ringCount = 0;
        Ring rings = null;
        Ring currentRing = null;
        int firstX = 0;
        int firstY = 0;
        int lastX = 0;
        int lastY = 0;
        while (segment != null)
        {
            int[] coords = segment.coords;
            int segmentCoordinateCount = coords.length;
            int segmentFirstX = coords[0];
            int segmentFirstY = coords[1];
            int segmentLastX = coords[segmentCoordinateCount - 2];
            int segmentLastY = coords[segmentCoordinateCount - 1];
            if (currentRing == null)
            {
                ringCount++;
                currentRing = new Ring();
                currentRing.number = ringCount;
                currentRing.firstSegment = segment;
                currentRing.coordinateCount = segmentCoordinateCount;
                currentRing.next = rings;
                rings = currentRing;
                firstX = segmentFirstX;
                firstY = segmentFirstY;
            }
            else
            {
                currentRing.coordinateCount += segmentCoordinateCount - 2;
                if (segmentLastX == lastX && segmentLastY == lastY)
                {
                    segment.backward = true;
                    segmentLastX = segmentFirstX;
                    segmentLastY = segmentFirstY;
                }
                else if (segmentFirstX != lastX || segmentFirstY != lastY)
                {
                    // segment doesn't connect to previous -> failed
                    // TODO: restore chain of Segments
                    return null;
                }
            }
            Segment nextSegment = segment.next;
            if (segmentLastX == firstX && segmentLastY == firstY)
            {
                // Any ring with less than 4 points is defective
                if (currentRing.coordinateCount < 8) return null;
                segment.next = null;
                currentRing = null;
            }
            else
            {
                lastX = segmentLastX;
                lastY = segmentLastY;
            }
            segment = nextSegment;
        }
        // TODO: restore chain of Segments
        // If the current ring is still open, ring-building failed
        return currentRing == null ? rings : null;
    }

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
        assert !segment.backward : "First segment must always be forward";
        int[] segmentCoords = segment.coords;
        coords[0] = segmentCoords[0];
        coords[1] = segmentCoords[1];
        int pos = 2;
        do
        {
            segmentCoords = segment.coords;
            int segmentCoordinateCount = segmentCoords.length;
            if (segment.backward)
            {
                for (int i = segmentCoordinateCount - 4; i >= 0; i -= 2)
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
            String.format("Last point should be %d,%d instead of %d,%d",
                coords[0], coords[1], coords[coords.length - 2], coords[coords.length - 1]);
        return coords;
    }

    private static LinearRing createLinearRing(GeometryFactory factory, Ring ring)
    {
        return factory.createLinearRing(new WayCoordinateSequence(
            getRingCoordinates(ring)));
    }


    private static Polygon createPolygon(GeometryFactory factory, Ring outer)
    {
        LinearRing[] holes = null;
        Ring inner = outer.firstInner;
        if (inner != null)
        {
            holes = new LinearRing[inner.number];
            int i = 0;
            do
            {
                holes[i++] = createLinearRing(factory, inner);
                inner = inner.next;
            }
            while (inner != null);
            assert i == holes.length;
        }
        return factory.createPolygon(createLinearRing(factory, outer), holes);
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

    public static Geometry buildGeometryFast(GeometryFactory factory, Segment outerSegments, Segment innerSegments)
    {
        Ring outerRings = buildRingsFast(outerSegments);
        if (outerRings == null) return null;
        Ring innerRings = null;
        if (innerSegments != null)
        {
            innerRings = buildRingsFast(innerSegments);
            if (innerRings == null) return null;
        }

        /*
        log.debug("  {} outer, {} inner", outerRings.number,
            innerRings==null ? 0 : innerRings.number);

         */

        if (innerRings != null)
        {
            if (outerRings.number > 1)
            {
                // TODO: assign polygons
                return null;
            }
            else
            {
                outerRings.firstInner = innerRings;
            }
        }
        return createPolygonals(factory, outerRings);
    }

    public static Geometry buildGeometryFast(GeometryFactory factory, Relation rel)
    {
        Segment outerSegments = null;
        Segment innerSegments = null;

        // TODO: use proper member filtering

        // segments are ordered in reverse

        for (Feature member : rel)
        {
            if (member instanceof Way way)
            {
                if (way.role().equals("outer"))
                {
                    outerSegments = new Segment(way, outerSegments);
                }
                else if (way.role().equals("inner"))
                {
                    innerSegments = new Segment(way, innerSegments);
                }
            }
        }
        if (outerSegments == null) return null;
        // log.debug("Attempting to build {} ...", rel);
        return buildGeometryFast(factory, outerSegments, innerSegments);
    }

    private static class Candidate
    {
        Segment segment;
        int x,y;
        boolean backward;
        Candidate next;
    }

    private static Ring buildRingsSlow(Segment segment)
    {
        return null;
    }
}