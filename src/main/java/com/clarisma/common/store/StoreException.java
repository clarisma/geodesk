package com.clarisma.common.store;

import java.nio.file.Path;

public class StoreException extends RuntimeException
{
    private Path path;

    public StoreException(String msg, Path path)
    {
        this(msg, path, null);
    }

    public StoreException(String msg, Throwable cause)
    {
        super(msg, cause);
    }

    public StoreException(String msg, Path path, Throwable cause)
    {
        super(String.format("%s: %s", path, msg), cause);
        this.path = path;
    }
}
