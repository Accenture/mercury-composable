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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.platformlambda.core.models.EventEnvelope;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.*;

/**
 * P2 hardening for {@link FileElasticStore} — the file-specific behaviors {@link ElasticStoreContractTest}
 * does not stress (the contract test uses the 16 MB default segment, so it never rolls). Uses a tiny
 * segment size to force many segment rolls, then asserts: FIFO across segment boundaries, O(1)
 * reclamation (bounded disk), no leftover files across reuse cycles, clean reuse after close, and
 * degenerate inputs.
 */
class FileElasticStoreTest {

    private static final String SEG_PROP = "elastic.queue.segment.size.bytes";
    private static final int M = ElasticStore.MEMORY_BUFFER;

    @AfterEach
    void clearSegmentSizeOverride() {
        System.clearProperty(SEG_PROP);
    }

    private static byte[] toRecord(String body) {
        return new EventEnvelope().setBody(body).toBytes();
    }

    private static String body(byte[] b) {
        EventEnvelope e = new EventEnvelope();
        e.load(b);
        return (String) e.getBody();
    }

    /** Read until the store reports empty - the last read closes it and reclaims its segments. */
    private static int drain(FileElasticStore q) {
        int records = 0;
        while (q.read().length > 0) {
            records++;
        }
        return records;
    }

    /** Count live on-disk segment files for a given queue id (baseDir is package-private for this). */
    private static int segmentFiles(String id) {
        File[] files = FileElasticStore.baseDir == null ? null : FileElasticStore.baseDir.listFiles();
        if (files == null) {
            return 0;
        }
        int n = 0;
        String prefix = "eq-" + id + "-";
        for (File f : files) {
            if (f.getName().startsWith(prefix) && f.getName().endsWith(".dat")) {
                n++;
            }
        }
        return n;
    }

    @Test
    void spillsAcrossManySegmentsInFifoOrderAndReclaims() {
        System.setProperty(SEG_PROP, "2048"); // tiny segments → force rolling
        String id = "seg.roll";
        FileElasticStore q = new FileElasticStore(id);
        int total = M + 300; // 300 records on disk
        for (int i = 0; i < total; i++) {
            q.write(toRecord("rec-" + i));
        }
        // the disk tier must have rolled into several segments
        assertTrue(segmentFiles(id) > 1, "expected multiple segments, got " + segmentFiles(id));
        // read everything back in strict FIFO order, spanning segment boundaries
        for (int i = 0; i < total; i++) {
            assertEquals("rec-" + i, body(q.read()));
        }
        assertEquals(0, q.read().length);
        assertTrue(q.isClosed());
        // every sealed, fully-consumed segment was deleted → no disk left behind
        assertEquals(0, segmentFiles(id), "all segments reclaimed on drain");
    }

    @Test
    void diskStaysBoundedUnderSustainedOverflow() {
        System.setProperty(SEG_PROP, "2048");
        String id = "sustained";
        FileElasticStore q = new FileElasticStore(id);
        // maintain a steady on-disk backlog, then churn write+read 1:1 for many iterations
        for (int i = 0; i < M + 100; i++) {
            q.write(toRecord("prefill-" + i));
        }
        int peak = 0;
        for (int i = 0; i < 5000; i++) {
            q.write(toRecord("evt-" + i));
            assertNotEquals(0, q.read().length);
            peak = Math.max(peak, segmentFiles(id));
        }
        // without reclamation this would grow to ~thousands/records-per-segment; reclamation keeps it small
        assertTrue(peak < 20, "live segments should stay bounded (peak=" + peak + ")");
        assertEquals(M + 100, drain(q), "every prefilled record is still there");
        assertEquals(0, segmentFiles(id), "fully reclaimed after drain");
        q.destroy();
    }

    @Test
    void noLeftoverFilesAcrossReuseCycles() {
        System.setProperty(SEG_PROP, "2048");
        String id = "reuse.cycles";
        FileElasticStore q = new FileElasticStore(id);
        for (int cycle = 0; cycle < 50; cycle++) {
            for (int i = 0; i < M + 40; i++) {
                q.write(toRecord("c" + cycle + "-" + i));
            }
            // drain → auto close() → reclaim segments
            assertEquals(M + 40, drain(q), "every record read back, cycle " + cycle);
            assertTrue(q.isClosed(), "closed after drain, cycle " + cycle);
            assertEquals(0, segmentFiles(id), "no leftover segments after cycle " + cycle);
        }
        q.destroy();
        assertEquals(0, segmentFiles(id));
    }

    @Test
    void reuseAfterCloseStartsClean() {
        System.setProperty(SEG_PROP, "2048");
        FileElasticStore q = new FileElasticStore("clean.reuse");
        for (int i = 0; i < M + 50; i++) {
            q.write(toRecord("a-" + i));
        }
        for (int i = 0; i < M + 50; i++) {
            assertEquals("a-" + i, body(q.read()));
        }
        assertEquals(0, q.read().length);
        assertTrue(q.isClosed());
        // reuse the same instance: must not surface any stale "a-" data
        for (int i = 0; i < M + 50; i++) {
            q.write(toRecord("b-" + i));
        }
        for (int i = 0; i < M + 50; i++) {
            assertEquals("b-" + i, body(q.read()));
        }
        assertEquals(0, q.read().length);
        assertTrue(q.isClosed());
        q.destroy();
    }

    @Test
    void closeAfterPeekReuseClearsCachedEvent() {
        FileElasticStore q = new FileElasticStore("peek.close.reuse");
        q.write(toRecord("a"));
        assertEquals("a", body(q.peek()));
        q.close();
        q.write(toRecord("b"));
        assertEquals("b", body(q.read()));
        assertEquals(0, q.read().length);
        q.destroy();
    }

    @Test
    void openChannelsStayBoundedAfterManySegmentRolls() {
        System.setProperty(SEG_PROP, "512");
        // destroy() purges the segments and closes the store; the try's own close() is then a no-op
        try (FileElasticStore q = new FileElasticStore("fd.bound")) {
            for (int i = 0; i < M + 1000; i++) {
                q.write(toRecord("fd-" + i + "-" + "x".repeat(200)));
            }
            assertTrue(segmentFiles("fd.bound") > 10, "expected many segment rolls");
            assertTrue(q.openSegmentChannels() <= 2, "open segment channels should stay O(1)");
            q.destroy();
        }
    }

    @Test
    void staleStoreDirectoriesAreCleanedWithoutDeletingCurrent() throws IOException {
        try (FileElasticStore q = new FileElasticStore("stale.cleanup.bootstrap")) {
            File current = FileElasticStore.baseDir;
            File tmpRoot = current.getParentFile();
            assertNotNull(tmpRoot, "file store should be under a transient root");
            File stale = new File(tmpRoot, "stale-file-store-" + System.nanoTime());
            assertTrue(stale.mkdirs(), "stale directory created");
            Files.writeString(new File(stale, "RUNNING").toPath(), "old");
            Files.write(new File(stale, "eq-stale-1-0.dat").toPath(), new byte[] {1, 2, 3});
            assertTrue(new File(stale, "RUNNING").setLastModified(System.currentTimeMillis() - 2L * 60 * 60 * 1000));
            File currentLeftover = new File(current, "eq-current-leftover.dat");
            Files.write(currentLeftover.toPath(), new byte[] {4, 5, 6});
            FileElasticStore.scanExpiredStores(tmpRoot, current);
            assertFalse(stale.exists(), "stale prior store should be removed");
            assertTrue(current.exists(), "current store directory must not be deleted");
            assertTrue(currentLeftover.exists(), "stale scan must not purge current directory segments");
            FileElasticStore.purgeLeftoverSegments(current, null);
            assertFalse(currentLeftover.exists(), "startup purge removes current leftover eq segments");
            q.destroy();
        }
    }

    @Test
    void staleRunningOnlyDirectoryIsNotDeleted() throws IOException {
        File unrelated;
        File running;
        try (FileElasticStore q = new FileElasticStore("stale.running.only.bootstrap")) {
            File current = FileElasticStore.baseDir;
            File tmpRoot = current.getParentFile();
            assertNotNull(tmpRoot, "file store should be under a transient root");
            unrelated = new File(tmpRoot, "stale-running-only-" + System.nanoTime());
            assertTrue(unrelated.mkdirs(), "unrelated directory created");
            running = new File(unrelated, "RUNNING");
            Files.writeString(running.toPath(), "old");
            assertTrue(running.setLastModified(System.currentTimeMillis() - 2L * 60 * 60 * 1000));
            FileElasticStore.scanExpiredStores(tmpRoot, current);
            assertTrue(unrelated.exists(), "RUNNING-only directory without eq segments must not be removed");
            q.destroy();
        }
        FileElasticStore.purgeLeftoverSegments(unrelated, null);
        Files.deleteIfExists(running.toPath());
        Files.deleteIfExists(unrelated.toPath());
    }

    @Test
    void shutdownRemovesTheInstanceFolderItEmptied(@TempDir File root) throws IOException {
        // the shutdown hook purges the segments and the RUNNING marker, then removes the emptied instance folder
        File instance = new File(root, "app-instance");
        assertTrue(instance.mkdirs(), "instance folder created");
        Files.writeString(new File(instance, "RUNNING").toPath(), "now");
        Files.write(new File(instance, "eq-route-1-0.dat").toPath(), new byte[] {1, 2, 3});
        FileElasticStore.removeHoldingArea(instance, true);
        assertFalse(instance.exists(), "the emptied instance folder is removed");
        // a folder that holds a file of another kind stays, without its marker
        File shared = new File(root, "shared");
        assertTrue(shared.mkdirs(), "shared folder created");
        Files.writeString(new File(shared, "RUNNING").toPath(), "now");
        File other = new File(shared, "notes.txt");
        Files.writeString(other.toPath(), "not a segment");
        FileElasticStore.removeHoldingArea(shared, true);
        assertTrue(other.exists(), "a file of another kind keeps its folder");
        assertFalse(new File(shared, "RUNNING").exists(), "the marker is gone");
        // with running.in.cloud the folder is the configured store itself, so it stays
        File cloud = new File(root, "cloud");
        assertTrue(cloud.mkdirs(), "cloud store created");
        Files.writeString(new File(cloud, "RUNNING").toPath(), "now");
        FileElasticStore.removeHoldingArea(cloud, false);
        assertTrue(cloud.exists(), "the configured store stays");
        assertFalse(new File(cloud, "RUNNING").exists(), "the marker is gone");
    }

    @Test
    void ignoresDegenerateInputAndKeepsPeekReadConsistent() {
        FileElasticStore q = new FileElasticStore("degenerate");
        q.write(null);
        q.write(new byte[0]);
        assertEquals(0, q.getWriteCounter(), "null/empty writes are ignored");
        assertEquals(0, q.read().length);
        assertTrue(q.isClosed());
        // peek returns the same event a subsequent read returns
        q.write(toRecord("only"));
        byte[] peeked = q.peek();
        byte[] read = q.read();
        assertEquals("only", body(peeked));
        assertEquals(body(peeked), body(read));
        assertEquals(0, q.read().length);
        assertTrue(q.isClosed());
        q.destroy();
    }
}
