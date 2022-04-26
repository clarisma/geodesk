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
