package com.geodesk.feature;

/**
 * A {@link Feature} that represents a single point.
 */
public interface Node extends Feature
{
    /**
     * Checks whether this `Node` belongs to a `Way`.
     *
     * @return `true` if this `Node` forms part of a `Way`, or `false` if it
     *   is a stand-alone node
     */
    boolean belongsToWay();

    /**
     * Returns all `Way`s to which this `Node` belongs.
     *
     * @return a collection of ways, or an empty collection if this a
     *   stand-alone node
     *
     */
    Features<Way> parentWays();
    // Features<Way> parentWays(String q);
}
