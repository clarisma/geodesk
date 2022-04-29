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

    /**
     * Closes the library and releases its resources. **Important**: Do not call
     * the methods of any collections or features you have retrieved from this
     * library after you've closed it. Doing so leads to undefined results and
     * may cause a segmentation fault.
     */
    public void close()
    {
        store.close();
    }
}
