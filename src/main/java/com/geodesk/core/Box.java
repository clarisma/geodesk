/*
 * Copyright (c) Clarisma / GeoDesk contributors
 *
 * This source code is licensed under the Apache 2.0 license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.geodesk.core;	// TODO: rename to util?

import com.geodesk.feature.store.WayCoordinateSequence;
import com.geodesk.geom.Bounds;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineSegment;

/**
 * An axis-aligned bounding box. A `Box` represents minimum and maximum
 * X and Y coordinates in a Mercator-projected plane. It can straddle the
 * Antimeriaian (in which case `minX` is *larger* than `maxX`). A `Box` can
 * also be empty (in which case `minY` is *larger* than `maxY`)
 */
// TODO: should this be immutable?
//  can't, because we need `expandToInclude`
//  we could have a separate MutableBox type (or RubberBox)
public class Box implements Bounds
{
	private int minX;
	private int minY;
	private int maxX;
	private int maxY;

	/**
	 * Creates a null Box.
	 */
	public Box()
	{
		setNull();
	}

	public Box(int minX, int minY, int maxX, int maxY)
	{
		this.minX = minX;
		this.minY = minY;
		this.maxX = maxX;
		this.maxY = maxY;
	}

	public Box(Bounds b)
	{
		minX = b.minX();
		minY = b.minY();
		maxX = b.maxX();
		maxY = b.maxY();
	}

	public Box(int x, int y)
	{
		this(x, y, x, y);
	}

	public void setNull()
	{
		minX = Integer.MAX_VALUE;
		minY = Integer.MAX_VALUE;
		maxX = Integer.MIN_VALUE;
		maxY = Integer.MIN_VALUE;
	}

	/**
	 * Returns `true` if this bounding box straddles the Antimeridian.
	 *
	 * @return `true` if `minX` and `maxX` lie on different sides of the
	 *         Antimeridian
	 */
	public boolean crossesAntimeridian()
	{
		return maxX < minX;
	}

	/**
	 * Returns `true` if this bounding box is empty.
	 *
	 * @return `true` if this is an empty bounding box
	 *         (`maxY` is less than `minY`)
	 */
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
	// TODO: turns 180-crossing box into regular box
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

	/**
	 * Checks if this bounding box includes another bounding box, and expands
	 * it if necessary. If either bounding box straddles the Antimeridian, the
	 * results of this method are undefined (as it cannot tell in which
	 * direction the box should be expanded).
	 *
	 * @param otherMinX   minimum X-coordinate of the other bounding box
	 * @param otherMinY   minimum Y-coordinate of the other bounding box
	 * @param otherMaxX   maximum X-coordinate of the other bounding box
	 * @param otherMaxY   maximum Y-coordinate of the other bounding box
	 */
	public void expandToInclude(int otherMinX, int otherMinY, int otherMaxX, int otherMaxY)
	{
		if (otherMinX < minX) minX = otherMinX;
		if (otherMinY < minY) minY = otherMinY;
		if (otherMaxX > maxX) maxX = otherMaxX;
		if (otherMaxY > maxY) maxY = otherMaxY;
	}

	public void expandToInclude(int[] coords)
	{
		for (int i = 0; i < coords.length; i += 2) expandToInclude(coords[i], coords[i + 1]);
	}

	/**
	 * Creates a JTS {@link Envelope} with the same dimensions as this
	 * bounding box.
	 *
	 * @return a new `Envelope`
	 */
	public Envelope toEnvelope()
	{
		return new Envelope(minX, maxX, minY, maxY);    // Envelope specifies both x first, then both y
	}

	@Override public boolean equals(Object other)
	{
		if (!(other instanceof Bounds)) return false;
		Bounds o = (Bounds) other;
		return minX == o.minX() && maxX == o.maxX() && minY == o.minY() && maxY == o.maxY();
	}

	@Override public int hashCode()
	{
		return Integer.hashCode(minX) ^ Integer.hashCode(minY) ^ Integer.hashCode(maxX) ^ Integer.hashCode(maxY);
	}


	/**
	 * Creates a new bounding box that is the result of the intersection between
	 * this bounding box and another.
	 *
	 * @param o  the other bounding box
	 * @return   new Box
	 */
	// TODO: fix: what happens if empty or boxes don't overlap?
	public Box intersection(Bounds o)
	{
		int x1 = minX > o.minX() ? minX : o.minX();
		int y1 = minY > o.minY() ? minY : o.minY();
		int x2 = maxX < o.maxX() ? maxX : o.maxX();
		int y2 = maxY < o.maxY() ? maxY : o.maxY();
		return new Box(x1, y1, x2, y2);
	}

	/**
	 * Overflow-safe subtraction
	 *
	 * @param x
	 * @param y
	 * @return the result of the subtraction; or the lowest negative value in case of an overflow
	 */
	private static int trimmedSubtract(int x, int y)
	{
		int r = x - y;
		if (((x ^ y) & (x ^ r)) < 0) return Integer.MIN_VALUE;
		return r;
	}

	/**
	 * Overflow-safe addition
	 *
	 * @param x
	 * @param y
	 * @return the result of the addition; or the highest positive value in case of an overflow
	 */
	private static int trimmedAdd(int x, int y)
	{
		int r = x + y;
		if (((x ^ r) & (y ^ r)) < 0) return Integer.MAX_VALUE;
		return r;
	}


	/**
	 * Expands or contracts all sides of this bounding box by a specified
	 * number of imps. If the bounding box is empty, the result is undefined.
	 *
	 * @param b the buffer (in imps)
	 */
	// TODO: define and test Antimeridian behaviour
	public void buffer(int b)
	{
		minX -= b;
		maxX += b;
		if(b >= 0)
		{
			minY = trimmedSubtract(minY, b);
			maxY = trimmedAdd(maxY, b);
		}
		else
		{
			minY = trimmedAdd(minY, -b);
			maxY = trimmedSubtract(maxY, -b);
			if(maxY < minY) setNull();
			// TODO: check if width flipped
		}
	}

	/**
	 * Expands or contracts all sides of this bounding box by a specified
	 * number of meters. If the bounding box is empty, the result is undefined.
	 *
	 * @param m the buffer (in meters)
	 */
	// TODO: Do we need a method that buffers by minimum of meters?
	//  (use scale closest to Equator)
	public void bufferMeters(double m)
	{
		buffer((int)Mercator.deltaFromMeters(m, centerY()));
	}

	/**
	 * Moves the bounding box horizontally and vertically by the specified
	 * number of units. Attempts to move the box beyond the cut-offs in the
	 * polar regions result in the box being trimmed. If the bounding box is
	 * empty, the result is undefined.
	 *
	 * @param deltaX	X-offset (imps)
	 * @param deltaY    Y-offset (imps)
	 */
	public void translate(int deltaX, int deltaY)
	{
		minX += deltaX;
		maxX += deltaX;
		if (deltaY > 0)
		{
			minY = trimmedAdd(minY, deltaY);
			maxY = trimmedAdd(maxY, deltaY);
		}
		else
		{
			minY = trimmedSubtract(minY, deltaY);
			maxY = trimmedSubtract(maxY, deltaY);
		}
	}

	@Override public String toString()
	{
		return isNull() ? "[empty]" : String.format("[%d,%d -> %d,%d]", minX, minY, maxX, maxY);
	}

	// TODO: rounding before (int)
	public static Box ofWSEN(double west, double south, double east, double north)
	{
		return new Box((int) Mercator.xFromLon(west), (int) Mercator.yFromLat(south),
			(int) Mercator.xFromLon(east), (int) Mercator.yFromLat(north));
	}

	/**
	 * Creates a `Box` that covers a single point.
	 *
	 * @param lon  the longitude of the point
	 * @param lat  the latitude of the point
	 * @return a `Box` that is 1 imp wide and 1 imp tall
	 */
	public static Box atLonLat(double lon, double lat)
	{
		int x = (int) Mercator.xFromLon(lon);
		int y = (int) Mercator.yFromLat(lat);
		return new Box(x,y,x,y);
	}

	/**
	 * Creates a `Box` that covers a single point.
	 *
	 * @param x  X-coordinate (Mercator-projected) of the point
	 * @param y  Y-coordinate (Mercator-projected) of the point
	 * @return a `Box` that is 1 imp wide and 1 imp tall
	 */
	public static Box atXY(int x, int y)
	{
		return new Box(x,y,x,y);
	}

	public static Box ofXYXY(int x1, int y1, int x2, int y2)
	{
		return new Box(x1,y1,x2,y2);
	}

	// TODO: decide what width/height mean
	public static Box ofXYWidthHeight(int x, int y, int w, int h)
	{
		return new Box(x,y,x+w-1,y+h-1);
	}

	/**
	 * Creates a bounding box whose sides extend by a specific distance relative
	 * to a coordinate pair.
	 *
	 * @param meters	the distance (in meters) by which each side of the original
	 *                  bounds are buffered
	 * @param x         X-coordinate of the center point (Mercator-projected)
	 * @param y		    Y-coordinate of the center point (Mercator-projected)
	 * @return			a new bounding box
	 */
	public static Box metersAroundXY(double meters, int x, int y)
	{
		int b = (int)Mercator.deltaFromMeters(meters, y);
		return new Box(x-b, trimmedSubtract(y, b), x+b, trimmedAdd(y, b));
	}

	/**
	 * Creates a bounding box whose sides extend by a specific distance relative
	 * to a coordinate pair.
	 *
	 * @param d			the distance (in imps) by which each side of the original
	 *                  bounds are buffered
	 * @param x         X-coordinate of the center point (Mercator-projected)
	 * @param y		    Y-coordinate of the center point (Mercator-projected)
	 * @return			a new bounding box
	 */
	public static Box impsAroundXY(int d, int x, int y)
	{
		return new Box(x-d, trimmedSubtract(y, d), x+d, trimmedAdd(y, d));
	}

	/**
	 * Creates a bounding box whose sides extend by a specific distance relative
	 * to a coordinate pair.
	 *
	 * @param meters	the distance (in meters) by which each side of the original
	 *                  bounds are buffered
	 * @param lon       longitude of the center point
	 * @param lat		latitude of the center point
	 * @return			a new bounding box
	 */
	public static Box metersAroundLonLat(double meters, double lon, double lat)
	{
		return metersAroundXY(meters, (int)Mercator.xFromLon(lon), (int)Mercator.yFromLat(lon));
	}

	/**
	 * Creates a bounding box whose sides are extended by a specific distance relative
	 * to another bounding box.
	 *
	 * @param meters	the distance (in meters) by which each side of the original
	 *                  bounds are buffered
	 * @param other		the original bounding box
	 * @return			a new bounding box
	 */
	public static Box metersAround(double meters, Bounds other)
	{
		int b = (int)Mercator.deltaFromMeters(meters, other.centerY());
		return new Box(other.minX()-b, trimmedSubtract(other.minY(), b),
			other.maxX()+b, trimmedAdd(other.maxY(), b));
	}

	public static Box fromEnvelope(Envelope env)
	{
		return new Box(
			(int) Math.floor(env.getMinX()),
			(int) Math.round(env.getMinY()),	// TODO: why not floor?
			(int) Math.ceil(env.getMaxX()),
			(int) Math.round(env.getMaxY()));	// TODO: why not ceil?
	}

	public static Box of(Geometry geom)
	{
		return fromEnvelope(geom.getEnvelopeInternal());
	}

	public static Box of(LineSegment seg)
	{
		double x1 = seg.p0.x;
		double y1 = seg.p0.y;
		double x2 = seg.p1.x;
		double y2 = seg.p1.y;
		return new Box(
			(int) Math.floor(x1 < x2 ? x1 : x2),
			(int) Math.floor(y1 < y2 ? y1 : y2),
			(int) Math.ceil(x1 > x2 ? x1 : x2),
			(int) Math.ceil(y1 > y2 ? y1 : y2));
	}

	private static double parseCoordinate(String s, String name, double max)
	{
		try
		{
			double val = Double.parseDouble(s);
			if(val > max)
			{
				throw new IllegalArgumentException(
					String.format("Coordinate value for %s must not exceed %f",
					name, max));
			}
			if(val < -max)
			{
				throw new IllegalArgumentException(
					String.format("Coordinate value for %s must be at least %f",
					name, -max));
			}
			return val;
		}
		catch(NumberFormatException ex)
		{
			throw new IllegalArgumentException(s + " is not a valid coordinate value");
		}
	}

	/**
	 * Creates a Box from a string that specifies four coordinates (west, south,
	 * east, north), in degrees longitude/latitude.
	 *
	 * @param s		the string (e.g. `2.2,48.8,2.5,18.9`)
	 * @return a new bounding box
	 */
	public static Box fromWSEN(String s)
	{
		// TODO: fix this, make more lenient
		String[] coords = s.split(",");
		if(coords.length != 4)
		{
			throw new IllegalArgumentException("Must specify 4 coordinate values (W,S,E,N)");
		}
		double west = parseCoordinate(coords[0], "W", 180);
		double south = parseCoordinate(coords[1],"S", 90);
		double east = parseCoordinate(coords[2], "E", 180);
		double north = parseCoordinate(coords[3],"N", 90);
		return Box.ofWSEN(west,south,east,north);
	}

	/**
	 * Creates a bounding box that covers the entire world.
	 *
	 * @return a new bounding box
	 */
	public static Box ofWorld()
	{
		return new Box(
			Integer.MIN_VALUE,Integer.MIN_VALUE,Integer.MAX_VALUE,Integer.MAX_VALUE);
	}

	public Geometry toGeometry(GeometryFactory factory)
	{
		int[] coords = new int[10];
		coords[0] = minX;
		coords[1] = minY;
		coords[2] = maxX;
		coords[3] = minY;
		coords[4] = maxX;
		coords[5] = maxY;
		coords[6] = minX;
		coords[7] = maxY;
		coords[8] = minX;
		coords[9] = minY;
		return factory.createPolygon(new WayCoordinateSequence(coords));
	}
}