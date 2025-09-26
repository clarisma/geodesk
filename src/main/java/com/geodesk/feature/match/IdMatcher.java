/*
 * Copyright (c) Clarisma / GeoDesk contributors
 *
 * This source code is licensed under the Apache 2.0 license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.geodesk.feature.match;

import java.nio.ByteBuffer;

public class IdMatcher extends Matcher
{
    private final long idBits;
    private static final long TYPE_ID_MASK = 0xffff_ffff_ffff_f018l;

    public IdMatcher(int typeCode, long id)
    {
        super(TypeBits.ALL);
        idBits = (id << 12) | ((long)typeCode << 3);
        // TODO: change if FeatureFlags change
    }

    @Override public boolean accept(ByteBuffer buf, int pos)
    {
        return (buf.getLong(pos) & TYPE_ID_MASK) == idBits;
    }
}
