/*
 * Copyright (c) Clarisma / GeoDesk contributors
 *
 * This source code is licensed under the Apache 2.0 license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.geodesk.feature.query;

import com.geodesk.geom.Box;
import com.geodesk.feature.*;
import com.geodesk.feature.filter.AndFilter;
import com.geodesk.feature.filter.FalseFilter;
import com.geodesk.feature.filter.FilterStrategy;
import com.geodesk.feature.match.Matcher;
import com.geodesk.feature.match.TypeBits;
import com.geodesk.feature.store.FeatureStore;
import com.geodesk.feature.store.StoredFeature;
import com.geodesk.geom.Bounds;

import java.util.Iterator;

// TODO: do we need to create a defensive copy of the bbox?
//  --> need to resolve mutability of Box

// Queries that don't target a Relation ignore "role" clauses
// Be sure to construct filter combos for e.g.:
// wa[role=a][highway], w[role=b][railway]
// non-Relation queries should return areas that are highways,
//  as well as ways that are railway or highway

// TODO: multiple bboxes
//  bbox should not be intersected; a query with 2 bboxes simply means
//  "this feature must intersect both bboxes" -- this does not mean that
//  the bboxes themselves must intersect
//  (rarely used, only needed to fulfill the API contract)

/**
 * A Feature Collection that is materialized by running a query against
 * a FeatureStore.
 *
 * @param <T> subtype of Feature
 */
public class WorldView extends View
{
    protected final Bounds bounds;

    protected final static Box WORLD = Box.ofWorld();

    public WorldView(FeatureStore store)
    {
        super(store, TypeBits.ALL, Matcher.ALL, null);
        this.bounds = WORLD;
    }
    public WorldView(FeatureStore store, int types, Bounds bounds, Matcher matcher, Filter filter)
    {
        super(store, types, matcher, filter);
        this.bounds = bounds;
    }

    @Override protected Features newWith(int types, Matcher matcher, Filter filter)
    {
        return new WorldView(store, types, bounds, matcher, filter);
    }

    private WorldView(WorldView other, Bounds bounds)
    {
        super(other.store, other.types, other.matcher, other.filter);
        this.bounds = bounds;           // TODO: intersect bbox
    }

    @Override public Features in(Bounds bbox)
    {
        return new WorldView(this, bbox);
    }

    @Override public boolean contains(Object obj)
    {
        if(obj instanceof StoredFeature feature)
        {
            // Feature must come from this library
            if(feature.store() != store) return false;

            // Feature must fit into the filtered types
            int featureType = 1 << (feature.flags() >>> 1); // shift uses only bottom 5 bits
            if((types & featureType) == 0) return false;

            // Feature must intersect the view's bbox
            //  TODO: improve bounds(), should return immutable
            if(!feature.bounds().intersects(bounds)) return false;

            // Feature must be accepted by matcher
            if(!feature.matches(matcher)) return false;

            // If this view has a spatial filter, the feature must match
            // that one as well
            if(filter == null) return true;
            return filter.accept(feature);
        }
        return false;
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

        // TODO: review: filter type check

        Bounds filterBounds = filter.bounds();
        // TODO: proper combining of bboxes
        return new WorldView(store, types, filterBounds != null ? filterBounds : bounds,
            matcher, filter);
    }

    @Override public Iterator<Feature> iterator()
    {
        return new Query(this);
    }
}
