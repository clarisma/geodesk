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
    public MemberView(FeatureStore store, ByteBuffer buf, int pTable,
        int types, Matcher matcher, Filter filter)
    {
        super(store, buf, pTable, types, matcher, filter);
    }

    @Override protected Features newWith(int types, Matcher matcher, Filter filter)
    {
        return new MemberView(store, buf, ptr, types, matcher, filter);
    }

    @Override public Iterator<Feature> iterator()
    {
        return new MemberIterator(store, buf, ptr, types, matcher, filter);
    }
}
