package com.geodesk.feature.filter;

import com.geodesk.feature.Filter;

import static com.geodesk.feature.filter.TypeBits.*;
import static com.geodesk.feature.filter.TypeBits.RELATIONS;

public class FilterSet
{
    private final int types;
    private final Filter nodes;
    private final Filter ways;
    private final Filter areas;
    private final Filter relations;
    private final MemberFilter members;

    public static final FilterSet ALL = new FilterSet(TypeBits.ALL, Filter.ALL);

    public FilterSet (int types, Filter filter)
    {
        this.types = types;
        nodes = ways = areas = relations = filter;
        members = new SimpleMemberFilter(filter);
    }

    public FilterSet (int types, Filter n, Filter w, Filter a, Filter r, MemberFilter m)
    {
        this.types = types;
        nodes = n;
        ways = w;
        areas = a;
        relations = r;
        members = m;
    }

    /*
    public FilterSet (FilterSet other)
    {
        nodes = other.nodes;
        ways = other.ways;
        areas = other.areas;
        relations = other.relations;
    }
     */

    public int types() { return types; }

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

    public MemberFilter members()
    {
        return members;
    }

    private static Filter mergeFilter(int newTypes, int indexType, Filter a, Filter b)
    {
        newTypes &= indexType;
        if(newTypes == 0) return null;
        Filter filter = new AndFilter(a, b);
        if(newTypes == indexType) return filter;
        return new TypeFilter(newTypes, filter);
    }

    private static Filter constrainFilter(int newTypes, int indexType, Filter filter)
    {
        newTypes &= indexType;
        if(newTypes == 0) return null;
        if(newTypes == indexType) return filter;
        return new TypeFilter(newTypes, filter);
    }


    /*
    private static FilterSet mergeFilters(int types, FilterSet a, FilterSet b)
    {
        Filter nodes = mergeFilter(types, NODES, a.nodes(), b.nodes());
        Filter ways = mergeFilter(types, WAYS, a.ways(), b.ways());
        Filter areas = mergeFilter(types, AREAS, a.areas(), b.areas());
        Filter relations = mergeFilter(types, RELATIONS, a.relations(), b.relations());
        MemberFilter members = new AndMemberFilter(a.members(), b.members());
        return new FilterSet(types, nodes, ways, areas, relations, members);
    }
     */

    public FilterSet and(int newTypes, FilterSet other)
    {
        newTypes &= types & other.types;
        Filter n = mergeFilter(newTypes, NODES, nodes, other.nodes);
        Filter w = mergeFilter(newTypes, NONAREA_WAYS, ways, other.ways);
        Filter a = mergeFilter(newTypes, AREAS, areas, other.areas);
        Filter r = mergeFilter(newTypes, NONAREA_RELATIONS, relations, other.relations);
        MemberFilter m = new AndMemberFilter(members, other.members);
        return new FilterSet(newTypes, n, w, a, r, m);
    }

    public FilterSet and(int newTypes)
    {
        newTypes &= types;
        Filter n = constrainFilter(newTypes, NODES, nodes);
        Filter w = constrainFilter(newTypes, NONAREA_WAYS, ways);
        Filter a = constrainFilter(newTypes, AREAS, areas);
        Filter r = constrainFilter(newTypes, NONAREA_RELATIONS, relations);
        return new FilterSet(newTypes, n, w, a, r, members);
    }
}
