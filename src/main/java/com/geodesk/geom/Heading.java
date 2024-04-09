/*
 * Copyright (c) Clarisma / GeoDesk contributors
 *
 * This source code is licensed under the Apache 2.0 license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.geodesk.geom;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineSegment;
import org.locationtech.jts.geom.LineString;

/**
 * Compass headings.
 *
 * Keep in mind that unlike screen coordinates, planar coordinate values
 * *increase* as one moves "up" (north).
 */
public enum Heading
{
	NORTH("N",1,0,0),
	NORTHEAST("NE",1,1,45),
	EAST("E",0,1,90),
	SOUTHEAST("SE",-1,1,135),
	SOUTH("S",-1,0,180),
	SOUTHWEST("SW",-1,-1,225),
	WEST("W",0,-1,270),
	NORTHWEST("NW",1,-1,315);
	
	private final String id;
	private final int northFactor;
	private final int eastFactor;
	private final int degrees;
			
	private Heading(String id, int northFactor, int eastFactor, int degrees)
	{
		this.id = id;
		this.northFactor = northFactor;
		this.eastFactor = eastFactor;
		this.degrees = degrees;
	}
	
	public String toString()
	{
		return id;
	}

	public String id()
	{
		return id;
	}

	public int northFactor()
	{
		return northFactor;
	}

	public int eastFactor()
	{
		return eastFactor;
	}

	/**
	 * Heading in degrees.
	 * 
	 * @return 0 = north, 90 = east, etc.
	 */
	public int toDegrees()
	{
		return degrees;
	}

	/**
	 * Returns the opposite heading.
	 * 
	 * @return the heading that lies 180 degrees opposite.
	 */
	public Heading reversed()
	{
		return values()[(ordinal() + 4) % 8];
	}
	
	public Heading turnedBy(double degrees)
	{
		return fromDegrees((degrees + (double)this.degrees) % 360);
	}
	
	/**
	 * Returns the Heading closest to the given compass heading
	 * in degrees (0 = north, 90 = east, etc.)
	 * 
	 * @param degrees (must be 0 <= degrees < 360)
	 * @return
	 */
	public static Heading fromDegrees(double degrees)
	{
		return values()[(int)(((degrees % 360) + 22.5) / 45)];
	}
	
	public static double turnedBy(double fromDegrees, double byDegrees)
	{
		return (fromDegrees + byDegrees) % 360;
	}
	
	/*
	public static double ofLine(double x1, double y1, double x2, double y2)
	{
		double deltaX = x2-x1;
		double deltaY = y2-y1;
		double angle = Math.toDegrees(Math.atan2(y, x))
	}
	*/

	
	/**
	 * Determines the coordinate that lies a given distance from
	 * the center of the plane, in a given heading
	 * 
	 * @param angle		heading in degrees (0 = north, 90 = east)
	 * @param distance	distance
	 * @return			the coordinate
	 */
	public static Coordinate project(double angle, double distance)
	{
		double radians = Math.toRadians(angle);
		double x = Math.sin(radians) * distance;
		double y = Math.cos(radians) * distance;
		return new Coordinate(x,y);
	}

	/**
	 * 
	 * @param angle
	 * @param distance
	 * @param from
	 * @return
	 */
	public static LineSegment project(double angle, double distance, Coordinate from)
	{
		Coordinate to = project(angle, distance);
		to.x += from.x;
		to.y += from.y;
		return new LineSegment(from, to);
	}
	
	public static LineString projectedLine(
		GeometryFactory factory, 
		double angle, double distance, Coordinate from)
	{
		Coordinate to = project(angle, distance);
		to.x += from.x;
		to.y += from.y;
		return factory.createLineString(new Coordinate[]{from, to});
	}
}
