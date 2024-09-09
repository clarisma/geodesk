/*
 * Copyright (c) Clarisma / GeoDesk contributors
 *
 * This source code is licensed under the Apache 2.0 license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.geodesk.feature.store;

import com.geodesk.feature.*;
import com.geodesk.feature.filter.AndFilter;
import com.geodesk.feature.query.NodeParentView;
import com.geodesk.feature.query.ParentRelationView;
import com.geodesk.geom.Box;
import com.geodesk.feature.match.*;
import com.geodesk.feature.query.EmptyView;
import com.geodesk.feature.query.WorldView;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;

import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.Iterator;

public class StoredNode extends StoredFeature implements Node
{
	public StoredNode(FeatureStore store, ByteBuffer buf, int ptr)
	{
		super(store, buf, ptr);
	}

    @Override public Iterator iterator()
    {
        return Collections.emptyIterator();
    }

	@Override public FeatureType type()
	{
		return FeatureType.NODE;
	}

    @Override public boolean isNode()
    {
        return true;
    }

	@Override public int x()
	{
		return buffer().getInt(ptr - 8);
	}

	@Override public int y()
	{
		return buffer().getInt(ptr - 4);
	}

	@Override public int minY() { return y(); }

	@Override public boolean isPlaceholder()
	{
		return (x() | y()) == 0;
	}

	@Override public Box bounds()
	{
		int x = x();
		int y = y();
		if ((x | y) == 0)
		{
			// If coordinates are 0/0, return an empty bbox
			// (to accommodate missing nodes)
			// TODO: 0/0 could also be the Null Island weather buoy
			// TODO: use x == Integer.MIN to indicate placeholder node
			return new Box();
		}
		return new Box(x, y);
	}

    @Override public int[] toXY()
    {
        int[] coords = new int[2];
        coords[0] = x();
        coords[1] = y();
        return coords;
    }

	// TODO: create CoordinateSequence instead of Coordinate here, because
	//  that's what GeometryFactory does anyway
	@Override public Geometry toGeometry()
	{
		return store.geometryFactory().createPoint(new Coordinate(x(), y()));
	}

	@Override public String toString()
	{
		return "node/" + id();
	}

    public WorldView parentWays(int types, Matcher matcher, Filter filter)
    {
        Filter newFilter = new ParentWayFilter(id());
        if(filter != null) newFilter = AndFilter.create(newFilter, filter);
        return new WorldView(store, types & TypeBits.WAYS &
            TypeBits.WAYNODE_FLAGGED, bounds(), matcher, newFilter);
    }

    public Features parents(int types, Matcher matcher, Filter filter)
	{
        // types &= TypeBits.WAYS | TypeBits.RELATIONS;  // TODO: should not be needed? (added 9/9/24)

        int acceptedFlags = ((types & TypeBits.RELATIONS) != 0) ?
            FeatureFlags.RELATION_MEMBER_FLAG : 0;
        acceptedFlags |= ((types & TypeBits.WAYS) != 0) ?
            FeatureFlags.WAYNODE_FLAG : 0;
        int flags = buf.getInt(ptr) & acceptedFlags;

        if(flags == FeatureFlags.WAYNODE_FLAG)
        {
            return parentWays(types, matcher, filter);
        }
        if (flags == FeatureFlags.RELATION_MEMBER_FLAG)
        {
            return new ParentRelationView(store, buf, getRelationTablePtr(),
                types & TypeBits.RELATIONS, matcher, filter);
        }
        if (flags == (FeatureFlags.WAYNODE_FLAG | FeatureFlags.RELATION_MEMBER_FLAG))
        {
            return new NodeParentView(store, buf, this,
                getRelationTablePtr(), types, matcher, filter);
        }
        return EmptyView.ANY;
	}

    @Override public Features parents()
    {
        return parents(TypeBits.RELATIONS | TypeBits.WAYS, Matcher.ALL, null);
    }

    @Override public Features parents(String query)
	{
        Matcher matcher = store.getMatcher(query);
        return parents(matcher.acceptedTypes(), matcher, null);
	}

	@Override public int getRelationTablePtr()
	{
		// A Node's body pointer is the pointer to its reltable
		int ppBody = ptr + 12;
		return buf.getInt(ppBody) + ppBody;
	}

	// TODO: No need to dereference the nodes in a way;
	//  we could simply check for same buffer and pointer
	//  (Nodes always live in one tile only)
	//  Could use existing iterator and receive StoredNode objects
	//  But instead of checking ID (which requires a memory access and
	//  potentially requires a page load), we simple compare buf/ptr
	private static class ParentWayFilter extends IdMatcher implements Filter
	{
		public ParentWayFilter(long nodeId)
		{
			super(0, nodeId);
		}

		@Override public boolean accept(Feature feature)
		{
			StoredWay way = (StoredWay)feature;
			return way.fastFeatureNodeIterator(this).hasNext();
		}
	}

}