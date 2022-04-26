package com.geodesk.geom;

import java.nio.ByteBuffer;

public class PointInPolygon_old
{
	// isLeft(): tests if a point is Left|On|Right of an infinite line.
//  Input:  three points P0, P1, and P2
//  Return: >0 for P2 left of the line through P0 and P1
//          =0 for P2  on the line
//          <0 for P2  right of the line
//  See: Algorithm 1 "Area of Triangles and Polygons"

	/**
	 * Tests if a point is left/on/right of an infinite line.
	 * 
	 * @param x0
	 * @param y0
	 * @param x1
	 * @param y1
	 * @param testX
	 * @param testY
	 * @return >0 if test point if left of the line through P0 and P1
          	   =0 if test point is on the line
               <0 if test point is right of the line
	 */
	// TODO: overflows???
	public static int isLeft(int x0, int y0, int x1, int y1, int testX, int testY)
	{
		return (x1-x0) * (testY-y0) - (testX -  x0) * (y1 - y0);
	}
	
	// See http://geomalgorithms.com/a03-_inclusion.html
	public static boolean test(int x, int y, ByteBuffer points, int start, int offsetX, int offsetY)
	{
		int wn = 0;    // the  winding number counter
		int x0, y0, x1, y1;
		
		int count = points.getInt(start);
		int p = points.get(start+1);
		// TODO: fix!!!
		x0 = (p & 0xffff) + offsetX;
		y0 = (p >> 16) + offsetY;
		int pos = start+2;
		
	    // loop through all edges of the polygon
	    for(;;) 
	    {   
	    	p = points.get(pos);
			// TODO: fix!!!
	    	x1 = (p & 0xffff) + offsetX;
			y1 = (p >> 16) + offsetY;
				
	    	if (y0 <= y) 
	        {          
	            if (y1  > y)      // an upward crossing
	            {
	            	if (isLeft(x0, y0, x1, y1, x, y) >= 0)  // P left of or on edge
	            	{
	            		wn++;            // have  a valid up intersect
	            	}
	            }
	        }
	        else 
	        {                        // start y > P.y (no test needed)
	            if (y1 <= y)     // a downward crossing
	            {
	            	if (isLeft(x0,y0,x1,y1,x,y) <= 0)  // P right of or on  edge
	            	{
	            		wn--;            // have  a valid down intersect
	            	}
	            }
	        }
	    	count--;
	    	if(count==0) break;
	    	if(count == 1)
	    	{
	    		// TODO
	    	}
	    	// TODO
	    }
	    return wn != 0;
	}
	
	
	/*
	public static int cn_PnPoly(int testX, int testY, ByteBuffer points, int start, int offsetX, int offsetY)
	{
	    int    cn = 0;    // the  crossing number counter

	    // loop through all edges of the polygon
	    for (int i=0; i<n; i++) 
	    {    // edge from V[i]  to V[i+1]
	       if (((V[i].y <= P.y) && (V[i+1].y > P.y))     // an upward crossing
	        || ((V[i].y > P.y) && (V[i+1].y <=  P.y)))
	        { // a downward crossing
	            // compute  the actual edge-ray intersect x-coordinate
	            float vt = (float)(P.y  - V[i].y) / (V[i+1].y - V[i].y);
	            if (P.x <  V[i].x + vt * (V[i+1].x - V[i].x)) // P.x < intersect
	            {
	                 ++cn;   // a valid crossing of y=P.y right of P.x
	            }
	        }
	    }
	    return (cn&1);    // 0 if even (out), and 1 if  odd (in)

	}
	*/
}
