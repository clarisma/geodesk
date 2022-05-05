package com.geodesk.feature.filter;

import com.geodesk.feature.Filter;

public class MemberFilter2 extends SimpleMemberFilter
{
    protected final Filter f1;

    public MemberFilter2(int types, Filter[] filters)
    {
        super(filters[0]);
        f1 = filters[1];
    }
}
