package com.geodesk.feature.store;

import com.geodesk.feature.*;
import com.geodesk.geom.Bounds;

import java.util.Collections;
import java.util.Iterator;

public class EmptyFeatures<T extends Feature> implements Features<T>
{
    public static final Features<?> ANY = new EmptyFeatures<>();
    /*
    public static final Nodes NODES = (Features<Node>)new EmptyFeatures<Node>();
    public static final Ways WAYS = (Ways)new EmptyFeatures<Way>();
    public static final Relations RELATIONS = (Relations)new EmptyFeatures<Relation>();
     */

    // public static final Features<?> XXX = NODES; // OK

    @Override public boolean isEmpty()
    {
        return true;
    }

    @Override public long count()
    {
        return 0;
    }

    @Override public Features<?> features(String filter)
    {
        return ANY;
    }

    @Override public Features<Node> nodes()
    {
        return (Features<Node>)ANY;
    }

    @Override public Features<Node> nodes(String filter)
    {
        return (Features<Node>)ANY;
    }

    @Override public Features<Way> ways()
    {
        return (Features<Way>)ANY;
    }

    @Override public Features<Way> ways(String filter)
    {
        return (Features<Way>)ANY;
    }

    /*
    @Override public Features<?> areas()
    {
        return ANY;
    }

    @Override public Features<?> areas(String filter)
    {
        return ANY;
    }
     */

    @Override public Features<Relation> relations()
    {
        return (Features<Relation>)ANY;
    }

    @Override public Features<Relation> relations(String filter)
    {
        return (Features<Relation>)ANY;
    }

    @Override public Features<T> in(Bounds bbox)
    {
        return this;
    }

    /*
    @Override public boolean contains(T f)
    {
        return false;
    }

    @Override public boolean containsNode(long id)
    {
        return false;
    }

    @Override public boolean containsWay(long id)
    {
        return false;
    }

    @Override public boolean containsRelation(long id)
    {
        return false;
    }

    @Override public Node node(long id)
    {
        return null;
    }

    @Override public Way way(long id)
    {
        return null;
    }

    @Override public Relation relation(long id)
    {
        return null;
    }
     */

    @Override public Iterator<T> iterator()
    {
        return Collections.emptyIterator();
    }
}
