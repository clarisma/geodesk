package com.clarisma.common.store;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Path;
import java.util.Arrays;

import static java.nio.file.StandardOpenOption.READ;

public class FreeStore
{
    private Path path;
    private FileChannel channel;
    private volatile MappedByteBuffer[] mappings = new MappedByteBuffer[0];
    protected MappedByteBuffer baseMapping;
    private long fileSize;
    private final Object mappingsLock = new Object();
    private FileLock fileLock;
    private int pageSizeShift = 12; // 4KB default page


    protected static final int SEGMENT_SIZE = 1 << 30;
    private static final int ACTIVE_SNAPSHOT_OFS = 16;
    private static final int LOCK_OFS = 512;

    public FreeStore(Path path)
    {
        // TODO: What if store is already open?

        try
        {
            this.path = path;
            channel = FileChannel.open(path, READ);
            ByteBuffer buf = ByteBuffer.allocate(24)
                .order(ByteOrder.LITTLE_ENDIAN);
            for (; ; )
            {
                buf.clear();
                int pos = 0;
                while (buf.hasRemaining())
                {
                    int n = channel.read(buf, pos);
                    if (n < 0)
                    {
                        throw new StoreException("Invalid store", path);
                    }
                    pos += n;
                }
                int activeSnapshot = buf.get(ACTIVE_SNAPSHOT_OFS);
                fileLock = channel.tryLock(LOCK_OFS + activeSnapshot * 2, 1, true);
                if (fileLock == null)
                {
                    throw new StoreException("Store locked", path);
                }
                fileSize = channel.size();
                baseMapping = getMapping(0);

                // TODO: check if snapshot changed post lock acqusition
                // TODO: Check and apply Journal

                break;
            }
        }
        catch (IOException ex)
        {
            throw new StoreException("Failed to open store", path, ex);
        }
        initialize();
    }

    public void close()
    {
        if(channel == null) return; // TODO: throw instead?
        unmapSegments();
        try
        {
            channel.close();
        }
        catch(IOException ex)
        {
            // ignore
        }
    }


    // === File Mapping ===

    // maps segments lazily
    protected MappedByteBuffer getMapping(int n)
    {
        MappedByteBuffer[] a = mappings;
        MappedByteBuffer buf;
        if(n < a.length && (buf = a[n]) != null) return buf;

        synchronized (mappingsLock)
        {
            // Read array and perform check again
            a = mappings;
            int len = a.length;
            if(n >= len)
            {
                a = Arrays.copyOf(a, n + 1);
            }
            else
            {
                buf = a[n];
                if (buf != null) return buf;
                a = Arrays.copyOf(a, a.length);
            }
            try
            {
                // Log.debug("Mapping segment %d...", i);
                long mappingOfs = (long) n * SEGMENT_SIZE;
                long mappingSize = Math.min(fileSize - mappingOfs, SEGMENT_SIZE);
                buf = channel.map(FileChannel.MapMode.READ_ONLY,
                    mappingOfs, mappingSize);
            }
            catch(IOException ex)
            {
                throw new StoreException(
                    String.format("%s: Failed to map segment at %X (%s)",
                        path, (long)n * SEGMENT_SIZE, ex.getMessage()), ex);
            }

            buf.order(ByteOrder.LITTLE_ENDIAN);		// TODO: check!
            // TODO: better: make it configurable
            a[n] = buf;
            mappings = a;
            return buf;
        }
    }

    private boolean unmapSegments()
    {
        // Log.debug("unmapping segments");

        synchronized (mappingsLock)
        {
            boolean res = unmapSegments(mappings);
            mappings = new MappedByteBuffer[0];
            return res;
        }
    }

    /**
     * Deterministically unmaps one of more `MappedByteBuffer` objects.
     * By default, the JVM only unmaps buffers once the `MappedByteBuffer`
     * object is being garbage-collected. This causes problems in cases
     * where GC never runs and we perform an action on the underlying file
     * (such as truncating its size). This method uses `Unsafe` to explicitly
     * release the buffer.
     *
     * === WARNING ===
     *
     * - The required methods in Unsafe may be removed without notice in future
     *   version of the JDK.
     * - After a buffer has been unmapped, its contents must not be accessed --
     *   doing so will result in an abnormal process termination
     *
     * TODO: Move this outside of this class? Required as a stand-alone mwthod
     *  to fix clarisma/gol-tool#100
     *
     * @param mappings
     * @return  `true` if the mappings were successfully released
     */
    public static boolean unmapSegments(MappedByteBuffer[] mappings)
    {
        try
        {
            // See https://stackoverflow.com/a/19447758

            Class unsafeClass;
            try
            {
                unsafeClass = Class.forName("sun.misc.Unsafe");
            }
            catch (Exception ex)
            {
                // jdk.internal.misc.Unsafe doesn't yet have an invokeCleaner() method,
                // but that method should be added if sun.misc.Unsafe is removed.
                unsafeClass = Class.forName("jdk.internal.misc.Unsafe");
            }
            Method clean = unsafeClass.getMethod("invokeCleaner", ByteBuffer.class);
            clean.setAccessible(true);
            Field theUnsafeField = unsafeClass.getDeclaredField("theUnsafe");
            theUnsafeField.setAccessible(true);
            Object theUnsafe = theUnsafeField.get(null);

            MappedByteBuffer[] a = mappings;
            for (int i = 0; i < a.length; i++)
            {
                MappedByteBuffer buf = a[i];
                if(buf != null) clean.invoke(theUnsafe, buf);
                // TODO: set array entry to null just in case one of the
                //  cleaner invocations fails?
            }
            return true;
        }
        catch (Exception ex)
        {
            return false;
        }
    }

    protected void initialize()
    {
        // do nothing
    }


    public ByteBuffer bufferOfPage(int page)
    {
        return getMapping(page >> (30 - pageSizeShift));
    }

    public int offsetOfPage(int page)
    {
        return (page << pageSizeShift) & 0x3fff_ffff;
    }
}
