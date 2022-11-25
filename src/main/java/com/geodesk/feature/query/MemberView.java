/*
 * Copyright (c) Clarisma / GeoDesk contributors
 *
 * This source code is licensed under the Apache 2.0 license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.geodesk.feature.query;

import com.geodesk.feature.*;
import com.geodesk.feature.filter.AndFilter;
import com.geodesk.feature.match.AndMatcher;
import com.geodesk.feature.match.Matcher;
import com.geodesk.feature.match.MatcherSet;
import com.geodesk.feature.match.TypeBits;
import com.geodesk.feature.store.FeatureStore;
import com.geodesk.geom.Bounds;

import java.nio.ByteBuffer;
import java.util.Iterator;

public class MemberView<T extends Feature> extends TableView<T>
{
    private final int types;

    public MemberView(FeatureStore store, ByteBuffer buf, int pTable, int types, Matcher matcher)
    {
        super(store, buf, pTable, matcher);
        this.types = types;
    }

    public MemberView(MemberView other, int types, Matcher matcher)
    {
        super(other, matcher);
        this.types = types;
    }

    public MemberView(MemberView other, Filter filter)
    {
        super(other, filter);
        this.types = other.types;
    }


    private Features<T> select(int types, String query)
    {
        MatcherSet matchers = store.getMatchers(query);
        return select(types & matchers.types(), matchers.members());
    }

    private Features<T> select(int types, Matcher matcher)
    {
        types &= this.types;
        if(types == 0) return (Features<T>)EmptyView.ANY;
        return new MemberView<>(this, types, matcher);
    }

    @Override public Features<T> select(String query)
    {
        return select(TypeBits.ALL, query);
    }

    @Override public Features<Node> nodes()
    {
        return (Features<Node>)select(TypeBits.NODES, Matcher.ALL);
    }

    @Override public Features<Node> nodes(String query)
    {
        return (Features<Node>)select(TypeBits.NODES, query);
    }

    @Override public Features<Way> ways()
    {
        return (Features<Way>)select(TypeBits.WAYS, Matcher.ALL);
    }

    @Override public Features<Way> ways(String query)
    {
        return (Features<Way>)select(TypeBits.WAYS, query);
    }

    @Override public Features<Relation> relations()
    {
        return (Features<Relation>)select(TypeBits.RELATIONS, Matcher.ALL);
    }

    @Override public Features<Relation> relations(String query)
    {
        return (Features<Relation>)select(TypeBits.RELATIONS, query);
    }

    @Override public Iterator<T> iterator()
    {
        return (Iterator<T>)new MemberIterator(store, buf, ptr, types, matcher, filter);
    }

    @Override public Features<T> select(Filter filter)
    {
        throw new RuntimeException("todo");
    }

}
