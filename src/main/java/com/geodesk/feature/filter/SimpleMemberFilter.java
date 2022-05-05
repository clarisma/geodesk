package com.geodesk.feature.filter;

import com.geodesk.feature.Filter;

public class SimpleMemberFilter implements MemberFilter
{
    protected final Filter f0;

    public SimpleMemberFilter(Filter filter)
    {
        f0 = filter;
    }

    @Override public Filter filterForRole(int roleCode, String roleString)
    {
        return f0;
    }
}
