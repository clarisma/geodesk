package com.geodesk.feature;

import com.geodesk.feature.store.FeatureStoreBase;
import com.geodesk.feature.store.WorldView;
import com.geodesk.geom.Bounds;

import java.nio.file.Path;
import java.util.Iterator;

/**
 * A Geographic Object Library containing features.
 */
public class FeatureLibrary extends WorldView<Feature>
{
    /**
     * Creates a `FeatureLibrary` instance associated with an existing GOL file.
     *
     * @param path the path of the GOL file.
     */
    public FeatureLibrary(String path)
    {
        super(new FeatureStoreBase());
        store.setPath(Path.of(path));
        store.open();
        store.enableQueries();
    }

    /**
     * Creates a `FeatureLibrary` instance associated with the given GOL file;
     * if the file does not exist, an empty library will be created.
     *
     * @param path the path of the GOL file
     * @param url  the URL from which missing tiles are downloaded into the library
     */
    public FeatureLibrary(String path, String url)
    {
        this(path);
        // TODO: set repository URL
    }

    public void close()
    {
        store.close();
    }
}
