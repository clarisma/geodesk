/*
 * Copyright (c) Clarisma / GeoDesk contributors
 *
 * This source code is licensed under the Apache 2.0 license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.geodesk.feature.store;

import com.geodesk.feature.Tags;

import java.util.Map;

public class EmptyTags implements Tags
{
    public static final EmptyTags SINGLETON = new EmptyTags();

    @Override public boolean isEmpty()
    {
        return true;
    }

    @Override public int size()
    {
        return 0;
    }

    @Override public boolean next()
    {
        return false;
    }

    @Override public String key()
    {
        return null;
    }

    @Override public Object value()
    {
        return null;
    }

    @Override public String stringValue()
    {
        return null;
    }

    @Override public Map<String, Object> toMap()
    {
        return null;
    }
}
