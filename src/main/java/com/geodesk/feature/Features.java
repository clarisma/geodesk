package com.geodesk.feature;

import com.geodesk.geom.Bounds;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

// TODO: make a hierarchy of queries:
//  FeatureLibrary
//  +- filtered
//     +- filtered&bboxed
//        Allows us to store bbox coords, no need to reference a Box;
//        then we don't care about Box being mutable

/**
 * A collection of features.
 *
 * @param <T> the subtype of features ({@link Node}, {@link Way} or
 *            {@link Relation}) or `?` for any type of feature
 */
public interface Features<T extends Feature> extends Iterable<T>
{
    /**
     * Returns a view of this collection that only contains features
     * matching the given query.
     *
     * @param query a query in <a href="/goql">GOQL</a> format
     * @return a feature collection
     */
    Features<?> features(String query);

    /**
     * Returns a view of this collection that contains only nodes.
     *
     * @return a collection of {@link Node} objects
     */
    Features<Node> nodes();

    /**
     * Returns a view of this collection that contains only nodes matching
     * the given query.
     *
     * @param query a query in <a href="/goql">GOQL</a> format
     * @return a collection of {@link Node} objects
     */
    Features<Node> nodes(String query);

    /**
     * Returns a view of this collection that contains only ways.
     *
     * @return a collection of {@link Way} objects
     */
    Features<Way> ways();

    /**
     * Returns a view of this collection that contains only ways matching
     * the given query.
     *
     * @param query a query in <a href="/goql">GOQL</a> format
     * @return a collection of {@link Way} objects
     */
    Features<Way> ways(String query);
    /*
    Features<?> areas();
    Features<?> areas(String query);
     */

    /**
     * Returns a view of this collection that contains only relations.
     *
     * @return a collection of {@link Relation} objects
     */
    Features<Relation> relations();

    /**
     * Returns a view of this collection that contains only relations matching
     * the given query.
     *
     * @param query a query in <a href="/goql">GOQL</a> format
     * @return a collection of {@link Relation} objects
     */
    Features<Relation> relations(String query);

    /**
     * Returns a view of this collection that contains only features whose
     * bounding box intersects the given {@link Bounds}.
     *
     * @param bbox the bounding box to use as a filter
     * @return a collection of {@link Relation} objects
     */
    Features<?> in(Bounds bbox);

    /**
     * Returns the first feature in the collection. If the collection is unordered,
     * this method selects one of multiple features in a non-deterministic way.
     *
     * @return the first feature, or `null` if the collection is empty
     */
    default T first()
    {
        Iterator<T> iter = iterator();
        return(iter.hasNext() ? iter.next() : null);
    }

    /**
     * Returns the number of features in this collection.
     *
     * @return the number of features
     */
    default long count()
    {
        long count = 0;
        Iterator<T> iter = iterator();
        while(iter.hasNext())
        {
            iter.next();
            count++;
        }
        return count;
    }

    /**
     * Returns `true` if this collection contains no features.
     *
     * @return `true` if this collection contains no features
     */
    default boolean isEmpty()
    {
        return first() != null;
    }

    /**
     * Creates a {@link List} containing all features in this collection.
     *
     * @return  a list containing all features
     */
    default List<T> toList()
    {
        List<T> list = new ArrayList<>();
        for(T f: this) list.add(f);
        return list;
    }

    /**
     * Creates an array containing all features in this collection.
     *
     * @return  an array containing all features
     */
    default Object[] toArray()
    {
        return toList().toArray();
    }

    default T[] toArray(T[] a)
    {
        return toList().toArray(a);
    }

    /**
     * Checks whether this collection contains the given object.
     *
     * @param f the object whose presence in this collection is to be tested
     *
     * @return  `true` if this collection contains the specified object
     */
    // TODO: be sure to override this brute-force default implementation;
    // in mot cases, we can get a "fast false":
    // - A way's node collection can only contain nodes for which belongsToWay() is true
    // - Similar for relation collection: belongsToRelation must be true
    // - For query-based collections, we can simply apply the filter to the given object;
    //   no need to execute the query
    default boolean contains(Object f)
    {
        Iterator<T> iter = iterator();
        while(iter.hasNext())
        {
            if(f.equals(iter.next())) return true;
        }
        return false;
    }

        // TODO: implement later
    /*
    boolean contains(T f);
    boolean containsNode(long id);
    boolean containsWay(long id);
    boolean containsRelation(long id);
    Node node(long id);
    Way way(long id);
    Relation relation(long id);
    */

    // TODO: probably not needed:
    // Features<Feature> nodesAndAreas();
    // Features<Feature> nodesAndAreas(String filter);
    // Features<Feature> waysAndAreas();
    // Features<Feature> waysAndAreas(String filter);


    /*
    Features<Node> wayNodes();                      // not needed: use of()
    Features<Node> wayNodesOf(String filter);       // not needed: use of()
    Features<?> relationMembers();                  // not needed: use of()
    Features<?> membersOf(String filter);           // not needed: use of()
    Features<T> ofType(Class<T extends Feature);    // not needed: use way() & relation()
    Features<T> of(Feature parent);                 // only useful one!
    Features<T> of(Features<?> parents);
    Features<T> with(Features<?> children);

     */
}
