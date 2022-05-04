package com.geodesk.feature.filter;

import com.geodesk.feature.Filter;

public class FilterSet
{
    private final Filter nodes;
    private final Filter ways;
    private final Filter areas;
    private final Filter relations;

    public static final FilterSet ALL = new FilterSet(Filter.ALL);

    public FilterSet (Filter filter)
    {
        nodes = ways = areas = relations = filter;
    }

    public FilterSet (Filter n, Filter w, Filter a, Filter r)
    {
        nodes = n;
        ways = w;
        areas = a;
        relations = r;
    }

    public FilterSet (FilterSet other)
    {
        nodes = other.nodes;
        ways = other.ways;
        areas = other.areas;
        relations = other.relations;
    }

    public Filter nodes()
    {
        return nodes;
    }

    public Filter ways()
    {
        return ways;
    }

    public Filter areas()
    {
        return areas;
    }

    public Filter relations()
    {
        return relations;
    }

}
