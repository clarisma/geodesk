/*
 * Copyright (c) Clarisma / GeoDesk contributors
 *
 * This source code is licensed under the Apache 2.0 license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.geodesk.feature.query;

import com.geodesk.feature.*;
import com.geodesk.geom.Bounds;

import java.util.Collections;
import java.util.Iterator;

public class EmptyView implements Features
{
    public static final Features ANY = new EmptyView();

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
        return this;
    }

    @Override public Features in(Bounds bbox)
    {
        return this;
    }

    @Override public Features of(Feature parent) { return this; }

    @Override public Features with(Feature child) { return this; }

    @Override public Iterator<Feature> iterator()
    {
        return Collections.emptyIterator();
    }

    @Override public Features select(Filter filter) { return this; }
}
