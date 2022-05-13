package com.geodesk.feature;

import org.locationtech.jts.geom.Geometry;

public interface Filter
{
    default boolean accept(Feature feature)
    {
        return true;
    }

    default boolean acceptGeometry(Geometry geom)
    {
        return true;
    }
}
