package com.geodesk.feature.match;

import com.geodesk.feature.store.FeatureStore;

public class RoleMatcher extends Matcher
{
    private final int roleCode;

    public RoleMatcher(FeatureStore store, String role)
    {
        roleCode = store.codeFromString(role);
        assert roleCode != 0;
    }

    public Matcher acceptRole(int roleCode, String roleString)
    {
        return roleCode == this.roleCode ? this : null;
    }
}
