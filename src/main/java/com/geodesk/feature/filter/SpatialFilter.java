package com.geodesk.feature.filter;

import com.geodesk.feature.Feature;
import com.geodesk.feature.Filter;

public class SpatialFilter implements Filter
{
    @Override public boolean accept(Feature feature)
    {
        return acceptGeometry(feature.toGeometry());
    }
}
