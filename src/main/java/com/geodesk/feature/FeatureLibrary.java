/*
 * Copyright (c) Clarisma / GeoDesk contributors
 *
 * This source code is licensed under the Apache 2.0 license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.geodesk.feature;

import com.geodesk.feature.store.FeatureStore;
import com.geodesk.feature.query.WorldView;
import org.locationtech.jts.geom.GeometryFactory;

import java.nio.file.Path;
import java.nio.file.Paths;

/// A Geographic Object Library containing features.
///
public class FeatureLibrary extends WorldView implements AutoCloseable
{
    /// Creates a `FeatureLibrary` instance associated with an existing GOL file.
    ///
    /// @param path the path of the GOL file
    ///
    /// @deprecated Use {@link Features#open(String)} instead.
    ///
    @Deprecated(since = "2.0")
    public FeatureLibrary(String path)
    {
        this(Paths.get(path));
    }

    /// Creates a `FeatureLibrary` instance associated with an existing GOL file.
    ///
    /// @param path the path of the GOL file
    ///
    /// @deprecated Use {@link Features#open(Path)} instead.
    ///
    @Deprecated(since = "2.0")
    public FeatureLibrary(Path path)
    {
        super(new FeatureStore(path));
    }

    public GeometryFactory geometryFactory()
    {
        return store.geometryFactory();
    }


    /// Closes the library and releases its resources.
    ///
    /// **Important**: Do not call the methods of any collections
    /// or features you have retrieved from this library after
    /// you've closed it. Doing so leads to undefined results and
    /// may cause a segmentation fault.
    ///
    public void close()
    {
        store.close();
    }

    // TODO: remove from public API
    /// @hidden
    public FeatureStore store()
    {
        return store;
    }
}
