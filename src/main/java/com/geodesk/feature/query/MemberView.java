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

}
