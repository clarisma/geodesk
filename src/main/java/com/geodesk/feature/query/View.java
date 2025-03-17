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

/// @hidden
public abstract class View implements Features
{
    /// @hidden
    protected final FeatureStore store;
    /// @hidden
    protected final int types;
    /// @hidden
    protected final Matcher matcher;
    /// @hidden
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
                // An anonymous nodes *always* has at least one parent way,
                // and *never* has parent relations
                return wayNode.parents(types, matcher, filter);
            }
            return ((StoredNode)child).parents(types, matcher, filter);
        }
        else
        {
            // Ways and relations can only have relations as parents
            if ((types & TypeBits.RELATIONS) == 0) return EmptyView.ANY;
            StoredFeature f = (StoredFeature) child;
            if(!f.belongsToRelation()) return EmptyView.ANY;
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
        if ((types & TypeBits.NODES) == 0) return EmptyView.ANY;
        if(parent.isWay())
        {
            StoredWay way = (StoredWay) parent;
            if(matcher != Matcher.ALL &&
                (way.flags() & FeatureFlags.WAYNODE_FLAG) == 0)
            {
                // GOQL queries only return feature nodes; if the Way's
                // waynode_flag is cleared, it only contains anonymous
                // nodes, so we return an empty set
                return EmptyView.ANY;
            }
            return new WayNodeView(store, way.buffer(), way.pointer(),
                    types, matcher, filter);
        }

        // TODO: nodesOf() for relations

        return EmptyView.ANY;
    }

    @Override public Features select(Features otherFeatures)
    {
        // TODO: This assumes both views are WorldViews (which is wrong)
        //  At least one must be a WorldView
        //  restrict the non-WorldView with types/matcher/filter of WorldView
        // TODO: Throw exception if the Views have different stores

        View other = (View)otherFeatures;
            // TODO: For now, all Features are implemented as a View,
            //  but this may change in the future
        int newTypes = types & other.types;
        if(newTypes == 0) return EmptyView.ANY;
        Matcher newMatcher = other.matcher;
        if (matcher != Matcher.ALL)
        {
            if(newMatcher != Matcher.ALL)
            {
                newMatcher = new AndMatcher(matcher, newMatcher);
            }
            else
            {
                newMatcher = matcher;
            }
        }
        Filter newFilter = other.filter;
        if (filter != null)
        {
            if(newFilter != null)
            {
                newFilter = AndFilter.create(filter, newFilter);
            }
            else
            {
                newFilter = filter;
            }
        }
        return newWith(newTypes, newMatcher, newFilter);
    }
}
