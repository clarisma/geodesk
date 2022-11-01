/*
 * Copyright (c) Clarisma / GeoDesk contributors
 *
 * This source code is licensed under the Apache 2.0 license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.geodesk.feature.query;

import com.geodesk.feature.Way;
import com.geodesk.feature.match.MatcherSet;

/**
 * A view that contains the parent ways of a specific nodes.
 *
 * We use a WorldView whose bounding box is constrained to the coordinates of
 * the node. If the node is a feature node, we select only ways that have the
 * way-node flag set. We apply a Filter (that is also a Matcher) that accepts
 * only ways that contain the node as part of their geometry.
 */
/*
public class ParentWayView extends WorldView<Way>
{
    // TODO: for "in()", can't use the default approach of intersecting;
    //  instead, need to add the bbox as a Filter (this is rarely used)
    //  No: bbox should not be intersected; a query with 2 bboxes simply means
    //  "this feature must intersect both bboxes" -- this does not mean that
    //  the bboxes themselves must intersect

    public ParentWayView(WorldView<?> other, int types, MatcherSet filters)
    {
        super(other, types, filters);
    }
}
 */
