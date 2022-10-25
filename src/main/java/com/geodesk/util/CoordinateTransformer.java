package com.geodesk.util;

import java.io.IOException;

// OSM standard precision is 7 digits (100 nano-degrees)
// reasonable precision is 6 digits (10cm resolution)

public class CoordinateTransformer
{
    private final double scale;

    public CoordinateTransformer(int precision)
    {
        scale = Math.pow(10, precision);
    }

    public double transformX(double x)
    {
        return x;
    }

    public double transformY(double y)
    {
        return y;
    }

    private String toString(double v)
    {
        v = Math.round(v * scale) / scale;
        long lv = (long)v;
        if(lv == v) return Long.toString(lv);
        return Double.toString(v);
    }

    public void writeX(Appendable out, double x) throws IOException
    {
        out.append(toString(transformX(x)));
    }

    public void writeY(Appendable out, double y) throws IOException
    {
        out.append(toString(transformY(y)));
    }
}
