/*
 * Copyright (c) Clarisma / GeoDesk contributors
 *
 * This source code is licensed under the Apache 2.0 license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.geodesk.geom;

import com.geodesk.feature.store.StoredWay;

/// @hidden
public class PointInPolygon
{
    /**
     * Fast but non-robust point-in-polygon test using the ray-crossing method.
     * Points located on a polygon edge (or very close to it) may or may not
     * be considered "inside." Vertexes, however, are always identified correctly.
     *
     * This test can be applied to multiple line strings of the polygon
     * in succession. In that case, the result of each test must be XOR'd
     * with the previous results.
     *
     * @param coords    pairs of x/y coordinates that form a polygon
     *                  or part thereof
     * @param cx        the X-coordinate to test
     * @param cy        the Y-coordinate to test
     * @return          0 if even number of edges are crossed ("not inside")
     *                  1 if odd number of edges are crossed ("inside")
     *                  -1 if the candidate point corresponds to a vertex
     */
    // TODO: get rid of special test for vertex? can check separately
    // Test if there is a speed difference
    public static int testFast(int[] coords, double cx, double cy)
    {
        int odd = 0;
        int len = coords.length - 2;
        for (int i=0; i<len; i+=2)
        {
            double x1 = coords[i];
            double y1 = coords[i+1];
            double x2 = coords[i+2];
            double y2 = coords[i+3];

            // TODO: Special test for vertex?
            // Almost 20% speedup if we don't do it
            // if(cx==x1 && cy==y1) return -1;

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


    /**
     * Fast but non-robust point-in-polygon test using the ray-crossing method.
     * Points located on a polygon edge (or very close to it) may or may not
     * be considered "inside." Vertexes, however, are always identified correctly.
     *
     * This test can be applied to multiple line strings of the polygon
     * in succession. In that case, the result of each test must be XOR'd
     * with the previous results.
     *
     * @param iter      an XY iterator (consumed by this method)
     * @param cx        the X-coordinate to test
     * @param cy        the Y-coordinate to test
     * @return          0 if even number of edges are crossed ("not inside")
     *                  1 if odd number of edges are crossed ("inside")
     */
    public static int testFast(StoredWay.XYIterator iter, double cx, double cy)
    {
        int odd = 0;
        long xy = iter.nextXY();
        double x1 = XY.x(xy);
        double y1 = XY.y(xy);

        while(iter.hasNext())
        {
            xy = iter.nextXY();
            double x2 = XY.x(xy);
            double y2 = XY.y(xy);

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
            x1 = x2;
            y1 = y2;
        }
        return odd;
    }
}
