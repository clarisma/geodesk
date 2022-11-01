/*
 * Copyright (c) Clarisma / GeoDesk contributors
 *
 * This source code is licensed under the Apache 2.0 license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.geodesk.geom;

import com.geodesk.core.Box;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.CoordinateSequence;
import org.locationtech.jts.geom.Envelope;

public class Coordinates 
{
	public static Envelope envelope(int[] coords)
	{
		Envelope env = new Envelope();
		for(int i=0; i<coords.length; i+=2)
		{
			env.expandToInclude(coords[i], coords[i+1]);
		}
		return env;
	}
	
	public static Box bounds(int[] coords)
	{
		Box bbox = new Box();
		for(int i=0; i<coords.length; i+=2)
		{
			bbox.expandToInclude(coords[i], coords[i+1]);
		}
		return bbox;
	}
	
	public static boolean fixMissing(int[] c, int nullX, int nullY)
	{
		boolean success = true;
		
		for(int i=0; i<c.length; i += 2)
		{
			if(c[i] == nullX && c[i+1] == nullY)
			{
				if(i > 0)
				{
					c[i] = c[i-2];
					c[i+1] = c[i-1];
				}
				else
				{
					int valid = findValid(c, i+2, nullX, nullY);
					if(valid < 0)
					{
						success = false;
					}
					else
					{
						c[i] = c[valid];
						c[i+1] = c[valid+1];
					}
				}
			}
		}
		return success;
	}
	
	private static int findValid(int[] c, int index, int nullX, int nullY)
	{
		while(index < c.length)
		{
			if(c[index] != nullX || c[index+1] != nullY) return index;
			index += 2;
		}
		return -1;
	}
	
	public static int countLongDeltas(int[] c)
	{
		int count = 0;
		for(int i=2; i<c.length; i+=2)
		{
			int xDelta = c[i] - c[i-2];
			if(xDelta > Short.MAX_VALUE || xDelta < Short.MIN_VALUE)
			{
				count++;
				continue;
			}
			int yDelta = c[i+1] - c[i-1];
			if(yDelta > Short.MAX_VALUE || yDelta < Short.MIN_VALUE)
			{
				count++;
			}
		}
		return count;
	}

	public static boolean isClosedRing(int[] coords)
	{
		return coords[0] == coords[coords.length-2] &&
			coords[1] == coords[coords.length-1];
	}

	public static double[] fromCoordinates(Coordinate[] coords)
	{
		double[] points = new double[coords.length * 2];
		for(int i=0; i<coords.length; i++)
		{
			points[i * 2] = coords[i].x;
			points[i * 2 + 1] = coords[i].y;
		}
		return points;
	}

	public static double[] fromCoordinateSequence(CoordinateSequence coords)
	{
		double[] points = new double[coords.size() * 2];
		for(int i=0; i<coords.size(); i++)
		{
			points[i * 2] = coords.getX(i);
			points[i * 2 + 1] = coords.getY(i);
		}
		return points;
	}
}
