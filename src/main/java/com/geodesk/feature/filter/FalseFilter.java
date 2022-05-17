package com.geodesk.feature.filter;

import com.geodesk.feature.Feature;
import com.geodesk.feature.Filter;

public class FalseFilter implements Filter
{
    public static final Filter INSTANCE = new FalseFilter();

    @Override public boolean accept(Feature f)
    {
        return false;
    }
}
