package com.geodesk.feature;

import com.geodesk.feature.filter.WithinFilter;
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
        return within(PreparedGeometryFactory.prepare(geom));
    }

    public static Filter within(PreparedGeometry prepared)
    {
        return new WithinFilter(prepared);
    }
}
