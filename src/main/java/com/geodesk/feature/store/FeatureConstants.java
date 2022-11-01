/*
 * Copyright (c) Clarisma / GeoDesk contributors
 *
 * This source code is licensed under the Apache 2.0 license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.geodesk.feature.store;

public class FeatureConstants
{
    /**
     * The initial TIP used in iterators for relation members,
     * relation tables, and feature nodes of ways. Rather than 0,
     * we start at this value, because we can address a range of TIPs
     * from 0 to 32,767 with a narrow (15-bit) TIP delta.
     *
     * TIP deltas are signed and range from -16,384 (0xC000) to 16,383
     * (0x3FFF) in their 2-byte representation (1 bit is used for the
     * wide-value flag).
     */
    public static final int START_TIP = 0x4000;  // 16,384
}
