/*
 * Copyright (c) Clarisma / GeoDesk contributors
 *
 * This source code is licensed under the Apache 2.0 license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.geodesk.feature.query;

import com.geodesk.core.Box;
import com.geodesk.feature.*;
import com.geodesk.feature.filter.AndFilter;
import com.geodesk.feature.filter.FalseFilter;
import com.geodesk.feature.filter.FilterStrategy;
import com.geodesk.feature.match.MatcherSet;
import com.geodesk.feature.store.FeatureStore;
import com.geodesk.feature.store.StoredFeature;
import com.geodesk.geom.Bounds;

import java.util.Iterator;
import static com.geodesk.feature.match.TypeBits.*;

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
public class WorldView<T extends Feature> implements Features<T>
{
    protected final FeatureStore store;
    protected final int types;
    protected final Bounds bbox;
    protected final MatcherSet matchers;
    protected final Filter filter;

    protected final static Box WORLD = Box.ofWorld();

    public WorldView(FeatureStore store)
    {
        this.store = store;
        types= ALL;
        matchers = MatcherSet.ALL;
        bbox = WORLD;
        filter = null;
    }

    public WorldView(FeatureStore store, int types, Bounds bbox, MatcherSet matchers, Filter filter)
    {
        this.store = store;
        this.types= types;
        this.bbox = bbox;
        this.matchers = matchers;
        this.filter = filter;
    }

    private WorldView(WorldView<?> other, Bounds bbox)
    {
        this.store = other.store;
        this.types = other.types;
        this.matchers = other.matchers;
        this.bbox = bbox;           // TODO: intersect bbox
        this.filter = other.filter; // TODO: clip bbox based on spatial filters
    }

    // TODO: decide if matchers should be merged or replaced
    //  (Tableview merges)
    private WorldView(WorldView<?> other, int types, MatcherSet matchers)
    {
        this.store = other.store;
        this.bbox = other.bbox;
        this.types = types;
        this.matchers = matchers;
        this.filter = other.filter;
    }

    private WorldView(WorldView<?> other, Bounds bbox, Filter filter)
    {
        this.store = other.store;
        this.types = other.types;
        this.matchers = other.matchers;
        this.bbox = bbox;
        this.filter = filter;
    }

    public int types()
    {
        return types;
    }


    /**
     *
     * - no change:          return this view
     * - no possible result: return empty view
     * - new view with merged filters
     * - new view with constrained filters (e.g. only want areas that are Ways)
     * - new view with just stricter type
     *
     * Optimize for common case: unconstrained type with new FilterSet
     *
     * TODO: possibly broken. What happens for
     *    world.relations().features("a")?
     *    We would need to create a TypeMatcher, to only return area-relations
     *
     * @param newTypes
     * @param indexesCovered
     * @param query
     * @return
     */
    private Features<T> select(int newTypes, int indexesCovered, String query)
    {
        MatcherSet newMatchers;
        if(query != null)
        {
            newMatchers = store.getMatchers(query);
            newTypes &= types & newMatchers.types();
            if(newTypes == 0) return (Features<T>)EmptyView.ANY;

            if(matchers != MatcherSet.ALL)
            {
                newMatchers = matchers.and(newTypes, newMatchers);
            }
        }
        else
        {
            newTypes &= types;
            if (newTypes == types) return this;
            if (newTypes == 0) return (Features<T>)EmptyView.ANY;
            newMatchers = matchers;
        }
        if(newTypes != (newMatchers.types() & indexesCovered))
        {
            newMatchers = newMatchers.and(newTypes);
        }
        return new WorldView<>(this, newTypes, newMatchers);
    }

    @Override public long count()
    {
        long count = 0;
        for(Feature f: this) count++;
        return count;
    }

    @Override public Features<T> select(String query)
    {
        return select(ALL, ALL, query);
    }

    @Override public Features<Node> nodes()
    {
        return (Features<Node>)select(NODES, NODES, null);
    }

    @Override public Features<Node> nodes(String query)
    {
        return (Features<Node>)select(NODES, NODES, query);
    }

    @Override public Features<Way> ways()
    {
        return (Features<Way>) select(WAYS, WAYS | AREAS, null);
    }

    @Override public Features<Way> ways(String query)
    {
        return (Features<Way>) select(WAYS, WAYS | AREAS, query);
    }

    @Override public Features<Relation> relations()
    {
        return (Features<Relation>) select(RELATIONS, RELATIONS | AREAS, null);
    }

    @Override public Features<Relation> relations(String query)
    {
        return (Features<Relation>) select(RELATIONS, RELATIONS | AREAS, query);
    }

    @Override public Features<T> in(Bounds bbox)
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
            if(!feature.bounds().intersects(bbox)) return false;

            // Feature must be accepted by one of the tag filters
            // appropriate for its conceptual type
            if((featureType & NODES) != 0)
            {
                if(!feature.matches(matchers.nodes())) return false;
            }
            else if((featureType & NONAREA_WAYS) != 0)
            {
                if(!feature.matches(matchers.ways())) return false;
            }
            else if((featureType & AREAS) != 0)
            {
                if(!feature.matches(matchers.areas())) return false;
            }
            else
            {
                assert (featureType & NONAREA_RELATIONS) != 0;
                if(!feature.matches(matchers.relations())) return false;
            }

            // If this view has a spatial filter, the feature must match
            // that one as well
            if(filter == null) return true;
            return filter.accept(feature);
        }
        return false;
    }

    @Override public Features<T> select(Filter filter)
    {
        if(this.filter != null)
        {
            filter = AndFilter.create(this.filter, filter);
            if (filter == FalseFilter.INSTANCE) return (Features<T>)EmptyView.ANY;
        }
        int strategy = filter.strategy();
        if((strategy & FilterStrategy.RESTRICTS_TYPES) != 0)
        {
            int newTypes = types & filter.acceptedTypes();
            if(newTypes != types)
            {
                if(newTypes == 0) return (Features<T>)EmptyView.ANY;
                // TODO: restrict matchers (e.g. Filter only selects relations,
                //  members, etc.)
            }
        }

        Bounds filterBounds = filter.bounds();
        // TODO: proper combining of bboxes
        return new WorldView<>(this, filterBounds != null ? filterBounds : bbox, filter);
    }

    @Override public Iterator<T> iterator()
    {
        return (Iterator<T>)new Query(this);
    }
}
