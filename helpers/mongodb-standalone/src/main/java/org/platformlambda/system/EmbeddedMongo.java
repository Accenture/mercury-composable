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

import de.bwaldvogel.mongo.MongoServer;
import de.bwaldvogel.mongo.backend.memory.MemoryBackend;
import org.platformlambda.core.system.Platform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.IntConsumer;

/**
 * Lifecycle wrapper around a MongoDB-compatible server written in Java ({@code mongo-java-server}). It speaks the
 * MongoDB wire protocol, so an application connects with the official MongoDB driver and its usual connection
 * string, and it needs <b>no Docker image and no binary download</b>: the server is a Maven dependency, which is what
 * makes it usable on a virtual desktop without Docker.
 *
 * <p>Like the {@code redis-standalone} helper, this is for development and testing only - not production. The
 * backend keeps the databases in memory, so each start begins with an empty server and nothing outlives a stop.
 * It emulates MongoDB rather than running it: the common commands, queries, updates, upserts and unique indexes
 * behave as they do on MongoDB, but not every operator or aggregation stage is implemented. Verify an
 * application against a real MongoDB before it goes to production.</p>
 */
public class EmbeddedMongo {
    private static final Logger log = LoggerFactory.getLogger(EmbeddedMongo.class);
    private static final String HOST = "127.0.0.1";

    private final int port;
    private final IntConsumer exit;
    private MongoServer server;

    /**
     * @param port to listen on, or 0 for a free port, which {@link #getPort()} then answers
     */
    public EmbeddedMongo(int port) {
        this(port, System::exit);
    }

    /**
     * Package-visible so a unit test can observe the exit that a startup failure asks for, which a standalone
     * dev server otherwise performs: it has nothing to fall back to.
     */
    EmbeddedMongo(int port, IntConsumer exit) {
        this.port = port;
        this.exit = exit;
    }

    /** Start the server, stop it when the application shuts down, and exit when it cannot start. */
    public void start() {
        try {
            startServer();
            Platform.getInstance().onShutdown(this::stop);
        } catch (Exception e) {
            // MongoServer.bind declares no exception but rethrows Netty's, such as a BindException for a port in use
            log.error("Unable to start MongoDB server on port {} - {}", port, e.getMessage());
            exit.accept(-1);
        }
    }

    /**
     * Start the server on {@code 127.0.0.1}, with empty in-memory databases. Package-visible so a unit test can
     * see the failure that {@link #start()} turns into an exit.
     */
    void startServer() {
        var instance = new MongoServer(new MemoryBackend());
        try {
            instance.bind(HOST, port);
        } catch (Exception e) {
            // release the threads that a failed bind has started, then report the failure as it came
            instance.shutdownNow();
            throw e;
        }
        server = instance;
        log.info("Standalone MongoDB server started - connection string mongodb://{}:{}", HOST, getPort());
    }

    /**
     * @return the port the server listens on, or -1 when it is not running
     */
    public int getPort() {
        return server == null ? -1 : server.getLocalAddress().getPort();
    }

    /** Stop the server. Its databases are in memory, so they go with it. */
    public void stop() {
        if (server != null) {
            var stopped = server;
            var boundPort = getPort();
            server = null;
            stopped.shutdownNow();
            log.info("Standalone MongoDB server on port {} stopped", boundPort);
        }
    }
}
