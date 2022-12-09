/*
 * Copyright (c) Clarisma / GeoDesk contributors
 *
 * This source code is licensed under the Apache 2.0 license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.geodesk.feature;

import com.geodesk.feature.query.EmptyView;
import com.geodesk.geom.Bounds;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public interface View<T extends Feature> extends Iterable<T>
{
    /**
     * Returns a view of this collection that only contains features
     * matching the given query.
     *
     * @param query a query in <a href="/goql">GOQL</a> format
     * @return a feature collection
     */
    View<T> select(String query);

    /**
     * Returns a view of this collection that contains only features whose
     * bounding box intersects the given {@link Bounds}.
     *
     * @param bbox the bounding box to use as a filter
     * @return a collection of {@link Relation} objects
     */
    View<T> in(Bounds bbox);

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
        return first() == null;
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

    View<T> select(Filter filter);

    default <U extends T> View<U> select(Class<U> type)
    {
        return (View<U>)this;
    }
}
