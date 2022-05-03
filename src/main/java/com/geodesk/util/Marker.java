package com.geodesk.util;

import com.geodesk.geom.Bounds;
import org.locationtech.jts.geom.CoordinateSequence;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public abstract class Marker
{
    private Map<String,Object> options = new HashMap<>();
    private String tooltip;
    private String url;
    protected MapMaker map;

    public Marker tooltip(String tooltip)
    {
        this.tooltip = tooltip;
        return this;
    }

    public Marker url(String url)
    {
        this.url = url;
        return this;
    }

    public void setMap(MapMaker map)
    {
        this.map = map;
    }

    public Marker options(Map<String,Object> moreOptions)
    {
        options.putAll(moreOptions);
        return this;
    }

    public Marker option(String key, Object value)
    {
        options.put(key, value);
        return this;
    }

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
