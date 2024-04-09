/*
 * Copyright (c) Clarisma / GeoDesk contributors
 *
 * This source code is licensed under the Apache 2.0 license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.geodesk.geom;

import org.locationtech.jts.geom.Coordinate;

/**
 * Methods for working with coordinates that are represented as a single long
 * value. Y coordinate is stored in the upper 32 bits, X in the lower.
 */
public class XY
{
    /**
     * Creates a long coordinate based on the given X and Y.
     *
     * @param x
     * @param y
     * @return
     */
    public static long of(int x, int y)
    {
        return ((long)y << 32) | ((long)x & 0xffff_ffffL);
        // TODO: was return ((long)y << 32) | (x & 0xffffffffl);
    }

    /**
     * Creates a long coordinate based on the given JTS {@link Coordinate}.
     * Coordinates must be within the numeric range of a signed 32-bit integer
     * and are rounded.
     *
     * @param c
     * @return
     */
    public static long of(Coordinate c)
    {
        return of((int)Math.round(c.x), (int)Math.round(c.y));
    }

    /**
     * Turns a long coordinate into a JTS {@link Coordinate}.
     *
     * @param xy
     * @return
     */
    public static Coordinate toCoordinate(long xy)
    {
        return new Coordinate(x(xy), y(xy));
    }

    public static Coordinate[] toCoordinates(long[] xy)
    {
        Coordinate[] coords = new Coordinate[xy.length];
        for(int i=0; i<xy.length; i++)
        {
            coords[i] = toCoordinate(xy[i]);
        }
        return coords;
    }

    /**
     * Returns the X coordinate of the given long coordinate.
     *
     * @param coord
     * @return
     */
    public static int x(long coord)
    {
        return (int)coord;		// TODO: check
        // return (int)(coord & 0xffff_ffffL);
    }

    /**
     * Returns the Y coordinate of the given long coordinate.
     *
     * @param coord
     * @return
     */
    public static int y(long coord)
    {
        return (int)(coord >> 32);
    }

    /*
    public static long ofLonLat(double lon, double lat)
    {
        return of((int)Mercator.xFromLon(lon), (int)Mercator.yFromLat(lat));
        // TODO: rounding needed?
    }
    */

    /**
     * Returns an array of LatLongs as an array of x/y coordinate
     * pairs.
     *
     * @param coords
     * @return
     */
    public static int[] of(long[] coords)
    {
        int[] xy = new int[coords.length * 2];
        for(int i=0; i<coords.length; i++)
        {
            xy[i*2] = x(coords[i]);
            xy[i*2+1] = y(coords[i]);
        }
        return xy;
    }

    /**
     * Checks whether a given set of coordinates represents a linear ring.
     *
     * @param coords    array of X/Y coordinates
     * @return
     */
    public static boolean isClosed(int[] coords)
    {
        int len = coords.length;
        if(len < 6) return false;
        return coords[0] == coords[len-2] && coords[1] == coords[len-1];
    }

    public static boolean contains(int[] coords, int x, int y)
    {
        int len = coords.length;
        for(int i=0; i<len; i+=2)
        {
            if (coords[i] == x && coords[i + 1] == y) return true;
        }
        return false;
    }

    /**
     * Fast but non-robust method to check how many times a line from a point
     * intersects the given segments, using the ray-casting algorithm
     * (https://en.wikipedia.org/wiki/Point_in_polygon#Ray_casting_algorithm).
     * This is suitable for a point-in-polygon test, but be aware that points
     * that are vertexes or are located on the edge (or very close to it)
     * may or may not be considered "inside."
     *
     * This test can be applied to multiple line strings of the polygon
     * in succession. In that case, the result of each test must be XOR'd
     * with the previous results.
     *
     * The winding order is irrelevant, but the result is undefined if
     * the segments are self-intersecting.
     *
     * @param coords    pairs of x/y coordinates that form a polygon
     *                  or segment thereof
     * @param cx        the X-coordinate to test
     * @param cy        the Y-coordinate to test
     * @return          0 if even number of edges are crossed ("not inside")
     *                  1 if odd number of edges are crossed ("inside")
     */
    public static int castRay(int[] coords, double cx, double cy)
    {
        int odd = 0;
        int len = coords.length - 2;
        for (int i=0; i<len; i+=2)
        {
            double x1 = coords[i];
            double y1 = coords[i+1];
            double x2 = coords[i+2];
            double y2 = coords[i+3];

            if (((y1 <= cy) && (y2 > cy))     // upward crossing
                || ((y1 > cy) && (y2 <= cy))) // downward crossing
            {
                // compute edge-ray intersect x-coordinate
                double vt = (cy  - y1) / (y2 - y1);
                if (cx <  x1 + vt * (x2 - x1)) // P.x < intersect
                {
                    odd ^= 1;
                }
            }
        }
        return odd;
    }


    /*
    public static long fromLatLon(String s)
    {
        int n = s.indexOf(',');
        if (n>0)
        {
            double lat = Double.parseDouble(s.substring(0, n).trim());
            double lon = Double.parseDouble(s.substring(n+1).trim());
            return fromXY((int)(lon * 10_000_000),
                (int)(lat * 10_000_000));  // TODO: rounding?
        }
        throw new RuntimeException(String.format(
            "expected \"<lat>,<lon>\" instead of \"%s\"", s));
    }
     */
}
