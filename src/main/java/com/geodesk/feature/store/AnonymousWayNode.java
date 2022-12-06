/*
 * Copyright (c) Clarisma / GeoDesk contributors
 *
 * This source code is licensed under the Apache 2.0 license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.geodesk.feature.store;

import com.geodesk.core.XY;
import com.geodesk.feature.*;
import com.geodesk.core.Box;
import com.geodesk.feature.match.Matcher;
import com.geodesk.feature.match.MatcherSet;
import com.geodesk.feature.match.TypeBits;
import com.geodesk.feature.match.TypeMatcher;
import com.geodesk.feature.query.EmptyView;
import com.geodesk.feature.query.WorldView;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;

import java.util.Collections;
import java.util.Iterator;

/**
 * A Node without tags that doesn't belong to a Relation, i.e. one
 * that isn't a proper feature, but merely defines the geometry of a Way.
 */
public class AnonymousWayNode implements Node
{
    private final FeatureStore store;
    private final int x;
    private final int y;

    public AnonymousWayNode(FeatureStore store, int x, int y)
    {
        this.store = store;
        this.x = x;
        this.y = y;
    }

    @Override public long id()
    {
        return 0;
    }

    @Override public FeatureType type()
    {
        return FeatureType.NODE;
    }

    @Override public int x()
    {
        return x;
    }

    @Override public int y()
    {
        return y;
    }

    @Override public boolean isPlaceholder()
    {
        return false;
    }

    @Override public Box bounds()
    {
        return new Box(x,y);
    }

    @Override public Tags tags()
    {
        return EmptyTags.SINGLETON;
    }

    @Override public String tag(String k)
    {
        return "";
    }

    @Override public boolean hasTag(String k)
    {
        return false;
    }

    @Override public boolean hasTag(String k, String v)
    {
        return false;
    }

    @Override public boolean belongsTo(Feature parent)
    {
        if(parent instanceof StoredWay way)     // TODO: other possible types?
        {
            long xy = XY.of(x,y);
            StoredWay.XYIterator iter = way.iterXY(0);
            while (iter.hasNext())
            {
                if(iter.nextXY() == xy) return true;
            }
        }
        return false;
    }

    @Override public String role()
    {
        return null;
    }

    @Override public String stringValue(String key)
    {
        return "";
    }

    @Override public int intValue(String key)
    {
        return 0;
    }

    @Override public double doubleValue(String key)
    {
        return 0;
    }

    @Override public boolean booleanValue(String key)
    {
        return false;
    }

    /**
     * Always returns `false`, because this type of `Node` by definition
     * does not belong to any relation.
     *
     * @return `false`
     */
    @Override public boolean belongsToRelation()
    {
        return false;
    }

    /**
     * Returns an empty feature collection, because this type of `Node` by
     * definition has no parent relations.
     *
     * @return an empty feature collection
     */
    @Override public Features parentRelations()
    {
        return EmptyView.ANY;
    }

    @Override public boolean isArea()
    {
        return false;
    }

    @Override public Geometry toGeometry()
    {
        return store.geometryFactory().createPoint(new Coordinate(x(), y()));
    }

    @Override public boolean equals(Object other)
    {
        if(!(other instanceof Node otherNode)) return false;
        return otherNode.id() == 0 && otherNode.x() == x && otherNode.y() == y;
    }

    @Override public int hashCode()
    {
        return x ^ y;
    }

    @Override public String toString()
    {
        return "node@" + x + "," + y;
    }

    @Override public int[] toXY()
    {
        return new int[] { x,y };
    }

    @Override public Nodes nodes()
    {
        return null; // TODO: return a Nodes collection with only this node
    }

    @Override public Features parentWays()
    {
        final Matcher matcher = new TypeMatcher(TypeBits.WAYS, Matcher.ALL);
        return new WorldView(store, TypeBits.WAYS,
            bounds(), new MatcherSet(TypeBits.WAYS, null, Matcher.ALL, matcher, null, null),
            new ParentWayFilter(x,y));
            // TODO: these should be singletons
    }

    @Override public Iterator<Feature> iterator()
    {
        return Collections.emptyIterator();
    }

    private static class ParentWayFilter implements Filter
    {
        private long xy;

        public ParentWayFilter(int x, int y)
        {
            this.xy = XY.of(x,y);
        }

        @Override public boolean accept(Feature feature)
        {
            StoredWay way = (StoredWay)feature;
            StoredWay.XYIterator iter = way.iterXY(0);
            while (iter.hasNext())
            {
                if(iter.nextXY() == xy) return true;
            }
            return false;
        }
    }

}
