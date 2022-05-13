package com.geodesk.feature.query;

import com.geodesk.feature.*;
import com.geodesk.feature.match.Matcher;
import com.geodesk.feature.store.FeatureStore;
import com.geodesk.geom.Bounds;

import java.nio.ByteBuffer;
import java.util.Iterator;

public class MemberView<T extends Feature> implements Features<T>
{
    private final FeatureStore store;
    private final ByteBuffer buf;
    private final int pTable;
    private final Matcher filter;

    public MemberView(FeatureStore store, ByteBuffer buf, int pTable, Matcher filter)
    {
        this.store = store;
        this.buf = buf;
        this.pTable = pTable;
        this.filter = filter;
    }

    @Override public boolean isEmpty()
    {
        // TODO
        return false;
    }

    @Override public long count()
    {
        // TODO
        return 0;
    }

    @Override public Features<?> features(String filter)
    {
        return null;
    }

    @Override public Features<Node> nodes()
    {
        return null;
    }

    @Override public Features<Node> nodes(String filter)
    {
        return null;
    }

    @Override public Features<Way> ways()
    {
        return null;
    }

    @Override public Features<Way> ways(String filter)
    {
        return null;
    }

    @Override public Features<Relation> relations()
    {
        return null;
    }

    @Override public Features<Relation> relations(String filter)
    {
        return null;
    }

    @Override public Features<T> in(Bounds bbox)
    {
        return null;
    }

    @Override public Iterator<T> iterator()
    {
        return (Iterator<T>)new MemberIterator(store, buf, pTable, filter);
    }
}
