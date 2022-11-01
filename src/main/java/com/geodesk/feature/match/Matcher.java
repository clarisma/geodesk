/*
 * Copyright (c) Clarisma / GeoDesk contributors
 *
 * This source code is licensed under the Apache 2.0 license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.geodesk.feature.match;

import java.nio.ByteBuffer;

public class Matcher
{
    /**
     * Checks whether a feature meets the conditions of this Matcher.
     *
     * @param buf       the Buffer of the feature
     * @param pos       the anchor position of the feature in the Buffer
     * @return          `true` if the feature matches the filter condition
     */
    public boolean accept(ByteBuffer buf, int pos)
    {
        return true;
    }

    /**
     * Accepts this feature only if its type matches the given type mask.
     *
     * @param types     the type mask to match (must not be 0)
     * @param buf       the Buffer of the feature
     * @param pos       the anchor position of the feature in the Buffer
     * @return          `true` if the feature matches the filter condition
     */
    public boolean acceptTyped(int types, ByteBuffer buf, int pos)
    {
        return (types & (1 << (buf.get(pos) >> 1))) != 0;
    }

    /**
     * Checks whether this Matcher might be fulfilled by features that
     * are stored in an index with the given key bits.
     *
     * @param keys      the key bits of the index
     * @return          `true` if features that match this Matcher might be found
     *                  in the given index, or `false` if none of those
     *                  features could be a match
     */
    public boolean acceptIndex(int keys)
    {
        return true;
    }

    /**
     * Checks whether relation members with the given role are accepted by this
     * Matcher. If so, returns a Matcher that can be applied to each individual
     * member with the same role.
     *
     * @param roleCode      the global-string code of the role; a negative value
     *                      indicates that the role is passed as `roleString`
     * @param roleString    the role (only valid if `roleCode` is negative)
     * @return  a `Matcher` to be applied to member features with the given role,
     *          or `null` if members with this role are not accepted.
     */
    public Matcher acceptRole(int roleCode, String roleString)
    {
        return this;
    }

    public static final Matcher ALL = new Matcher() {};
}
