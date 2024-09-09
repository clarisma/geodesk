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
        this(store, buf, ptr, TypeBits.RELATIONS, Matcher.ALL, null);
    }

    public ParentRelationView(FeatureStore store, ByteBuffer buf, int ptr,
        int types, Matcher matcher, Filter filter)
    {
        super(store, buf, ptr, types, matcher, filter);
    }

    @Override protected Features newWith(int types, Matcher matcher, Filter filter)
    {
        return new ParentRelationView(store, buf, ptr, types, matcher, filter);
    }

    @Override public boolean isEmpty()
    {
        // can never be empty
        return false;
    }

    @Override public Iterator<Feature> iterator()
    {
        return new Iter();
    }

    protected class Iter extends TableIterator<Feature>
    {
        private int p;
        private int rel;
        private Feature current;

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
                        // TODO: Doesn't need rebasing; pointer is always 2-byte aligned
                        //  But we still need to mask off the last-flag
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

        @Override public Feature next()
        {
            Feature next = current;
            fetchNext();
            return next;
        }
    }
}
