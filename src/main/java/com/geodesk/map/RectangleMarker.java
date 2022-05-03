package com.geodesk.map;

import com.geodesk.util.JavaScript;
import org.locationtech.jts.geom.Envelope;

import java.io.IOException;

public class RectangleMarker extends Marker
{
	double x1,y1,x2,y2;
	
	public RectangleMarker(double left, double top, 
		double right, double bottom)
		// , Map<String,Object> options)
	{
		x1 = left;
		y1 = top;
		x2 = right;
		y2 = bottom;
	}

	public Envelope envelope()
	{
		return new Envelope(x1,x2,y1,y2);
	}
	
	protected void emitPart(Appendable out) throws IOException
	{
		double[][] coords = { map.latLonFromXY(x1, y1),
			map.latLonFromXY(x2, y2) };
		out.append("L.rectangle(");
		JavaScript.writeArray(out, coords);
	}
}
