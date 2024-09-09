/*
 * Copyright (c) Clarisma / GeoDesk contributors
 *
 * This source code is licensed under the Apache 2.0 license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.geodesk.feature.query;

import com.geodesk.feature.Feature;
import com.geodesk.feature.Features;
import com.geodesk.feature.Filter;
import com.geodesk.feature.filter.AndFilter;
import com.geodesk.feature.match.Matcher;
import com.geodesk.feature.match.TypeBits;
import com.geodesk.feature.store.FeatureStore;
import com.geodesk.feature.store.StoredNode;

import java.nio.ByteBuffer;
import java.util.Iterator;

/**
 * A view that contains the parent ways/relations of a specific node.
 */

public class NodeParentView extends ParentRelationView
{
    final StoredNode node;

    public NodeParentView(FeatureStore store, ByteBuffer buf,
        StoredNode node, int pRelations, int types, Matcher matcher, Filter filter)
    {
        super(store, buf, pRelations, types, matcher, filter);
        assert((types & TypeBits.WAYS) != 0 && (types & TypeBits.RELATIONS) != 0);
        this.node = node;
    }

    @Override protected Features newWith(int types, Matcher matcher, Filter filter)
    {
        if((types & TypeBits.RELATIONS) == 0)
        {
            // view has been restricted to ways only
            assert((types & TypeBits.WAYS) != 0);
            return node.parentWays(types, matcher, filter);
        }
        else if ((types & TypeBits.WAYS) == 0)
        {
            // view has been restricted to relations only
            assert((types & TypeBits.RELATIONS) != 0);
            return new ParentRelationView(store, buf, ptr, types, matcher, filter);
        }
        assert((types & TypeBits.WAYS) != 0 && (types & TypeBits.RELATIONS) != 0);
        return new NodeParentView(store, buf, node, ptr, types, matcher, filter);
    }

    @Override public Iterator<Feature> iterator()
    {
        return new Iter();
    }

    private class Iter extends ParentRelationView.Iter
    {
        private final Query wayQuery;
        private Feature nextFeature;
        private int phase;

        public Iter()
        {
            assert((types & (TypeBits.RELATIONS | TypeBits.WAYS)) != 0);
            wayQuery = new Query(node.parentWays(types, matcher, filter));
            // TODO: To improve performance, we could start the query so it
            //  can fetch the parent ways in the background, while the caller
            //  is iterating over the parent relations; but may not be worth
            //  the added complexity
            fetchNext();
        }

        private void fetchNext()
        {
            if(phase == 0)
            {
                nextFeature = super.next();
                if(nextFeature != null) return;
                phase++;
            }
            nextFeature = wayQuery.next();
        }

        @Override public boolean hasNext()
        {
            return nextFeature != null;
        }

        @Override public Feature next()
        {
            Feature next = nextFeature;
            fetchNext();
            return next;
        }
    }
}




