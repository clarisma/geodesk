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

public class ParentRelationView implements Features<Relation>
{
    private final FeatureStore store;
    private final ByteBuffer buf;
    private final int ptr;
    private final Matcher filter;


    public ParentRelationView(FeatureStore store, ByteBuffer buf, int ptr)
    {
        this.store = store;
        this.buf = buf;
        this.ptr = ptr;
        filter = Matcher.ALL;
    }

    public ParentRelationView(ParentRelationView other, Matcher filter)
    {
        this.store = other.store;
        this.buf = other.buf;
        this.ptr = other.ptr;
        this.filter = filter;
    }

    @Override public boolean isEmpty()
    {
        // can never be empty
        return false;
    }

    @Override public Features<?> features(String query)
    {
        return relations(query);
    }

    @Override public Features<Node> nodes()
    {
        return (Features<Node>) EmptyView.ANY;
    }

    @Override public Features<Node> nodes(String filter)
    {
        return (Features<Node>) EmptyView.ANY;
    }

    @Override public Features<Way> ways()
    {
        return (Features<Way>) EmptyView.ANY;
    }

    @Override public Features<Way> ways(String filter)
    {
        return (Features<Way>) EmptyView.ANY;
    }

    @Override public Features<Relation> relations()
    {
        return this;
    }

    @Override public Features<Relation> relations(String query)
    {
        MatcherSet filters = store.getMatchers(query);
        if((filters.types() & TypeBits.RELATIONS) == 0) return EmptyView.RELATIONS;
        Matcher newFilter = filters.relations();
        if(filter != Matcher.ALL) newFilter = new AndMatcher(filter, newFilter);
        return new ParentRelationView(this, newFilter);
    }

    @Override public Features<Relation> in(Bounds bbox)
    {
        // TODO
        return null;
    }

    @Override public Iterator<Relation> iterator()
    {
        return new Iter();
    }

    private class Iter extends TableIterator<Relation>
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
                if (filter.accept(relBuf, pRel))
                {
                    current = new StoredRelation(store, relBuf, pRel);
                    return;
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
