package com.geodesk.feature.query;

import com.geodesk.feature.*;
import com.geodesk.feature.Filter;
import com.geodesk.feature.filter.FilterSet;
import com.geodesk.feature.store.FeatureStore;

public abstract class AbstractView<T extends Feature> implements Features<T>
{
    protected FeatureStore store;
    protected int featureTypes;
    protected FilterSet filters;

    protected static final int NODES = 1;
    protected static final int WAYS = 1 << 4;
    protected static final int AREA_WAYS = 1 << 5;
    protected static final int RELATIONS = (1 << 8);
    protected static final int AREA_RELATIONS = (1 << 9);

    protected abstract AbstractView<T> newView();

    /*
    protected void addTypeFilter()
    {
        if(filter)
    }
     */

    protected void addFilter(Filter f)
    {
        if(filters == null)
        {
            filters = new FilterSet(f,f,f,f);
            return;
        }
    }

    protected AbstractView<?> constrainTypes(int types)
    {
        int newTypes = featureTypes & types;
        if (newTypes == 0)
        {
            // TODO: empty view
            // return EmptyFeatures.ANY;
            return null;
        }
        if (newTypes == featureTypes) return this;
        AbstractView<T> v = newView();
        v.featureTypes = newTypes;
        return v;
    }

    @Override public Features<Node> nodes()
    {
        return (Features<Node>)constrainTypes(NODES);
    }

    @Override public Features<Way> ways()
    {
        return (Features<Way>)constrainTypes(WAYS);
    }

    /*
    @Override public Features<?> areas()
    {
        return constrainTypes(AREA_WAYS | AREA_RELATIONS);
    }
     */

    @Override public Features<Relation> relations()
    {
        return (Features<Relation>)constrainTypes(RELATIONS);
    }
}
