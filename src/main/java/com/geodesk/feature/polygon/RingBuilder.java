/*
 * Copyright (c) Clarisma / GeoDesk contributors
 *
 * This source code is licensed under the Apache 2.0 license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.geodesk.feature.polygon;

import com.geodesk.geom.XY;

public class RingBuilder
{
    /**
     * Turns a series of Segments into a series of Rings, which works only if
     * the Segments are ordered such that neighboring segments connect and
     * form valid linear rings.
     *
     * @param segment one or more Segments
     * @return one or more Rings, or null if the segments are not ordered properly
     */
    public static Ring buildFast(Segment segment)
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
                rings = new Ring(++ringCount, segment, segmentCoordinateCount, rings);
                currentRing = rings;
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
                    break;
                }
            }
            Segment nextSegment = segment.next;
            if (segmentLastX == firstX && segmentLastY == firstY)
            {
                // Any ring with less than 4 points is defective
                if (currentRing.coordinateCount < 8) break;
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
        if(currentRing == null) return rings;
        relinkSegments(rings);
        return null;
    }

    private static void relinkSegments(Ring ring)
    {
        Segment firstSegment = ring.firstSegment;
        ring = ring.next;
        while(ring != null)
        {
            Segment segment = ring.firstSegment;
            for(;;)
            {
                if (segment.next == null)
                {
                    segment.next = firstSegment;
                    firstSegment = ring.firstSegment;
                    break;
                }
                segment = segment.next;
            }
            ring = ring.next;
        }
    }

    private static class Candidate
    {
        final Segment segment;
        final Candidate next;

        Candidate(Segment segment, Candidate next)
        {
            this.segment = segment;
            this.next = next;
        }
    }



    /**
     * Finds the first segment which:
     *  - has not been assigned;
     *  - is not the current segment; and
     *  - whose start or end point matches the given coordinates, which
     *    are the start point of the current segment
     *
     * @param table
     * @param current
     * @return
     */
    private static Segment findNeighbor(Candidate[] table, Segment current)
    {
        int[] coords = current.coords;
        int x = coords[current.backward ? coords.length-2 : 0];
        int y = coords[current.backward ? coords.length-1 : 1];
        Candidate c = table[(x ^ y) & (table.length-1)];
        while(c != null)
        {
            Segment segment = c.segment;
            if(segment.status < Segment.ASSIGNED && segment != current)
            {
                coords = segment.coords;
                if(coords[0] == x && coords[1] == y)
                {
                    // if(segment.status == Segment.UNASSIGNED)
                    segment.backward = true;
                    return segment;
                }
                if(coords[coords.length-2] == x && coords[coords.length-1] == y)
                {
                    // if(segment.status == Segment.UNASSIGNED)
                    segment.backward = false;
                    return segment;
                }
            }
            c = c.next;
        }
        return null;
    }

    private static int markAndCount(Segment segment)
    {
        int coordinateCount = segment.coords.length;
        for(;;)
        {
            segment.status = Segment.ASSIGNED;
            segment = segment.next;
            if(segment == null) return coordinateCount;
            coordinateCount += segment.coords.length-2;
        }
    }

    private static void addToTable(Candidate[] table, Segment segment, int x, int y)
    {
        int slot = (x ^ y) & (table.length-1);
        table[slot] = new Candidate(segment, table[slot]);
    }

    /*
    private static Candidate[] buildHashTable(Segment[] segments)
    {
        int tableSize = (-1 >>> Integer.numberOfLeadingZeros(
            segments.length - 1)) + 1;
        Candidate[] table = new Candidate[tableSize];
        for(int i=0; i<segments.length; i++)
        {
            Segment segment = segments[i];
            int[] coords = segment.coords;
            addToTable(table, segment, coords[0], coords[1]);
            addToTable(table, segment, coords[coords.length-2], coords[coords.length-1]);
        }
        return table;
    }
     */


    /**
     * Attempts to assemble closed Rings from a linked list of Segments,
     * which can be in any order. Segments are marked ASSIGNED or DANGLING.
     *
     * @param   firstSegment  first Segment in a linked list
     * @return  a linked list of Ring objects, or null if none were found
     */
    public static Ring build(Segment firstSegment)
    {
        if(firstSegment.number == 1)
        {
            int[] coords = firstSegment.coords;
            if(XY.isClosed(coords))
            {
                firstSegment.status = Segment.ASSIGNED;
                return new Ring(1, firstSegment, coords.length, null);
            }
            firstSegment.status = Segment.DANGLING;
            return null;
        }
        Segment[] segments = new Segment[firstSegment.number];
        int tableSize = (-1 >>> Integer.numberOfLeadingZeros(
            segments.length - 1)) + 1;
        Candidate[] table = new Candidate[tableSize];
        assert tableSize > 0: "Bad tableSize for " + segments.length + " segments";
        int i=0;
        Segment segment = firstSegment;
        while(segment != null)
        {
            segments[i++] = segment;
            int[] coords = segment.coords;
            addToTable(table, segment, coords[0], coords[1]);
            addToTable(table, segment, coords[coords.length-2], coords[coords.length-1]);
            segment = segment.next;
        }
        assert i == segments.length;

        int ringCount = 0;
        Ring rings = null;

        for(i=0; i<segments.length; i++)
        {
            segment = segments[i];
            if (segment.status != Segment.UNASSIGNED) continue;
            segment.backward = false;
            segment.next = null;
            int[] coords = segment.coords;
            if (XY.isClosed(coords))
            {
                segment.status = Segment.ASSIGNED;
                rings = new Ring(++ringCount, segment, coords.length, rings);
                continue;
            }
            segment.status = Segment.TENTATIVE;
            for (; ; )
            {
                Segment candidate = findNeighbor(table, segment);
                if (candidate == null)
                {
                    segment.status = Segment.DANGLING;
                    segment = segment.next;
                }
                else if (candidate.status == Segment.TENTATIVE)
                {
                    Segment nextSegment = candidate.next;
                    candidate.next = null;
                    rings = new Ring(++ringCount, segment,
                        markAndCount(segment),rings);
                    /*
                    log.debug("Created Ring#{}: starts with {}, {} ({}) and ends with {}, {} ({})- next = {}",
                        ringCount, segment.coords[0],segment.coords[1], segment.backward,
                        candidate.coords[candidate.coords.length-2],
                        candidate.coords[candidate.coords.length-1], candidate.backward,
                        nextSegment);
                     */
                    segment = nextSegment;
                }
                else if(XY.isClosed(candidate.coords))
                {
                    candidate.status = Segment.ASSIGNED;
                    candidate.next = null;
                    rings = new Ring(++ringCount, candidate,
                        candidate.coords.length, rings);
                    continue;
                }
                else
                {
                    candidate.status = Segment.TENTATIVE;
                    candidate.next = segment;
                    segment = candidate;
                    continue;
                }
                if (segment == null) break;
            }
        }
        return rings;
    }
}
