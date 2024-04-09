/*
 * Copyright (c) Clarisma / GeoDesk contributors
 *
 * This source code is licensed under the Apache 2.0 license found in the
 * LICENSE file in the root directory of this source tree.
 */

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
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Point;

import java.nio.ByteBuffer;
import java.util.*;

public class StoredRelation extends StoredFeature implements Relation
{
	public StoredRelation(FeatureStore store, ByteBuffer buf, int ptr)
	{
		super(store, buf, ptr);
	}

	@Override public FeatureType type() { return FeatureType.RELATION; }

    @Override public boolean isRelation()
    {
        return true;
    }

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

	public static int bodyPointer(ByteBuffer buf, int ptr)
	{
		int ppMembers = ptr + 12;
		return ppMembers + buf.getInt(ppMembers);
	}

	public Iterator<Feature> iterator(int types, Matcher matcher)
	{
		int ppMembers = ptr + 12;
		int pMembers = ppMembers + buf.getInt(ppMembers);
		if(isEmpty(pMembers)) return Collections.emptyIterator();
		return new MemberIterator(store, buf, pMembers, types, matcher, null);
	}


	@Override public Geometry toGeometry()
	{
		if(isArea())
		{
			return PolygonBuilder.build(store.geometryFactory(), this);
		}
		else
		{
			return toGeometryCollection();
		}
	}

    // TODO: Decide what this should return
    @Override public int[] toXY()
    {
        return new int[0];  // TODO
    }

	/**
	 * Recursively gathers the geometries of the relation's members
	 *
	 * @param geoms		list where to add the member geometries
	 * @param processedRelations	set of relations (IDs) we've already processed
	 *                              (used to guard against circular refs)
	 */
	// TODO: could OR type bits of members instead of checking geometry type,
	//  avoid branching
	private Class<?> gatherGeometries(List<Geometry> geoms,
		MutableLongSet processedRelations, Class<?> commonType)
	{
		processedRelations.add(id());

		for(Feature member: this)
		{
			if(member instanceof StoredRelation memberRel && !memberRel.isArea())
			{
				// Gather geometries from sub-relations that aren't areas

				if (!processedRelations.contains(memberRel.id()))
				{
					// avoid endless recursion in case relations are in
					// a reference cycle
					commonType = memberRel.gatherGeometries(geoms, processedRelations, commonType);
				}
			}
			else if(!member.isPlaceholder())		// skip placeholders
			{
				// Add points, lines, (multi)polygons
				Geometry g = member.toGeometry();
				Class<?> geomType = g.getClass();
				if(geomType != commonType)
				{
					commonType = (commonType==null) ? geomType : Geometry.class;

					// TODO: This won't work if spec changed so Way returns LinearRing
					//  as well as LineString)
					//  (but for now, it always returns LineString)
					//  See Issue #58
				}
				geoms.add(g);
			}
		}
		return commonType;
	}

	/**
	 * Creates a GeometryCollection (used to represent non-area relations)
	 *
	 * @return a GeometryCollection 
	 */
	private Geometry toGeometryCollection()
	{
		// FeatureStoreBase.log.debug("Creating GeometryCollection for {} ...", this);
		List<Geometry> geoms = new ArrayList<>();
		Class<?> commonType = gatherGeometries(geoms, new LongHashSet(), null);
			// TODO: could create set lazily, and also replace it with
			//  HashSet<Relation> to get rid of Eclipse Collections dependency
		GeometryFactory factory = store.geometryFactory();
		if(commonType == LineString.class)
		{
			return factory.createMultiLineString(geoms.toArray(new LineString[0]));
		}
		if(commonType == Point.class)
		{
			return factory.createMultiPoint(geoms.toArray(new Point[0]));
		}

		// TODO: should a collection of polygons be treated as a MultiPolygon,
		//  even though it is not a relation with type=multipolygon ?

		return store.geometryFactory().createGeometryCollection(
			geoms.toArray(new Geometry[0]));
	}

	@Override public Features members()
	{
		return members(TypeBits.ALL, Matcher.ALL, null);
	}

	private Features members(int types, String query)
	{
		Matcher matcher = store.getMatcher(query);
		return members(types & matcher.acceptedTypes(), matcher, null);
	}

	public Features members(int types, Matcher matcher, Filter filter)
	{
		// if(types == 0) return EmptyView.ANY;
            // not needed
		int ppMembers = ptr + 12;
		int pMembers = ppMembers + buf.getInt(ppMembers);
		if(isEmpty(pMembers)) return EmptyView.ANY;
		return new MemberView(store, buf, pMembers, types, matcher, filter);
	}

	@Override public Features members(String q)
	{
		return members(TypeBits.ALL, q);
	}
}
