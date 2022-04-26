package com.geodesk.feature.store;

import com.geodesk.feature.*;
import com.geodesk.feature.filter.Filter;
import com.geodesk.feature.polygon.PolygonBuilder;
import org.eclipse.collections.api.set.primitive.MutableLongSet;
import org.eclipse.collections.impl.set.mutable.primitive.LongHashSet;
import org.locationtech.jts.geom.Geometry;

import java.nio.ByteBuffer;
import java.util.*;

public class StoredRelation extends StoredFeature implements Relation
{
	public StoredRelation(FeatureStoreBase store, ByteBuffer buf, int ptr)
	{
		super(store, buf, ptr);
	}

	@Override public FeatureType type() { return FeatureType.RELATION; }

	@Override public String toString()
	{
		return "relation/" + id();
	}

	private boolean isEmpty(int pMembers)
	{
		return buf.getInt(pMembers) == 0;
	}

	@Override public Iterator<Feature> iterator()
	{
		int ppMembers = ptr + 12;
		int pMembers = ppMembers + buf.getInt(ppMembers);
		if(isEmpty(pMembers)) return Collections.emptyIterator();
		return new MemberIterator(store, buf, pMembers, Filter.ALL);
	}


	@Override public Geometry toGeometry()
	{
		if(isArea())
		{
			return PolygonBuilder.build(store.geometryFactory(), this);
		}
		else
		{
			return toGeometryCollection(new LongHashSet());
		}
	}

	private Geometry toGeometryCollection(MutableLongSet includedRelations)
	{
		// FeatureStoreBase.log.debug("Creating GeometryCollection for {} ...", this);
		includedRelations.add(id());
		List<Geometry> geoms = new ArrayList<>();
		for(Feature member: this)
		{
			if(member instanceof StoredRelation rel)
			{
				if (!includedRelations.contains(rel.id()))
				{
					// avoid endless recursion in case relations are in
					// a reference cycle
					geoms.add(rel.toGeometryCollection(includedRelations));
				}
			}
			else
			{
				geoms.add(member.toGeometry());
			}
		}
		return store.geometryFactory().createGeometryCollection(
			geoms.toArray(new Geometry[0]));
	}

	@Override public Features<?> members()
	{
		int ppMembers = ptr + 12;
		int pMembers = ppMembers + buf.getInt(ppMembers);
		if(isEmpty(pMembers)) return EmptyFeatures.ANY;
		return new MemberView<>(store, buf, pMembers, Filter.ALL);
	}

	@Override public Features<?> members(String q)
	{
		return null;
	}

	@Override public Features<Node> memberNodes()
	{
		return null;
	}

	@Override public Features<Node> memberNodes(String q)
	{
		return null;
	}

	@Override public Features<Way> memberWays()
	{
		return null;
	}

	@Override public Features<Way> memberWays(String q)
	{
		return null;
	}

	@Override public Features<Relation> memberRelations()
	{
		return null;
	}

	@Override public Features<Relation> memberRelations(String q)
	{
		return null;
	}

	@Override public Set<String> memberRoles()
	{
		return null;
	}

}
