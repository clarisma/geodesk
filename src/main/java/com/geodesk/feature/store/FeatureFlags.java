/*
 * Copyright (c) Clarisma / GeoDesk contributors
 *
 * This source code is licensed under the Apache 2.0 license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.geodesk.feature.store;

// turn into class

public interface FeatureFlags
{
    int LAST_SPATIAL_ITEM_FLAG = 1;
    int AREA_FLAG = 1 << 1;
    int RELATION_MEMBER_FLAG = 1 << 2;
    int FEATURE_TYPE_BITS = 3;              // Bit 3 & 4
    int WAYNODE_FLAG = 1 << 5;
    // int FEATURE_NODES_FLAG = 1 << 5;
    int MULTITILE_BITS = 6;
    int MULTITILE_WEST_BIT = 6;
    int MULTITILE_NORTH_BIT = 7;
    int MULTITILE_WEST = 1 << MULTITILE_WEST_BIT;
    int MULTITILE_NORTH = 1 << MULTITILE_NORTH_BIT;
    int MULTITILE_FLAGS = MULTITILE_WEST | MULTITILE_NORTH;
}
