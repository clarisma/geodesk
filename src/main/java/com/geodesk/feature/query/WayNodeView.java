package com.geodesk.feature.query;

import com.geodesk.core.XY;
import com.geodesk.feature.*;
import com.geodesk.feature.filter.FilterSet;
import com.geodesk.feature.filter.TypeBits;
import com.geodesk.feature.store.AnonymousWayNode;
import com.geodesk.feature.store.FeatureFlags;
import com.geodesk.feature.store.FeatureStore;
import com.geodesk.feature.store.StoredWay;
import com.geodesk.geom.Bounds;

import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.Iterator;

public class WayNodeView extends TableView<Node>
{
    private final int flags;

    private static final int INCLUDE_GEOMETRY_NODES = 256;

    public WayNodeView(FeatureStore store, ByteBuffer buf, int ptr)
    {
        super(store, buf, ptr, Filter.ALL);
        flags = (buf.get(ptr) & 0xff) | INCLUDE_GEOMETRY_NODES;
    }

    public WayNodeView(FeatureStore store, ByteBuffer buf, int ptr, Filter filter)
    {
        super(store, buf, ptr, filter);
        flags = buf.get(ptr) & 0xff;
    }

    public WayNodeView(WayNodeView other, Filter filter, int flags)
    {
        super(other, filter);
        this.flags = flags;
    }

    @Override public Features<?> features(String query)
    {
        return nodes(query);
    }

    @Override public Features<Node> nodes()
    {
        return this;
    }

    @Override public Features<Node> nodes(String query)
    {
        if((flags & FeatureFlags.WAYNODE_FLAG) == 0) return EmptyView.NODES;
        FilterSet filters = store.getFilters(query);
        if((filters.types() & TypeBits.NODES) == 0) return EmptyView.NODES;
        return new WayNodeView(this, filters.nodes(), flags & ~INCLUDE_GEOMETRY_NODES);
    }

    @Override public Features<Way> ways()
    {
        return EmptyView.WAYS;
    }

    @Override public Features<Way> ways(String query)
    {
        return EmptyView.WAYS;
    }

    @Override public Features<Relation> relations()
    {
        return EmptyView.RELATIONS;
    }

    @Override public Features<Relation> relations(String query)
    {
        return EmptyView.RELATIONS;
    }

    private int bodyPtr()
    {
        int ppBody = ptr + 12;
        return buf.getInt(ppBody) + ppBody;
    }

    @Override public Iterator<Node> iterator()
    {
        if((flags & INCLUDE_GEOMETRY_NODES) == 0)
        {
            return new StoredWay.Iter(store, buf, bodyPtr() - 4 -
                (flags & FeatureFlags.RELATION_MEMBER_FLAG), filter);
        }
        return new AllNodesIter(bodyPtr());
    }

    // TODO: apply spatial filters to geometric nodes
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
                    (flags & FeatureFlags.RELATION_MEMBER_FLAG), Filter.ALL);
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
}