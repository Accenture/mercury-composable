/*

    Copyright 2018-2026 Accenture Technology

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

        http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

 */

package org.platformlambda.core.util;

import org.platformlambda.core.system.Platform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Dependency-free elastic overflow buffer: the first {@link ElasticStore#MEMORY_BUFFER} events stay in
 * memory, then the overflow spills to a per-route sequence of fixed-size append-only <b>segment</b> files.
 *
 * <p><b>Format (portable — see draft-design-specs/elastic_queue_file_fifo_design.md §5.2):</b> each record is
 * {@code [4-byte big-endian length][payload bytes]}, appended in order within a segment. A segment is sealed
 * once it passes the size threshold; reads advance a per-segment offset and a fully-consumed sealed segment is
 * <b>deleted immediately</b> (O(1) reclamation — no compaction, no cleaner thread, no background housekeeping,
 * which is the whole point vs. Berkeley DB).</p>
 *
 * <p><b>Threading:</b> single-threaded per route (the Vert.x consumer's event-loop thread), exactly like the
 * BDB store. A {@link FileChannel} per segment gives write-then-read visibility within the process via the OS
 * page cache; no fsync (the buffer is transient — not durable across restart).</p>
 */
class FileElasticStore implements ElasticStore {
    private static final Logger log = LoggerFactory.getLogger(FileElasticStore.class);
    private static final Utility util = Utility.getInstance();
    private static final int MEMORY_BUFFER = ElasticStore.MEMORY_BUFFER;
    private static final byte[] NOTHING = new byte[0];
    private static final int LENGTH_PREFIX = 4;
    private static final String SEGMENT_PREFIX = "eq-";
    private static final String SEGMENT_SUFFIX = ".dat";
    private static final long DEFAULT_SEGMENT_BYTES = 16L * 1024 * 1024;
    private static final long MIN_SEGMENT_BYTES = 512;
    private static final String SEGMENT_SIZE_CONFIG = "elastic.queue.segment.size.bytes";

    private static final ReentrantLock SAFETY = new ReentrantLock();
    private static final AtomicBoolean LOADED = new AtomicBoolean(false);
    static File baseDir;   // package-private: read by tests to assert segment reclamation

    private final String id;
    private final String safeId;
    private final long segmentBytes;
    private final ConcurrentLinkedQueue<byte[]> memory = new ConcurrentLinkedQueue<>();
    private final Deque<Segment> segments = new ArrayDeque<>();
    private long readCounter;
    private long writeCounter;
    private boolean empty = false;
    private byte[] peeked = NOTHING;
    private int generation = 0;

    /**
     * @param id service route path
     */
    FileElasticStore(String id) {
        this.id = util.validServiceName(id) ? id : util.filteredServiceName(id);
        this.safeId = sanitize(this.id);
        this.segmentBytes = Math.max(MIN_SEGMENT_BYTES, util.str2long(
                AppConfigReader.getInstance().getProperty(SEGMENT_SIZE_CONFIG,
                        String.valueOf(DEFAULT_SEGMENT_BYTES))));
        resetCounter();
        ensureBaseDir();
    }

    private static void ensureBaseDir() {
        if (!LOADED.get()) {
            SAFETY.lock();
            try {
                if (!LOADED.get()) {
                    LOADED.set(true);
                    Platform platform = Platform.getInstance();
                    AppConfigReader config = AppConfigReader.getInstance();
                    boolean runningInCloud = "true".equals(config.getProperty("running.in.cloud", "false"));
                    File tmpRoot = new File(config.getProperty("transient.data.store", "/tmp/reactive"));
                    baseDir = runningInCloud ? tmpRoot
                            : new File(tmpRoot, platform.getName() + "-" + platform.getOrigin());
                    if (!baseDir.exists() && baseDir.mkdirs()) {
                        log.info("{} created", baseDir);
                    }
                    // transient store: clear any leftover segment files from a prior run
                    purgeLeftoverSegments(null);
                    log.info("Elastic file store ready ({})", baseDir);
                }
            } finally {
                SAFETY.unlock();
            }
        }
    }

    /** Best-effort removal of leftover segment files (all, or only those for a given safe id prefix). */
    private static void purgeLeftoverSegments(String idPrefix) {
        File[] files = baseDir.listFiles();
        if (files != null) {
            String prefix = idPrefix == null ? SEGMENT_PREFIX : SEGMENT_PREFIX + idPrefix + "-";
            for (File f : files) {
                if (f.getName().startsWith(prefix) && f.getName().endsWith(SEGMENT_SUFFIX)) {
                    try {
                        Files.deleteIfExists(f.toPath());
                    } catch (IOException e) {
                        log.debug("Unable to delete leftover {} - {}", f, e.getMessage());
                    }
                }
            }
        }
    }

    private static String sanitize(String s) {
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            sb.append((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9')
                    || c == '.' || c == '_' || c == '-' ? c : '_');
        }
        return sb.toString();
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public long getReadCounter() {
        return readCounter;
    }

    @Override
    public long getWriteCounter() {
        return writeCounter;
    }

    @Override
    public void write(byte[] event) {
        if (event != null && event.length > 0) {
            if (writeCounter < MEMORY_BUFFER) {
                // for highest performance, save to memory for the first few blocks
                memory.add(event);
            } else {
                appendToDisk(event);
            }
            writeCounter++;
            empty = false;
        }
    }

    @Override
    public byte[] peek() {
        if (peeked.length > 0) {
            return peeked;
        }
        peeked = read();
        return peeked;
    }

    @Override
    public byte[] read() {
        if (peeked.length > 0) {
            byte[] result = peeked;
            peeked = NOTHING;
            return result;
        }
        if (readCounter >= writeCounter) {
            // catch up with writes and thus nothing to read
            close();
            return NOTHING;
        }
        if (readCounter < MEMORY_BUFFER) {
            byte[] event = memory.poll();
            if (event != null) {
                readCounter++;
            }
            return event;
        }
        byte[] event = readFromDisk();
        if (event.length > 0) {
            readCounter++;
        }
        return event;
    }

    @Override
    public void close() {
        if (!isClosed()) {
            resetCounter();
        }
    }

    /**
     * This method may be called when the route supported by this elastic queue is no longer in service
     */
    @Override
    public void destroy() {
        close();
        // final clean-up: remove any stray segment files for this route (across generations)
        purgeLeftoverSegments(safeId);
    }

    @Override
    public boolean isClosed() {
        return writeCounter == 0;
    }

    @Override
    public boolean supportsVirtualThreadDispatch() {
        // per-route files, no shared lock, no synchronized in the hot path — a blocking segment I/O op
        // parks the virtual thread's carrier cleanly instead of pinning it, so off-loop dispatch is safe.
        return true;
    }

    private void resetCounter() {
        if (!empty) {
            empty = true;
            readCounter = writeCounter = 0;
            memory.clear();
            for (Segment s : segments) {
                s.closeAndDelete();
            }
            segments.clear();
            generation++;
        }
    }

    private void appendToDisk(byte[] event) {
        Segment tail = segments.peekLast();
        if (tail == null || tail.sealed) {
            tail = openSegment(tail == null ? 0 : tail.index + 1);
            segments.addLast(tail);
        }
        ByteBuffer buf = ByteBuffer.allocate(LENGTH_PREFIX + event.length);
        buf.putInt(event.length).put(event).flip();
        tail.writePos = writeFully(tail.channel, buf, tail.writePos);
        tail.recordsWritten++;
        if (tail.writePos >= segmentBytes) {
            tail.sealed = true;
        }
    }

    private byte[] readFromDisk() {
        Segment head = segments.peekFirst();
        if (head == null) {
            log.error("Missing segment for {} at read position {}", id, readCounter);
            return NOTHING;
        }
        ByteBuffer lenBuf = ByteBuffer.allocate(LENGTH_PREFIX);
        readFully(head.channel, lenBuf, head.readPos);
        int len = lenBuf.getInt(0);
        ByteBuffer payload = ByteBuffer.allocate(len);
        readFully(head.channel, payload, head.readPos + LENGTH_PREFIX);
        head.readPos += LENGTH_PREFIX + len;
        head.recordsRead++;
        // a sealed, fully-consumed segment is reclaimed immediately (O(1), no cleaner thread)
        if (head.sealed && head.recordsRead >= head.recordsWritten) {
            head.closeAndDelete();
            segments.removeFirst();
        }
        return payload.array();
    }

    private Segment openSegment(int index) {
        File f = new File(baseDir, SEGMENT_PREFIX + safeId + "-" + generation + "-" + index + SEGMENT_SUFFIX);
        try {
            FileChannel ch = FileChannel.open(f.toPath(), StandardOpenOption.CREATE,
                    StandardOpenOption.READ, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
            return new Segment(index, f, ch);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to open elastic segment " + f + " - " + e.getMessage(), e);
        }
    }

    private static long writeFully(FileChannel ch, ByteBuffer buf, long pos) {
        long p = pos;
        try {
            while (buf.hasRemaining()) {
                int n = ch.write(buf, p);
                if (n <= 0) {
                    throw new IOException("short write at " + p);
                }
                p += n;
            }
        } catch (IOException e) {
            throw new IllegalStateException("Elastic spill write failed - " + e.getMessage(), e);
        }
        return p;
    }

    private static void readFully(FileChannel ch, ByteBuffer buf, long pos) {
        long p = pos;
        try {
            while (buf.hasRemaining()) {
                int n = ch.read(buf, p);
                if (n < 0) {
                    throw new IOException("unexpected end of segment at " + p);
                }
                p += n;
            }
        } catch (IOException e) {
            throw new IllegalStateException("Elastic spill read failed - " + e.getMessage(), e);
        }
        buf.flip();
    }

    private static final class Segment {
        private final int index;
        private final File file;
        private final FileChannel channel;
        private long writePos = 0;
        private long readPos = 0;
        private long recordsWritten = 0;
        private long recordsRead = 0;
        private boolean sealed = false;

        private Segment(int index, File file, FileChannel channel) {
            this.index = index;
            this.file = file;
            this.channel = channel;
        }

        private void closeAndDelete() {
            try {
                channel.close();
            } catch (IOException e) {
                log.debug("Unable to close segment {} - {}", file, e.getMessage());
            }
            try {
                Files.deleteIfExists(file.toPath());
            } catch (IOException e) {
                log.debug("Unable to delete segment {} - {}", file, e.getMessage());
            }
        }
    }
}
