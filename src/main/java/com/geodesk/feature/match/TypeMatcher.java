/*
 * Copyright (c) Clarisma / GeoDesk contributors
 *
 * This source code is licensed under the Apache 2.0 license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.geodesk.feature.match;

import java.nio.ByteBuffer;


public class TypeMatcher extends Matcher
{
    private final int acceptedTypes;
    private final Matcher matcher;

    public TypeMatcher(int acceptedTypes, Matcher matcher)
    {
        this.acceptedTypes = acceptedTypes;
        this.matcher = matcher;
    }

    @Override public boolean accept(ByteBuffer buf, int pos)
    {
        int flags = buf.get(pos);
        int type = 1 << ((flags >>> 1) & 0x1f);
        if((type & acceptedTypes) == 0) return false;
        return matcher.accept(buf, pos);
    }

    @Override public boolean acceptTyped(int types, ByteBuffer buf, int pos)
    {
        return matcher.acceptTyped(types & acceptedTypes, buf, pos);
    }

    @Override public boolean acceptIndex(int keys)
    {
        return matcher.acceptIndex(keys);
    }
}
