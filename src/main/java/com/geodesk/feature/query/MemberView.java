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

import static com.geodesk.feature.match.TypeBits.*;

public class MemberView extends TableView
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


    private Features select(int types, String query)
    {
        MatcherSet matchers = store.getMatchers(query);
        return select(types & matchers.types(), matchers.members());
    }

    private Features select(int types, Matcher matcher)
    {
        types &= this.types;
        if(types == 0) return EmptyView.ANY;
        return new MemberView(this, types, matcher);
    }

    @Override public Features select(String query)
    {
        return select(TypeBits.ALL, query);
    }

    @Override public Iterator iterator()
    {
        return new MemberIterator(store, buf, ptr, types, matcher, filter);
    }

    @Override public Features select(Filter filter)
    {
        throw new RuntimeException("todo");
    }

    // TODO: consolidate this
    @Override public <U extends Feature> View<U> select(Class<U> type)
    {
        int newTypes;
        if(type == Way.class)
        {
            newTypes = types & WAYS;
        }
        else if(type == Relation.class)
        {
            newTypes = types & RELATIONS;
        }
        else
        {
            assert type == Node.class;
            newTypes = types & NODES;
        }
        if(newTypes == types) return (View<U>)this;
        if(newTypes == 0) return (View<U>)EmptyView.ANY;
        return (View<U>) new MemberView(this, newTypes, matcher);
    }
}
