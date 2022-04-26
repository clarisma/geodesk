package com.geodesk.feature;

import com.geodesk.geom.Bounds;

import java.util.Iterator;

/**
 * A Geographic Object Library containing features.
 */
public class FeatureLibrary implements Features<Feature>
{
    /**
     * Creates a `FeatureLibrary` instance associated with an existing GOL file.
     *
     * @param path the path of the GOL file.
     */
    public FeatureLibrary(String path)
    {
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
    }

    public void close()
    {
    }

    @Override public Features<?> features(String query)
    {
        return null;
    }

    @Override public Features<Node> nodes()
    {
        return null;
    }

    @Override public Features<Node> nodes(String query)
    {
        return null;
    }

    @Override public Features<Way> ways()
    {
        return null;
    }

    @Override public Features<Way> ways(String query)
    {
        return null;
    }

    @Override public Features<Relation> relations()
    {
        return null;
    }

    @Override public Features<Relation> relations(String query)
    {
        return null;
    }

    @Override public Features<?> in(Bounds bbox)
    {
        return null;
    }

    @Override public Iterator<Feature> iterator()
    {
        return null;
    }
}
