package com.geodesk.feature.store;

import com.clarisma.common.pbf.PbfDecoder;
import com.geodesk.core.Mercator;
import com.geodesk.core.XY;
import com.geodesk.feature.FeatureType;
import com.geodesk.feature.Features;
import com.geodesk.feature.Node;
import com.geodesk.feature.Way;
import com.geodesk.feature.filter.Filter;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;

import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.Iterator;

public class StoredWay extends StoredFeature implements Way
{
	public StoredWay(FeatureStoreBase store, ByteBuffer buf, int ptr)
	{
		super(store, buf, ptr);
	}

	@Override public FeatureType type() { return FeatureType.WAY; }

	@SuppressWarnings("unchecked")
	@Override
	public Features<Node> nodes()
	{
		// TODO
		return null;
	}

	@Override public String toString()
	{
		return "way/" +  id();
	}

	// TODO: use tuple iterator form
	// TODO: expose as public, provide lat and lon
	private static class XYIterator extends PbfDecoder
	{
		private int x;
		private int y;
		private int remaining;
		private final int firstX;
		private final int firstY;
		private int duplicatedLastCoord;
		protected final int flags;

		XYIterator(ByteBuffer buf, int pos, int prevX, int prevY, int flags)
		{
			super(buf, pos);
			x = prevX;
			y = prevY;
			this.flags = flags;
			remaining = (int) readVarint();
			if ((flags & FeatureFlags.AREA_FLAG) != 0)
			{
				remaining++;
				duplicatedLastCoord = 0;
			}
			else
			{
				duplicatedLastCoord = -1;
			}
			readNext();
			firstX = x;
			firstY = y;
		}

		private void readNext()
		{
			remaining--;
			if (remaining == duplicatedLastCoord)
			{
				x = firstX;
				y = firstY;
				duplicatedLastCoord--;
					// This avoids buffer overrun; if readNext() is called
					// again, we return the first coordinate
				return;
			}
			x += (int) readSignedVarint();
			y += (int) readSignedVarint();
		}

		public boolean hasNext()
		{
			return remaining >= 0;
		}

		public long nextXY()
		{
			long c = XY.of(x, y);
			readNext();
			return c;
		}
	}


	@Override public Iterator<Node> iterator()
	{
		int flags = buf.getInt(ptr);
		if((flags & FeatureFlags.WAYNODE_FLAG) == 0) return Collections.emptyIterator();
		int ppBody = ptr + 12;
		int pBody = buf.getInt(ppBody) + ppBody;
		return new Iter(store, buf, pBody - 4 -
			(flags & FeatureFlags.RELATION_MEMBER_FLAG), Filter.ALL);
	}

	@Override public int[] toXY()
	{
		int flags = buf.getInt(ptr);
		XYIterator iter = iterXY(flags);
		int[] coords = new int[(iter.remaining + 1) * 2];
		// need to add one because the iterator decremented remaining
		// when it fetched the first coordinate
		for (int i = 0; i < coords.length; i += 2)
		{
			long xy = iter.nextXY();
			coords[i] = XY.x(xy);
			coords[i + 1] = XY.y(xy);
		}
		return coords;
	}

	@Override public Geometry toGeometry()
	{
		GeometryFactory factory = store.geometryFactory();
		WayCoordinateSequence coords = new WayCoordinateSequence(toXY());
		if(isArea()) return factory.createPolygon(coords);
		return factory.createLineString(coords);
		// TODO: LinearRing?
	}

	private XYIterator iterXY(int flags)
	{
		int ppBody = ptr + 12;
		int pBody = buf.getInt(ppBody) + ppBody;
		int minX = buf.getInt(ptr - 16);
		int minY = buf.getInt(ptr - 12);
		return new XYIterator(buf, pBody, minX, minY, flags);
	}

	public double length()
	{
		if (isArea()) return 0;
		XYIterator iter = iterXY(0);
		int prevX;
		int prevY;
		double total = 0;
		long xy = iter.nextXY();
		prevX = XY.x(xy);
		prevY = XY.y(xy);
		while (iter.hasNext())
		{
			xy = iter.nextXY();
			int x = XY.x(xy);
			int y = XY.y(xy);
			total += Mercator.distance(prevX, prevY, x, y);
			prevX = x;
			prevY = y;
		}
		return total;
	}

	/*
	@Override public boolean hasNode(Node node)
	{
		long nodeId = node.id();
		if(nodeId == 0)
		{
			long nodeXY = XY.of(node.x(), node.y());
			XYIterator iter = iterXY(getFlags());
			while(iter.hasNext())
			{
				long xy = iter.nextXY();
				if(xy == nodeXY) return true;
			}
			return false;
		}

		// TODO: make sure iterator() only iterates feature nodes
		for(Node n: this) if(n.id() == nodeId) return true;
		return false;
	}
	 */

	// TODO: area, circumference

	public static class Iter implements Iterator<Node>
	{
		private final FeatureStoreBase store;
		private final ByteBuffer buf;
		private final Filter filter;
		private int pNext;
		private Node featureNode;
		private int tip = FeatureConstants.START_TIP;
		private ByteBuffer foreignBuf;
		private int pForeignTile;

		// TODO: consolidate these flags
		private static final int NF_LAST = 1;
		private static final int NF_FOREIGN = 2;
		private static final int NF_DIFFERENT_TILE = 8;


		public Iter(FeatureStoreBase store, ByteBuffer buf, int pFirst, Filter filter)
		{
			this.store = store;
			this.buf = buf;
			this.pNext = pFirst;
			this.filter = filter;
			fetchNext();
		}

		private void fetchNext()
		{
			while(pNext != 0)
			{
				ByteBuffer nodeBuf;
				int pNode;
				int pCurrent = pNext;
				int node = buf.getInt(pCurrent);
				if((node & NF_FOREIGN) != 0)
				{
					if ((node & NF_DIFFERENT_TILE) != 0)
					{
						// TODO: wide tip delta
						pNext -= 2;
						int tipDelta = buf.getShort(pNext);
						tipDelta >>= 1;     // signed
						tip += tipDelta;
						int tilePage = store.fetchTile(tip);
						foreignBuf = store.bufferOfPage(tilePage);
						pForeignTile = store.offsetOfPage(tilePage);
					}
					nodeBuf = foreignBuf;
					pNode = pForeignTile + ((node >>> 4) << 2);
				}
				else
				{
					nodeBuf = buf;
					pNode = (pCurrent & 0xffff_fffe) + ((node >> 2) << 1);
						// TODO: simplify alignment rules!
				}
				pNext -= 4;
				pNext &= -1 + (node & NF_LAST);
				if(filter.accept(nodeBuf, pNode))
				{
					featureNode = new StoredNode(store, nodeBuf, pNode);
					return;
				}
			}
			featureNode = null;
		}

		@Override public boolean hasNext()
		{
			return featureNode != null;
		}

		@Override public Node next()
		{
			Node next = featureNode;
			fetchNext();
			return next;
		}
	}
}