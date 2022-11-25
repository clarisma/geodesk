/*
 * Copyright (c) Clarisma / GeoDesk contributors
 *
 * This source code is licensed under the Apache 2.0 license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.geodesk.feature.store;

import com.geodesk.feature.*;
import com.geodesk.core.Box;
import com.geodesk.feature.match.*;
import com.geodesk.feature.query.EmptyView;
import com.geodesk.feature.query.WorldView;
import com.geodesk.geom.Bounds;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;

import java.nio.ByteBuffer;

public class StoredNode extends StoredFeature implements Node
{
	public StoredNode(FeatureStore store, ByteBuffer buf, int ptr)
	{
		super(store, buf, ptr);
	}

	@Override public FeatureType type()
	{
		return FeatureType.NODE;
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

	@Override public Geometry toGeometry()
	{
		return store.geometryFactory().createPoint(new Coordinate(x(), y()));
	}

	@Override public String toString()
	{
		return "node/" + id();
	}

	@Override public boolean belongsToWay()
	{
		return (buf.getInt(ptr) & FeatureFlags.WAYNODE_FLAG) != 0;
	}

	@Override public Features<Way> parentWays()
	{
		if ((buf.getInt(ptr) & FeatureFlags.WAYNODE_FLAG) == 0) return EmptyView.WAYS;
		int types = TypeBits.WAYS & TypeBits.WAYNODE_FLAGGED;
		final Matcher matcher = new TypeMatcher(types, Matcher.ALL);
		return new WorldView<>(store, types,
			bounds(), new MatcherSet(types, matcher),
			new ParentWayFilter(id()));

		// TODO: This could be more efficient; we can create singleton matchers
		//  Also don't need TypeMatcher on lineal way index
	}

	@Override protected int getRelationTablePtr()
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