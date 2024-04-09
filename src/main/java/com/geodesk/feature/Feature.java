/*
 * Copyright (c) Clarisma / GeoDesk contributors
 *
 * This source code is licensed under the Apache 2.0 license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.geodesk.feature;

import com.geodesk.geom.Mercator;
import com.geodesk.geom.Box;
import com.geodesk.feature.query.EmptyView;
import org.locationtech.jts.geom.Geometry;

/**
 * A geographic feature.
 */
public interface Feature extends Iterable<Feature>
{
	/**
	 * Returns the OSM ID of the feature. For nodes that have no tags and are
	 * not member of any relation, the ID may be 0.
	 * 
	 * @return the feature's OSM ID
	 */
	long id();

	/**
	 * Returns the feature's type.
	 *
	 * @return `NODE`, `WAY` or `RELATION`
	 */
	// TODO: Drop? instanceof does the job just fine!
	FeatureType type();

    /**
     * Checks if this Feature is an OSM node.
     */
    default boolean isNode()
    {
        return false;
    }

    /**
     * Checks if this Feature is an OSM way.
     */
    default boolean isWay()
    {
        return false;
    }

    /**
     * Checks if this Feature is an OSM relation.
     */
    default boolean isRelation()
    {
        return false;
    }

	// TODO: geometryType(): POINT, LINE, POLYGON, COLLECTION

	/**
	 * Returns the X coordinate of this feature. For a `Way` or `Relation`,
	 * this is the horizontal midpoint of its bounding box.
	 *
	 * @return X coordinate in Mercator projection
	 */
	int x();

	/**
	 * Returns the Y coordinate of this feature. For a `Way` or `Relation`,
	 * this is the vertical midpoint of its bounding box.
	 *
	 * @return Y coordinate in Mercator projection
	 */
	int y();

	/**
	 * Returns the longitude of this feature. For a {@link Way} or {@link Relation},
	 * this is the horizontal midpoint of its bounding box.
	 *
	 * @return longitude as degrees
	 */
	default double lon() { return Mercator.lonFromX(x()); }

	/**
	 * Returns the latitude of this feature. For a {@link Way} or {@link Relation},
	 * this is the vertical midpoint of its bounding box.
	 *
	 * @return longitude as degrees
	 */
	default double lat() { return Mercator.latFromY(y()); }

	/**
	 * Retrieves the bounding box of the feature.
	 * 
	 * @return a copy of the Feature's bounding box
	 */
	// TODO: maybe call this toBox?
	Box bounds();

  	/**
	 * Returns the way's coordinates as an array of integers. X coordinates are
	 * stored at even index positions, Y at odd.
	 *
	 * @return an array of coordinate pairs
	 */
	int[] toXY();

	/**
	 * Returns the tags of this feature.
	 *
	 * @return the feature's tags
	 */
	Tags tags();

	/**
	 * Returns the string value of the given key.
	 *
	 * @param key the tag's key
	 * @return the string value of the tag, or an empty string if this
	 *  feature does not have the requested tag
	 */
	String tag(String key);

	/**
	 * Checks whether this feature has a tag with the given key.
	 *
	 * @param key the key (e.g. <code>highway</code>)
	 * @return true if feature is tagged with this key, otherwise false
	 */
	// TODO: Does not check for "no"; what should be the expected behaviour?
	//  maybe: booleanValue()?
	boolean hasTag(String key);

	/**
	 * Checks whether this feature has a tag with the given key and value.
	 *
	 * @param key the key (e.g. <code>highway</code>)
	 * @param value the value (e.g. <code>residential</code>)
	 * @return true if the feature is tagged with this key/value, otherwise false
	 */
	boolean hasTag(String key, String value);

	/**
	 * Checks whether this feature is a member of the given Relation, or
	 * a node in the given Way.
	 *
	 * @param parent `Way` or `Relation`
	 * @return `true` if this Feature belongs to the given `Way` or `Relation`
	 * 				(always `false` if `parent` is a `Node`)
	 */
	boolean belongsTo(Feature parent);

	/**
	 * If this Feature was returned by a call to {@link Relation#members()}
	 * (or its variants) of a Relation, returns this Feature's role in
	 * that Relation.
	 * 
	 * @return the feature's role, or an empty String (if the feature is a
	 *   member without an assigned role), or `null` if it was obtained via
	 *   another kind of query
	 */
	String role();

	/**
	 * Returns the value of a tag as a String.
	 *
	 * @param key	the key of the tag
	 * @return		the tag's value, or an empty String if the tag
	 * 				does not exist
	 */
	String stringValue(String key);

	// TODO: what is the intValue of "120 mph" or "12a"?
	/**
	 * Returns the value of a tag as an integer.
	 *
	 * @param key	the key of the tag
	 * @return		the tag's value, or `0` if the tag does not exist
	 * 				or has a value that cannot be converted to an integer
	 */
	int intValue(String key);

	/**
	 * Returns the value of the given key as a double.
	 *
	 * @param key
	 * @return the key's value, or 0 if the key does not
	 * exist, or its value is not a valid number
	 */
	double doubleValue(String key);

	// TODO: call this isTagged(key) instead?
	boolean booleanValue(String key);

	// TODO
	// boolean hasKey(String k);
	// boolean isTagged(String k);
	// String value(String k);
	// String name(); 
	// String ref();
	// int layer();
	// String roleIn(Relation rel);
	// boolean belongsTo(Relation rel, String role);
	// boolean belongsToRelation(long id);

	/**
	 * Checks whether this Feature is a member of a Relation.
	 *
	 * @return `true` if this Feature belongs to at least one Relation
	 */
	boolean belongsToRelation();

	/**
	 * Checks whether this Feature represents an area. Areas are closed ways
	 * that have certain tags (e.g. <code>landuse</code>), or are explicitly
	 * tagged with <code>area=yes</code>; or relations that represent (multi-)
	 * polygons.
	 *
	 * @return true if this feature is an area, otherwise false
	 */
	boolean isArea();

	/**
	 * Checks whether this feature is a *placeholder*. A placeholder is a
	 * feature that is referenced by a relation, but is not actually
	 * present in a dataset.
	 *
	 * @return
	 */
	boolean isPlaceholder();

	/**
	 * Measures the length of a feature.
	 *
	 * @return length (in meters), or 0 if the feature is not lineal.
	 *
	 * TODO: should return circumference for areas
	 */
	default double length() { return 0; }

	/**
	 * Measures the area of a feature.
	 *
	 * @return area (in square meters), or 0 if the feature is not polygonal
	 */
	default double area() { return 0; }

	/**
	 * Creates a JTS {@link Geometry} object for this feature. The returned
	 * following types of geometries are created:
	 *
	 * - For a `Node`: {@link org.locationtech.jts.geom.Point}
	 *
	 * - For a non-closed `Way`: {@link org.locationtech.jts.geom.LineString}
	 *
	 * - For a closed `Way`: {@link org.locationtech.jts.geom.Polygon} if it
	 *   represents an area, otherwise {@link org.locationtech.jts.geom.LinearRing}
	 *
	 * - For a `Relation` that is an area: {@link org.locationtech.jts.geom.Polygon}
	 *
	 * - For a non-area `Relation`: {@link org.locationtech.jts.geom.GeometryCollection}
	 *
	 * @return a newly created Geometry.
	 */
	Geometry toGeometry();
	// String toGeoJson();
	// String toWkt();

    /**
	 * Returns the way's nodes.
	 *
	 * @return an ordered collection of {@link Feature} objects
	 */
	default Features nodes()
    {
        return EmptyView.ANY;
    }

    /**
	 * Returns the way's nodes that match the given query.
     *
     *  @param  query  a query in <a href="/goql">GOQL</a> format
     *
     *  @return an ordered collection of {@link Feature} objects
     *          that match the given query (may be empty)
	 */
	default Features nodes(String query)
    {
        return EmptyView.ANY;
    }

	/**
	 * Returns the members of this `Relation`.
	 *
	 * @return a collection of features that belong to this relation,
	 *   or an empty collection if this relation has no members
	 */
	default Features members()
	{
		return EmptyView.ANY;
	}

	/**
	 * Returns the members of this `Relation` that match the given query.
	 *
	 * @param  query  a query in <a href="/goql">GOQL</a> format
	 *
	 * @return a collection of member features that match the given query
	 *   (may be empty)
	 */
	default Features members(String query)
	{
		return EmptyView.ANY;
	}

   	/**
	 * Returns all ways and relations to which this Feature belongs.
	 *
	 * @return a collection of ways and/or relations (may be empty)
	 */
	Features parents();

    /**
	 * Returns all ways and relations to which this Feature belongs
     * that match the given query.
	 *
     * @param  query  a query in <a href="/goql">GOQL</a> format
     *
	 * @return a collection of ways and/or relations (may be empty)
	 */
	Features parents(String query);
}
