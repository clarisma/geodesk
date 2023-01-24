/*
 * Copyright (c) Clarisma / GeoDesk contributors
 *
 * This source code is licensed under the Apache 2.0 license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.geodesk.geom;

import java.util.List;

public interface SpatialTreeFactory<B extends Bounds>
{
    B createLeaf(List<? extends Bounds> children, int start, int end);
    B createBranch(List<B> children, int start, int end);
}
