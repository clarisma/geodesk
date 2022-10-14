package com.geodesk.core;

import org.locationtech.jts.geom.*;
import org.locationtech.jts.operation.distance.DistanceOp;

/**
 * Methods for working with Mercator-projected coordinates.
 *
 * GeoDesk uses a Pseudo-Mercator projection that projects
 * coordinates onto a square Cartesian plane 2^32 units wide/tall
 * (in essence, the value range fully uses a 32-bit signed
 * int; a pair of coordinates fits into a 64-bit signed long).
 *
 * This projection is compatible with Web Mercator EPSG:3785,
 * except that instead of meters at the Equator, it uses a made-up
 * unit called "imp" ("integer, Mercator-projected").
 *
 * See <a href=/core-concepts#coordinate-system">Coordinate System</a>
 */

//  TODO -- decide on unit name:
//  Mercator-projected Integer Coordinate (MICs)
//  Integer, Mercator-Projected (imps)
//
//  85.051129 deg north approx. = (1 << 31) - 1 peps
//  85.051129 deg north approx. = -(1 << 31) peps
//  180 deg east = (1 << 31)-1 peps
//  180 deg west = -(1 << 31) peps




// TODO: should we reserve Integer.min to mean "invalid"?
//  lon 180 = Integer.max
//  lon -180 = Integer.min+1



// TODO Naming: "from" vs "of"

public class Mercator 
{
	private static final double MAP_WIDTH = 4_294_967_294.9999d;
	// original: 4_294_967_296d
	// change this to MAP_WIDTH = 4_294_967_297d ?
	// This would produce -180 lon = -max vs. -max+1

	// the width and height of the coordinate space (1 << 32)
	private static final double EARTH_CIRCUMFERENCE = 40_075_016.68558;
		// in meters, at the equator
	
	/**
	 * Converts a longitude to imps.
	 * 
	 * @param lon longitude (in degrees)
	 * @return equivalent imps
	 */
	// TODO: must be rounded prior to conversion to int
	// TODO: change return type to int?
	public static double xFromLon(double lon)
	{
		return MAP_WIDTH * lon / 360;
	}

	/**
	 * Converts a longitude to imps.
	 * 
	 * @param lon longitude (in 100-nanodegree increments)
	 * @return equivalent imps
	 */
	public static int xFromLon100nd(int lon)
	{
		return (int)(xFromLon(lon) / 10_000_000 + .5);
	}
	// TODO: check rounding
	// TODO: divide lon before passing to method; we may change xFromLon to int

	/**
	 * Converts a latitude to imps.
	 * 
	 * @param lat latitude (in degrees)
	 * @return equivalent imps
	 */
	// TODO: must be rounded prior to conversion to int
	// TODO: change return type to int?
	public static double yFromLat(double lat)
	{
		return Math.log(Math.tan((lat+90)*Math.PI/360)) *
			(MAP_WIDTH / 2 / Math.PI);
	}
	
	/**
	 * Converts a latitude to imps.
	 * 
	 * @param lat latitude (in 100-nanodegree increments)
	 * @return equivalent imps
	 */
	public static int yFromLat100nd(int lat)
	{
		return (int)(yFromLat((double)lat / 10_000_000) + .5);
	}
	// TODO: check rounding
	
	public static double scale(double y)
	{
		return Math.cosh(y *2 * Math.PI / MAP_WIDTH);
	}
	
	/**
	 * Converts a projected longitude to WGS84.
	 * 
	 * @param x projected latitude (in imps)
	 * @return equivalent WSG-84 longitude in degrees
	 */
	public static double lonFromX(double x)
	{
		return x * 360 / MAP_WIDTH;
	}

	/**
	 * Converts a projected latitude to WGS84.
	 * 
	 * @param y projected latitude (in imps)
	 * @return equivalent WSG-84 latitude in degrees
	 */
	public static double latFromY(double y)
	{
		return Math.atan(Math.exp(y * Math.PI * 2 / MAP_WIDTH))
			* 360 / Math.PI - 90;
	}

	public static double metersAtY(int y)
	{
		return EARTH_CIRCUMFERENCE / MAP_WIDTH / scale(y);
	}

	// TODO: move to "Measure" class
	// TODO: provide overloaded version that takes Features
	/**
	 * Calculates the Euclidean distance between two projected 
	 * points. A simple method that is sufficiently accurate only
	 * for short distances. 
	 * 
	 * @param x1 (in imps)
	 * @param y1 (in imps)
	 * @param x2 (in imps)
	 * @param y2 (in imps)
	 * @return distance in meters
	 */
	public static double distance(double x1, double y1, double x2, double y2)
	{
		double xDelta = Math.abs(x1 - x2);
		double yDelta = Math.abs(y1 - y2);
		double d = Math.sqrt(xDelta * xDelta + yDelta * yDelta);
		return d * EARTH_CIRCUMFERENCE / MAP_WIDTH / scale(
			(y1 + y2) / 2);
	}
	
	public static double distance(Coordinate c1, Coordinate c2)
	{
		return distance(c1.x, c1.y, c2.x, c2.y);
	}
	
	public static double distance(Geometry a, Geometry b)
	{
		Coordinate[] nearestPoints = DistanceOp.nearestPoints(a,b);
		return distance(nearestPoints[0], nearestPoints[1]);
	}
	
	/**
	 * Calculates the equivalent number of imps that
	 * are equal to the given distance in meters at a 
	 * planar-projected latitude.
	 * 
	 * @param meters distance in meters
	 * @param atY the projected latitude (i.e. in imps, not degrees)
	 * @return the distance in imps
	 */
	public static double deltaFromMeters(double meters, double atY)
	{
		return meters * MAP_WIDTH / EARTH_CIRCUMFERENCE *
			scale(atY);
	}

	/**
	 * Calculates the area of the given geometry (in square
	 * meters). A simple method that is sufficiently accurate only
	 * for small areas.
	 * 
	 * @param geom the geometry
	 * @return area in square meters
	 */
	// TODO: check
	public static double area(Geometry geom)
	{
		double area = geom.getArea();
		assert area >= 0: "Negative area for " + geom;
		if(area==0) return 0;
		double scale = EARTH_CIRCUMFERENCE / MAP_WIDTH / scale(
			geom.getCentroid().getY()); 
		return area * scale * scale;
	}
	
	public static Envelope expandEnvelope(Envelope env, double meters)
	{
		env.expandBy(deltaFromMeters(meters, 
			(env.getMaxY() + env.getMinY()) / 2));
		return env;
	}

	public static Envelope envelope(double lon1, double lat1, double lon2, double lat2)
	{
		return new Envelope(xFromLon(lon1), xFromLon(lon2),
			yFromLat(lat1),	yFromLat(lat2));
	}

	/*
	public static double averageLatitude(Geometry geom)
	{
		Envelope env = geom.getEnvelopeInternal();
		return (env.getMinY() + env.getMaxY()) / 2;
	}
	 */

	/*
	// TODO: remove?
	public static Box bounds(double lon1, double lat1, double lon2, double lat2)
	{
		return new Box((int)xFromLon(lon1), (int)yFromLat(lat1),
			(int)xFromLon(lon2), (int)yFromLat(lat2));
	}
	 */

	/**
	 * Converts the WGS84 (longitude/latitude) coordinates of a
	 * {@link Geometry} into Mercator projection. The Geometry is
	 * modified in-place.
	 *
	 * @param geom the `Geometry` whose coordinates to project
	 */
	public static void project(Geometry geom)
	{
		geom.apply(new CoordinateSequenceFilter()
		{
			@Override public void filter(CoordinateSequence seq, int i)
			{
				seq.setOrdinate(i,0,xFromLon(seq.getX(i)));
				seq.setOrdinate(i,1,yFromLat(seq.getY(i)));
			}

			@Override public boolean isDone()
			{
				return false;
			}

			@Override public boolean isGeometryChanged()
			{
				return true;
			}
		});
	}
}
