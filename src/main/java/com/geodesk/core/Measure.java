package com.geodesk.core;

import com.geodesk.feature.Feature;

public class Measure
{
    public static double length(Feature f)
    {
        return f.length();
    }

    public static double area(Feature f)
    {
        return f.area();
    }
}
