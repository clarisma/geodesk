/*
 * Copyright (c) Clarisma / GeoDesk contributors
 *
 * This source code is licensed under the Apache 2.0 license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.geodesk.feature.query;

import com.geodesk.feature.*;
import com.geodesk.feature.match.Matcher;
import com.geodesk.feature.match.QueryException;
import com.geodesk.geom.Bounds;

import java.util.Collections;
import java.util.Iterator;

public class EmptyView extends View
{
    public static final Features ANY = new EmptyView();

    public EmptyView()
    {
        super(null, 0, null, null);
    }

    @Override protected Features select(int newTypes, String query)
    {
        return this;
    }

    @Override protected Features newWith(int types, Matcher matcher, Filter filter)
    {
        return this;
    }

    @Override public boolean isEmpty()
    {
        return true;
    }

    @Override public long count()
    {
        return 0;
    }

    @Override public boolean contains(Object f)
    {
        return false;
    }

    @Override public Features select(String filter)
    {
        return ANY;
    }

    @Override public Features in(Bounds bbox)
    {
        return this;
    }

    @Override public Iterator iterator()
    {
        return Collections.emptyIterator();
    }

    @Override public Features select(Filter filter) { return this; }

    @Override public Features nodesOf(Feature parent)
    {
        return this;
    }

    @Override public Features membersOf(Feature parent)
    {
        return this;
    }

    @Override public Features parentsOf(Feature child)
    {
        return this;
    }
}
