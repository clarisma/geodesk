package com.geodesk.feature;

import com.geodesk.geom.Bounds;
import org.locationtech.jts.geom.Geometry;

/**
 * An interface for classes that select the features to be returned by a query.
 */
public interface Filter
{
    /**
     * Checks whether the given feature should be included in the query results.
     *
     * @param feature   the feature to check
     * @return          `true` if this feature should be included in the results
     */
    default boolean accept(Feature feature)
    {
        return true;
    }

    default boolean acceptGeometry(Geometry geom)
    {
        return true;
    }

    default Bounds bounds() { return null; }
}
