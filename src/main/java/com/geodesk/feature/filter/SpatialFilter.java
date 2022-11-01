/*
 * Copyright (c) Clarisma / GeoDesk contributors
 *
 * This source code is licensed under the Apache 2.0 license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.geodesk.feature.filter;

import com.geodesk.feature.Feature;
import com.geodesk.feature.Filter;

public class SpatialFilter implements Filter
{
    @Override public boolean accept(Feature feature)
    {
        return acceptGeometry(feature.toGeometry());
    }
}
