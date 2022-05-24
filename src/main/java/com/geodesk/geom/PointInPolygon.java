package com.geodesk.geom;

import com.geodesk.core.XY;
import com.geodesk.feature.store.StoredWay;

public class PointInPolygon
{
    /*
    public static boolean isInside(int[] coords, int x, int y)
    {
        boolean odd = false;
        int len = coords.length - 2;
        for (int i=0; i<len; i+=2)
        {
            if (((coords[i+1] > y) != (coords[i+3] > y))
                && (x < (coords[i+2] - coords[i]) * (y - coords[i+1]) /
                (coords[i+3] - coords[i+1]) + coords[i]))
            {
                odd = !odd;
            }
        }
        return odd;
    }
     */

    // return -1 if vertex, 1 if inside, 0 if outside
    public static boolean isInside(int[] coords, double cx, double cy)
    {
        boolean odd = false;
        int len = coords.length - 2;
        for (int i=0; i<len; i+=2)
        {
            double x1 = coords[i];
            double y1 = coords[i+1];
            double x2 = coords[i+2];
            double y2 = coords[i+3];

            // Added this so vertices are always considered inside
            if(cx==x1 && cy==y1) return true;

            if (((y1 <= cy) && (y2 > cy))     // upward crossing
                || ((y1 > cy) && (y2 <= cy))) // downward crossing
            {
                // compute edge-ray intersect x-coordinate
                double vt = (cy  - y1) / (y2 - y1);
                if (cx <  x1 + vt * (x2 - x1)) // P.x < intersect
                {
                    odd = !odd;
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

    /*

    From https://web.archive.org/web/20161108113341/https://www.ecse.rpi.edu/Homepages/wrf/Research/Short_Notes/pnpoly.html


    int pnpoly(int nvert, float *vertx, float *verty, float testx, float testy)
{
  int i, j, c = 0;
  for (i = 0, j = nvert-1; i < nvert; j = i++) {
    if ( ((verty[i]>testy) != (verty[j]>testy)) &&
	 (testx < (vertx[j]-vertx[i]) * (testy-verty[i]) / (verty[j]-verty[i]) + vertx[i]) )
       c = !c;
  }
  return c;
}
     */

}
