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
     *
     * If set, `Filter ` must implement `bounds()`.
     */
    public static final int USES_BBOX = 1;

    /**
     * Features accepted by the Filter must be fully contained within the
     * bounding box (USES_BBOX must be set as well)
     */
    public static final int STRICT_BBOX = 2;

    /**
     * Given a specific tile, the Filter is able to accept all features
     * within this tile, reject the tile entirely, or at least offer
     * a simplified filter.
     *
     * If set, `Filter` must implement `filterForTile()`
     */
    public static final int FAST_TILE_FILTER = 4;

    /**
     * A flag to indicate that this Filter expects the Geometry of the
     * feature passed to `accept()`. If this flag is not set, the `Geometry`
     * argument may be `null` (in which case the Filter has to obtain the
     * feature's geometry explicitly).
     */
    public static final int NEEDS_GEOMETRY = 8;

    /**
     * The filter accepts only a subset of types (e.g. only areas, only
     * relation members).
     *
     * If set, `Filter` must implement `acceptedTypes()`
     */
    public static final int RESTRICTS_TYPES = 16;
}
