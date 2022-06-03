package com.clarisma.common.store;

import com.clarisma.common.util.Log;
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
import java.util.concurrent.locks.ReentrantLock;
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

/**
 * Base class for persistent data stores that supports transactions and
 * journaling. A Store is backed by a sparse file that is memory-mapped
 * in segments, 1 GB each.
 *
 * All updates to a Store are journaled, to prevent corruption of the Store's
 * contents in the event updates are only partially written due to abnormal
 * process termination or power loss.
 *
 * Stores are designed to be shared among processes. Multiple processes can
 * read existing data, while one process may add new data. Modifying existing
 * data requires exclusive access by one single process. Access is mediated
 * via file locks. File locks are treated as advisory. The distinction between
 * "new data" and "existing data" is left to the subclass.
 *
 * Adding and updating data must be done within transactions. Multiple threads
 * may read data, but only one thread is allowed to write. Note that the Store
 * base class does not enforce access rules, it only provides the building
 * blocks for transactional semantics and journaling. It is up to the concrete
 * subclasses to ensure proper concurrency.
 *
 * The Store base class imposes no format for the contents of the store,
 * except for the following:
 *
 * - Write access that requires journaling must be performed via 4-KB blocks
 *
 * - Contiguous chunks of data must not cross 1-GB segment boundaries
 *
 * - The first 8 bytes of the store file must be metadata, or which the first
 *   4 bytes must be the type indicator ("magic number") which cannot be zero
 *   (zero indicates an uninitialized file). The other 4 bytes typically
 *   store the version number of the file format.
 *
 * - The store file must record its creation time (as an 8-byte timestamp).
 *   (It is recommended not to rely on the filesystem-provided metadata,
 *   but to record this timestamp in the store file itself)
 *
 * - Store data assumes little-endian byte order
 *
 * Modifications to a Store follow this pattern:
 *
 * - beginTransaction()  -- either APPEND or EXCLUSIVE
 *
 * - One or more calls to getBlock() to obtain a 4-KB block at a specific
 *   address. This block is represented as a ByteBuffer, which can then
 *   be modified. When changes are committed, the original contents of these
 *   blocks are first written to the journal, so the store file can be restored
 *   to its original state if the transaction fails to complete. Once the
 *   journal has been safely stored on disk, the changes are written to the
 *   store file itself.
 *
 * - commit() or rollback() to apply or discard all changes since the last
 *   call to beginTransaction(), commit() or rollback()
 *
 * - endTransaction()
 *
 * For performance reasons, changes may be written directly into the data
 * store's buffers (without obtaining staging buffers via getBlock() ) but
 * only for sections of the store file that are impervious to undefined data
 * due to partially-performed writes (for example, the inner blocks of
 * a previously freed blob in a BlobStore, since these blocks by definition
 * contain garbage). However, at least one block in the same segment must
 * have been obtained via getBlock(), and its contents must be actually
 * modified, in order for the segment to be properly synched by a subsequent
 * call to commit().
 *
 * Data that is appended to the end of a file does not require journaling;
 * it can be written directly into the buffers (the above caveat regarding
 * synching does not apply)
 *
 * Journal File
 * ============
 *
 * The journal file has the following format:
 *
 * Action (4 bytes): Indicates what should happen when the store file is
 * opened and the journal file is present: 0 = do nothing, 1 = apply changes
 *
 * Timestamp (8 bytes): The creation timestamp of the data store. If there
 * is a mismatch between the timestamps of the store and the journal, the
 * journal is discarded (for example, the store was deleted and re-created,
 * but the journal of the old store file was left behind).
 *
 * Journaling Instruction (zero or more):
 *
 *     Offset and length (8 bytes): The top 54 bits contain the offset of the
 *     word (*not* the byte) where the first change should be written.
 *     The lower 10 bits contain the number of words to write (-1):
 *      e.g. 0x8ffc04 means "write 5 words starting at offset 0x23ff0"
 *      (0x8ffc * 4)
 *
 *     Value (4 bytes; one or more): The 4-byte values to write
 *
 * End Marker (8 bytes):   0xffff_ffff_ffff_ffff
 * Checksum (4 bytes):     A CRC32 calculated over the Journaling Instructions
 *
 * - Journaling instructions never cross block boundaries
 * - Each journaling instruction can encode up to 1024 words (one 4-KB block)
 *
 * File Recovery
 * =============
 *
 * If a process terminates abnormally while a Store Transaction is open,
 * it may leave behind a journal file. The next time the Store is opened,
 * the open() method checks for presence of a "hot" journal (a journal
 * that contains valid instructions) and resets the store file to the
 * state after the last successful call to commit(), or its the state
 * propor to the start of the transaction if commit() was not called
 * (or did not complete successfully).
 *
 * This ensures that users of the Store class will never find the store file
 * in an inconsistent state due to partially-executed writes.
 *
 * (A partially-written journal is discarded; commit() will never begin
 * writing journaled changes to the store file until the journal is
 * completely written to disk).
 *
 * TODO: byte order of journal
 */
// TODO: allow writing only within a transaction

// TODO: journal recovery may need exclusive lock
//  Process A opens append xaction, writes journal, appends tile,
//  enters tile into index, gets killed before synching and clearing the journal
//  Process B reads index, sees tile, reads tile
//  Process C opens transaction; transaction sees journal, starts to apply,
//   rolls back the new tile written by A while B is reading it = BAD

// TODO:
//  Synching must respect write order for Append to be safe:
//  Process A downloads a tile: allocs blob, downloads into new blob, finally
//    enters blob into the index. But commit() applies the writes in arbitrary
//    order! This means thread B could see the index entry before the tile
//    is written into the store = BAD
//  Possible solution: write blocks in the order they were obtained
//       - may be brittle
//    OR: make write order explicit
//    OR: always write metadata blocks last
//       but Store has no concept of metadata
//    OR: let subclass determine write order

// TODO: could add extra safety by also writing timestamp of journal,
//  CRC on this timestamp as well

public abstract class Store
{
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
    private final ReentrantLock transactionLock = new ReentrantLock();
    private MutableLongObjectMap<TransactionBlock> transactionBlocks;
    private long preTransactionFileSize;

    protected static final int MAPPING_SIZE = 1 << 30;

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

    // === Abstract Methods / Common Overrides ===

    /**
     * Creates the metadata for an empty store. Called by open() if the
     * file-type indicator is zero.
     */
    protected abstract void createStore();

    /**
     * Checks the file-type indicator and other metadata fields (e.g.
     * file format version). Called by open().
     *
     * @throws StoreException if the file that is being opened isn't suitable
     *      for use with this subclass of `Store`, or if its contents have
     *      other problems that prevent the store from being opened.
     */
    protected abstract void verifyHeader();

    protected void initialize() throws IOException
    {
        // by default, do nothing
    }

    /**
     * Gets the "real" size of the store file.
     * Memory-mapping causes the file to grow, so the file size returned
     * by the OS is typically larger than the actual space used by the file.
     *
     * @return file size in bytes
     */
    protected abstract long getTrueSize();

    /**
     * Gets the creation timestamp of the Store. Where this is stored is
     * at the discretion of subclasses; however, it should be recorded in the
     * file itself, as OS-provided metadata is not always reliable.
     *
     * @return  the creation timestamp (milliseconds since
     *          midnight, January 1, 1970 UTC)
     */
    protected abstract long getTimestamp();


    // === File Mapping ===

    // maps segments lazily
    // TODO: consider option to map in read-only mode
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
                buf = channel.map(
                    FileChannel.MapMode.READ_WRITE,
                    (long) n * MAPPING_SIZE, MAPPING_SIZE);
            }
            catch(IOException ex)
            {
                throw new StoreException(
                    String.format("%s: Failed to map segment at %X (%s)",
                        path, (long)n * MAPPING_SIZE, ex.getMessage()), ex);
            }

            buf.order(ByteOrder.LITTLE_ENDIAN);		// TODO: check!
            // TODO: better: make it configurable
            a[n] = buf;
            mappings = a;
            return buf;
        }
    }

    // old version
    /*
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
                            // Log.debug("Mapping segment %d...", i);
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
     */

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
                    MappedByteBuffer buf = a[i];
                    if(buf != null) clean.invoke(theUnsafe, buf);
                    // TODO: set array entry to null just in case one of the
                    //  cleaner invocations fails?
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

    /**
     * Locks (or unlocks) the store file. There are four lock levels:
     *
     * NONE:        this process may not access any data
     * READ:        this process and others may read existing data; one other
     *              process (but not this one) may add new data
     * APPEND:      this process may read existing data and add new data;
     *              other processes may only read existing data
     * EXCLUSIVE:   this process may read and modify any data; all other
     *              processes must wait
     *
     * The locking paradigm uses two file locks, covering two short regions
     * of the file: `lockRead` (bytes 0-3) and `lockWrite` (bytes 4-7).
     * The four lock levels use these file locks as follows:
     *
     *              lockRead    lockWrite
     * NONE:        ---         ---
     * READ:        shared      ---
     * APPEND:      shared      exclusive
     * EXCLUSIVE:   exclusive   ---
     *
     * (EXCLUSIVE does not need to hold lockWrite, because no other process
     * can obtain lockRead)
     *
     * This method blocks until the desired lock level can be obtained.
     *
     * If the requested lock level is already active, this method does nothing.
     *
     * @param newLevel      NONE, READ, APPEND or EXCLUSIVE
     * @return  the previous lock level
     *
     * @throws java.nio.channels.FileLockInterruptionException if the invoking
     *      thread was interrupted while waiting for a lock
     *
     * @throws IOException if an I/O error occurred
     */
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
                lockWrite = channel.lock(4, 4, false);
            }
            lockLevel = newLevel;
        }
        return oldLevel;
    }

    /**
     * Attempts to obtain an exclusive lock on the store file.
     * The store file must be unlocked.
     * If another process holds any lock on this Store, the
     * method returns immediately.
     *
     * @return  true if the exclusive lock was obtained, or false if
     *          another process holds a lock on this Store
     *
     * @throws IOException if an I/O error occurred
     */
    protected boolean tryExclusiveLock() throws IOException
    {
        assert lockLevel == LOCK_NONE;
        lockRead = channel.tryLock(0, 4, false);
        if(lockRead == null) return false;
        lockLevel = LOCK_EXCLUSIVE;
        return true;
    }


    public void open()
    {
        open(LOCK_READ);
    }

    // TODO: not needed, enter exclusive transaction instead?
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

            // Always do this first, even if journal is present
            baseMapping = getMapping(0);
            int headerWord = baseMapping.getInt(0);
            if (headerWord == 0)
            {
                createStore();
            }

            File journalFile = getJournalFile();
            if (journalFile.exists())
            {
                processJournal(journalFile);
            }
            verifyHeader();     // TODO: when to do this?
            // debugCheck();
            initialize();
        }
        catch(IOException ex)
        {
            // TODO: make more robust
            close();
            throw new StoreException("Failed to open store", path, ex);
        }
    }

    // TODO: use Bytes.putInt
    private static void intToBytes(byte[] ba, int v)
    {
        ba[0] = (byte)v;
        ba[1] = (byte)(v >>> 8);
        ba[2] = (byte)(v >>> 16);
        ba[3] = (byte)(v >>> 24);
    }

    // === Journaling ===

    protected File getJournalFile()
    {
        return new File(path.toString() + "-journal");
    }

    /**
     * Opens the journal file.
     *
     * @param journalFile   the journal file
     * @throws IOException
     */
    private void openJournal(File journalFile) throws IOException
    {
        assert journal == null;
        journal = new RandomAccessFile(journalFile, "rw");
    }

    // TODO: store fingerprint in journal to guard against mismatch
    //  between store and journal (e.g. process writing to store crashes,
    //  user deletes the store and re-creates it, but leaves old journal
    //  behind)
    // TODO: Use modification timestamp instead of fingerprint;
    //  fingerprint identifies content, timestamp & fingerprint
    //  identify content and its representation
    protected boolean processJournal(File journalFile) throws IOException
    {
        if(journal == null) openJournal(journalFile);
        journal.seek(0);
        int instruction = journal.readInt();
        if(instruction == 0) return false;
        // TODO: should we clear the journal anyway, in case
        //  journal was reset but not truncated?

        // Log.debug("Getting lock to apply journal...");

        // Even though we may be making modifications other than additions,
        // we only need to obtain the append lock: If another process died
        // while making additions, then exclusive read access isn't necessary.
        // If another process made modifications that did not complete
        // normally, it would have had to hold exclusive read access -- this
        // means that if we are here, we have been waiting to open the file,
        // so we are the first to see the journal instructions.

        int prevLockLevel = lock(LOCK_APPEND);  // TODO: need exclusive lock!

        // Check header again, because another process may have already
        // processed the journal while we were waiting for the lock

        journal.seek(0);
        instruction = journal.readInt();
        if(instruction == 0) return false;

        // Log.debug("Still need to apply journal...");

        boolean appliedJournal = false;
        if(verifyJournal())
        {
            // Log.debug("Journal is valid.");
            applyJournal();
            // debugCheck();
            appliedJournal = true;
        }
        else
        {
            // Log.debug("Journal is invalid.");
        }
        clearJournal();
        lock(prevLockLevel);
        return appliedJournal;
    }

    /*
    protected void debugCheck()
    {
    }
     */

    /**
     * Checks whether the Journal File is valid.
     *
     * The Journal File must be open prior to calling this method.
     * A Journal is "invalid" if:
     *
     * - Its timestamp does not match the Store's timestamp (This is a
     *   Journal that belonged to a previous store file with the same
     *   name); OR
     *
     * - It is missing its trailer, or its checksum is invalid
     *   (A previous process died before it could completely write the
     *   journal; since journaled changes are only applied to the store
     *   once the journal has been safely stored, we can ignore the
     *   journal in this case)
     *
     * @return `true` if the journal file is complete and valid
     *
     * @throws IOException
     */
    private boolean verifyJournal() throws IOException
    {
        byte[] ba = new byte[4];
        CRC32 crc = new CRC32();
        try
        {
            journal.seek(4);
            long timestamp = journal.readLong();
            if(timestamp != getTimestamp()) return false;
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

    /**
     * Applies the edit instructions from the journal to the data store,
     * and syncs the resulting changes to disk.
     *
     * The journal must be opened and verified prior to calling this method.
     *
     * @throws IOException
     */
    private void applyJournal() throws IOException
    {
        MutableIntSet affectedSegments = new IntHashSet();

        Log.debug("Applying journal...");

        int patchCount = 0;
        journal.seek(12);
        for (; ; )
        {
            int patchLow = journal.readInt();
            int patchHigh = journal.readInt();
            if (patchHigh == 0xffff_ffff && patchLow == 0xffff_ffff) break;
            long pos = ((long)patchHigh << 32) | ((long)patchLow & 0xffff_ffffL);
            pos = (pos >> 10) << 2; // TODO: careful of sign
            int len = (patchLow & 0x3ff) + 1;
            // Log.debug("Patching %d words at %X", len, pos);
            int segmentNumber = (int)(pos >> 30);
            int ofs = (int)pos & 0x3fff_ffff;
            MappedByteBuffer buf = getMapping(segmentNumber);
            affectedSegments.add(segmentNumber);
            for (int i = 0; i < len; i++)
            {
                int v = journal.readInt();
                // Log.debug("- %d (0x%X)", v, v);
                buf.putInt(ofs, v);
                ofs += 4;
                patchCount++;
            }
        }
        Log.debug("Syncing patches...");
        syncSegments(affectedSegments);
        Log.debug("Patched %d words in %d segments.", patchCount, affectedSegments.size());
    }

    /**
     * Resets the journal and flushes it to disk.
     *
     * @throws IOException
     */
    private void clearJournal() throws IOException
    {
        journal.seek(0);
        journal.writeInt(0);
        journal.setLength(4);   // TODO: just trim to 0 instead?
        // journal.getChannel().force(false);
        journal.getFD().sync();
    }

    /**
     * Writes the rollback instructions for the current transaction
     * into the Journal File.
     *
     * @throws IOException
     */
    private void saveJournal() throws IOException
    {
        if(journal == null) openJournal(getJournalFile());
        journal.seek(0);
        journal.writeInt(1);    // TODO
        journal.writeLong(getTimestamp());
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
                    assert (pos & 0x3ff) == 0; // lower 10 bits must be clear
                    int len = (pCurrent - pCurrentStart) / 4;
                    assert len > 0 && len <= 1024;
                    int patchLow = (int)pos | (len - 1);
                    int patchHigh = (int)(pos >>> 32);
                    journal.writeInt(patchLow);
                    journal.writeInt(patchHigh);
                    intToBytes(ba, patchLow);
                    crc.update(ba);
                    intToBytes(ba, patchHigh);
                    crc.update(ba);
                    // Log.debug("Journaling %d words at %X: ", len, pos >>> 8);

                    // ByteBuffer buf = original;
                    int pEnd = pCurrent;
                    int p = pCurrentStart;
                    p += originalOfs;
                    pEnd += originalOfs;
                    for(;p<pEnd;p+=4)
                    {
                        int v = original.getInt(p);
                        journal.writeInt(v);
                        // int newVal = current.getInt(p - originalOfs);
                        // Log.debug("- %d (0x%X) -- New value: %d (0x%X)", v, v, newVal, newVal);
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
        // journal.getChannel().force(false);
        journal.getFD().sync();
    }

    // === Transactions ===

    /**
     * Ensures that modified segments are written to disk.
     *
     * @param affectedSegments  a set of integers which specify the segments
     *                          to sync to disk
     */
    // TODO: order matters! see notes above re downloading tile: index entry
    //  must be written last, else we'll get a race condition

    private void syncSegments(IntSet affectedSegments)
    {
        IntIterator iter = affectedSegments.intIterator();
        while(iter.hasNext())
        {
            int segment = iter.next();
            // Log.debug("Syncing segment %d...", segment);
            getMapping(segment).force();
            // TODO: handle UncheckedIOException
        }
    }


    // TODO: do not call close() if any threads are still accessing buffers
    // implement a cleanup method?
    // TODO: create an awaitOperations() method

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

    protected void beginTransaction(int transactionLockLevel) throws IOException
    {
        assert transactionLockLevel == LOCK_APPEND || transactionLockLevel == LOCK_EXCLUSIVE;
        transactionLock.lock();
        try
        {
            lock(transactionLockLevel);
            try
            {
                File journalFile = getJournalFile();
                if (journalFile.exists()) processJournal(journalFile);
                preTransactionFileSize = getTrueSize();
                    // do this after processing journal, because file size
                    // may have changed as a result
            }
            catch(Throwable ex)
            {
                lock(LOCK_READ);
                throw ex;
            }
        }
        catch(Throwable ex)
        {
            transactionLock.unlock();
            throw ex;
        }
        transactionBlocks = new LongObjectHashMap<>();
    }

    protected void endTransaction() throws IOException
    {
        assert isInTransaction();
        assert transactionLock.isHeldByCurrentThread();
        transactionBlocks = null;
        try
        {
            lock(LOCK_READ);
        }
        catch(Throwable ex)
        {
            throw ex;
        }
        finally
        {
            transactionLock.unlock();
        }
    }

    protected boolean isInTransaction()
    {
        return transactionBlocks != null;
    }

    protected void rollback()
    {
        assert transactionLock.isHeldByCurrentThread();
        transactionBlocks.clear();
    }

    /**
     * Returns the number of the segment in which the given file position
     * is located.
     *
     * @param pos   the absolute position in the file
     * @return      the corresponding segment number
     */
    protected static int segmentOfPos(long pos)
    {
        return (int)(pos >> 30);
    }

    protected void commit() throws IOException
    {
        assert isInTransaction();
        assert transactionLock.isHeldByCurrentThread();

        // Save the rollback instructions and make sure the journal file
        // is safely written to disk
        saveJournal();

        // Copy the contents of all journaled blocks into their segments
        // (each semgent is a 1-GB MappedByteBuffer); track which segments
        // have been modified
        MutableIntSet affectedSegments = new IntHashSet();
        for(TransactionBlock block: transactionBlocks.values())
        {
            int segment = segmentOfPos(block.pos);
            int ofs = (int)block.pos & 0x3fff_ffff;
            assert (ofs & 0xfff) == 0;
            assert block.current.array().length == 4096;
            block.original.put(ofs, block.current.array());
            affectedSegments.add(segment);
        }

        // Blocks that are appended to the file during the transaction are
        // not journaled (they are simply truncated in case of a rollback);
        // nevertheless, we need to record their segments as well
        long currentFileSize = getTrueSize();
        if(currentFileSize > preTransactionFileSize)
        {
            int firstSegment = segmentOfPos(preTransactionFileSize);
            int lastSegment = segmentOfPos(currentFileSize - 1);
            for(int segment=firstSegment; segment<=lastSegment; segment++)
            {
                affectedSegments.add(segment);
            }
        }

        // Ensure that the modified segments are written to disk
        syncSegments(affectedSegments);

        // Log.debug("Committed %d blocks in %d segments",
        //    transactionBlocks.size(), affectedSegments.size());

        // throw new RuntimeException("causing commit to fail");

        // Now that all changes are safely stored, we can reset the journal
        clearJournal();
    }

    protected ByteBuffer getBlock(long pos)
    {
        assert (pos & 0xfff) == 0: String.format(
            "%d: Block must start at 4KB-aligned position", pos);
        assert isInTransaction();
        assert transactionLock.isHeldByCurrentThread();

        if(pos < preTransactionFileSize)
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


    public long currentFileSize() throws IOException
    {
        return Files.size(path);
    }
}
