/*
 * Copyright (c) Clarisma / GeoDesk contributors
 *
 * This source code is licensed under the Apache 2.0 license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.geodesk.geom;

import com.geodesk.feature.Feature;

/// @hidden
public class Measure
{
    public static double length(Feature f)
    {
        return f.length();
    }

    public static double area(Feature f)
    {
        return f.area();
    }
}
