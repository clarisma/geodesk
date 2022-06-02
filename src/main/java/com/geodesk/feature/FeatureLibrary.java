package com.geodesk.feature;

import com.geodesk.feature.store.FeatureStore;
import com.geodesk.feature.query.WorldView;
import org.locationtech.jts.geom.GeometryFactory;

import java.nio.file.Path;

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
        this(path, null);
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
        super(new FeatureStore());
        store.setPath(Path.of(path));
        if(url != null) store.setRepository(url);
        store.open();
        store.enableQueries();
    }

    public GeometryFactory geometryFactory()
    {
        return store.geometryFactory();
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

    // TODO: remove from public API
    public FeatureStore store()
    {
        return store;
    }
}
