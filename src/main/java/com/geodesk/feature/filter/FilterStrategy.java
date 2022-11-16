/*
 * Copyright (c) Clarisma / GeoDesk contributors
 *
 * This source code is licensed under the Apache 2.0 license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.geodesk.feature.filter;

public class FilterStrategy
{
    /**
     * Filter uses spatial index.
     */
    public static final int USES_BBOX = 1;

    /**
     * Features accepted by the Filter must be fully contained within thre
     * bounding box (implies USES_BBOX)
     */
    public static final int STRICT_BBOX = 2;

    /**
     * The Filter is able to reject specific tiles entirely.
     */
    public static final int FAST_REJECT = 4;

    /**
     * For specific tiles, the Filter will accept all features (or can
     * use a simplified acceptance test)
     */
    public static final int FAST_ACCEPT = 8;

    /**
     * In order to test whether a Feature is accepted, the Filter needs the
     * Feature's Geometry.
     */
    public static final int NEEDS_GEOMETRY = 16;

    /**
     * The filter accepts only a subset of types (e.g. only areas, only
     * relation members).
     */
    public static final int RESTRICTS_TYPES = 32;
}
