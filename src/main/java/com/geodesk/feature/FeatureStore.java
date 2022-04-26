package com.geodesk.feature;

import com.geodesk.feature.store.FeatureStoreBase;

import java.nio.file.Path;

public class FeatureStore extends FeatureStoreBase
{
    public FeatureStore(Path path)
    {
        setPath(path);
        open();
        enableQueries();  // TODO: re-enable
    }
}
