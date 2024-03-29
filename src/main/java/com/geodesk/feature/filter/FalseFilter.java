/*
 * Copyright (c) Clarisma / GeoDesk contributors
 *
 * This source code is licensed under the Apache 2.0 license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.geodesk.feature.filter;

import com.geodesk.feature.Feature;
import com.geodesk.feature.Filter;

public class FalseFilter implements Filter
{
    public static final Filter INSTANCE = new FalseFilter();
        // TODO: move to Filters

    @Override public boolean accept(Feature f)
    {
        return false;
    }
}
