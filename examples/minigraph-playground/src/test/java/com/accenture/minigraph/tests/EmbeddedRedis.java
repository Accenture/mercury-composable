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

package com.accenture.minigraph.tests;

import org.platformlambda.core.util.Utility;
import redis.embedded.RedisServer;

import java.io.File;
import java.io.IOException;

/**
 * One embedded Redis for every test class that suspends a graph (tutorial-14 and the order-assistant
 * demo): a real redis-server binary on a fixed port, started once per test JVM.
 * <p>
 * The state-store connection is lazy and reads its configuration at first use, so the system property
 * set here is honored even when another test class boots the application first. The macOS arm64 binary
 * links Homebrew's OpenSSL 3 (see docs/guides/getting-started.md); CI's Ubuntu runner has libssl.so.3.
 */
final class EmbeddedRedis {
    static final int PORT = 16381;
    private static RedisServer server;

    private EmbeddedRedis() {
        // a shared fixture
    }

    static synchronized void start() throws IOException {
        if (server != null) {
            return;
        }
        System.setProperty("redis.port", String.valueOf(PORT));
        var dir = new File("/tmp/minigraph-playground-redis");
        Utility.getInstance().cleanupDir(dir);
        if (!dir.exists() && !dir.mkdirs()) {
            throw new IllegalStateException("Unable to create " + dir);
        }
        RedisServer embedded = RedisServer.newRedisServer()
                .port(PORT)
                .setting("dir " + dir.getAbsolutePath())
                .setting("save \"\"")
                .setting("appendonly no")
                .build();
        embedded.start();
        server = embedded;
    }
}
