package com.geodesk.feature.filter;

import com.geodesk.feature.Feature;
import com.geodesk.feature.Filter;
import com.geodesk.feature.store.StoredWay;

public class ParentWayFilterXY implements Filter
{
    private final long xy;

    public ParentWayFilterXY(long xy)
    {
        this.xy = xy;
    }

    @Override public boolean accept(Feature feature)
    {
        StoredWay way = (StoredWay)feature;
        StoredWay.XYIterator iter = way.iterXY(0);
            // pass 0 as the area flag, because we don't want the start node
            // returned twice
            // TODO: make sure iterator does not depend on other flags
        while(iter.hasNext())
        {
            if(iter.nextXY() == xy) return true;
        }
        return false;
    }
}
