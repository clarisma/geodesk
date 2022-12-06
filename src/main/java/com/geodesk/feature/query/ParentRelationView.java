/*
 * Copyright (c) Clarisma / GeoDesk contributors
 *
 * This source code is licensed under the Apache 2.0 license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.geodesk.feature.query;

import com.geodesk.feature.*;
import com.geodesk.feature.match.Matcher;
import com.geodesk.feature.match.AndMatcher;
import com.geodesk.feature.match.MatcherSet;
import com.geodesk.feature.match.TypeBits;
import com.geodesk.feature.store.FeatureStore;
import com.geodesk.feature.store.StoredRelation;
import com.geodesk.geom.Bounds;

import java.nio.ByteBuffer;
import java.util.Iterator;

public class ParentRelationView extends TableView
{
    public ParentRelationView(FeatureStore store, ByteBuffer buf, int ptr)
    {
        super(store, buf, ptr, Matcher.ALL);
    }

    public ParentRelationView(ParentRelationView other, Matcher matcher)
    {
        super(other, matcher);
    }

    public ParentRelationView(ParentRelationView other, Filter filter)
    {
        super(other, filter);
    }

    @Override public boolean isEmpty()
    {
        // can never be empty
        return false;
    }

    @Override public Features select(String query)
    {
        MatcherSet filters = store.getMatchers(query);
        if((filters.types() & TypeBits.RELATIONS) == 0) return EmptyView.ANY;
        Matcher newFilter = filters.relations();
            // TODO: This is wrong, need to include areas!
        return new ParentRelationView(this, newFilter);
    }

    @Override public Features select(Filter filter)
    {
        return new ParentRelationView(this, filter);
    }

    @Override public Iterator<Feature> iterator()
    {
        return new Iter();
    }

    private class Iter extends TableIterator
    {
        private int p;
        private int rel;
        private Relation current;

        public Iter()
        {
            p = ptr;
            fetchNext();
        }

        private void fetchNext()
        {
            for (; ; )
            {
                ByteBuffer relBuf;
                int pRel;
                if ((rel & LAST_FLAG) != 0)
                {
                    current = null;
                    return;
                }
                int pCurrent = p;
                rel = buf.getInt(pCurrent);
                p += 4;
                if ((rel & FOREIGN_FLAG) != 0)
                {
                    if ((rel & DIFFERENT_TILE_FLAG) != 0)
                    {
                        // TODO: wide tip delta
                        int tipDelta = buf.getShort(p);
                        tipDelta >>= 1;     // signed
                        tip += tipDelta;
                        p += 2;
                        int tilePage = store.fetchTile(tip);
                        foreignBuf = store.bufferOfPage(tilePage);
                        pForeignTile = store.offsetOfPage(tilePage);
                    }
                    relBuf = foreignBuf;
                    pRel = pForeignTile + ((rel >>> 4) << 2);
                }
                else
                {
                    relBuf = buf;
                    pRel = (pCurrent & 0xffff_fffe) + ((rel >> 2) << 1);
                        // TODO: simplify alignment rules!
                }
                if (matcher.accept(relBuf, pRel))
                {
                    Relation rel = new StoredRelation(store, relBuf, pRel);
                    if(filter == null || filter.accept(rel))
                    {
                        current = rel;
                        return;
                    }
                }
            }
        }

        @Override public boolean hasNext()
        {
            return current != null;
        }

        @Override public Relation next()
        {
            Relation next = current;
            fetchNext();
            return next;
        }
    }
}
