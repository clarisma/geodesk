/*
 * Copyright (c) Clarisma / GeoDesk contributors
 *
 * This source code is licensed under the Apache 2.0 license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.geodesk.feature.store;

import com.clarisma.common.store.BlobStoreConstants;

public class FeatureStoreConstants extends BlobStoreConstants
{
    public static final int MAGIC = 0x1CE50D6E;  // "geodesic"
    public static final int VERSION = 1_000_000;

    public static final int MAGIC_CODE_OFS = 32;
    public static final int VERSION_OFS = 36;
    public static final int ZOOM_LEVELS_OFS = 40;
    public static final int TILE_INDEX_PTR_OFS = 44;
    public static final int PROPERTIES_PTR_OFS = 48;
    public static final int STRING_TABLE_PTR_OFS = 52;
    public static final int INDEX_SCHEMA_PTR_OFS = 56;

}
