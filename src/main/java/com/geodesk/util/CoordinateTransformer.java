/*
 * Copyright (c) Clarisma / GeoDesk contributors
 *
 * This source code is licensed under the Apache 2.0 license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.geodesk.util;

import com.geodesk.core.Mercator;

import java.io.IOException;

// OSM standard precision is 7 digits (100 nano-degrees; 1cm)
// reasonable precision is 6 digits (10cm resolution)

// TODO: rename class to CoordinateConverter?
// TODO: rename write to format?
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

    public String toString(double v)
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

    public static class FromMercator extends CoordinateTransformer
    {
        public FromMercator(int precision)
        {
            super(precision);
        }

        @Override public double transformX(double x)
        {
            return Mercator.lonFromX(x);
        }

        @Override public double transformY(double y)
        {
            return Mercator.latFromY(y);
        }
    }

    public static class ToMercator extends CoordinateTransformer
    {
        public ToMercator()
        {
            super(7);
            // TODO: technically, imps are always integer
        }

        @Override public double transformX(double x)
        {
            return Mercator.xFromLon(x);
        }

        @Override public double transformY(double y)
        {
            return Mercator.yFromLat(y);
        }
    }
}
