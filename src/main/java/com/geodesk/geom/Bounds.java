/*
 * Copyright (c) Clarisma / GeoDesk contributors
 *
 * This source code is licensed under the Apache 2.0 license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.geodesk.geom;

import org.locationtech.jts.geom.Coordinate;

// TODO: maybe at minLon, maxLat, etc.
// TODO: We need a meridian-safe bbox for queries that cross lon 180
// TODO: use width/heihgt instead of maxX/maxY in order to determine antimeridian crossing
//  But beware of int overflow
// TODO: change to "Bounded", reduce methods
//  Consider if it makes sense for Feature to inherit Bounds/Bounded
//  Be careful because intersects/contains have different meaning for features,
//  since user would expect that they consider the shape (true segment intersection,
//  point-in-polygon test) vs. simply checking the bounding box
//  Or make the Bounded-specific methods static:
//   Bounded.intersect(box, feature)
//  Or, could move interface to the implementing class:
//   class StoredFeature implements Feature, Bounds
//    Bounds bounds() { return this; }
// TODO: decide what width/height mean

public interface Bounds 
{
	int minX();
	int minY();
	int maxX();
	int maxY();

	// TODO: doesn't work if both cross the 180
	default boolean intersects(Bounds other)
	{
		return !(other.minX() > maxX() ||
			other.maxX() < minX() ||
	        other.minY() > maxY() ||
	        other.maxY() < minY());
	}
	
	default boolean contains(int x, int y)
	{
		int minX = minX();
		int maxX = maxX();
		if(maxX < minX) return (x >= minX || x <= maxX) && y >= minY() && y <= maxY();
		return x >= minX && x <= maxX && y >= minY() && y <= maxY();
	}

	// TODO: assumes Bounds are non-null!
	default boolean contains(Bounds other) 
	{
		return other.minX() >= minX() && other.maxX() <= maxX() && 
			other.minY() >= minY() && other.maxY() <= maxY();
	}

	// TODO: check these, calculations are not consistent

	default long width()
	{
		return (maxY() < minY()) ? 0 : ((((long)maxX() - minX()) & 0xffff_ffffL) + 1);
	}
	
	default long height()
	{
		return maxY() < minY() ? 0 : ((long)maxY() - minY() + 1);
	}

	// TODO: may overflow
	default long area()
	{
		return width() * height();
	}
	
	default int centerX()
	{
		return minX() + (maxX() - minX()) / 2;
	}
	
	default int centerY()
	{
		return minY() + (maxY() - minY()) / 2;
	}

	/*
	default Coordinate[] toCoordinates()
	{
		Coordinate[] c = new Coordinate[5];
		c[0] = new Coordinate(minX(), minY());
		c[1] = new Coordinate(minX(), maxY());
		c[2] = new Coordinate(maxX(), maxY());
		c[3] = new Coordinate(maxX(), minY());
		c[4] = new Coordinate(minX(), minY());
		return c;
	}
	 */
}
