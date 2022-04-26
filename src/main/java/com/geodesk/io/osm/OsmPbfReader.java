package com.geodesk.io.osm;

import com.clarisma.common.pbf.PbfBuffer;
import com.clarisma.common.pbf.PbfException;
import com.clarisma.common.util.Log;
import com.geodesk.feature.Tags;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

import static com.geodesk.io.osm.OsmPbf.*;

/**
 * A base class for reading (and processing) OSM PBF files, which uses a
 * pool of worker threads to maximize throughput.
 *
 * A well-formed OSM PBF file consists of a header block and one or more data
 * blocks. The data blocks contain the OSM features: nodes, ways, and relations.
 * Nodes must appear before ways, ways must appear before relations.
 *
 * (See https://wiki.openstreetmap.org/wiki/PBF_Format)
 *
 * Blocks are processed concurrently. The OsmPbfReader processes the three
 * types of feature in phases. This means that all nodes are guaranteed to
 * have been processed before way processing begins; likewise, all ways
 * have been processed by the time relation processing begins.
 *
 * Processing of features happens in the form of events. Subclasses should
 * define a custom WorkerThread class that overrides the event methods of
 * interest:
 *
 *     <pre>
 *     header()
 *     beginNodes()
 *     node()
 *     ndNodes()
 *     beginWays()
 *     way()
 *     endWays()
 *     beginRelations()
 *     relation()
 *     endRelations()
 *     postProcess()
 *     </pre>
 *
 * This custom WorkerThread must be created in the overridden createWorker method:
 *
 *     <pre>
 *     &#64;Override protected WorkerThread createWorker()
 *     {
 *         return new MyCustomWorkerThread();
 *     }
 *     </pre>
 *
 * If the WorkerThread instances generate output that must be written
 * sequentially (e.g. by appending to a file), instructions can be passed to a
 * separate output thread by calling {@link #output(Runnable)}. This is more
 * efficient than wrapping such instructions in a {@code synchronized}
 * block, as it uses a queue to process {@link Runnable} objects.
 *
 * Worker threads may throw exceptions, in which case all threads are
 * interrupted and processing is shut down. Such exceptions are wrapped
 * in an {@link IOException}, which is in turn thrown by {@link #read(File)}.
 *
 * <h2>Current limitations</h2>
 *
 * Decoding of single node messages is not supported, as all major OSM-related
 * software uses the more efficient DenseNodes groups to encode nodes.
 *
 */

// TODO: why do we need an input thread? we could simply run it off main thread!
// TODO: Base on Processor

// TODO: node(), way(), and relation() could accept Node, Way, Relation?
//  no, spec for Node requires Mercator projection

public class OsmPbfReader
{
    // protected static final Logger log = LogManager.getLogger();
    private static Tags EMPTY_TAGS = new EmptyTags();

    private long startTime;
    private Phase[] phases = new Phase[5];
    private InputStream in;
    private long fileSize;
    private Throwable error;
    private int threadCount = Runtime.getRuntime().availableProcessors(); // TODO
    private int queueSize = threadCount * 2; // TODO
    private InputThread inputThread;
    private WorkerThread[] workerThreads;
    private OutputThread outputThread;
    private BlockingQueue<Block> inputQueue;
    private BlockingQueue<Runnable> outputQueue;

    protected static final int PHASE_HEADER = 0;
    protected static final int PHASE_NODES = 1;
    protected static final int PHASE_WAYS = 2;
    protected static final int PHASE_RELATIONS = 3;
    protected static final int PHASE_POSTPROCESS = 4;
    protected static final int PHASE_DONE = 5;

    protected static void log(String s)
    {
        Log.debug("%s: %s", Thread.currentThread().getName(), s);
    }

    protected long fileSize()
    {
        return fileSize;
    }

    /*
    // TODO: read != processed
    protected double percentageCompleted()
    {
        return (double)(bytesRead.get() * 100) / totalBytes;
    }
    */

    // TODO: same as Processor
    protected long timeElapsed()
    {
        return System.currentTimeMillis() - startTime;
    }

    /**
     * Called to create a new worker thread. Subclasses should override this
     * method and create a custom WorkerThread that processes the OSM features.
     *
     * @return a new WorkerThread instance
     */
    protected WorkerThread createWorker()
    {
        return new WorkerThread();
    }

    private static class Phase extends CountDownLatch
    {
        private String name;

        public Phase(String name, int threadCount)
        {
            super(threadCount);
            this.name = name;
        }

        public String name()
        {
            return name;
        }
    }

    /**
     * Aborts all processing. Interrupts all threads and shuts down.
     * The {@link Throwable} argument will be wrapped in an {@link IOException},
     * which will be thrown by the {@link #read(File)} method.
     *
     * @param ex a {@link Throwable} indicating the cause of the failure
     */
    protected void fail(Throwable ex)
    {
        /*
        synchronized (log)
        {
            // TODO: remove
            log.error("Thread {} failed: {}",
                Thread.currentThread().getName(), ex.getMessage());
            ex.printStackTrace();
        }
         */
        error = ex;
        inputThread.interrupt();
        outputThread.interrupt();
        for(int i=0; i<workerThreads.length; i++)
        {
            workerThreads[i].interrupt();
            phases[PHASE_POSTPROCESS].countDown();
        }
    }

    /**
     * A block containing OSM header or OSM data.
     */
    protected static class Block
    {
        /**
         * {@code "OSMHeader"} or {@code "OSMData"}
         */
        public String type;
        /**
         * The gross length of the block (in bytes), including its BlobHeader
         * and payload, as well as the length indicator itself.
         */
        public int length;
        /**
         * The block's payload (the Blob message, which can contain raw or
         * compressed data).
         */
        public byte[] data;
    }

    /**
     * A special marker that signals that no more blocks are available to read,
     * to trigger worker threads to begin the post-processing phase.
     * (Queues do not accept {@code null})
     */
    private static final Block END_INPUT = new Block();
    /*
    private static final Runnable END_OUTPUT = new Runnable()
    {
        @Override public void run()
        {
            Thread.currentThread().interrupt();
        }
    };
    */

    /**
     * Thread that reads the blocks in an OSM PBF file and queues them
     * for processing.
     */
    private class InputThread extends Thread
    {
        private byte[] headerLengthBuffer = new byte[4];
        private PbfBuffer headerBuffer = new PbfBuffer();
        private long bytesRead;
        private int percentageReported = -1;

        @Override public void run()
        {
            try
            {
                for (;;)
                {
                    if (in.read(headerLengthBuffer) != 4) break;
                    int headerLength = (headerLengthBuffer[0] << 24) |
                        (headerLengthBuffer[1] << 16) | (headerLengthBuffer[2] << 8) |
                        headerLengthBuffer[3];
                    byte[] headerData = new byte[headerLength];
                    if (in.read(headerData) != headerLength)
                    {
                        throw new PbfException("Header block truncated.");
                    }
                    Block block = new Block();
                    headerBuffer.wrap(headerData);
                    int dataLength = 0;
                    while (headerBuffer.hasMore())
                    {
                        int field = headerBuffer.readTag();
                        switch (field)
                        {
                        case BLOBHEADER_TYPE:
                            block.type = headerBuffer.readString();
                            break;
                        case BLOBHEADER_DATASIZE:
                            dataLength = (int) headerBuffer.readVarint();
                            break;
                        }
                    }
                    if (block.type == null)
                    {
                        throw new PbfException("Header Type not specified.");
                    }
                    try
                    {
                        block.data = new byte[dataLength];
                    }
                    catch(OutOfMemoryError ex)
                    {
                        throw new RuntimeException(
                            String.format("Failed to allocate block data (%d bytes)", dataLength, ex));
                    }
                    if (in.read(block.data) != dataLength)
                    {
                        throw new PbfException("Data block truncated.");
                    }
                    block.length = 4 + headerLength + dataLength;
                    inputQueue.put(block);
                    bytesRead += block.length;
                    int currentPercentage = (int)((bytesRead-1) * 100 / fileSize);
                    if(currentPercentage != percentageReported)
                    {
                        System.out.format("Reading... %d%%\r", currentPercentage);
                        percentageReported = currentPercentage;
                    }
                }
                for(int i=0; i<threadCount; i++) inputQueue.put(END_INPUT);
                in.close();
            }
            catch(Throwable ex)
            {
                fail(ex);
            }
        }
    }

    /**
     * Thread that processes sequential instructions submitted via
     * {@link #output(Runnable)}.
     */
    private class OutputThread extends Thread
    {
        @Override public void run()
        {
            try
            {
                for (;;)
                {
                    if(interrupted()) break;
                    Runnable task = outputQueue.take();
                    task.run();
                }
            }
            catch(InterruptedException ex)
            {
                // do nothing, we're done
            }
            catch(Throwable ex)
            {
                fail(ex);
            }
        }
    }

    /**
     * Executes the given {@link Runnable} on the output thread, ensuring
     * that such operations happen synchronously. This method can be safely
     * called by the worker threads. It is more efficient than wrapping
     * instructions in a {@code synchronized} block, as it uses a blocking
     * queue to hand off tasks to the output thread.
     *
     * @param task a {@code Runnable} that performs synchronous instructions
     * @throws InterruptedException if interrupted while waiting for space
     *     in the output queue
     *
     */
    protected void output(Runnable task) throws InterruptedException
    {
        outputQueue.put(task);
    }

    protected class WorkerThread extends Thread
    {
        private int currentPhase;
        private List<String> strings = new ArrayList<>();
        private List<Integer> groups = new ArrayList<>();
        // private MutableIntList groups = new IntArrayList();
        // TODO: It looks like plain old ArrayList with boxed integers
        //  is faster than the primitive list??

        protected int currentPhase()
        {
            return currentPhase;
        }

        /*
        protected int currentBlockLength()
        {
            return currentBlockLength;
        }
         */

        /**
         * Called when the file's header is encountered. This method is
         * only called once (as a well-formed file only has one header).
         *
         * @param hd
         */
        protected void header(HeaderData hd)
        {
            /*
            log.debug("Source: {}", hd.source);
            log.debug("Writing Program: {}", hd.writingProgram);
            log.debug("Required features:");
            for(String s: hd.requiredFeatures) log.debug("- {}", s);
            log.debug("Optional features:");
            for(String s: hd.optionalFeatures) log.debug("- {}", s);
            */
        }

        /**
         * Called before node processing begins. This method is called once
         * for each thread. The header data (if any) has been processed at
         * this point.
         */
        protected void beginNodes()
        {
        }

        /**
         * Called for each node.
         */
        protected void node(long id, int x, int y, Tags tags)
        {
        }

        /**
         * Called after all nodes have been processed. This method is
         * called once for each thread.
         *
         * Other threads may still be processing nodes at this point, but
         * none of them will move on to way/relation processing until all
         * threads have completed {@code endNodes}.
         */
        protected void endNodes()
        {
        }

        /**
         * Called before way processing begins. This method is called once
         * for each thread. All nodes have been processed at this point.
         */
        protected void beginWays()
        {
        }

        /**
         * Called for each way.
         */
        protected void way(long id, Tags tags, Nodes nodes)
        {
        }

        /**
         * Called after all ways have been processed. This method is
         * called once for each thread.
         *
         * Other threads may still be processing ways at this point, but
         * none of them will move on to relation processing until all
         * threads have completed {@code endWays}.
         */
        protected void endWays()
        {
        }

        /**
         * Called before relation processing begins. This method is called once
         * for each thread. All nodes and ways have been processed at this point.
         */
        protected void beginRelations()
        {
        }

        /**
         * Called for each relation.
         *
         * @param id        the ID of the relation
         * @param tags      its tags
         * @param members   its members
         */
        protected void relation(long id, Tags tags, Members members)
        {
        }

        /**
         * Called after all relations have been processed.
         *
         * Other threads may still be processing relations at this point,
         * but none of them will move on to post-processing until all
         * threads have completed {@code endRelations}.
         */
        protected void endRelations()
        {
        }

        /**
         * Called when all features in a block have been processed.
         *
         * @param block
         */
        protected void endBlock(Block block)
        {
        }

        /**
         * Called once per thread after all nodes, ways, and relations
         * have been processed.
         */
        protected void postProcess()
        {
        }

        /**
         * Moves the current thread into the specified phase. The thread
         * waits until all other threads have completed the preceding
         * phases.
         *
         * @param newPhase
         * @throws InterruptedException
         */
        private void switchPhase(int newPhase) throws InterruptedException
        {
            if(newPhase == currentPhase) return;

            /*
            log(String.format("Switching phase from %s to %s...",
                phases[currentPhase].name(),
                newPhase == PHASE_DONE ? "DONE" : phases[newPhase].name()));
            */

            if(newPhase < currentPhase)
            {
                throw new PbfException(String.format(
                    "Encountered %s after %s", phases[newPhase].name(),
                    phases[currentPhase].name()));
            }
            switch(currentPhase)
            {
            case PHASE_NODES:
                //log("Calling endNodes...");
                endNodes();
                //log("endNodes called.");
                break;
            case PHASE_WAYS:
                //log("Calling endWays...");
                endWays();
                //log("endWays called.");
                break;
            case PHASE_RELATIONS:
                //log("Calling endRelations...");
                endRelations();
                //log("endRelations called.");
                break;
            }
            for(int i=currentPhase; i<newPhase; i++)
            {
                // log("Finished phase " + phases[i].name());

                // The output thread needs to perform the phase countdown;
                // if we were to do it directly, any tasks submitted by
                // the end...() methods could otherwise still be pending,
                // and we need to finish all phase processing before the
                // workers can begin the next phase

                Phase phase = phases[i];
                outputQueue.put(() -> phase.countDown());
                    // TODO: is this needed? countDown should be threadsafe,
                    //  no need to call it in synchronous fashion
                    //  --> see note above

                /*
                final String threadName = Thread.currentThread().getName();
                outputQueue.put(() ->
                {
                    log(String.format("%s completed %s", threadName, phase.name()));
                    phase.countDown();
                });
                 */
            }
            try
            {
                // log("Waiting for other threads to finish phase " + phases[newPhase-1].name());
                phases[newPhase-1].await();
            }
            catch (InterruptedException ex)
            {
                throw ex;
            }
            currentPhase = newPhase;
            switch(currentPhase)
            {
            case PHASE_NODES:
                // log("Calling beginNodes...");
                beginNodes();
                break;
            case PHASE_WAYS:
                // log("Calling beginWays...");
                beginWays();
                break;
            case PHASE_RELATIONS:
                // log("Calling beginRelations...");
                beginRelations();
                break;
            }
        }

        private byte[] inflate(byte[] data, int start, int len, int uncompressedLen) throws DataFormatException
        {
            Inflater unzip = new Inflater();
            unzip.setInput(data, start, len);
            byte[] output = new byte[uncompressedLen];
            int bytesUnzipped = unzip.inflate(output);
            if (bytesUnzipped != uncompressedLen)
            {
                throw new PbfException("Unzipped " + bytesUnzipped
                    + " bytes (expected " + uncompressedLen + ")");
            }
            return output;
        }

        @Override public void run()
        {
            for(;;)
            {
                try
                {
                    Block block = inputQueue.take();
                    if(block == END_INPUT)
                    {
                        switchPhase(PHASE_POSTPROCESS);
                        // log("Calling postProcess...");
                        postProcess();
                        // log("postProcess called.");
                        switchPhase(PHASE_DONE);
                        break;
                    }
                    processBlock(block);
                }
                catch (Throwable ex)
                {
                    fail(ex);
                }
            }
        }

        private void processBlock(Block block) throws InterruptedException, DataFormatException, IOException
        {
            PbfBuffer buf = new PbfBuffer(block.data);
            int rawPos = 0;
            int rawLen = 0;
            int zippedPos = 0;
            int zippedLen = 0;
            int uncompressedLen = 0;
            while (buf.hasMore())
            {
                int field = buf.readTag();
                switch (field)
                {
                case BLOB_RAW_DATA:
                    rawLen = (int) buf.readVarint();
                    rawPos = buf.pos();
                    buf.skip(rawLen);
                    break;
                case BLOB_RAW_SIZE:
                    uncompressedLen = (int) buf.readVarint();
                    break;
                case BLOB_ZLIB_DATA:
                    zippedLen = (int) buf.readVarint();
                    zippedPos = buf.pos();
                    buf.skip(zippedLen);
                    break;
                default:
                    throw new PbfException("Illegal tag: " + field);
                }
            }

            if (rawPos > 0)
            {
                decodeBlock(block.type, block.data, rawPos, rawLen);
            }
            if (zippedPos > 0)
            {
                byte[] inflated = inflate(block.data, zippedPos, zippedLen,
                    uncompressedLen);
                decodeBlock(block.type, inflated, 0, inflated.length);
            }
            endBlock(block);
        }

        /**
         * Decodes and processes an OSM header block or OSM data block.
         * Blocks of unknown type are ignored.
         *
         * @param type          {@code "OSMHeader"} or {@code "OSMData"}
         * @param data          the block data (uncompressed)
         * @param dataStart     starting offset of the data
         * @param dataLen       length of the data (in bytes)
         *
         * @throws InterruptedException
         */
        private void decodeBlock(String type, byte[] data, int dataStart, int dataLen) throws InterruptedException
        {
            switch(type)
            {
            case "OSMData":
                decodeDataBlock(data, dataStart, dataLen);
                break;
            case "OSMHeader":
                decodeHeaderBlock(data, dataStart, dataLen);
                break;
            }
        }

        /**
         * Processes an OSM header block, and calls the {@link #header(HeaderData)}
         * event method.
         *
         * @param data
         * @param dataStart
         * @param dataLen
         */
        private void decodeHeaderBlock(byte[] data, int dataStart, int dataLen)
        {
            // currentPhase = PHASE_HEADER;		// TODO: check!!!!

            PbfBuffer buf = new PbfBuffer(data, dataStart, dataLen);
            HeaderData hd = new HeaderData();
            List<String> requiredFeatures = new ArrayList<>();
            List<String> optionalFeatures = new ArrayList<>();

            while(buf.hasMore())
            {
                int marker = buf.readTag();
                switch (marker)
                {
                case HEADER_BBOX:
                    buf.skipEntity(marker);
                    // TODO
                    break;
                case HEADER_REQUIRED_FEATURES:
                    requiredFeatures.add(buf.readString());
                    break;
                case HEADER_OPTIONAL_FEATURES:
                    optionalFeatures.add(buf.readString());
                    break;
                case HEADER_WRITINGPROGRAM:
                    hd.writingProgram = buf.readString();
                    break;
                case HEADER_SOURCE:
                    hd.source = buf.readString();
                    break;
                case HEADER_REPLICATION_TIMESTAMP:
                    hd.replicationTimestamp = buf.readVarint();
                    break;
                case HEADER_REPLICATION_SEQUENCE:
                    hd.replicationSequence = buf.readVarint();
                    break;
                case HEADER_REPLICATION_URL:
                    hd.replicationUrl = buf.readString();
                    break;
                default:
                    buf.skipEntity(marker);
                    break;
                }
            }
            hd.requiredFeatures = requiredFeatures.toArray(new String[0]);
            hd.optionalFeatures = optionalFeatures.toArray(new String[0]);
            header(hd);
        }

        /**
         * Decodes an OSM data block, and calls the various event methods
         * to process the contained features.
         *
         * @param data
         * @param dataStart
         * @param dataLen
         */
        private void decodeDataBlock(byte[] data, int dataStart, int dataLen) throws InterruptedException
        {
            PbfBuffer buf = new PbfBuffer(data, dataStart, dataLen);
            int granularity = 100;
            int dateGranularity = 1000;
            long latOffset = 0;
            long lonOffset = 0;

            while(buf.hasMore())
            {
                int b = buf.readTag();
                switch (b)
                {
                case BLOCK_STRINGTABLE:
                    // log.debug("Decoding string table...");
                    int len = (int)buf.readVarint();
                    int endPos = buf.pos() + len;
                    while(buf.pos() < endPos)
                    {
                        int marker = buf.readTag();
                        if (marker != STRINGTABLE_ENTRY)
                        {
                            throw new PbfException("Bad string table. Unexpected field: " + marker);
                        }
                        strings.add(buf.readString());
                    }
                    break;
                case BLOCK_GROUP:
                    len = (int)buf.readVarint();
                    groups.add(buf.pos());
                    groups.add(len);
                    buf.skip(len);
                    break;
                case BLOCK_GRANULARITY:
                    granularity = (int)buf.readVarint();
                    break;
                case BLOCK_DATE_GRANULARITY:
                    dateGranularity = (int)buf.readVarint();
                    break;
                case BLOCK_LAT_OFFSET:
                    latOffset = buf.readVarint();
                    break;
                case BLOCK_LON_OFFSET:
                    lonOffset = buf.readVarint();
                    break;
                default:
                    throw new PbfException("Illegal marker: " + b);
                }
            }

            // Need to ensure that we read the granularity info
            // before we start decoding the primitive groups

            for (int i=0; i<groups.size(); i+=2)
            {
                PbfBuffer groupBuf = new PbfBuffer(buf.buf(),
                    groups.get(i), groups.get(i+1));
                decodePrimitiveGroup(groupBuf, strings,
                    latOffset, lonOffset, granularity);
            }

            strings.clear();
            groups.clear();
        }

        protected void decodeDenseNodes(
            PbfBuffer buf, List<String> strings,
            long latOffset, long lonOffset, int granularity)
        {
            PbfBuffer idBuf = null;
            PbfBuffer latBuf = null;
            PbfBuffer lonBuf = null;
            PbfBuffer tagsBuf = null;
            int len = (int)buf.readVarint();
            int endPos = buf.pos() + len;
            while (buf.pos() < endPos)
            {
                int marker = buf.readTag();
                switch(marker)
                {
                case DENSENODE_IDS:
                    idBuf = buf.readMessage();
                    break;
                case DENSENODE_INFO:
                    buf.skipEntity(marker);
                    break;
                case DENSENODE_LATS:
                    latBuf = buf.readMessage();
                    break;
                case DENSENODE_LONS:
                    lonBuf = buf.readMessage();
                    break;
                case DENSENODE_TAGS:
                    tagsBuf = buf.readMessage();
                    break;
                default:
                    // System.out.println("- Unrecognized: " + marker);
                    buf.skipEntity(marker);
                    break;
                }
            }
            assert(buf.pos() == endPos);
            if (idBuf != null)
            {
                long id = idBuf.readSignedVarint();
                long lat = latBuf.readSignedVarint();
                long lon = lonBuf.readSignedVarint();

                DenseTags tags = new DenseTags(strings, tagsBuf);

                /*
                Tags tags = tagsBuf==null ? EMPTY_TAGS :
                    new DenseTags(strings, tagsBuf);
                 */
                // TODO: guard against empty buffer (if none of nodes have tags)
                // TODO: fails if client does not read all tags, or keeps
                //  calling next() after last item --> make DenseTags more robust

                for(;;)
                {
                    long latInNanoDeg = (latOffset + (granularity * lat));
                    long lonInNanoDeg = (lonOffset + (granularity * lon));
                    node(id, (int)(lonInNanoDeg / 100),
                        (int)(latInNanoDeg / 100), tags);
                    if(!idBuf.hasMore()) break;
                    id += idBuf.readSignedVarint();
                    lat += latBuf.readSignedVarint();
                    lon += lonBuf.readSignedVarint();
                    tags.advanceGroup();
                }
                // assert(!tagsBuf.hasMore()); // TODO: check, end marker remains
            }

        }

        protected void decodeWay(PbfBuffer buf, List<String> strings)
        {
            long id = 0;
            PbfBuffer keyBuf = null;
            PbfBuffer valueBuf = null;
            PbfBuffer nodesBuf = null;
            PbfBuffer coordsBuf = null;
            int len = (int)buf.readVarint();
            int endPos = buf.pos() + len;
            while (buf.pos() < endPos)
            {
                int marker = buf.readTag();
                switch(marker)
                {
                case ELEMENT_ID:
                    id = buf.readVarint();
                    break;
                case ELEMENT_KEYS:
                    keyBuf = buf.readMessage();
                    break;
                case ELEMENT_VALUES:
                    valueBuf = buf.readMessage();
                    break;
                case ELEMENT_INFO:
                    buf.skipEntity(marker);
                    // TODO
                    break;
                case WAY_NODES:
                    nodesBuf = buf.readMessage();
                    break;
                case WAY_COORDS:
                    // TODO: this is non-standard, drop support?
                    coordsBuf = buf.readMessage();
                    break;
                default:
                    System.out.println("- Unrecognized: " + marker);
                    buf.skipEntity(marker);
                    break;
                }
            }
            way(id, keyBuf==null ? EMPTY_TAGS :
                    new KeyValueTags(strings, keyBuf, valueBuf),
                new Nodes(nodesBuf==null ? PbfBuffer.EMPTY : nodesBuf));
        }

        protected void decodeRelation(PbfBuffer buf, List<String> strings)
        {
            long id = 0;
            PbfBuffer keyBuf = null;
            PbfBuffer valueBuf = null;
            // Guard against relations without members
            PbfBuffer memberRolesBuf = PbfBuffer.EMPTY;
            PbfBuffer memberIdsBuf = PbfBuffer.EMPTY;
            PbfBuffer memberTypesBuf = PbfBuffer.EMPTY;
            int len = (int)buf.readVarint();
            int endPos = buf.pos() + len;
            while (buf.pos() < endPos)
            {
                int marker = buf.readTag();
                switch(marker)
                {
                case ELEMENT_ID:
                    id = buf.readVarint();
                    // System.out.println("Reading relation/" + id);
                    break;
                case ELEMENT_KEYS:
                    keyBuf = buf.readMessage();
                    break;
                case ELEMENT_VALUES:
                    valueBuf = buf.readMessage();
                    break;
                case ELEMENT_INFO:
                    buf.skipEntity(marker);
                    // TODO
                    break;
                case RELATION_MEMBER_ROLES:
                    memberRolesBuf = buf.readMessage();
                    break;
                case RELATION_MEMBER_IDS:
                    memberIdsBuf = buf.readMessage();
                    break;
                case RELATION_MEMBER_TYPES:
                    memberTypesBuf = buf.readMessage();
                    break;
                default:
                    System.out.println("- Unrecognized: " + marker);
                    buf.skipEntity(marker);
                    break;
                }
            }
            relation(id, keyBuf==null ? EMPTY_TAGS :
                    new KeyValueTags(strings, keyBuf, valueBuf),
                new Members(strings, memberRolesBuf, memberIdsBuf, memberTypesBuf));

        }

        protected void decodePrimitiveGroup(
            PbfBuffer buf, List<String> strings,
            long latOffset, long lonOffset, int granularity) throws InterruptedException
        {
            // log("Decoding primitive group...");
            while(buf.hasMore())
            {
                int marker = buf.readTag();
                switch(marker)
                {
                case GROUP_NODE:
                    System.out.println("Node");
                    // TODO
                    buf.skipEntity(marker);
                    break;
                case GROUP_DENSENODES:
                    if(currentPhase != PHASE_NODES) switchPhase(PHASE_NODES);
                    decodeDenseNodes(buf, strings,
                        latOffset, lonOffset, granularity);
                    break;
                case GROUP_WAY:
                    if(currentPhase != PHASE_WAYS) switchPhase(PHASE_WAYS);
                    decodeWay(buf, strings);
                    break;
                case GROUP_RELATION:
                    if(currentPhase != PHASE_RELATIONS) switchPhase(PHASE_RELATIONS);
                    decodeRelation(buf, strings);
                    break;
                case GROUP_CHANGESET:
                    buf.skipEntity(marker);
                    break;
                default:
                    // TODO: ignore
                    System.out.println("Unrecognized marker: " + marker);
                    return;
                }
            }
        }
    }

    /**
     * Called after processing of the file has finished (whether completed
     * successfully or aborted).
     */
    protected void endFile()
    {
    }

    /**
     * {@see #read}.
     * 
     * @param filename
     * @throws IOException
     */
    public void read(String filename) throws IOException
    {
        read(new File(filename));
    }

    /**
     * Reads and processes the given OSM PBF file.
     *
     * @param file
     * @throws IOException
     */
    public void read(File file) throws IOException
    {
        inputQueue = new LinkedBlockingQueue<>(queueSize);
        outputQueue = new LinkedBlockingQueue<>(queueSize);
        phases[PHASE_HEADER] = new Phase("header", threadCount);
        phases[PHASE_NODES] = new Phase("nodes", threadCount);
        phases[PHASE_WAYS] = new Phase("ways", threadCount);
        phases[PHASE_RELATIONS] = new Phase("relations", threadCount);
        phases[PHASE_POSTPROCESS] = new Phase("postprocess", threadCount);
        startTime = System.currentTimeMillis();
        fileSize = file.length();
        in = new FileInputStream(file);
        inputThread = new InputThread();
        inputThread.setName("input");
        workerThreads = new WorkerThread[threadCount];
        for(int i=0; i<threadCount; i++)
        {
            WorkerThread thread = createWorker();
            thread.setName("worker-" + i);
            workerThreads[i] = thread;
        }
        outputThread = new OutputThread();
        outputThread.setName("output");

        // Only start the threads once all of them have been created
        inputThread.start();
        outputThread.start();
        for(int i=0; i<workerThreads.length; i++) workerThreads[i].start();

        try
        {
            phases[PHASE_POSTPROCESS].await();
        }
        catch(InterruptedException ex)
        {
            // do nothing
        }
        outputThread.interrupt();   // TODO: move into catch?
        in.close(); // TODO: catch ex here
        endFile();
        inputThread = null;
        outputThread = null;
        workerThreads = null;
        inputQueue = null;
        outputQueue = null;
        if(error != null)
        {
            throw new IOException(String.format("Failed to read %s: %s",
                file, error.getMessage()), error);
        }
    }

}
