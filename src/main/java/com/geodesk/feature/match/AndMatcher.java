/*
 * Copyright (c) Clarisma / GeoDesk contributors
 *
 * This source code is licensed under the Apache 2.0 license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.geodesk.feature.match;

import java.nio.ByteBuffer;

public class AndMatcher extends Matcher
{
    private final Matcher a;
    private final Matcher b;

    public AndMatcher(Matcher a, Matcher b)
    {
        this.a = a;
        this.b = b;
    }

    @Override public boolean accept(ByteBuffer buf, int pos)
    {
        return a.accept(buf, pos) && b.accept(buf, pos);
    }

    @Override public boolean acceptTyped(int types, ByteBuffer buf, int pos)
    {
        return a.acceptTyped(types, buf, pos) && b.acceptTyped(types, buf, pos);
    }

    @Override public boolean acceptIndex(int keys)
    {
        return a.acceptIndex(keys) && b.acceptIndex(keys);
    }

    @Override public Matcher acceptRole(int roleCode, String roleString)
    {
        Matcher ma = a.acceptRole(roleCode, roleString);
        if(ma == null) return null;
        Matcher mb = b.acceptRole(roleCode, roleString);
        if(mb == null) return null;
        return new AndMatcher(ma, mb);
    }
}
