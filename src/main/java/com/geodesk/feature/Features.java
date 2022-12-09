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

// TODO: make a hierarchy of queries:
//  FeatureLibrary
//  +- filtered
//     +- filtered&bboxed
//        Allows us to store bbox coords, no need to reference a Box;
//        then we don't care about Box being mutable

// TODO:
//  Maybe create two subtypes: FeatureSet and FeatureList
//  This way, users can write Set<Feature> and List<Node>

/**
 * A collection of features.
 */
public interface Features extends View<Feature>
{
    /**
     * Returns a view of this collection that only contains features
     * matching the given query.
     *
     * @param query a query in <a href="/goql">GOQL</a> format
     * @return a feature collection
     */
    Features select(String query);

   /**
     * Returns a view of this collection that contains only features whose
     * bounding box intersects the given {@link Bounds}.
     *
     * @param bbox the bounding box to use as a filter
     * @return a collection of {@link Relation} objects
     */
    Features in(Bounds bbox);

    Features select(Filter filter);
}
