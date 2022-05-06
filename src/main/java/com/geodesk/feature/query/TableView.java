package com.geodesk.feature.query;

import com.geodesk.feature.Feature;
import com.geodesk.feature.Features;
import com.geodesk.feature.Filter;
import com.geodesk.feature.Node;
import com.geodesk.feature.filter.AndFilter;
import com.geodesk.feature.store.FeatureStore;
import com.geodesk.geom.Bounds;

import java.nio.ByteBuffer;

public abstract class TableView<T extends Feature> implements Features<T>
{
    protected final FeatureStore store;
    protected final ByteBuffer buf;
    protected final int ptr;
    protected final Filter filter;

    public TableView(FeatureStore store, ByteBuffer buf, int ptr, Filter filter)
    {
        this.store = store;
        this.buf = buf;
        this.ptr = ptr;
        this.filter = filter;
    }

    public TableView(TableView other, Filter filter)
    {
        this.store = other.store;
        this.buf = other.buf;
        this.ptr = other.ptr;
        if(other.filter != Filter.ALL)
        {
            filter = new AndFilter(other.filter, filter);
        }
        this.filter = filter;
    }

    @Override public Features<T> in(Bounds bbox)
    {
        throw new UnsupportedOperationException("todo");
    }

}
