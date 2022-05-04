package com.geodesk.feature.query;

import com.geodesk.feature.*;
import com.geodesk.feature.Filter;
import com.geodesk.feature.filter.FilterSet;
import com.geodesk.feature.filter.TypeBits;
import com.geodesk.feature.store.FeatureStore;
import com.geodesk.geom.Bounds;

import java.util.Iterator;

// TODO: do we need to create a defensive copy of the bbox?

public class WorldView<T extends Feature> implements Features<T>
{
    protected FeatureStore store;
    protected Bounds bbox;
    protected FilterSet filters;

    public WorldView(FeatureStore store)
    {
        this.store = store;
        filters = FilterSet.ALL;
    }

    public WorldView(FeatureStore store, Bounds bbox)
    {
        this.store = store;
        this.bbox = bbox;
        filters = FilterSet.ALL;
    }

    public WorldView(FeatureStore store, FilterSet filters)
    {
        this.store = store;
        this.filters = filters;
    }

    public WorldView(WorldView<?> other, Bounds bbox)
    {
        this.store = other.store;
        this.filters = other.filters;
        this.bbox = bbox;           // TODO: intersect bbox
    }

    public WorldView(WorldView<?> other, int types)
    {
        this.store = other.store;
        this.bbox = other.bbox;
        // TODO: types
        this.filters = other.filters;
    }

    public WorldView(WorldView<?> other, FilterSet filters)
    {
        this.store = other.store;
        this.bbox = other.bbox;
        // TODO: types
        this.filters = filters;
    }

    @Override public boolean isEmpty()
    {
        return iterator().hasNext();
    }

    @Override public long count()
    {
        long count = 0;
        for(Feature f: this) count++;
        return count;
    }

    @Override public Features<?> features(String filter)
    {
        return new WorldView<>(this, store.getFilters(filter));
    }

    @Override public Features<Node> nodes()
    {
        return null;
    }

    @Override public Features<Node> nodes(String filter)
    {
        // TODO
        return new WorldView<Node>(this, TypeBits.NODES);
    }

    @Override public Features<Way> ways()
    {
        return new WorldView<Way>(this, TypeBits.WAYS);
    }

    @Override public Features<Way> ways(String filter)
    {
        return null;
    }

//    @Override public Features<?> areas()
//    {
//        return null;
//    }
//
//    @Override public Features<?> areas(String filter)
//    {
//        return null;
//    }

    @Override public Features<Relation> relations()
    {
        return null;
    }

    @Override public Features<Relation> relations(String filter)
    {
        return null;
    }

    @Override public Features<?> in(Bounds bbox)
    {
        return new WorldView(this, bbox);
    }

    /*
    @Override public boolean contains(T f)
    {
        return false;
    }

    @Override public boolean containsNode(long id)
    {
        return false;
    }

    @Override public boolean containsWay(long id)
    {
        return false;
    }

    @Override public boolean containsRelation(long id)
    {
        return false;
    }

    @Override public Node node(long id)
    {
        return null;
    }

    @Override public Way way(long id)
    {
        return null;
    }

    @Override public Relation relation(long id)
    {
        return null;
    }
     */

    @Override public Iterator<T> iterator()
    {
        return (Iterator<T>)new Query(this);
    }
}
