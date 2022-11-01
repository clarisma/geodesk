/*
 * Copyright (c) Clarisma / GeoDesk contributors
 *
 * This source code is licensed under the Apache 2.0 license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.geodesk.feature;

import java.util.Collection;
import java.util.Set;

/**
 * A {@link Feature} that represents a grouping of related features or a
 * complex polygon.
 */
public interface Relation extends Feature, Iterable<Feature>
{
	/**
	 * Returns the members of this `Relation`.
	 *
	 * @return a collection of features that belong to this relation,
	 *   or an empty collection if this relation has no members
	 */
	Features<?> members();

	/**
	 * Returns the members of this `Relation` that match the given query.
	 *
	 * @param  q  a query in <a href="/goql">GOQL</a> format
	 *
	 * @return a collection of member features that match the given query
	 *   (may be empty)
	 */
	Features<?> members(String q);

	/**
	 * Returns the nodes that are members of this `Relation`.
	 *
	 * @return a collection of nodes that belong to this relation,
	 * 	       or an empty collection if this relation has no nodes as members
	 */
	Features<Node> memberNodes();

	/**
	 * Returns the nodes of this `Relation` that match the given query.
	 *
	 * @param  q  a query in <a href="/goql">GOQL</a> format
	 *
	 * @return a collection of member nodes that match the given query
	 *   (may be empty)
	 */
	Features<Node> memberNodes(String q);

	/**
	 * Returns the ways that are members of this `Relation`.
	 *
	 * @return a collection of ways that belong to this relation,
	 * 	       or an empty collection if this relation has no ways as members
	 */
	Features<Way> memberWays();

	/**
	 * Returns the ways of this `Relation` that match the given query.
	 *
	 * @param  q  a query in <a href="/goql">GOQL</a> format
	 *
	 * @return a collection of member ways that match the given query
	 *   (may be empty)
	 */
	Features<Way> memberWays(String q);

	/**
	 * Returns the relations that are members of this `Relation`.
	 *
	 * @return a collection of relations that belong to this relation,
	 * 	       or an empty collection if this relation has no sub-relations as members
	 */
	Features<Relation> memberRelations();

	/**
	 * Returns the sub-relations of this `Relation` that match the given query.
	 *
	 * @param  q  a query in <a href="/goql">GOQL</a> format
	 *
	 * @return a collection of sub-relations that match the given query
	 *   (may be empty)
	 */
	Features<Relation> memberRelations(String q);

	/**
	 * Returns the roles that are assigned to members of this `Relation`.
	 *
	 * @return a set of strings of the roles used in this relation,
	 *         or an empty set if the relations has no members, or none
	 *         have been assigned a role
	 */
	Set<String> memberRoles();
	/*
	Features<Feature> membersWithRole(String role);
	Features<Node> nodesWithRole(String role);
	Features<Way> waysWithRole(String role);
	Features<Relation> relationsWithRole(String role);
	 */
	// boolean hasMember(Feature f)
	// boolean hasMember(FeatureType type, long id)
	// Set<String> memberRoles();
}
