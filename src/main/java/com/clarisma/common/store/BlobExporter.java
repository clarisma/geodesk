package com.clarisma.common.store;

import com.clarisma.common.util.Bytes;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.*;
import java.util.zip.CRC32;
import java.util.zip.DeflaterOutputStream;

import static com.clarisma.common.store.BlobStoreConstants.*;

public class BlobExporter<T extends BlobStore>
{
    protected final T store;

    public BlobExporter(T store)
    {
        this.store = store;
    }

    protected void resetMetadata(ByteBuffer buf)
    {
        // reset the free tables
        buf.putInt(TRUNK_FT_RANGE_BITS_OFS, 0);
        for (int i=0; i<512; i++) buf.putInt(TRUNK_FREE_TABLE_OFS+i, 0);

        // reset the file size
        buf.putInt(TOTAL_PAGES_OFS, 0);
    }

    protected void export(Path path, /* int id, */ int page) throws IOException
    {
        final ByteBuffer buf;
        int p;
        int len;
        if(page == 0)
        {
            len = store.baseMapping.getInt(METADATA_SIZE_OFS);
            buf = ByteBuffer.allocateDirect(len);
            buf.put(0, store.baseMapping, 0, len);
            resetMetadata(buf);
            p = 0;
            // for metadata, include all
        }
        else
        {
            buf = store.bufferOfPage(page);
            p = store.offsetOfPage(page);
            len = buf.getInt(p) & 0x3fff_ffff;

            // TODO: assumes word at pos 4 is checksum, not included in exported payload
            //  len includes length of checksum itself

            p += 8;
            len -= 4;       // skip not only the header word, but also checksum word
        }
        export(path, /* id, */ buf, p, len);

    }

    private void export(Path path, /* int id, */
        ByteBuffer buf, int p, int len) throws IOException
    {
        Path tempPath = Paths.get(path.toString() + ".tmp");
        FileOutputStream fout = new FileOutputStream(tempPath.toFile());
        final int BUF_SIZE = 64 * 1024;
        final byte[] b = new byte[BUF_SIZE];
        int bytesRemaining = len;

        // Write header
        Bytes.putInt(b, 0, EXPORTED_MAGIC);
        Bytes.putInt(b, 4, VERSION);
        // TODO: GUID
        // Bytes.putInt(b, 8, id); // TODO
        Bytes.putInt(b, EXPORTED_ORIGINAL_LEN_OFS, len);
        fout.write(b, 0, EXPORTED_HEADER_LEN);

        CRC32 checksum = new CRC32();
        DeflaterOutputStream out = new DeflaterOutputStream(fout);
        while(bytesRemaining > 0)
        {
            int chunkSize = Integer.min(bytesRemaining, BUF_SIZE);
            buf.get(p, b, 0, chunkSize);
            out.write(b, 0, chunkSize);
            checksum.update(b, 0, chunkSize);
            bytesRemaining -= chunkSize;
            p += chunkSize;
        }

        out.finish();       // TODO: needed?
        // out.close();
            // Don't close ZIP stream; on Linux, this closes
            // the underlying FileOutputStream as well

        // write checksum at end
        Bytes.putInt(b,0, (int)checksum.getValue());
        fout.write(b,0,4);
        fout.close();

        // TODO: sync needed?

        Files.move(tempPath, path, StandardCopyOption.REPLACE_EXISTING);
    }
}
