package com.geodesk.feature.store;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class Tip
{
    public static String toString(int tip)
    {
        return String.format("%06X", tip);
    }

    public static Path folder(Path rootPath, int tip)
    {
        return rootPath.resolve(String.format("%03X", tip >>> 12));
    }

    public static Path path(Path root, int tip, String suffix)
    {
        return folder(root, tip).resolve(String.format("%03X%s", tip & 0xfff, suffix));
    }
}
