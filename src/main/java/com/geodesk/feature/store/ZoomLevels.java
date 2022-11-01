/*
 * Copyright (c) Clarisma / GeoDesk contributors
 *
 * This source code is licensed under the Apache 2.0 license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.geodesk.feature.store;

/**
 * Methods for dealing with zoom levels
 */
public class ZoomLevels 
{
	public static final String DEFAULT = "4,6,8,10,12";
	
	public static final int MAX_LEVEL = 12;
	
	public static int fromString(String s)
	{
		int zoomLevels = 0;
		String[] a = s.split(",");
		for(int i=0; i<a.length; i++)
		{
			int zoom;
			try
			{
				zoom = Integer.parseInt(a[i]);
			}
			catch(NumberFormatException ex)
			{
				zoom = -1;
			}
			if(zoom < 0 || zoom > 12)
			{
				throw new IllegalArgumentException("Zoom level must be between 0 and 12, inclusive");
			}
			int bit = 1 << zoom;
			if((zoomLevels & bit) != 0)
			{
				throw new IllegalArgumentException(String.format(
					"Zoom level %d specified more than once", zoom));
			}
			zoomLevels |= bit;
		}
		
		if(zoomLevels == 0)
		{
			throw new IllegalArgumentException("No zoom level specified");
		}
		return zoomLevels;
	}
	
	public static boolean isValidZoomLevel(int zoomLevels, int zoom)
	{
		return (zoomLevels & (1 << zoom)) != 0;
	}

	public static int minZoom(int zoomLevels)
	{
		return Integer.numberOfTrailingZeros(zoomLevels);
	}
	
	public static int maxZoom(int zoomLevels)
	{
		return 31 - Integer.numberOfLeadingZeros(zoomLevels);
	}
	
	public static int numberOfLevels(int zoomLevels)
	{
		return Integer.bitCount(zoomLevels);
	}

	public static int[] toArray(int zoomLevels)
	{
		int[] a = new int[numberOfLevels(zoomLevels)];
		int pos = 0;
		for(int level=0;level<=MAX_LEVEL;level++)
		{
			if(isValidZoomLevel(zoomLevels, level)) a[pos++] = level;
		}
		return a;
	}
	
	/**
	 * Returns the steps between zoom levels, encoded in an int.
	 * Each step, starting from the minimum zoom level to the 
	 * next-higher zoom level, is encoded using 2 bits (e.g. for
	 * zoom levels 4,6, and 9 the pattern would be 0b1110, to
	 * signify 2 steps between 4 and 6, and 3 steps between 6 and
	 * 9). If there are more than 3 steps between any level, 
	 * this method returns -1 to indicate that the zoom levels
	 * are not valid.
	 * 
	 * @param zoomLevels	
	 * @return
	 */
	public static int zoomSteps(int zoomLevels)
	{
		int zoomSteps = 0;
		int pos = 0;
		int step = Integer.numberOfTrailingZeros(zoomLevels) + 1;
		for(;;)
		{
			zoomLevels >>>= step;
			step = Integer.numberOfTrailingZeros(zoomLevels) + 1;
			if(step == 33) return zoomSteps;
				// once zoomLevels is zero, we count 32 bits (+1), so are done
			if(step > 3) return -1;
			zoomSteps |= step << pos;
			pos += 2;
		}
	}
}
