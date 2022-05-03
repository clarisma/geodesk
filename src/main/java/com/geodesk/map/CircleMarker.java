package com.geodesk.map;

import com.geodesk.util.JavaScript;
import org.locationtech.jts.geom.Envelope;

import java.io.IOException;

public class CircleMarker extends Marker
{
	double x, y;
	double radius;
	boolean radiusInPixels;
	
	public CircleMarker(double x, double y)
	{
		this.x = x;
		this.y = y;
	}

	public CircleMarker radiusInPixels(double radius)
	{
		this.radius = radius;
		radiusInPixels = true;
		options.put("radius", radius);
		return this;
	}
	
	public Envelope envelope()
	{
		return new Envelope(x,x,y,y);
		// TODO: radius
	}
	
	protected void emitPart(Appendable out) throws IOException
	{
		out.append(radiusInPixels ? "L.circleMarker(" : "L.circle(");
		JavaScript.writeArray(out, map.latLonFromXY(x, y));
	}
}
