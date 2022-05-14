package com.geodesk.feature.query;

import com.geodesk.feature.*;
import com.geodesk.feature.filter.AndFilter;
import com.geodesk.feature.filter.BoundsFilter;
import com.geodesk.feature.match.Matcher;
import com.geodesk.feature.match.AndMatcher;
import com.geodesk.feature.store.FeatureStore;
import com.geodesk.geom.Bounds;

import java.nio.ByteBuffer;

public abstract class TableView<T extends Feature> implements Features<T>
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
            filter = new AndFilter(other.filter, filter);
        }
        this.filter = filter;
    }

    @Override public Features<Node> nodes()
    {
        return EmptyView.NODES;
    }

    @Override public Features<Node> nodes(String filter)
    {
        return EmptyView.NODES;
    }

    @Override public Features<Way> ways()
    {
        return EmptyView.WAYS;
    }

    @Override public Features<Way> ways(String query)
    {
        return EmptyView.WAYS;
    }

    @Override public Features<Relation> relations()
    {
        return EmptyView.RELATIONS;
    }

    @Override public Features<Relation> relations(String query)
    {
        return EmptyView.RELATIONS;
    }


    @Override public Features<T> in(Bounds bbox)
    {
        return select(new BoundsFilter(bbox));
    }
}
