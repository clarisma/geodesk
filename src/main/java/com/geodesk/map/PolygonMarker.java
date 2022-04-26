package com.geodesk.map;

import com.geodesk.geom.Coordinates;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Polygon;

import java.io.IOException;

public class PolygonMarker extends Marker
{
	double[] shell;
	double[][] holes;

	public PolygonMarker(Polygon p)
	{
		shell = Coordinates.fromCoordinateSequence(
			p.getExteriorRing().getCoordinateSequence());
		int holeCount = p.getNumInteriorRing();
		if(holeCount > 0)
		{
			holes = new double[holeCount][];
			for(int i=0; i<holeCount; i++)
			{
				holes[i] = Coordinates.fromCoordinateSequence(
					p.getInteriorRingN(i).getCoordinateSequence());
			}
		}
	}

	public PolygonMarker(double... coords)
	{
		this.shell = coords;
	}

	public PolygonMarker(double[] shell, double[]... holes)
	{
		this.shell = shell;
		this.holes = holes;
	}

	
	public PolygonMarker(int... coords)
	{
		this.shell = new double[coords.length];
		for(int i=0; i<coords.length; i++)
		{
			this.shell[i] = coords[i];
		}
	}
	
	public PolygonMarker(Coordinate... shell)
	{
		this.shell = Coordinates.fromCoordinates(shell);
		/*
		this.shell = new double[coords.length * 2];
		for(int i=0; i<coords.length; i++)
		{
			this.shell[i * 2] = coords[i].x;
			this.shell[i * 2 + 1] = coords[i].y;
		}
		*/
	}
	
	public PolygonMarker(Coordinate[] shell, Coordinate[]... holes)
	{
		this.shell = Coordinates.fromCoordinates(shell);
		this.holes = new double[holes.length][];
		for(int i=0; i<holes.length; i++)
		{
			this.holes[i] = Coordinates.fromCoordinates(holes[i]);
		}
	}
	
	
	public Envelope envelope()
	{
		Envelope env = new Envelope(shell[0], shell[2], shell[1], shell[3]);
		for(int i=4; i<shell.length; i+=2)
		{
			env.expandToInclude(shell[i], shell[i+1]);
		}
		return env;
	}

	protected double[][] makeRing(double[] coords)
	{
		double[][] ring = new double[coords.length / 2][];
		for(int i=0; i<coords.length; i+=2)
		{
			ring[i/2] = map.latLonFromXY(coords[i], coords[i+1]); 
		} 
		return ring;
	}
	
	protected void emitPart(Appendable out) throws IOException 
	{
		if(holes==null)
		{
			out.append("L.polygon(");
				JavaScript.writeArray(out, makeRing(shell));
		}
		else
		{
			double[][][] rings = new double[holes.length+1][][];
			rings[0] = makeRing(shell);
			for(int i=0; i<holes.length; i++)
			{
				rings[i+1] = makeRing(holes[i]);
			}
			out.append("L.polygon(");
			JavaScript.writeArray(out, rings);
		}
	}
}
