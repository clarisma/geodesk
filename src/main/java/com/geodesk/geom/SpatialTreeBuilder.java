/*
 * Copyright (c) Clarisma / GeoDesk contributors
 *
 * This source code is licensed under the Apache 2.0 license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.geodesk.geom;

import java.util.ArrayList;

/// @hidden
public interface SpatialTreeBuilder<B extends Bounds>
{
    B build(ArrayList<? extends Bounds> items);
}
