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

package org.platformlambda.demo;

import org.junit.jupiter.api.Test;
import org.platformlambda.core.util.AppConfigReader;
import org.platformlambda.demo.common.TestBase;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Progressive result set rendering: the hello.sse function streams test messages
 * through its dedicated reply lane ("stream: true" in rest.yaml) and the HTTP
 * client receives them as Server-Sent Events with a terminal "done" event.
 * <p>
 * The client's send() returns as soon as the response head arrives, and the body
 * is a lazy stream. Each line is therefore timestamped at the moment it arrives,
 * and the test asserts the arrival spread rather than just the final content.
 */
class SseDemoTest extends TestBase {

    private record TimedLine(String line, long nanoTime) { }

    @Test
    void sseEndpointRendersMessagesProgressively() throws IOException, InterruptedException {
        var config = AppConfigReader.getInstance();
        var port = config.getProperty("rest.server.port", "8885");
        // 3 messages paced 100 ms apart keep the unit test fast - the default pace is one second
        var url = "http://127.0.0.1:" + port + "/api/hello/sse?delay=100&count=3";
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(20)).header("Accept", "text/event-stream").build();
        List<TimedLine> lines = new ArrayList<>();
        try (HttpClient client = HttpClient.newHttpClient()) {
            HttpResponse<java.util.stream.Stream<String>> response =
                    client.send(request, HttpResponse.BodyHandlers.ofLines());
            assertEquals(200, response.statusCode());
            assertEquals("text/event-stream", response.headers().firstValue("content-type").orElse(""));
            // consume the lazy body stream, capturing the arrival time of every line
            response.body().forEach(line -> lines.add(new TimedLine(line, System.nanoTime())));
        }
        List<String> plain = lines.stream().map(TimedLine::line).toList();
        int intro = plain.indexOf("data: The following messages are rendered slowly to demonstrate the SSE feature:");
        assertTrue(intro >= 0, "intro line expected: " + plain);
        for (int i = 1; i <= 3; i++) {
            assertTrue(plain.contains("data: test message " + i), "message " + i + " expected: " + plain);
        }
        int done = plain.indexOf("event: done");
        assertTrue(done > intro, "terminal done event expected: " + plain);
        assertEquals("data: end of SSE page.", plain.get(done + 1));
        // progressive delivery: the three paced messages put the done event ~300 ms
        // after the intro line - a buffered response would deliver every line at
        // virtually the same instant
        long elapsedMs = (lines.get(done).nanoTime() - lines.get(intro).nanoTime()) / 1_000_000;
        assertTrue(elapsedMs >= 200, "progressive delivery expected, elapsed " + elapsedMs + " ms");
    }
}
