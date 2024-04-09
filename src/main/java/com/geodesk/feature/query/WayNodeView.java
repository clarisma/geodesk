/*
 * Copyright (c) Clarisma / GeoDesk contributors
 *
 * This source code is licensed under the Apache 2.0 license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.geodesk.feature.query;

import com.geodesk.geom.XY;
import com.geodesk.feature.*;
import com.geodesk.feature.match.Matcher;
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

    private static final int INCLUDE_GEOMETRY_NODES = 256;

    public WayNodeView(FeatureStore store, ByteBuffer buf, int ptr)
    {
        this(store, buf, ptr, TypeBits.NODES, Matcher.ALL, null);
    }

    // TODO: If the Matcher includes only negative clauses (e.g. n[!highway]),
    //  untagged nodes ("geometry nodes") will still match
    //  We need a way to indicate that the Matcher can return untagged nodes
    //  so we can set the INCLUDE_GEOMETRY_NODES flag here
    //  No! Per spec, tag queries only return feature nodes, so current
    //  behavior is correct.


    public WayNodeView(FeatureStore store, ByteBuffer buf, int ptr,
        int types, Matcher matcher, Filter filter)
    {
        super(store, buf, ptr, types, matcher, filter);
        flags = (buf.get(ptr) & 0xff) |
            ((matcher == Matcher.ALL) ? INCLUDE_GEOMETRY_NODES : 0);
    }

    @Override protected Features newWith(int types, Matcher matcher, Filter filter)
    {
        return new WayNodeView(store, buf, ptr, types, matcher, filter);
    }

    private int bodyPtr()
    {
        int ppBody = ptr + 12;
        return buf.getInt(ppBody) + ppBody;
    }

    @Override public Iterator<Feature> iterator()
    {
        if((flags & INCLUDE_GEOMETRY_NODES) == 0)
        {
            return new StoredWay.Iter(store, buf, bodyPtr() - 4 -
                (flags & FeatureFlags.RELATION_MEMBER_FLAG), matcher);
        }
        return new AllNodesIter(bodyPtr());
    }

    // TODO: apply spatial filters to geometric nodes
    // TODO: inverse this: derive from feature iterator instead?
    private class AllNodesIter extends StoredWay.XYIterator implements Iterator<Feature>
    {
        private Feature nextFeatureNode;
        private Iterator<Feature> featureNodeIter;

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

        @Override public Feature next()
        {
            long xy = nextXY();
            int x = XY.x(xy);
            int y = XY.y(xy);
            if(nextFeatureNode != null)
            {
                if(nextFeatureNode.x() == x && nextFeatureNode.y() == y)
                {
                    Feature node = nextFeatureNode;
                    nextFeatureNode = featureNodeIter.hasNext() ? featureNodeIter.next() : null;
                    return node;
                }
            }
            return new AnonymousWayNode(store, x, y);
        }
    }
}
