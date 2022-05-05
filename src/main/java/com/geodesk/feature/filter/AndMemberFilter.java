package com.geodesk.feature.filter;

import com.geodesk.feature.Filter;

public class AndMemberFilter implements MemberFilter
{
    private final MemberFilter a;
    private final MemberFilter b;

    public AndMemberFilter(MemberFilter a, MemberFilter b)
    {
        this.a = a;
        this.b = b;
    }

    @Override public Filter filterForRole(int roleCode, String roleString)
    {
        Filter aFilter = a.filterForRole(roleCode, roleString);
        Filter bFilter = b.filterForRole(roleCode, roleString);
        if(aFilter != null)
        {
            return bFilter==null ? aFilter : new AndFilter(aFilter, bFilter);
        }
        return bFilter;
    }
}
