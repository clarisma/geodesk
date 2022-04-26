package com.geodesk.map;

import com.geodesk.geom.Coordinates;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Polygon;

import java.io.IOException;

public class MultiPolygonMarker extends Marker
{
	double[][][] polygons;
	
	public MultiPolygonMarker(MultiPolygon mp)
	{
		polygons = new double[mp.getNumGeometries()][][];
		for(int i=0; i<mp.getNumGeometries(); i++)
		{
			Polygon p = (Polygon)mp.getGeometryN(i);
			polygons[i] = new double[p.getNumInteriorRing()+1][];
			polygons[i][0] = Coordinates.fromCoordinateSequence(
				p.getExteriorRing().getCoordinateSequence());
			for(int i2=0; i2<p.getNumInteriorRing(); i2++)
			{
				polygons[i][i2+1] = Coordinates.fromCoordinateSequence(
					p.getInteriorRingN(i2).getCoordinateSequence());
			}
		}
	}
	
	public Envelope envelope()
	{
		Envelope env = new Envelope();
		for(int i1=0; i1<polygons.length; i1++)
		{
			double[][] p = polygons[i1];
			for(int i2=0; i2<p.length; i2++)
			{
				double[] ring = p[i2];
				for(int i3=0; i3<ring.length; i3+=2)
				{
					env.expandToInclude(ring[i3], ring[i3+1]);
				}
			}
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
		double[][][][] coords = new double[polygons.length][][][];
		for(int i1=0; i1<polygons.length; i1++)
		{
			double[][] p = polygons[i1];
			coords[i1] = new double[p.length][][];
			for(int i2=0; i2<p.length; i2++)
			{
				coords[i1][i2] = makeRing(p[i2]);
			}
		}
		out.append("L.polygon(");
		JavaScript.writeArray(out, coords);
	}
}
