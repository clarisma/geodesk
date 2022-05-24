package com.geodesk.feature.store;

import com.geodesk.feature.*;
import com.geodesk.feature.match.Matcher;
import com.geodesk.feature.match.MatcherSet;
import com.geodesk.feature.match.TypeBits;
import com.geodesk.feature.polygon.PolygonBuilder;
import com.geodesk.feature.query.EmptyView;
import com.geodesk.feature.query.MemberIterator;
import com.geodesk.feature.query.MemberView;
import org.eclipse.collections.api.set.primitive.MutableLongSet;
import org.eclipse.collections.impl.set.mutable.primitive.LongHashSet;
import org.locationtech.jts.geom.Geometry;

import java.nio.ByteBuffer;
import java.util.*;

public class StoredRelation extends StoredFeature implements Relation
{
	public StoredRelation(FeatureStore store, ByteBuffer buf, int ptr)
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
		return new MemberIterator(store, buf, pMembers, TypeBits.ALL, Matcher.ALL, null);
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
		return members(TypeBits.ALL, Matcher.ALL);
	}

	private Features<?> members(int types, String query)
	{
		MatcherSet matchers = store.getMatchers(query);
		return members(types & matchers.types(), matchers.members());
	}

	private Features<?> members(int types, Matcher matcher)
	{
		if(types == 0) return EmptyView.ANY;
		int ppMembers = ptr + 12;
		int pMembers = ppMembers + buf.getInt(ppMembers);
		if(isEmpty(pMembers)) return EmptyView.ANY;
		return new MemberView<>(store, buf, pMembers, types, matcher);
	}

	@Override public Features<?> members(String q)
	{
		return members(TypeBits.ALL, q);
	}

	@Override public Features<Node> memberNodes()
	{
		return (Features<Node>)members(TypeBits.NODES, Matcher.ALL);
	}

	@Override public Features<Node> memberNodes(String q)
	{
		return (Features<Node>)members(TypeBits.NODES, q);
	}

	@Override public Features<Way> memberWays()
	{
		return (Features<Way>)members(TypeBits.WAYS, Matcher.ALL);
	}

	@Override public Features<Way> memberWays(String q)
	{
		return (Features<Way>)members(TypeBits.WAYS, q);
	}

	@Override public Features<Relation> memberRelations()
	{
		return (Features<Relation>)members(TypeBits.RELATIONS, Matcher.ALL);
	}

	@Override public Features<Relation> memberRelations(String q)
	{
		return (Features<Relation>)members(TypeBits.RELATIONS, q);
	}

	@Override public Set<String> memberRoles()
	{
		return null;	// TODO
	}

}
