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

package org.platformlambda.example;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * End-to-end test of the standalone Redis helper as the field would run it: {@link MainApp#start} boots a
 * real embedded {@code redis-server} subprocess, then a minimal RESP client round-trips PING / SET / GET
 * over TCP (no Redis client dependency required). The test port ({@code redis.port=6399}) avoids clashing
 * with any Redis already running on the default 6379; the server is shut down explicitly afterwards.
 * <p>
 * The failure-handling branches of {@code EmbeddedRedis} are covered separately in
 * {@code org.platformlambda.system.EmbeddedRedisTest}.
 */
class RedisStandaloneTest {

    private static final int PORT = 6399;
    private static MainApp app;

    @BeforeAll
    static void start() {
        app = new MainApp();
        app.start(new String[0]);   // startServer() blocks until the redis-server subprocess is ready
    }

    @AfterAll
    static void stop() {
        if (app != null && app.redis != null) {
            app.redis.stop();
        }
    }

    @Test
    void pingSetGetRoundTrip() throws Exception {
        try (Socket socket = new Socket("127.0.0.1", PORT)) {
            socket.setSoTimeout(5000);
            OutputStream out = socket.getOutputStream();
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), UTF_8));
            assertEquals("+PONG", sendInline(out, in, "PING"));
            assertEquals("+OK", sendInline(out, in, "SET greeting hello"));
            // GET returns a RESP bulk string: "$5\r\nhello\r\n"
            out.write("GET greeting\r\n".getBytes(UTF_8));
            out.flush();
            assertEquals("$5", in.readLine());
            assertEquals("hello", in.readLine());
        }
    }

    private static String sendInline(OutputStream out, BufferedReader in, String command) throws Exception {
        out.write((command + "\r\n").getBytes(UTF_8));
        out.flush();
        return in.readLine();
    }
}
