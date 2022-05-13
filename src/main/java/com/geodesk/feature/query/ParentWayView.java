package com.geodesk.feature.query;

import com.geodesk.feature.Way;
import com.geodesk.feature.match.MatcherSet;

public class ParentWayView extends WorldView<Way>
{
    // TODO: for "in()", can't use the default approach of intersecting;
    //  instead, need to add the bbox as a Filter (this is rarely used)


    public ParentWayView(WorldView<?> other, int types, MatcherSet filters)
    {
        super(other, types, filters);
    }
}
