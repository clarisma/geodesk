package com.clarisma.common.store;

import java.nio.file.Files;
import java.nio.file.Path;

public class BlobStoreDownloadTest
{
    public static void main(String[] args) throws Exception
    {
        Files.deleteIfExists(Path.of("c:\\geodesk\\empty.store"));
        BlobStore store = new BlobStoreTester("c:\\geodesk\\empty.store");
        store.setRepository("file:///c:\\geodesk\\tests\\midi-saved");
        store.open();
        Thread.sleep(5_000);
        store.close();
    }
}
