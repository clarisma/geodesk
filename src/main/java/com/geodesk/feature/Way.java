package com.geodesk.feature;

/**
 * A {@link Feature} that represents a linestring, linear ring, or a simple
 * polygon.
 */
public interface Way extends Feature, Iterable<Node>
{
	// TODO: this should return an iterator
	//  The iterator should provide toXY, toLatLon, toSequence, etc.

	/**
	 * Returns the way's coordinates as an array of integers. X coordinates are
	 * stored at even index positions, Y at odd.
	 *
	 * @return an array of coordinate pairs
	 */
	int[] toXY();

	/**
	 * Returns the way's nodes.
	 *
	 * @return an ordered collection of {@link Node} objects
	 */
	Features<Node> nodes();

	// TODO: needed? Could use nodes().contains(node)
	// boolean hasNode(Node node);

	// TODO: provide a segments() iterator?
}
