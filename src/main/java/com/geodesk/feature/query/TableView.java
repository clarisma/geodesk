/*
 * Copyright (c) Clarisma / GeoDesk contributors
 *
 * This source code is licensed under the Apache 2.0 license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.geodesk.feature.query;

import com.geodesk.feature.*;
import com.geodesk.feature.filter.AndFilter;
import com.geodesk.feature.filter.BoundsFilter;
import com.geodesk.feature.match.Matcher;
import com.geodesk.feature.match.AndMatcher;
import com.geodesk.feature.match.MatcherSet;
import com.geodesk.feature.store.FeatureStore;
import com.geodesk.geom.Bounds;

import java.nio.ByteBuffer;

/**
 * A Feature Collection that is materialized by scanning a table.
 *
 * @param <T>
 */
public abstract class TableView extends View
{
    protected final ByteBuffer buf;
    protected final int ptr;

    public TableView(FeatureStore store, ByteBuffer buf, int ptr,
        int types, Matcher matcher, Filter filter)
    {
        super(store, types, matcher, filter);
        this.buf = buf;
        this.ptr = ptr;
    }

    @Override public Features in(Bounds bbox)
    {
        return select(new BoundsFilter(bbox));
    }
}
