/*
 * Copyright (c) Clarisma / GeoDesk contributors
 *
 * This source code is licensed under the Apache 2.0 license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.geodesk.feature;

import com.geodesk.geom.Bounds;

public interface Nodes extends View<Node>
{
    Nodes select(String query);
    Nodes in(Bounds bbox);
    Nodes select(Filter filter);
}
