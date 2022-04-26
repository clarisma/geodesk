package com.geodesk.feature.store;

import com.geodesk.feature.FeatureType;
import com.geodesk.feature.Features;
import com.geodesk.feature.Node;
import com.geodesk.feature.Way;
import com.geodesk.core.Box;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;

import java.nio.ByteBuffer;

public class StoredNode extends StoredFeature implements Node
{
	public StoredNode(FeatureStoreBase store, ByteBuffer buf, int ptr)
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

	@Override public Box bounds()
	{
		int x = x();
		int y = y();
		if ((x | y) == 0)
		{
			// If coordinates are 0/0, return an empty bbox
			// (to accommodate missing nodes)
			// TODO: 0/0 could also be the Null Island weather buoy
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
		// TODO: query ways and area way; must have way-node flag set
		//  bbox is single pixel
		//  candidate way must have node as way-node
		return null;
	}

	@Override protected int getRelationTablePtr()
	{
		// A Node's body pointer is the pointer to its reltable
		int ppBody = ptr + 12;
		return buf.getInt(ppBody) + ppBody;
	}
}