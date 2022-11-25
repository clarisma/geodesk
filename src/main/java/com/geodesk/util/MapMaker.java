/*
 * Copyright (c) Clarisma / GeoDesk contributors
 *
 * This source code is licensed under the Apache 2.0 license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.geodesk.util;

import com.clarisma.common.util.Log;
import com.geodesk.core.Box;
import com.geodesk.core.Mercator;
import com.geodesk.feature.Feature;
import com.geodesk.geom.Bounds;
import org.locationtech.jts.geom.*;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

/**
 * A class for generating a Leaflet-based interactive map.
 */
public class MapMaker
{
    private String id = "map";
    private List<Marker> markers = new ArrayList<>();
    private int minZoom = 0;
    private int maxZoom = 19;
    private String tileServerUrl =
        // "http://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png";
        "https://{s}.tile.openstreetmap.de/tiles/osmde/{z}/{x}/{y}.png";
    private String attribution =
        "Map data © <a href=\"http://openstreetmap.org\">OpenStreetMap</a> contributors";
    private String leafletStyleSheetUrl =
        "https://unpkg.com/leaflet@1.8.0/dist/leaflet.css";
    private String leafletScriptUrl =
        "https://unpkg.com/leaflet@1.8.0/dist/leaflet.js";

    /**
     * Sets the URL template for the source of map tiles.
     *
     * @param url  the URL template
     */
    public void tiles(String url)
    {
        this.tileServerUrl = url;
    }

    /**
     * Sets the attribution displayed on the map.
     *
     * @param attribution   the attribution text
     */
    public void attribution(String attribution)
    {
        this.attribution = attribution;
    }

    private static class GeometryMarker extends Marker
    {
        private final Geometry geom;

        GeometryMarker(Geometry geom)
        {
            assert geom != null;
            this.geom = geom;
        }

        // TODO: winding order?
        private void writePolygonCoordinates(Appendable out, Polygon p) throws IOException
        {
            out.append('[');
            writeCoordinates(out, p.getExteriorRing().getCoordinateSequence());
            for(int i=0; i<p.getNumInteriorRing(); i++)
            {
                out.append(',');
                writeCoordinates(out, p.getInteriorRingN(i).getCoordinateSequence());
            }
            out.append(']');
        }

        @Override public Bounds bounds()
        {
            return Box.fromEnvelope(geom.getEnvelopeInternal());
        }

        @Override protected void writeStub(Appendable out) throws IOException
        {
            writeStub(out, geom);
        }

        protected void writeStub(Appendable out, Geometry g) throws IOException
        {
            if(g instanceof Polygonal)
            {
                out.append("L.polygon(");
                int geometryCount = g.getNumGeometries();
                if(geometryCount == 1) // single polygon
                {
                    writePolygonCoordinates(out, (Polygon)g);
                }
                else // multipolygon
                {
                    out.append('[');
                    for(int i=0; i<geometryCount; i++)
                    {
                        if(i>0) out.append(',');
                        writePolygonCoordinates(out, (Polygon)g.getGeometryN(i));
                    }
                    out.append(']');
                }
            }
            else if(g instanceof Lineal)
            {
                out.append("L.polyline(");
                int geometryCount = g.getNumGeometries();
                if(geometryCount == 1) // single polyline
                {
                    writeCoordinates(out, ((LineString)g.getGeometryN(0))
                        .getCoordinateSequence());
                }
                else // multipolyline
                {
                    out.append('[');
                    for(int i=0; i<geometryCount; i++)
                    {
                        if(i>0) out.append(',');
                        writeCoordinates(out, ((LineString)g.getGeometryN(i))
                            .getCoordinateSequence());
                    }
                    out.append(']');
                }
            }
            else if(g instanceof Point pt)
            {
                writePoint(out, pt.getX(), pt.getY());
            }
            else    // GeometryCollection
            {
                out.append("L.featureGroup([");
                for(int i=0; i<g.getNumGeometries(); i++)
                {
                    if(i>0) out.append(',');
                    writeStub(out, g.getGeometryN(i));
                    if(options.size() > 0)
                    {
                        out.append(',');
                        JavaScript.writeMap(out, options);
                    }
                    out.append(')');
                }
                out.append(']');
            }
        }
    }

    private static class BoxMarker extends Marker
    {
        private final Bounds box;

        BoxMarker(Bounds box)
        {
            this.box = box;
        }

        @Override public Bounds bounds()
        {
            return box;
        }

        @Override protected void writeStub(Appendable out) throws IOException
        {
            out.append("L.rectangle([");
            map.writeXY(out, box.minX(), box.minY());
            out.append(',');
            map.writeXY(out, box.maxX(), box.maxY());
            out.append(']');
        }
    }

    private static class EmptyMarker extends Marker
    {
        EmptyMarker()
        {
        }

        @Override public Bounds bounds()
        {
            return new Box();       // TODO: cache
        }

        public boolean isVisible()
        {
            return false;
        }

        @Override protected void writeStub(Appendable out)
        {
            // do nothing
        }

        @Override public void write(Appendable out)
        {
            // do nothing
        }
    }


    void writeXY(Appendable out, double x, double y) throws IOException
    {
        out.append('[');
        out.append(Double.toString(Mercator.latFromY(y)));
        out.append(',');
        out.append(Double.toString(Mercator.lonFromX(x)));
        out.append(']');
    }

    private Marker add(Marker marker)
    {
        marker.setMap(this);
        markers.add(marker);
        return marker;
    }

    /**
     * Adds a marker for the given JTS Geometry.
     *
     * @param geom  the Geometry (any type)
     * @return the marker
     */
    public Marker add(Geometry geom)
    {
        return add(geom.isEmpty() ? new EmptyMarker() : new GeometryMarker(geom));
    }

    /**
     * Adds a marker for the given bounding box.
     *
     * @param box  the bounding box
     * @return the marker
     */
    public Marker add(Bounds box)
    {
        return add(new BoxMarker(box));
    }

    /**
     * Adds a marker for the given feature.
     *
     * @param feature  the feature
     * @return the marker
     */
    public Marker add(Feature feature)
    {
        // TODO: could optimize for node & way, read coordinates directly
        //  without need to create a JTS Geometry
        // TODO: if(feature.isPlaceholder)

        Geometry geom = feature.toGeometry();
        assert geom != null: "Null geometry for " + feature;
        return add(new GeometryMarker(geom));
    }

    public void add(Iterable<? extends Feature> features)
    {
        for(Feature f: features) add(f);
    }

    /**
     * Generates a self-contained HTML file that displays the interactive map
     * and all its markers.
     *
     * @param path  name of the file to generate
     * @throws IOException
     */
    public void save(String path) throws IOException
    {
        PrintWriter out = new PrintWriter(path);
        write(out);
        out.close();
    }

    public void write(Appendable out) throws IOException
    {
        out.append("<html><head><link rel=\"stylesheet\" href=\"");
        out.append(leafletStyleSheetUrl);
        out.append("\">\n<script src=\"");
        out.append(leafletScriptUrl);
        out.append("\"></script>\n<style>\n#map {height: 100%;}\nbody {margin:0;}\n</style>\n");
        out.append("</head>\n<body>\n<div id=\"map\"> </div>\n");
        out.append("<script>");
        writeScript(out);
        out.append("</script></body></html>");
    }

    private void writeScript(Appendable out) throws IOException
    {
        out.append("var map = L.map('map');\n");
        out.append("var tilesUrl='");
        out.append(tileServerUrl);
        out.append("';\nvar tilesAttrib='");
        out.append(attribution);
        out.append("';\nvar tileLayer = new L.TileLayer(" +
            "tilesUrl, {minZoom: ");
        out.append(Integer.toString(minZoom));
        out.append(", maxZoom: ");
        out.append(Integer.toString(maxZoom));
        out.append(", attribution: tilesAttrib});\n" +
            "map.setView([51.505, -0.09], 13);\n" +     // TODO
            "map.addLayer(tileLayer);\n" +
            "L.control.scale().addTo(map);\n");

        Box bounds = new Box();
        for(Marker marker: markers)
        {
            if(marker.isVisible())
            {
                marker.write(out);
                bounds.expandToInclude(marker.bounds());
            }
        }
        out.append("map.fitBounds([");
        writeXY(out, bounds.minX(), bounds.minY());
        out.append(',');
        writeXY(out, bounds.maxX(), bounds.maxY());
        out.append("]);");
    }
}
