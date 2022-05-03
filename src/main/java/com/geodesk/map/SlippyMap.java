package com.geodesk.map;

import com.geodesk.core.Projection;
import com.geodesk.core.Tile;
import com.geodesk.feature.Feature;
import com.geodesk.feature.Node;
import com.geodesk.feature.Relation;
import com.geodesk.feature.Way;
import com.geodesk.geom.Bounds;
import com.geodesk.util.JavaScript;
import org.locationtech.jts.geom.*;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

public class SlippyMap 
{
	public static final String DEFAULT_ID = "map";
	
	protected String id = DEFAULT_ID;
	protected List<Marker> markers = new ArrayList<>();
	protected Projection projection;
	protected int minZoom = 0;
	protected int maxZoom = 19;
	protected String tileServerUrl = 
		// "http://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png";
		"https://{s}.tile.openstreetmap.de/tiles/osmde/{z}/{x}/{y}.png";
	protected String tileAttribution = 
		"Map data © <a href=\"http://openstreetmap.org\">OpenStreetMap</a> contributors";			
	protected String leafletStyleSheetUrl = 
		"https://unpkg.com/leaflet@1.7.1/dist/leaflet.css";
	protected String leafletScriptUrl = 
		"https://unpkg.com/leaflet@1.7.1/dist/leaflet.js";
			
	
	public void setProjection(Projection p)
	{
		this.projection = p;
	}
	
	public RectangleMarker addRectangle(double left, double top,
		double right, double bottom)
	{
		return add(new RectangleMarker(left, top, right, bottom));
	}
	
	public RectangleMarker addRectangle(Envelope env)
	{
		return addRectangle(env.getMinX(), env.getMaxY(), 
			env.getMaxX(), env.getMinY());
	}
	
	public RectangleMarker addRectangle(Bounds b)
	{
		return addRectangle(b.minX(), b.maxY(), b.maxX(), b.minY());
	}

	public CircleMarker addCircle(double x, double y)
	{
		return add(new CircleMarker(x,y));
	}
	
	public PolylineMarker addPolyline(double... coords)
	{
		return add(new PolylineMarker(coords));
	}
	
	public PolylineMarker addPolyline(int... coords)
	{
		return add(new PolylineMarker(coords));
	}
	
	public PolylineMarker addPolyline(Coordinate... coords)
	{
		return add(new PolylineMarker(coords));
	}
	
	public PolygonMarker addPolygon(double... coords)
	{
		return add(new PolygonMarker(coords));
	}
	
	public PolygonMarker addPolygon(int... coords)
	{
		return add(new PolygonMarker(coords));
	}

	public PolygonMarker addPolygon(Coordinate... coords)
	{
		return add(new PolygonMarker(coords));
	}

	public PolygonMarker addPolygon(Coordinate[] shell, Coordinate[]... holes)
	{
		return add(new PolygonMarker(shell, holes));
	}
	
	public PolygonMarker addPolygon(Polygon p)
	{
		return add(new PolygonMarker(p));
	}
	
	public MultiPolygonMarker addMultiPolygon(MultiPolygon mp)
	{
		return add(new MultiPolygonMarker(mp));
	}

	public Marker addFeature(Feature feature)
	{
		Marker marker;
		if(feature instanceof Node)
		{
			Node node = (Node)feature;
			marker = addCircle(node.x(), node.y());
		}
		else if(feature instanceof Way)
		{
			Way way = (Way)feature;
			int[] coords = way.toXY();
			if(way.isArea())
			{
				marker = addPolygon(coords);
			}
			else
			{
				marker = addPolyline(coords);
			}
		}
		else
		{
			Relation relation = (Relation)feature;
			Geometry geom = relation.toGeometry();
			marker = addGeometry(geom);
			/*
			System.out.format("%s has %d geometries\n",  feature, geom.getNumGeometries());
			if(geom.getNumGeometries() > 0)
			{
				Geometry childGeom = geom.getGeometryN(0);
				Geometry boundary = childGeom.getBoundary();
				marker = addPolygon(boundary.getCoordinates());
			}
			 */
			/*
			for(int i=0; i<geom.getNumGeometries(); i++)
			{
				Geometry childGeom = geom.getGeometryN(i);
				Geometry boundary = childGeom.getBoundary();
				marker = addPolygon(boundary.getCoordinates());
			}
			*/
		}
		return marker;
	}
	
	public Marker addGeometry(Geometry g)
	{
		if(g instanceof Polygon)
		{
			return addPolygon((Polygon)g);
		}
		if(g instanceof Point)
		{
			Point p = (Point)g;
			return addCircle(p.getX(), p.getY());
		}
		if(g instanceof MultiPolygon)
		{
			return addMultiPolygon((MultiPolygon)g);
		}
		assert false: String.format("Unable to create marker for %s", g.getClass());
		return null; // TODO
	}
	
	public Marker addTile(int tile)
	{
		return addRectangle(Tile.bounds(tile)).
			tooltip(Tile.toString(tile));
	}

	public <T extends Marker> T add(T marker)
	{
		markers.add(marker);
		marker.setMap(this);
		return marker;
	}
	
	public double[] latLonFromXY(double x, double y)
	{
		if(projection != null)
		{
			//System.out.println("Projecting...");
			x=projection.projectX(x);
			y=projection.projectY(y);
		}
		double[] c = {y,x};
		return c;
	}
	
	public void emitScript(Appendable out) throws IOException
	{
		out.append("var map = L.map('map');\n");
		out.append("var tilesUrl='");
		out.append(tileServerUrl);
		out.append("';\nvar tilesAttrib='");
		out.append(tileAttribution);
		out.append("';\nvar tileLayer = new L.TileLayer(" +
			"tilesUrl, {minZoom: ");
		out.append(Integer.toString(minZoom));
		out.append(", maxZoom: ");
		out.append(Integer.toString(maxZoom));
		out.append(", attribution: tilesAttrib});\n" +
			"map.setView([51.505, -0.09], 13);\n" +
			"map.addLayer(tileLayer);\n" +
			"L.control.scale().addTo(map);\n");

		/*
		double minX = Double.MAX_VALUE;
		double maxX = Double.MIN_VALUE;
		double minY = Double.MAX_VALUE;
		double maxY = Double.MIN_VALUE;
		*/
		
		Envelope bounds = new Envelope();
		for(Marker marker: markers)
		{
			marker.emit(out);
			bounds.expandToInclude(marker.envelope());
		}
		out.append("map.fitBounds([");
		JavaScript.writeArray(out, latLonFromXY(
			bounds.getMinX(), bounds.getMaxY()));
		out.append(',');
		JavaScript.writeArray(out, latLonFromXY(
			bounds.getMaxX(), bounds.getMinY()));
		out.append("]);");
	}
	
	public void writeHtml(File file) throws IOException
	{
		PrintWriter out = new PrintWriter(file);
		out.print("<html><head><link rel=\"stylesheet\" href=\"");
		out.print(leafletStyleSheetUrl);
		out.print("\">\n<script src=\"");
		out.print(leafletScriptUrl);
		out.print("\"></script>\n<style>#map {height: 100%;}</style>\n");
		out.print("</head>\n<body>\n<div id=\"map\"> </div>\n");
		out.print("<script>");
		emitScript(out);
		out.print("</script></body></html>");
		out.close();
	}
}
