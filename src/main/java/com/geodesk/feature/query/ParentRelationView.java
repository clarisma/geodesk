package com.geodesk.feature.query;

import com.geodesk.feature.*;
import com.geodesk.feature.Filter;
import com.geodesk.feature.store.FeatureStore;
import com.geodesk.feature.store.StoredRelation;
import com.geodesk.geom.Bounds;

import java.nio.ByteBuffer;
import java.util.Iterator;

public class ParentRelationView implements Features<Relation>
{
    private FeatureStore store;
    private ByteBuffer buf;
    private int ptr;
    private final Filter filter = Filter.ALL;   // TODO


    public ParentRelationView(FeatureStore store, ByteBuffer buf, int ptr)
    {
        this.store = store;
        this.buf = buf;
        this.ptr = ptr;
    }

    /*
    @Override public boolean isEmpty()
    {
        // TODO
        return false;
    }

    @Override public long count()
    {
        // TODO
        return 0;
    }
    */

    @Override public Features<?> features(String filter)
    {
        // TODO
        return null;
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

//    @Override public Features<?> areas()
//    {
//        // TODO
//        return null;
//    }
//
//    @Override public Features<?> areas(String filter)
//    {
//        // TODO
//        return null;
//    }

    @Override public Features<Relation> relations()
    {
        return this;
    }

    @Override public Features<Relation> relations(String filter)
    {
        // TODO
        return null;
    }

    @Override public Features<?> in(Bounds bbox)
    {
        // TODO
        return null;
    }

    /*
    @Override public boolean contains(Relation f)
    {
        // TODO
        return false;
    }

    @Override public boolean containsNode(long id)
    {
        return true;
    }

    @Override public boolean containsWay(long id)
    {
        return true;
    }

    @Override public boolean containsRelation(long id)
    {
        // TODO
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
        // TODO
        return null;
    }
     */

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
