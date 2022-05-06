package com.geodesk.feature.filter;

import com.geodesk.feature.Filter;

public interface MemberFilter
{
    Filter filterForRole(int roleCode, String roleString);
    // TODO: call it acceptRole
}
