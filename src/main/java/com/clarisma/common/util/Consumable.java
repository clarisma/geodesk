/*
 * Copyright (c) Clarisma / GeoDesk contributors
 *
 * This source code is licensed under the Apache 2.0 license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.clarisma.common.util;

/**
 * A lightweight Iterator-like data structure that can only be traversed once.
 */
public interface Consumable
{
    boolean isEmpty();
}
