/*
 * Copyright (c) Clarisma / GeoDesk contributors
 *
 * This source code is licensed under the Apache 2.0 license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.geodesk.feature.polygon;

import com.geodesk.geom.XY;
import com.geodesk.geom.Box;
import com.geodesk.geom.Bounds;

public class Ring
{
    final int number;       // TODO: remove? No!
    final Segment firstSegment;
    int coordinateCount;
    Box bbox;
    Ring firstInner;
    Ring next;

    public Ring(int number, Segment firstSegment, int coordinateCount, Ring next)
    {
        this.number = number;
        this.firstSegment = firstSegment;
        this.coordinateCount = coordinateCount;
        this.next = next;
    }

    public void calculateBounds()
    {
        Box b = new Box();
        for(Segment segment = firstSegment; segment != null; segment = segment.next)
        {
            b.expandToInclude(segment.way.bounds()); // TODO: use way itself as Bounds
        }
        bbox = b;
    }

    /**
     * Checks whether the given point is a vertex in this Ring
     *
     * @param x
     * @param y
     * @return true if x/y represent a vertex of this Ring
     */
    public boolean containsVertex(int x, int y)
    {
        for(Segment segment = firstSegment; segment != null; segment = segment.next)
        {
            Bounds b = segment.way.bounds();
            if(!b.contains(x,y)) continue;
            // We could skip one of the ending coordinates of each segment,
            // but since the segment may be backwards, we'll just check all
            // for the sake of simplicity
            if(XY.contains(segment.coords, x, y)) return true;
        }
        return false;
    }

    /**
     * Checks whether the given point (which must not be a vertex) lies
     * within this Ring.
     *
     * @param x
     * @param y
     * @return
     */
    private boolean containsPoint(int x, int y)
    {
        int odd = 0;
        for(Segment segment = firstSegment; segment != null; segment = segment.next)
        {
            Bounds b = segment.way.bounds();
            if(y >= b.minY() && y <= b.maxY())
            {
                odd ^= XY.castRay(segment.coords, x, y);
            }
        }
        return odd != 0;
    }

    /**
     * Checks whether this Ring contains another Ring, using a point-in-polygon
     * test of a non-vertex point of the other Ring. This method assumes that
     * the caller has already checked if this Ring's bounding box contains the
     * other Ring's bounding box (a much cheaper test to rule out whether it
     * is even possible for this Ring to contain the other).
     *
     * @param other     the potential inner Ring
     * @return true if this Ring contains the other
     */
    public boolean contains(Ring other)
    {
        int[] otherCoords = other.firstSegment.coords;
        int x = otherCoords[0];
        int y = otherCoords[1];
        if(!containsVertex(x,y)) return containsPoint(x,y);

        // If another ring shares a vertex with this ring, this does not
        // necessarily mean that it is an inner ring. Further, the fast
        // ray-casting point-in-polygon test does not work for vertexes,
        // anyway. That's why we want to perform the test with a non-vertex
        // point. Technically, we should scan all of the potential inner ring's
        // points to find a non-vertex point. However, since two shared vertexes
        // in a row would mean that the geometry created by the two rings is
        // invalid (two rings may touch in one or more points, but may not have
        // a shared edge), we only check whether the first point is a non-vertex,
        // and take the second point even though it may be a vertex as well.

        x = otherCoords[2];
        y = otherCoords[3];
        return containsPoint(x,y);
    }


    public void addInner(Ring inner)
    {
        inner.next = firstInner;
        firstInner = inner;
    }
}
