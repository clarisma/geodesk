/*
 * Copyright (c) Clarisma / GeoDesk contributors
 *
 * This source code is licensed under the Apache 2.0 license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.geodesk.feature;

import com.geodesk.feature.filter.*;
import com.geodesk.feature.match.QueryException;
import com.geodesk.feature.query.EmptyView;
import com.geodesk.geom.Bounds;
import com.geodesk.geom.Mercator;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.prep.PreparedGeometry;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

// TODO: make a hierarchy of queries:
//  FeatureLibrary
//  +- filtered
//     +- filtered&bboxed
//        Allows us to store bbox coords, no need to reference a Box;
//        then we don't care about Box being mutable

// TODO:
//  Maybe create two subtypes: FeatureSet and FeatureList
//  This way, users can write Set<Feature> and List<Node>


/// A collection of features.
///
public interface Features extends Iterable<Feature>
{
    /// Returns a view of this collection that only contains features
    /// matching the given query.
    ///
    /// @param query a query in [GOQL](https://docs.geodesk.com/goql) format
    /// @return a feature collection
    ///
    Features select(String query);

    /// Returns a view of this collection that contains only nodes.
    ///
    /// @return a collection of [Node] objects
    ///
    Features nodes();

    /// Returns a view of this collection that contains only nodes matching
    /// the given query.
    ///
    /// @param query a query in [GOQL](https://docs.geodesk.com/goql) format
    /// @return a collection of [Node] objects
    ///
    Features nodes(String query);

    /// Returns a view of this collection that contains only ways.
    ///
    /// @return a collection of [Way] objects
    ///
    Features ways();

    /// Returns a view of this collection that contains only ways matching
    /// the given query.
    ///
    /// @param query a query in [GOQL](https://docs.geodesk.com/goql) format
    /// @return a collection of [Way] objects
    ///
    Features ways(String query);

    /// Returns a view of this collection that contains only relations.
    ///
    /// @return a collection of [Relation] objects
    ///
    Features relations();

    /// Returns a view of this collection that contains only relations matching
    /// the given query.
    ///
    /// @param query a query in [GOQL](https://docs.geodesk.com/goql) format
    /// @return a collection of [Relation] objects
    ///
    Features relations(String query);

    /// Returns a sub-view that contains only the features that are nodes of the
    /// given way.
    ///
    /// @param parent    a way or relation
    /// @return          a collection of features
    ///
    default Features nodesOf(Feature parent)
    {
        throw new QueryException("Not implemented for this query.");
    }

    /// Returns the features that are nodes of the
    /// given way, or members of the given relation. If a node is passed as
    /// `parent`, an empty view is returned (as nodes cannot have child elements).
    ///
    /// @param parent    a way or relation
    /// @return          a collection of features
    ///
    default Features membersOf(Feature parent)
    {
        throw new QueryException("Not implemented for this query.");
    }

    /// Returns the features that are parent
    /// elements of the given feature (ways and/or relations).
    ///
    /// @param child     a way or relation
    /// @return          a collection of features
    ///
    default Features parentsOf(Feature child)
    {
        throw new QueryException("Not implemented for this query.");
    }

    /// Returns a view of this collection that contains only features whose
    /// bounding box intersects the given [Bounds].
    ///
    /// @param bbox the bounding box to use as a filter
    /// @return a collection of [Feature] objects
    ///
    Features in(Bounds bbox);

    /// Returns the first feature in the collection. If the collection is unordered,
    /// this method selects one of multiple features in a non-deterministic way.
    ///
    /// @return the first feature, or `null` if the collection is empty
    ///
    default Feature first()
    {
        Iterator<Feature> iter = iterator();
        return(iter.hasNext() ? iter.next() : null);
    }

    /// Returns the number of features in this collection.
    ///
    /// @return the number of features
    ///
    default long count()
    {
        long count = 0;
        Iterator<Feature> iter = iterator();
        while(iter.hasNext())
        {
            iter.next();
            count++;
        }
        return count;
    }

    /// Returns `true` if this collection contains no features.
    ///
    /// @return `true` if this collection contains no features
    ///
    default boolean isEmpty()
    {
        return first() == null;
    }

    /// Creates a [List] containing all features in this collection.
    ///
    /// @return  a list containing all features
    ///
    default List<Feature> toList()
    {
        List<Feature> list = new ArrayList<>();
        for(Feature f: this) list.add(f);
        return list;
    }

    /// Creates an array containing all features in this collection.
    ///
    /// @return  an array containing all features
    ///
    default Object[] toArray()
    {
        return toList().toArray();
    }

    default Feature[] toArray(Feature[] a)
    {
        return toList().toArray(a);
    }

    /// Checks whether this collection contains the given object.
    ///
    /// @param f the object whose presence in this collection is to be tested
    ///
    /// @return  `true` if this collection contains the specified object
    ///
    // TODO: be sure to override this brute-force default implementation;
    // in mot cases, we can get a "fast false":
    // - A way's node collection can only contain nodes for which belongsToWay() is true
    // - Similar for relation collection: belongsToRelation must be true
    // - For query-based collections, we can simply apply the filter to the given object;
    //   no need to execute the query
    default boolean contains(Object f)
    {
        Iterator<Feature> iter = iterator();
        while(iter.hasNext())
        {
            if(f.equals(iter.next())) return true;
        }
        return false;
    }

    Features select(Filter filter);

    // Filters

    /// Returns all features that have at least one common node
    /// with the given `Feature`.
    ///
    /// @param f the `Feature` whose nodes to check against
    /// @return
    ///
    default Features connectedTo(Feature f)
    {
        return select(new ConnectedFilter(f));
    }

    /// Returns all features that have at least one common
    /// vertex with the given `Geometry`. Coordinates of the `Geometry` are
    /// rounded to integers.
    ///
    /// @param geom the `Geometry` whose vertexes to check against
    /// @return
    ///
    default Features connectedTo(Geometry geom)
    {
        return select(new ConnectedFilter(geom));
    }

    /// Returns all features that contain the given Mercator-projected coordinate
    ///
    /// @param x
    /// @param y
    /// @return
    ///
    default Features containingXY(int x, int y)
    {
        return select(new ContainsPointFilter(x, y));
    }

    /// Returns all features that contain the given coordinate expressed
    /// at longitude and latitude
    ///
    /// @param lon
    /// @param lat
    /// @return
    ///
    default Features containingLonLat(double lon, double lat)
    {
        int x = (int)Mercator.xFromLon(lon);
        int y = (int)Mercator.yFromLat(lat);
        return select(new ContainsPointFilter(x, y));
    }

    /// Returns all features that contain the given feature.
    ///
    /// @param feature
    /// @return
    ///
    default Features containing(Feature feature)
    {
        if(feature instanceof Node)
        {
            return select(new ContainsPointFilter(feature.x(), feature.y()));
        }
        return select(new ContainsFilter(feature));
    }

    /// Returns all features that contain the given `Geometry`.
    ///
    /// @param geom
    /// @return
    ///
    default Features containing(Geometry geom)
    {
        return select(new ContainsFilter(geom));
    }

    /// Returns all features that contain the given `PreparedGeometry`.
    ///
    /// @param prepared
    /// @return
    ///
    default Features containing(PreparedGeometry prepared)
    {
        return select(new ContainsFilter(prepared));
    }

    default Features coveredBy(Feature feature)
    {
        return select(new CoveredByFilter(feature));
    }

    default Features coveredBy(Geometry geom)
    {
        return select(new CoveredByFilter(geom));
    }

    default Features coveredBy(PreparedGeometry prepared)
    {
        return select(new CoveredByFilter(prepared));
    }

    default Features crossing(Feature feature)
    {
        return select(new CrossesFilter(feature));
    }

    default Features crossing(Geometry geom)
    {
        return select(new CrossesFilter(geom));
    }

    default Features crossing(PreparedGeometry prepared)
    {
        return select(new CrossesFilter(prepared));
    }

    default Features disjoint(Feature feature)
    {
        return select(new DisjointFilter(feature));
    }

    default Features disjoint(Geometry geom)
    {
        return select(new DisjointFilter(geom));
    }

    default Features disjoint(PreparedGeometry prepared)
    {
        return select(new DisjointFilter(prepared));
    }

    default Features intersecting(Feature feature)
    {
        return select(new IntersectsFilter(feature));
    }

    default Features intersecting(Geometry geom)
    {
        return select(new IntersectsFilter(geom));
    }

    default Features intersecting(PreparedGeometry prepared)
    {
        return select(new IntersectsFilter(prepared));
    }

    /// Returns all features whose closest point lies within
    /// a given radius.
    ///
    /// @param distance  the maximum distance (in meters)
    /// @param x         the X coordinate of the center point
    /// @param y         the Y coordinate of the center point
    /// @return
    ///
    default Features maxMetersFromXY(double distance, int x, int y)
    {
        return select(new PointDistanceFilter(distance, x, y));
    }

    /// Returns all features whose closest point lies within
    /// a given radius.
    ///
    /// @param distance  the maximum distance (in meters)
    /// @param lon       the longitude of the center point
    /// @param lat       the latitude of the center point
    /// @return
    ///
    default Features maxMetersFromLonLat(double distance, double lon, double lat)
    {
        int x = (int) Mercator.xFromLon(lon);
        int y = (int)Mercator.yFromLat(lat);
        return select(new PointDistanceFilter(distance, x, y));
    }

    /// Returns all features that lie within a given distance
    /// from a `Geometry`. The Filter measures the distance between the closest
    /// points of the Geometry and the candidate Feature.
    ///
    /// @param distance  the maximum distance (in meters)
    /// @param geom      the Geometry from which to measure
    /// @return
    ///
    default Features maxMetersFrom(double distance, Geometry geom)
    {
        throw new RuntimeException("todo");     // TODO
    }

    /**
     * Returns all features that lie within a given distance
     * from another `Feature`. The Filter measures the distance between the closest
     * points of the features.
     *
     * @param distance  the maximum distance (in meters)
     * @param feature   the Feature from which to measure
     * @return
     */
    default Features maxMetersFrom(double distance, Feature feature)
    {
        throw new RuntimeException("todo");     // TODO
    }

    default Features overlapping(Feature feature)
    {
        return select(new OverlapsFilter(feature));
    }

    default Features overlapping(Geometry geom)
    {
        return select(new OverlapsFilter(geom));
    }

    default Features overlapping(PreparedGeometry prepared)
    {
        return select(new OverlapsFilter(prepared));
    }

    default Features touching(Feature feature)
    {
        return select(new TouchesFilter(feature));
    }

    default Features touching(Geometry geom)
    {
        return select(new TouchesFilter(geom));
    }

    default Features touching(PreparedGeometry prepared)
    {
        return select(new TouchesFilter(prepared));
    }

    default Features within(Feature feature)
    {
        return select(new WithinFilter(feature));
    }

    default Features within(Geometry geom)
    {
        return select(new WithinFilter(geom));
    }

    default Features within(PreparedGeometry prepared)
    {
        return select(new WithinFilter(prepared));
    }

    /// Returns the features present in both this collection and `other`.
    ///
    /// @param other
    /// @return
    ///
    Features select(Features other);

    /// Adds all features in this collection to the given collection.
    ///
    /// @param collection a general-purpose collection (such as `List`, `Set`,
    ///                   o another type derived from `java.util.Collection`)
    ///
    default void addTo(Collection<Feature> collection)
    {
        for(Feature f: this) collection.add(f);
    }
}
