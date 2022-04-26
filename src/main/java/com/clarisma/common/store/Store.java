package com.clarisma.common.store;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.collections.api.iterator.IntIterator;
import org.eclipse.collections.api.map.primitive.MutableLongObjectMap;
import org.eclipse.collections.api.set.primitive.IntSet;
import org.eclipse.collections.api.set.primitive.MutableIntSet;
import org.eclipse.collections.impl.map.mutable.primitive.LongObjectHashMap;
import org.eclipse.collections.impl.set.mutable.primitive.IntHashSet;

import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.CRC32;

import static java.nio.file.StandardOpenOption.*;
import static java.nio.file.StandardOpenOption.WRITE;

// TODO: Be aware that file locks are process-wide and don't prevent
//  multiple threads from opening the same file
//  Only one instance of a Store should exist in any given process

// TODO: Cannot safely mix "free blob" and "download" ops in one xaction
//  If we write to a segment that is not tracked by the journal, we need
//  to make sure this segment is forced as well

// TODO: must match up journal to store, file name is not enough
//  If we delete the store and re-create it, we risk having it corrupted
//  if there is a journal. Use store creation timestamp, stamp journal
//  with it, remove journal if mismatch

// TODO: At what level should we synchronize access?

public abstract class Store
{
    protected static final Logger log = LogManager.getLogger();

    private static final Set<String> openStores = new HashSet<>();
    private Path path;
    private FileChannel channel;
    private RandomAccessFile journal;
    private int lockLevel;
    private FileLock lockRead;
    private FileLock lockWrite;
    // TODO: call them segments / rootSegment? (or buffers)
    private volatile MappedByteBuffer[] mappings = new MappedByteBuffer[0];
    protected MappedByteBuffer baseMapping;
    private final Object mappingsLock = new Object();
    private int transactionState;
    private MutableLongObjectMap<TransactionBlock> transactionBlocks;

    protected static final int MAPPING_SIZE = 1 << 30;

    protected static final int TRANSACTION_EXCLUSIVE = 1;
    protected static final int TRANSACTION_RETRY = 2;
    private   static final int TRANSACTION_OPEN = 4;
    private   static final int TRANSACTION_SAVED = 8;

    protected final static int LOCK_NONE = 0;
    protected final static int LOCK_READ = 1;
    protected final static int LOCK_APPEND = 2;
    protected final static int LOCK_EXCLUSIVE = 3;

    private static class TransactionBlock
    {
        long pos;
        MappedByteBuffer original;
        ByteBuffer current;
    }

    public Path path()
    {
        return path;
    }

    public void setPath(Path path)
    {
        if(channel != null) throw new StoreException("Store is already open", path);
        this.path = path;
    }

    protected MappedByteBuffer getMapping(int n)
    {
        MappedByteBuffer[] a = mappings;
        if(n >= a.length)
        {
            synchronized (mappingsLock)
            {
                a = mappings;
                int len = a.length;
                if(n >= len)
                {
                    a = Arrays.copyOf(a, n+1);
                    for(int i=len; i<=n; i++)
                    {
                        try
                        {
                            MappedByteBuffer buf = channel.map(
                                FileChannel.MapMode.READ_WRITE,
                                (long)i * MAPPING_SIZE, MAPPING_SIZE);
                            buf.order(ByteOrder.LITTLE_ENDIAN);		// TODO: check!
                            // TODO: better: make it configurable
                            a[i] = buf;
                        }
                        catch(IOException ex)
                        {
                            throw new StoreException(
                                String.format("%s: Failed to map segment at %X (%s)",
                                    path, (long)i * MAPPING_SIZE, ex.getMessage()), ex);
                        }
                    }
                    mappings = a;
                }
            }
        }
        return a[n];
    }

    private boolean unmapSegments()
    {
        synchronized (mappingsLock)
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
                    clean.invoke(theUnsafe, a[i]);
                }
                mappings = new MappedByteBuffer[0];
                return true;
            }
            catch (Exception ex)
            {
                return false;
            }
        }
    }

    protected int lock(int newLevel) throws IOException
    {
        int oldLevel = lockLevel;
        if(newLevel != oldLevel)
        {
            if (lockLevel == LOCK_EXCLUSIVE || newLevel == LOCK_NONE)
            {
                lockRead.release();
                lockRead = null;
                lockLevel = LOCK_NONE;
            }
            if (lockLevel == LOCK_NONE && newLevel != LOCK_NONE)
            {
                lockRead = channel.lock(0, 4, newLevel != LOCK_EXCLUSIVE);
            }
            if (oldLevel == LOCK_APPEND)
            {
                lockWrite.release();
                lockWrite = null;
            }
            if (newLevel == LOCK_APPEND)
            {
                lockWrite = channel.lock(4, 4, true);
            }
            lockLevel = newLevel;
        }
        return oldLevel;
    }

    protected boolean tryExclusiveLock() throws IOException
    {
        assert lockLevel == LOCK_NONE;
        lockRead = channel.tryLock(0, 4, false);
        if(lockRead == null) return false;
        lockLevel = LOCK_EXCLUSIVE;
        return true;
    }

    protected abstract void createStore();

    protected abstract void verifyHeader();

    protected void initialize()
    {
        // by default, do nothing
    }

    protected File getJournalFile()
    {
        return new File(path.toString() + "-journal");
    }

    public void open()
    {
        open(LOCK_READ);
    }

    public void openExclusive()
    {
        open(LOCK_EXCLUSIVE);
    }

    protected void open(int lockMode)
    {
        if(channel != null)
        {
            throw new StoreException("Store is already open", path);
            // TODO: make it no-op instead?
        }
        String fileName = path.toString();   // TODO: normalize
        synchronized (openStores)
        {
            if(openStores.contains(fileName))
            {
                throw new StoreException(
                    "Only one instance may be open within the same process", path);
            }
            openStores.add(fileName);
        }
        try
        {
            if (!Files.exists(path))
            {
                channel = (FileChannel) Files.newByteChannel(
                    path, CREATE_NEW, READ, WRITE, SPARSE);
            }
            else
            {
                channel = FileChannel.open(path, READ, WRITE);
            }
            lock(lockMode);

            File journalFile = getJournalFile();
            if (journalFile.exists())
            {
                processJournal(journalFile);
            }
            baseMapping = getMapping(0);
            int headerWord = baseMapping.getInt(0);
            if (headerWord == 0)
            {
                createStore();
            }
            else
            {
                verifyHeader();
            }
            initialize();
        }
        catch(IOException ex)
        {
            // TODO: make more robust
            close();
            throw new StoreException("Failed to open store", path, ex);
        }
    }

    private static void intToBytes(byte[] ba, int v)
    {
        ba[0] = (byte)v;
        ba[1] = (byte)(v >>> 8);
        ba[2] = (byte)(v >>> 16);
        ba[3] = (byte)(v >>> 24);
    }

    private boolean verifyJournal() throws IOException
    {
        byte[] ba = new byte[4];
        CRC32 crc = new CRC32();
        try
        {
            journal.seek(4);
            for (; ; )
            {
                int patchLow = journal.readInt();
                int patchHigh = journal.readInt();
                if (patchHigh == 0xffff_ffff && patchLow == 0xffff_ffff) break;
                int len = (patchLow & 0x3ff) + 1;
                intToBytes(ba, patchLow);
                crc.update(ba);
                intToBytes(ba, patchHigh);
                crc.update(ba);
                for (int i = 0; i < len; i++)
                {
                    intToBytes(ba, journal.readInt());
                    crc.update(ba);
                }
            }
            return journal.readInt() == (int) crc.getValue();
        }
        catch (EOFException ex)
        {
            return false;
        }
    }

    private void applyJournal() throws IOException
    {
        MutableIntSet affectedSegments = new IntHashSet();

        journal.seek(4);
        for (; ; )
        {
            int patchLow = journal.readInt();
            int patchHigh = journal.readInt();
            if (patchHigh == 0xffff_ffff && patchLow == 0xffff_ffff) break;
            long pos = ((long)patchHigh << 32) | ((long)patchLow & 0xffff_ffffL);
            pos = (pos >> 10) << 2;
            int len = (patchLow & 0x3ff) + 1;
            int segmentNumber = (int)(pos >> 30);
            int ofs = (int)pos & 0x3fff_ffff;
            MappedByteBuffer buf = getMapping(segmentNumber);
            affectedSegments.add(segmentNumber);
            for (int i = 0; i < len; i++)
            {
                int v = journal.readInt();
                buf.putInt(ofs, v);
                ofs += 4;
            }
        }
        syncSegments(affectedSegments);
    }

    private void syncSegments(IntSet affectedSegments)
    {
        IntIterator iter = affectedSegments.intIterator();
        while(iter.hasNext())
        {
            getMapping(iter.next()).force();
        }
    }

    private void openJournal(File journalFile) throws IOException
    {
        assert journal == null;
        journal = new RandomAccessFile(journalFile, "rw");
    }

    // TODO: store fingerprint in journal to guard against mismatch
    //  between store and journal (e.g. process writing to store crashes,
    //  user deletes the store and re-creates it, but leaves old journal
    //  behind)
    protected boolean processJournal(File journalFile) throws IOException
    {
        if(journal == null) openJournal(journalFile);
        journal.seek(0);
        int instruction = journal.readInt();
        if(instruction == 0) return false;

        // Even though we may be making modifications other than additions,
        // we only need to obtain the append lock: If another process died
        // while making additions, then exclusive read access isn't necessary.
        // If another process made modifications that did not complete
        // normally, it would have had to hold exclusive read access -- this
        // means that if we are here, we have been waiting to open the file,
        // so we are the first to see the journal instructions.

        int prevLockLevel = lock(LOCK_APPEND);

        // Check header again, because another process may have already
        // processed the journal while we were waiting for the lock

        journal.seek(0);
        instruction = journal.readInt();
        if(instruction == 0) return false;

        boolean appliedJournal = false;
        if(verifyJournal())
        {
            applyJournal();
            appliedJournal = true;
        }
        clearJournal();
        lock(prevLockLevel);
        return appliedJournal;
    }

    private void clearJournal() throws IOException
    {
        journal.seek(0);
        journal.writeInt(0);
        journal.setLength(4);
        journal.getChannel().force(false);
    }

    protected abstract long getTrueSize();

    // TODO: do not call close() if any threads are still accessing buffers
    // implement a cleanup method?

    public void close()
    {
        if(channel == null) return; // TODO: throw instead?

        try
        {
            long trueSize = getTrueSize();
            boolean journalPresent = false;
            File journalFile = getJournalFile();
            if (journal != null)
            {
                journalPresent = true;
            }
            if (!journalPresent)
            {
                journalPresent = journalFile.exists();
            }

            lock(LOCK_NONE);
            boolean segmentUnmapAttempted = false;

            if (journalPresent || trueSize > 0)
            {
                if (tryExclusiveLock())
                {
                    if (journalPresent)
                    {
                        if (processJournal(journalFile))
                        {
                            // Get true file size again, because it may have
                            // changed after journal instructions were processed

                            trueSize = getTrueSize();
                        }
                        if (journal != null)
                        {
                            journal.close();
                            journal = null;
                        }
                        journalFile.delete();
                    }
                    if (trueSize > 0)
                    {
                        segmentUnmapAttempted = true;
                        if(unmapSegments())
                        {
                            channel.truncate(trueSize);
                        }
                    }
                    lock(LOCK_NONE);
                }
            }
            if(!segmentUnmapAttempted) unmapSegments();
            channel.close();
        }
        catch(IOException ex)
        {
            throw new StoreException("Error while closing file", path, ex);
        }
        finally
        {
            channel = null;
            lockLevel = LOCK_NONE;
            lockRead = null;
            lockWrite = null;
            mappings = null;
            synchronized (openStores)
            {
                openStores.remove(path.toString());
            }
        }
    }

    protected void beginTransaction()
    {
        transactionBlocks = new LongObjectHashMap<>();

        // TODO: lock

        // TODO: Check for presence of journal after acquiring the lock,
        //  because another process may have crashed and left the Store
        //  in an inconsistent state. In this case, we need to apply
        //  the journal
    }

    protected boolean isInTransaction()
    {
        return transactionBlocks != null;
    }

    protected void commit()
    {
        assert isInTransaction();
        try
        {
            saveJournal();

            MutableIntSet affectedSegments = new IntHashSet();
            for(TransactionBlock block: transactionBlocks.values())
            {
                int segmentNumber = (int)(block.pos >> 30);
                int ofs = (int)block.pos & 0x3fff_ffff;
                block.original.put(ofs, block.current.array());
                affectedSegments.add(segmentNumber);
            }
            syncSegments(affectedSegments);

            log.debug("Committed {} blocks in {} segments",
                transactionBlocks.size(), affectedSegments.size());

            clearJournal();
            transactionBlocks = null;

            // TODO: unlock

        }
        catch(IOException ex)
        {
            throw new StoreException("Commit failed.", path, ex);
        }
    }

    protected ByteBuffer getBlock(long pos)
    {
        assert (pos & 0xfff) == 0: String.format(
            "%d: Block must start at 4KB-aligned position", pos);
        if(transactionBlocks != null)
        {
            TransactionBlock block = transactionBlocks.get(pos);
            if(block == null)
            {
                block = new TransactionBlock();
                block.pos = pos;
                block.original = getMapping((int)(pos >> 30));
                int ofs = (int)pos & 0x3fff_ffff;
                byte[] copy = new byte[4096];
                block.original.get(ofs, copy);
                block.current = ByteBuffer.wrap(copy);
                block.current.order(block.original.order());    // TODO
                transactionBlocks.put(pos, block);
            }
            return block.current;
        }
        ByteBuffer buf = getMapping((int)(pos >> 30));
        ByteOrder order = buf.order();
        buf = buf.slice((int)pos & 0x3fff_ffff, 4096);
        buf.order(order);
        return buf;
    }

    protected void saveJournal() throws IOException
    {
        if(journal == null) openJournal(getJournalFile());
        journal.seek(0);
        journal.writeInt(1);    // TODO
        byte[] ba = new byte[4];
        CRC32 crc = new CRC32();
        for(TransactionBlock block: transactionBlocks)
        {
            int pCurrent = 0;
            ByteBuffer original = block.original;
            ByteBuffer current = block.current;
            int originalOfs = (int)(block.pos & 0x3fff_ffff);
            int pOriginal = originalOfs;
            for(;;)
            {
                int oldValue = original.getInt(pOriginal);
                int newValue = current.getInt(pCurrent);
                if(oldValue != newValue)
                {
                    int pCurrentStart = pCurrent;
                    for(;;)
                    {
                        pCurrent += 4;
                        pOriginal += 4;
                        if (pCurrent == 4096) break;
                        oldValue = original.getInt(pOriginal);
                        newValue = current.getInt(pCurrent);
                        if(oldValue == newValue) break;
                    }
                    long pos = (block.pos + pCurrentStart) << 8;
                    int len = (pCurrent - pCurrentStart) / 4;
                    int patchLow = (int)pos | (len - 1);
                    int patchHigh = (int)(pos >>> 32);
                    journal.writeInt(patchLow);
                    journal.writeInt(patchHigh);
                    intToBytes(ba, patchLow);
                    crc.update(ba);
                    intToBytes(ba, patchHigh);
                    crc.update(ba);

                    ByteBuffer buf = original;
                    int pEnd = pCurrent;
                    int p = pCurrentStart;
                    p += originalOfs;
                    pEnd += originalOfs;
                    for(;p<pEnd;p+=4)
                    {
                        int v = original.getInt(p);
                        journal.writeInt(v);
                        intToBytes(ba, v);
                        crc.update(ba);
                    }
                }
                pCurrent += 4;
                if(pCurrent >= 4096) break;
                pOriginal += 4;
            }
        }
        journal.writeInt(0xffff_ffff);
        journal.writeInt(0xffff_ffff);
        journal.writeInt((int)crc.getValue());
        journal.getChannel().force(false);
    }

    // TODO: The verifier mechanism seems awkward
    //  We use this only to get around the access rules
    //  Need to make more Store methods public, use wrapper
    //  class as the actual public API

    public static abstract class Verifier<T extends Store>
    {
        public T store;

        public abstract boolean verify();
    }

    public boolean verify(Verifier verifier)
    {
        verifier.store = this;
        return verifier.verify();
    }
}
