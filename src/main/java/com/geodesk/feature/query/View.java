/*
 * Copyright (c) Clarisma / GeoDesk contributors
 *
 * This source code is licensed under the Apache 2.0 license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.geodesk.feature.query;

import com.geodesk.feature.*;
import com.geodesk.feature.filter.AndFilter;
import com.geodesk.feature.filter.FalseFilter;
import com.geodesk.feature.filter.FilterStrategy;
import com.geodesk.feature.match.*;
import com.geodesk.feature.store.*;

import java.nio.ByteBuffer;


public abstract class View implements Features
{
    protected final FeatureStore store;
    protected final int types;
    protected final Matcher matcher;
    protected final Filter filter;

    protected View(FeatureStore store, int types, Matcher matcher, Filter filter)
    {
        this.store = store;
        this.types = types;
        this.matcher = matcher;
        this.filter = filter;
    }

    protected abstract Features newWith(int types, Matcher matcher, Filter filter);

    public int types()
    {
        return types;
    }

    protected Features select(int newTypes)
    {
        newTypes &= types;
        if(newTypes == 0) return EmptyView.ANY;
        return newWith(newTypes, matcher, filter);
    }

    protected Features select(int newTypes, String query)
    {
        Matcher newMatcher = store.getMatcher(query);
        if (matcher != Matcher.ALL)
        {
            newMatcher = new AndMatcher(matcher, newMatcher);
        }
        newTypes &= types & newMatcher.acceptedTypes();
        if (newTypes == 0) return EmptyView.ANY;
        return newWith(newTypes, newMatcher, filter);
    }

    @Override public Features select(String query)
    {
        return select(TypeBits.ALL, query);
    }

    @Override public Features nodes()
    {
        return select(TypeBits.NODES);
    }

    @Override public Features nodes(String query)
    {
        return select(TypeBits.NODES, query);
    }

    @Override public Features ways()
    {
        return select(TypeBits.WAYS);
    }

    @Override public Features ways(String query)
    {
        return select(TypeBits.WAYS, query);
    }

    @Override public Features relations()
    {
        return select(TypeBits.RELATIONS);
    }

    @Override public Features relations(String query)
    {
        return select(TypeBits.RELATIONS, query);
    }

    @Override public Features select(Filter filter)
    {
        int newTypes = types;
        if(this.filter != null)
        {
            filter = AndFilter.create(this.filter, filter);
            if (filter == FalseFilter.INSTANCE) return EmptyView.ANY;
        }
        int strategy = filter.strategy();
        if((strategy & FilterStrategy.RESTRICTS_TYPES) != 0)
        {
            newTypes &= filter.acceptedTypes();
            if(newTypes == 0) return EmptyView.ANY;
        }
        return newWith(newTypes, matcher, filter);
    }

    @Override public Features parentsOf(Feature child)
    {
        if(child.isNode())
        {
            if (child instanceof AnonymousWayNode wayNode)
            {
                return wayNode.parents(types, matcher, filter);
            }
            return ((StoredNode)child).parents(types, matcher, filter);
        }
        else
        {
            if ((types & TypeBits.RELATIONS) == 0) return EmptyView.ANY;
            StoredFeature f = (StoredFeature) child;
            return new ParentRelationView(store, f.buffer(),
                f.getRelationTablePtr(), types, matcher, filter);
        }
    }

    @Override public Features membersOf(Feature parent)
    {
        if(parent.isRelation())
        {
            return ((StoredRelation) parent).members(types, matcher, filter);
        }
        // TODO: membersOf() for ways

        return EmptyView.ANY;
    }

    @Override public Features nodesOf(Feature parent)
    {
        if(parent.isWay())
        {
            StoredWay way = (StoredWay) parent;
            if ((types & TypeBits.NODES) == 0) return EmptyView.ANY;
            return new WayNodeView(store, way.buffer(), way.pointer(),
                    types, matcher, filter);
        }

        // TODO: nodesOf() for relations

        return EmptyView.ANY;
    }
}
