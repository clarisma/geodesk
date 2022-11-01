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

public class EmptyView<T extends Feature> implements Features<T>
{
    public static final Features<?> ANY = new EmptyView<>();
    public static final Features<Node> NODES = (Features<Node>)ANY;
    public static final Features<Way> WAYS = (Features<Way>)ANY;
    public static final Features<Relation> RELATIONS = (Features<Relation>)ANY;

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

    @Override public Features<?> select(String filter)
    {
        return ANY;
    }

    @Override public Features<Node> nodes()
    {
        return NODES;
    }

    @Override public Features<Node> nodes(String filter)
    {
        return NODES;
    }

    @Override public Features<Way> ways()
    {
        return WAYS;
    }

    @Override public Features<Way> ways(String filter)
    {
        return WAYS;
    }

    @Override public Features<Relation> relations()
    {
        return RELATIONS;
    }

    @Override public Features<Relation> relations(String filter)
    {
        return RELATIONS;
    }

    @Override public Features<T> in(Bounds bbox)
    {
        return this;
    }

    @Override public Features<T> of(Feature parent) { return (Features<T>)ANY; }

    @Override public Features<T> with(Feature child) { return (Features<T>)ANY; }

    @Override public Iterator<T> iterator()
    {
        return Collections.emptyIterator();
    }

    @Override public Features<T> select(Filter filter) { return this; }
}
