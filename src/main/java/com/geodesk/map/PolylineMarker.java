package com.geodesk.map;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;

import java.io.IOException;

public class PolylineMarker extends Marker
{
	double[] coords;

	public PolylineMarker(double... coords)
	{
		this.coords = coords;
	}
	
	public PolylineMarker(int... coords)
	{
		this.coords = new double[coords.length];
		for(int i=0; i<coords.length; i++)
		{
			this.coords[i] = coords[i];
		}
	}

	public PolylineMarker(Coordinate... coords)
	{
		this.coords = new double[coords.length * 2];
		for(int i=0; i<coords.length; i++)
		{
			this.coords[i * 2] = coords[i].x;
			this.coords[i * 2 + 1] = coords[i].y;
		}
	}

	public Envelope envelope()
	{
		Envelope env = new Envelope(coords[0], coords[2], coords[1], coords[3]);
		for(int i=4; i<coords.length; i+=2)
		{
			env.expandToInclude(coords[i], coords[i+1]);
		}
		return env;
	}

	
	protected void emitPart(Appendable out) throws IOException 
	{
		double[][] trueCoords = new double[coords.length / 2][];
		for(int i=0; i<coords.length; i+=2)
		{
			trueCoords[i/2] = map.latLonFromXY(coords[i], coords[i+1]); 
		}
		out.append("L.polyline(");
		JavaScript.writeArray(out, trueCoords);
	}
	
}
