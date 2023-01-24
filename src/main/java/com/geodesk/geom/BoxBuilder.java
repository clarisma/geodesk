/*
 * Copyright (c) Clarisma / GeoDesk contributors
 *
 * This source code is licensed under the Apache 2.0 license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.geodesk.geom;

public class BoxBuilder implements Bounds
{
    private int minX;
	private int minY;
	private int maxX;
	private int maxY;

    public void reset()
	{
		minX = Integer.MAX_VALUE;
		minY = Integer.MAX_VALUE;
		maxX = Integer.MIN_VALUE;
		maxY = Integer.MIN_VALUE;
	}

    public boolean isNull()
	{
		return maxY < minY;
	}

	public int minX()
	{
		return minX;
	}

	public int minY()
	{
		return minY;
	}

	public int maxX()
	{
		return maxX;
	}

	public int maxY()
	{
		return maxY;
	}

    /**
	 * Checks if this bounding box includes the given coordinate, and expands
	 * it if necessary. If this bounding box straddles the Antimeridian, the
	 * results of this method are undefined (as it cannot tell in which
	 * direction the box should be expanded).
	 *
	 * @param x		X-coordinate
	 * @param y     Y-coordinate
	 */
	public void expandToInclude(int x, int y)
	{
		if (x < minX) minX = x;
		if (x > maxX) maxX = x;
		if (y < minY) minY = y;
		if (y > maxY) maxY = y;
	}

	/**
	 * Checks if this bounding box includes another bounding box, and expands
	 * it if necessary. If either bounding box straddles the Antimeridian, the
	 * results of this method are undefined (as it cannot tell in which
	 * direction the box should be expanded).
	 *
	 * @param b		the bounding box to include into this
	 */
	public void expandToInclude(Bounds b)
	{
		int otherMinX = b.minX();
		int otherMinY = b.minY();
		int otherMaxX = b.maxX();
		int otherMaxY = b.maxY();
		if (otherMinX < minX) minX = otherMinX;
		if (otherMinY < minY) minY = otherMinY;
		if (otherMaxX > maxX) maxX = otherMaxX;
		if (otherMaxY > maxY) maxY = otherMaxY;
	}
}
