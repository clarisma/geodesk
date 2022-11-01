/*
 * Copyright (c) Clarisma / GeoDesk contributors
 *
 * This source code is licensed under the Apache 2.0 license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.geodesk.feature.match;

import static com.geodesk.feature.match.TypeBits.*;

public class MatcherSet
{
    private final int types;
    private final Matcher nodes;
    private final Matcher ways;
    private final Matcher areas;
    private final Matcher relations;
    private final Matcher members;

    public static final MatcherSet ALL = new MatcherSet(TypeBits.ALL, Matcher.ALL);

    public MatcherSet(int types, Matcher matcher)
    {
        this.types = types;
        nodes = ways = areas = relations = members = matcher;
    }

    public MatcherSet(int types, Matcher n, Matcher w, Matcher a, Matcher r, Matcher m)
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

    public Matcher nodes()
    {
        return nodes;
    }

    public Matcher ways()
    {
        return ways;
    }

    public Matcher areas()
    {
        return areas;
    }

    public Matcher relations()
    {
        return relations;
    }

    public Matcher members()
    {
        return members;
    }

    private static Matcher mergeFilter(int newTypes, int indexType, Matcher a, Matcher b)
    {
        newTypes &= indexType;
        if(newTypes == 0) return null;
        Matcher filter = new AndMatcher(a, b);
        if(newTypes == indexType) return filter;
        return new TypeMatcher(newTypes, filter);
    }

    private static Matcher constrainFilter(int newTypes, int indexType, Matcher filter)
    {
        newTypes &= indexType;
        if(newTypes == 0) return null;
        if(newTypes == indexType) return filter;
        return new TypeMatcher(newTypes, filter);
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

    public MatcherSet and(int newTypes, MatcherSet other)
    {
        newTypes &= types & other.types;
        Matcher n = mergeFilter(newTypes, NODES, nodes, other.nodes);
        Matcher w = mergeFilter(newTypes, NONAREA_WAYS, ways, other.ways);
        Matcher a = mergeFilter(newTypes, AREAS, areas, other.areas);
        Matcher r = mergeFilter(newTypes, NONAREA_RELATIONS, relations, other.relations);
        Matcher m = new AndMatcher(members, other.members);
        return new MatcherSet(newTypes, n, w, a, r, m);
    }

    public MatcherSet and(int newTypes)
    {
        newTypes &= types;
        Matcher n = constrainFilter(newTypes, NODES, nodes);
        Matcher w = constrainFilter(newTypes, NONAREA_WAYS, ways);
        Matcher a = constrainFilter(newTypes, AREAS, areas);
        Matcher r = constrainFilter(newTypes, NONAREA_RELATIONS, relations);
        return new MatcherSet(newTypes, n, w, a, r, members);
    }
}
