package com.geodesk.core;

import org.junit.Test;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;
import static com.geodesk.core.Mercator.*;

import java.util.Random;

public class MercatorTest
{
	@Test public void testExtremeCoordinates()
	{
		long min = Long.MIN_VALUE;
		long max = Long.MAX_VALUE;
		double lat = Mercator.latFromY(XY.y(min));
		double lon = Mercator.lonFromX(XY.x(min));
		
		System.out.format("lat = %f\n", lat);
		System.out.format("lon = %f\n", lon);
		
		lat = Mercator.latFromY(XY.y(max));
		lon = Mercator.lonFromX(XY.x(max));
		
		System.out.format("lat = %f\n", lat);
		System.out.format("lon = %f\n", lon);
		
		int x = Mercator.xFromLon100nd(1_800_000_000);
		System.out.format("180 deg lon = %d\n", x);
		x = Mercator.xFromLon100nd(-1_800_000_000);
		System.out.format("-180 deg lon = %d\n", x);

		x = (int)Math.round(Mercator.xFromLon(180));
		System.out.format("180 deg lon (double) = %d\n", x);
		x = (int)Math.round(Mercator.xFromLon(-180));
		System.out.format("-180 deg lon (double) = %d\n", x);

		int y = Mercator.yFromLat100nd(900_000_000);
		System.out.format("90 deg lat = %d\n", y);
		y = Mercator.yFromLat100nd(-900_000_000);
		System.out.format("-90 deg lat = %d\n", y);

		y = (int)Mercator.yFromLat(999999);
		System.out.format("999999 deg lat = %d\n", y);
		y = (int)Mercator.yFromLat(-999999);
		System.out.format("-999999 deg lat = %d\n", y);
		
		y = Mercator.yFromLat100nd(850_511_290);
		System.out.format("85.051129 deg lat = %d\n", y);
		y = Mercator.yFromLat100nd(-850_511_290);
		System.out.format("-85.051129 deg lat = %d\n", y);

		lon = Mercator.lonFromX(Integer.MAX_VALUE);
		System.out.format("maxint x -> deg lon = %.08f\n", lon);
		lon = Mercator.lonFromX(Integer.MIN_VALUE);
		System.out.format("minint x -> deg lon = %.08f\n", lon);
		lon = Mercator.lonFromX(Integer.MIN_VALUE+1);
		System.out.format("minint+1 x -> deg lon = %.08f\n", lon);
		lon = Mercator.lonFromX(Integer.MIN_VALUE+2);
		System.out.format("minint+2 x -> deg lon = %.08f\n", lon);
		lon = Mercator.lonFromX(Integer.MIN_VALUE+3);
		System.out.format("minint+3 x -> deg lon = %.08f\n", lon);
		lon = Mercator.lonFromX(Integer.MIN_VALUE+4);
		System.out.format("minint+4 x -> deg lon = %.08f\n", lon);
		lon = Mercator.lonFromX(Integer.MIN_VALUE+100);
		System.out.format("minint+100 x -> deg lon = %.08f\n", lon);
	}
	
	private static final String P1 = "POLYGON ((137186237 667219324, 137185189 667220565, 137187672 667222660, 137193199 667216107, 137194247 667214866, 137191764 667212771, 137186237 667219324))";

	@Test public void testArea() throws ParseException
	{
		Geometry geom = new WKTReader().read(P1);
		double area = Mercator.area(geom);
		System.out.format("Area (m2) = %f\n", area); 
	}

	private void printScaleForLat(double lat, String label)
	{
		double meters = 5000;
		double y = yFromLat(lat);
		double scale = scale(y);
		double delta = deltaFromMeters(meters, y);
		double northY = y+delta;
		double northLat = latFromY(northY);
		double southY = y-delta;
		double southLat = latFromY(southY);
		double northScale = scale(northY);
		double southScale = scale(southY);
		double kmAtZoom12 = 40_075_016d / 4096 / 1000 / scale;
		System.out.format("Scale factor at latitude %f (%s) = %f\n", lat, label, scale);
		System.out.format("  At this latitude, %f meters equals %f units\n",
			meters, delta);
		System.out.format("  At this latitude, scale varies from %f to %f (+/- %f m), a variance of %f percent\n",
			southScale, northScale, meters, northScale / southScale * 100 - 100);
		System.out.format("  %f meters north is latitude %f\n", meters, northLat);
		System.out.format("  %f meters south is latitude %f\n", meters, southLat);
		System.out.format("  At this latitude, a zoom 12 tile covers %f km2\n",
			kmAtZoom12 * kmAtZoom12 );
		System.out.format("\n");
	}

	@Test public void testScale()
	{
		printScaleForLat(0, "Equator");
		printScaleForLat(1.29, "Singapore");
		printScaleForLat(9.92, "Madurai, India");
		printScaleForLat(-23.55, "Sao Paulo");
		printScaleForLat(48.14, "Munich");
		printScaleForLat(59.9133301, "Oslo");
		printScaleForLat(63.4, "Trondheim");
		printScaleForLat(71.2, "Knivskjelodden, Norway");
		printScaleForLat(83.6608453, "Cape Morris Jesup, Greenland");
	}

	@Test public void testReversability()
	{
		Random random = new Random();
		for(int i=0; i<10_000; i++)
		{
			double lat = random.nextDouble(170) - 85;
			double lon = random.nextDouble(360) - 180;

			int x = (int) Math.round(Mercator.xFromLon(lon));
			int y = (int) Math.round(Mercator.yFromLat(lat));
			double lonBack = Mercator.lonFromX(x);
			double latBack = Mercator.latFromY(y);
			int x2 = (int) Math.round(Mercator.xFromLon(lonBack));
			int y2 = (int) Math.round(Mercator.yFromLat(latBack));
			System.out.format("%.9f, %.9f -> %d, %d\n", lon, lat, x, y);
			System.out.format("%.9f, %.9f => %d, %d\n", lonBack, latBack, x2, y2);
		}
	}

	@Test public void testDrift()
	{
		double lon = Mercator.lonFromX(-2087724656);
		double lat = Mercator.latFromY(-482479435);
		System.out.format("%.7f, %.7f\n", lon, lat);
		double x2 = Mercator.xFromLon(lon);
	}
}


