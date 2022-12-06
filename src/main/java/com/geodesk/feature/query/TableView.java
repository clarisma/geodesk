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
import com.geodesk.feature.store.FeatureStore;
import com.geodesk.geom.Bounds;

import java.nio.ByteBuffer;

/**
 * A Feature Collection that is materialized by scanning a table.
 */
public abstract class TableView implements Features
{
    protected final FeatureStore store;
    protected final ByteBuffer buf;
    protected final int ptr;
    protected final Matcher matcher;
    protected final Filter filter;

    public TableView(FeatureStore store, ByteBuffer buf, int ptr, Matcher matcher)
    {
        this.store = store;
        this.buf = buf;
        this.ptr = ptr;
        this.matcher = matcher;
        this.filter = null;
    }

    // TODO: decide if matchers should be merged or replaced
    //  (WorldView replaces)
    public TableView(TableView other, Matcher matcher)
    {
        this.store = other.store;
        this.buf = other.buf;
        this.ptr = other.ptr;
        if(other.matcher != Matcher.ALL)
        {
            matcher = new AndMatcher(other.matcher, matcher);
            // TODO: do this elesewhere, combining matchers may result in an empty view
        }
        else if(matcher == Matcher.ALL)
        {
            matcher = other.matcher;
        }
        this.matcher = matcher;
        this.filter = other.filter;
    }

    public TableView(TableView other, Filter filter)
    {
        this.store = other.store;
        this.buf = other.buf;
        this.ptr = other.ptr;
        this.matcher = other.matcher;
        assert filter != null;
        if(other.filter != null)
        {
            filter = AndFilter.create(other.filter, filter);
            // TODO: do this elsewhere, combining filters may result in an empty view
        }
        this.filter = filter;
    }

    @Override public Features in(Bounds bbox)
    {
        return select(new BoundsFilter(bbox));
    }
}
