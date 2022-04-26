package com.geodesk.feature.store;

import com.geodesk.feature.*;
import com.geodesk.core.Box;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;

/**
 * A Node without tags that doesn't belong to a Relation, i.e. one
 * that isn't a proper feature, but merely defines the geometry of a Way.
 */
public class AnonymousWayNode implements Node
{
    private final FeatureStoreBase store;
    private final int x;
    private final int y;

    public AnonymousWayNode(FeatureStoreBase store, int x, int y)
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
        if(parent instanceof Way)
        {
            // TODO: execute query
            return false;
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
    @Override public Features<Relation> parentRelations()
    {
        return (Features<Relation>)EmptyFeatures.ANY;
    }

    @Override public boolean isArea()
    {
        return false;
    }

    @Override public Geometry toGeometry()
    {
        return store.geometryFactory().createPoint(new Coordinate(x(), y()));
    }

    @Override public boolean belongsToWay()
    {
        return true;
    }

    @Override public Features<Way> parentWays()
    {
        // TODO: execute query
        return null;
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
}
