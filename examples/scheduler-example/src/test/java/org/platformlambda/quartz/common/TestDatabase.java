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

package org.platformlambda.quartz.common;

import com.mongodb.client.MongoClients;
import de.bwaldvogel.mongo.MongoServer;
import de.bwaldvogel.mongo.backend.memory.MemoryBackend;

/**
 * The database the tests use: an embedded MongoDB-compatible server written in Java ({@code mongo-java-server}),
 * started once per test JVM on a free port, so the tests need no MongoDB, Docker or download. It is the server that
 * {@code helpers/mongodb-standalone} runs.
 * <p>
 * To run the same tests against a real MongoDB, pass its connection string as {@code -Dtest.mongodb.uri}, for
 * example {@code mvn test -Dtest.mongodb.uri=mongodb://127.0.0.1:27017}. The tests use their own database,
 * {@code scheduler_test}, which is dropped when they start.
 */
public final class TestDatabase {
    public static final String DATABASE = "scheduler_test";
    private static boolean started = false;

    private TestDatabase() {
        // static access only
    }

    /** Points the application at the test database before it first connects; later calls do nothing. */
    public static synchronized void start() {
        if (!started) {
            var uri = System.getProperty("test.mongodb.uri");
            if (uri == null) {
                var server = new MongoServer(new MemoryBackend());
                server.bind("127.0.0.1", 0);
                Runtime.getRuntime().addShutdownHook(new Thread(server::shutdownNow));
                uri = "mongodb://127.0.0.1:" + server.getLocalAddress().getPort();
            }
            try (var client = MongoClients.create(uri)) {
                client.getDatabase(DATABASE).drop();
            }
            // a system property takes precedence over application.properties
            System.setProperty("mongodb.uri", uri);
            System.setProperty("mongodb.database", DATABASE);
            started = true;
        }
    }
}
