package com.geodesk.feature.store;

import com.geodesk.feature.*;
import com.geodesk.feature.filter.Filter;
import com.geodesk.feature.filter.FilterSet;
import com.geodesk.geom.Bounds;

import java.util.Iterator;

// TODO: do we need to create a defensive copy of the bbox?

public class FeatureView<T extends Feature> implements Features<T>
{
    protected FeatureStoreBase store;
    protected Bounds bbox;
    protected FilterSet filters;

    public FeatureView(FeatureStoreBase store)
    {
        this.store = store;
        filters = FilterSet.ALL;
    }

    public FeatureView(FeatureStoreBase store, Bounds bbox)
    {
        this.store = store;
        this.bbox = bbox;
        filters = FilterSet.ALL;
    }

    public FeatureView(FeatureStoreBase store, FilterSet filters)
    {
        this.store = store;
        this.filters = filters;
    }

    public FeatureView(FeatureView<?> other, Bounds bbox)
    {
        this.store = other.store;
        this.filters = other.filters;
        this.bbox = bbox;           // TODO: intersect bbox
    }

    public FeatureView(FeatureView<?> other, int types)
    {
        this.store = other.store;
        this.bbox = other.bbox;
        // TODO: types
        this.filters = other.filters;
    }

    public FeatureView(FeatureView<?> other, FilterSet filters)
    {
        this.store = other.store;
        this.bbox = other.bbox;
        // TODO: types
        this.filters = filters;
    }

    @Override public boolean isEmpty()
    {
        // TODO
        return false;
    }

    @Override public long count()
    {
        long count = 0;
        for(Feature f: this) count++;
        return count;
    }

    @Override public Features<?> features(String filter)
    {
        return new FeatureView<>(this, store.getFilters(filter));
    }

    @Override public Features<Node> nodes()
    {
        return null;
    }

    @Override public Features<Node> nodes(String filter)
    {
        return new FeatureView<Node>(this, Filter.NODES);
    }

    @Override public Features<Way> ways()
    {
        return new FeatureView<Way>(this, Filter.WAYS);
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
        return new FeatureView(this, bbox);
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
