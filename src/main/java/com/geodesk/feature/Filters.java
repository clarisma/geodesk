/*
 * Copyright (c) Clarisma / GeoDesk contributors
 *
 * This source code is licensed under the Apache 2.0 license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.geodesk.feature;

import com.geodesk.core.Mercator;
import com.geodesk.feature.filter.*;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.prep.PreparedGeometry;
import org.locationtech.jts.geom.prep.PreparedGeometryFactory;

// TODO:
//  in order to create obtain the Geometry of a feature, we need a reference
//  to the FeatureStore; but the regular accept method does not provide
//  this (only buffer and pos).
//  We need to change the interface, so store reference is provided as an
//  argument, or build a separate class hierarchy for spatial filters
//  Maybe these Filters should accept Feature? Would make API more intuitive
//  Could separate Filter vs. PrimitiveFilter
//  The ugly part:
//   - We have to create a Feature in order to pass it to the Filter
//   - but we return a pointer to the query consumer, which has to re-create
//     the feature object once more
//   - We could pass Feature instead of pointer to consumer, but that complicates
//     the dupe-flag checking. Currently, we pass dupe flag along with pointer
//   - Checking the polytile flags of the feature is not sufficient;
//     - no need to de-dupe if query covers only one tile at the feature's
//       zoom level
//     - conversely, bi-tile features need to be de-duped for certain queries
//       (e.g. overlap)
//
//
// TODO:
//  Possible solution: Attach the FeatureStore to the Filter as it is passed
//  to select(); document that Filters must not be re-used across multiple
//  stores
//
// TODO:
//  Better idea: Use role of feature to mark whether feature must be deduplicated
//   (role is never used for WorldView queries)
//  This way, we let query tasks create Features
//  post-filters act on Features

public class Filters
{
    public static Filter within(Feature f)
    {
        return within(f.toGeometry());
    }

    public static Filter within(Geometry geom)
    {
        return geom==null ? FalseFilter.INSTANCE : within(PreparedGeometryFactory.prepare(geom));
    }

    public static Filter within(PreparedGeometry prepared)
    {
        return new WithinFilter(prepared);
    }

    public static Filter crosses(Feature f)
    {
        return new CrossesFilter(f);
    }

    /**
     * Creates a `Filter` that accept features that have at least one common node
     * with the given `Feature`.
     *
     * @param f the `Feature` whose nodes to check against
     * @return
     */
    public static Filter connectedTo(Feature f)
    {
        return new ConnectedFilter(f);
    }

    /**
     * Creates a `Filter` that accept features that have at least one common
     * vertex with the given `Geometry`. Coordinates of the `Geometry` are
     * rounded to integers.
     *
     * @param geom the `Geometry` whose vertexes to check against
     * @return
     */
    public static Filter connectedTo(Geometry geom)
    {
        return new ConnectedFilter(geom);
    }

    /**
     * Creates a `Filter` that accept features whose closest point lies within
     * a given radius.
     *
     * @param distance  the maximum distance (in meters)
     * @param x         the X coordinate of the center point
     * @param y         the Y coordinate of the center point
     * @return
     */
    public static Filter maxMetersFromXY(double distance, int x, int y)
    {
        return new PointDistanceFilter(distance, x, y);
    }

    /**
     * Creates a `Filter` that accept features whose closest point lies within
     * a given radius.
     *
     * @param distance  the maximum distance (in meters)
     * @param lon       the longitude of the center point
     * @param lat       the latitude of the center point
     * @return
     */
    public static Filter maxMetersFromLonLat(double distance, double lon, double lat)
    {
        int x = (int)Mercator.xFromLon(lon);
        int y = (int)Mercator.yFromLat(lat);
        return new PointDistanceFilter(distance, x, y);
    }

    /**
     * Creates a `Filter` that accept features that lie within a given distance
     * from a `Geometry`. The Filter measures the distance between the closest
     * points of the Geometry and the candidate Feature.
     *
     * @param distance  the maximum distance (in meters)
     * @param geom      the Geometry from which to measure
     * @return
     */
    public static Filter maxMetersFrom(double distance, Geometry geom)
    {
        throw new RuntimeException("todo");     // TODO
    }

    /**
     * Creates a `Filter` that accept features that lie within a given distance
     * from another `Feature`. The Filter measures the distance between the closest
     * points of the features.
     *
     * @param distance  the maximum distance (in meters)
     * @param feature   the Feature from which to measure
     * @return
     */
    public static Filter maxMetersFrom(double distance, Feature feature)
    {
        throw new RuntimeException("todo");     // TODO
    }

}
