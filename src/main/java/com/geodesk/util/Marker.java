/*
 * Copyright (c) Clarisma / GeoDesk contributors
 *
 * This source code is licensed under the Apache 2.0 license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.geodesk.util;

import com.geodesk.geom.Bounds;
import org.locationtech.jts.geom.CoordinateSequence;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * A marker on a Leaflet-based interactive map.
 */
public abstract class Marker
{
    private Map<String,Object> options = new HashMap<>();
    private String tooltip;
    private String url;
    protected MapMaker map;

    /**
     * Sets the content to be displayed whenever the cursor is hovered over
     * this Marker.
     *
     * @param tooltip   HTML-formatted content
     * @return          this Marker
     */
    public Marker tooltip(String tooltip)
    {
        this.tooltip = tooltip;
        return this;
    }

    /**
     * Sets the URL which is navigated when the user clicks on this Marker.
     *
     * @param url   a URL
     * @return      this Marker
     */
    public Marker url(String url)
    {
        this.url = url;
        return this;
    }

    void setMap(MapMaker map)
    {
        this.map = map;
    }

    /**
     * Specifies options for this Marker.
     *
     * See the <a href="https://leafletjs.com/reference.html#path">
     * Leaflet documentation</a> for a list of available options.
     *
     * @param moreOptions   a key-value map
     * @return              this Marker
     */
    public Marker options(Map<String,Object> moreOptions)
    {
        options.putAll(moreOptions);
        return this;
    }

    /**
     * Speficies a single option for this Marker.
     *
     * See the <a href="https://leafletjs.com/reference.html#path">
     * Leaflet documentation</a> for a list of available options.
     *
     * @param key       the name of the option (e.g. `opacity` or `lineCap`)
     * @param value     the value of the option
     * @return
     */
    public Marker option(String key, Object value)
    {
        options.put(key, value);
        return this;
    }

    /**
     * Specifies the color of this Marker.
     *
     * @param color     a named color or RGB spec (e.g. <code>#3388ff</code>)
     * @return          this Marker
     */
    public Marker color(String color)
    {
        return option("color", color);
    }

    public String defaultTooltip()
    {
        return null;
    }

    public boolean isVisible()
    {
        return true;
    }

    protected void writePoint(Appendable out, double x, double y) throws IOException
    {
        out.append("L.circle(");
        map.writeXY(out, x, y);
    }

    protected void writeCoordinates(Appendable out, CoordinateSequence coords) throws IOException
    {
        out.append("[");
        int len = coords.size();
        for(int i=0; i<len; i++)
        {
            if(i > 0) out.append(',');
            map.writeXY(out, coords.getX(i), coords.getY(i));
        }
        out.append("]");
    }

    public abstract Bounds bounds();

    protected abstract void writeStub(Appendable out) throws IOException;

    public void write(Appendable out) throws IOException
    {
        writeStub(out);
        if(options.size() > 0)
        {
            out.append(',');
            JavaScript.writeMap(out, options);
        }
        out.append(')');
        if(tooltip != null && tooltip.length() > 0)
        {
            out.append(".bindTooltip(");
            JavaScript.writeString(out, tooltip);
            out.append(")");
        }
        if(url != null && url.length() > 0)
        {
            out.append(".on('click', function(){window.location=");
            JavaScript.writeString(out, url);
            out.append(";})");
        }
        out.append(".addTo(map);\n");
    }
}
