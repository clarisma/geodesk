/*
 * Copyright (c) Clarisma / GeoDesk contributors
 *
 * This source code is licensed under the Apache 2.0 license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.geodesk.feature.query;

import com.geodesk.core.XY;
import com.geodesk.feature.*;
import com.geodesk.feature.match.Matcher;
import com.geodesk.feature.match.MatcherSet;
import com.geodesk.feature.match.TypeBits;
import com.geodesk.feature.store.AnonymousWayNode;
import com.geodesk.feature.store.FeatureFlags;
import com.geodesk.feature.store.FeatureStore;
import com.geodesk.feature.store.StoredWay;

import java.nio.ByteBuffer;
import java.util.Iterator;

public class WayNodeView extends TableView
{
    private final int flags;

    public WayNodeView(FeatureStore store, ByteBuffer buf, int ptr, Matcher matcher)
    {
        super(store, buf, ptr, matcher);
        flags = buf.get(ptr) & 0xff;
    }

    public WayNodeView(WayNodeView other, Matcher matcher, int flags)
    {
        super(other, matcher);
        this.flags = flags;
    }

    public WayNodeView(WayNodeView other, Filter filter)
    {
        super(other, filter);
        this.flags = other.flags;
    }

    @Override public Features select(String query)
    {
        if((flags & FeatureFlags.WAYNODE_FLAG) == 0) return EmptyView.ANY;
        MatcherSet filters = store.getMatchers(query);
        if((filters.types() & TypeBits.NODES) == 0) return EmptyView.ANY;
        return new WayNodeView(this, filters.nodes(), flags);
    }

    @Override public Features select(Filter filter)
    {
        return new WayNodeView(this, filter);
    }

    private int bodyPtr()
    {
        int ppBody = ptr + 12;
        return buf.getInt(ppBody) + ppBody;
    }

    @Override public Iterator<Feature> iterator()
    {
        return new StoredWay.Iter(store, buf, bodyPtr() - 4 -
            (flags & FeatureFlags.RELATION_MEMBER_FLAG), matcher);
    }

    /*
    // TODO: apply spatial filters to geometric nodes
    // TODO: inverse this: derive from feature iterator instead?
    private class AllNodesIter extends StoredWay.XYIterator implements Iterator<Node>
    {
        private Node nextFeatureNode;
        private Iterator<Node> featureNodeIter;

        public AllNodesIter(int pBody)
        {
            super(buf, pBody, buf.getInt(ptr - 16), buf.getInt(ptr - 12), flags);
            if((flags & FeatureFlags.WAYNODE_FLAG) != 0)
            {
                featureNodeIter = new StoredWay.Iter(store, buf, pBody - 4 -
                    (flags & FeatureFlags.RELATION_MEMBER_FLAG), Matcher.ALL);
                    // TODO: filters must apply to anonymous nodes as well!
                if(featureNodeIter.hasNext()) nextFeatureNode = featureNodeIter.next();
            }
        }

        @Override public Node next()
        {
            long xy = nextXY();
            int x = XY.x(xy);
            int y = XY.y(xy);
            if(nextFeatureNode != null)
            {
                if(nextFeatureNode.x() == x && nextFeatureNode.y() == y)
                {
                    Node node = nextFeatureNode;
                    nextFeatureNode = featureNodeIter.hasNext() ? featureNodeIter.next() : null;
                    return node;
                }
            }
            return new AnonymousWayNode(store, x, y);
        }
    }
     */
}
