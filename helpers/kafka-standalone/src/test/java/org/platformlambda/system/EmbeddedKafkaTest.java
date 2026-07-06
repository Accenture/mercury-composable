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

package org.platformlambda.system;

import org.junit.jupiter.api.Test;
import org.platformlambda.core.util.Utility;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link EmbeddedKafka} internals that do NOT require a running broker - the happy-path
 * boot-and-round-trip is covered by {@code org.platformlambda.example.MainAppTest}. This class lives in the
 * same package and reaches the private storage helpers reflectively to drive the fail-fast / cleanup
 * branches deterministically (missing resource, absent {@code log.dirs}, and re-formatting an existing dir).
 */
class EmbeddedKafkaTest {

    @Test
    void shutdownBeforeStartIsANoOp() {
        // shutdown() before startServer() must be a safe no-op (internal server is still null), not an error
        assertDoesNotThrow(() -> new EmbeddedKafka(true).shutdown());
    }

    @Test
    void startServerWithMissingResourceThrows() throws Exception {
        EmbeddedKafka kafka = new EmbeddedKafka(true);
        setPropPath(kafka, "/no-such-server.properties");
        Exception ex = assertThrows(IllegalArgumentException.class, kafka::startServer);
        assertTrue(ex.getMessage().contains("not available as resource"));
    }

    @Test
    void formatStorageWithoutLogDirsThrows() {
        InvocationTargetException ite = assertThrows(InvocationTargetException.class,
                () -> invokeFormatStorage(new EmbeddedKafka(true), new Properties()));
        assertInstanceOf(IllegalArgumentException.class, ite.getCause());
        assertTrue(ite.getCause().getMessage().contains("log.dirs is required"));
    }

    @Test
    void formatStorageCleansAndFormatsExistingDir() throws Exception {
        Path dir = Files.createTempDirectory("kafka-standalone-fmt");
        Files.writeString(dir.resolve("stale.tmp"), "left over from a previous run");
        Properties config = new Properties();
        config.setProperty("log.dirs", dir.toString());
        config.setProperty("node.id", "1");
        try {
            // exercises the "directory already exists" cleanup path, then the real KRaft Formatter
            invokeFormatStorage(new EmbeddedKafka(true), config);
            assertTrue(Files.exists(dir), "the log directory should be (re)created and formatted");
        } finally {
            Utility.getInstance().cleanupDir(dir.toFile());
        }
    }

    private static void setPropPath(EmbeddedKafka kafka, String path) throws Exception {
        Field field = EmbeddedKafka.class.getDeclaredField("serverPropPath");
        field.setAccessible(true);
        field.set(kafka, path);
    }

    private static void invokeFormatStorage(EmbeddedKafka kafka, Properties config) throws Exception {
        Method method = EmbeddedKafka.class.getDeclaredMethod("formatStorage", Properties.class);
        method.setAccessible(true);
        method.invoke(kafka, config);
    }
}
