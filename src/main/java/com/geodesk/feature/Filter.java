package com.geodesk.feature;

import org.locationtech.jts.geom.Geometry;

import java.nio.ByteBuffer;

public interface Filter
{
    /**
     * Checks whether a feature meets the conditions of this Filter.
     *
     * @param buf       the Buffer of the feature
     * @param pos       the anchor position of the feature in the Buffer
     * @return          `true` if the feature matches the filter condition
     */
    default boolean accept(ByteBuffer buf, int pos)
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
    default boolean acceptTyped(int types, ByteBuffer buf, int pos)
    {
        return true;
    }

    /**
     * Checks whether this Filter might be fulfilled by features that
     * are stored in an index with the given key bits.
     *
     * @param keys      the key bits of the index
     * @return          `true` if features that match this Filter might be found
     *                  in the given index, or `false` if none of those
     *                  features could be a match
     */
    default boolean acceptIndex(int keys)
    {
        return true;
    }

    default boolean acceptGeometry(Geometry geom)
    {
        return true;
    }

    Filter ALL = new Filter() {};
}
