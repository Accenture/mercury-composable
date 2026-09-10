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

package com.accenture.minigraph.playground;

import com.accenture.minigraph.services.GraphExecutor;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.config.Property;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.platformlambda.core.models.AsyncHttpRequest;
import org.platformlambda.core.models.EventEnvelope;
import org.platformlambda.core.system.AutoStart;
import org.platformlambda.core.system.PostOffice;
import org.platformlambda.core.util.AppConfigReader;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pins the stepwise traversal log trail of the production walker
 * (graph.traversal.log=true, the default): the same vocabulary the dry-run
 * GraphTraveler prints to the Playground console, suffixed with the graph id
 * and the run's trace id (fallback: flow instance id when tracing is off) so
 * OTel dashboards can join app logs with exported spans. The Rust engine
 * emits the identical lines (telemetry presentation parity).
 */
class GraphTraversalLoggingTest {
    private static final String ASYNC_HTTP_CLIENT = "async.http.request";
    private static final long TIMEOUT = 8000;
    private static final CapturingAppender appender = new CapturingAppender();
    private static String target;

    @BeforeAll
    static void setup() {
        AutoStart.main(new String[0]);
        var config = AppConfigReader.getInstance();
        var port = config.getProperty("rest.server.port");
        target = "http://localhost:" + port;
        appender.start();
        var context = (LoggerContext) LogManager.getContext(false);
        context.getLogger(GraphExecutor.class.getName()).addAppender(appender);
        Configurator.setLevel(GraphExecutor.class.getName(), Level.INFO);
    }

    @AfterAll
    static void teardown() {
        var context = (LoggerContext) LogManager.getContext(false);
        context.getLogger(GraphExecutor.class.getName()).removeAppender(appender);
        appender.stop();
    }

    @Test
    void traversalTrailIsLoggedWithGraphAndInstanceCorrelation() throws ExecutionException, InterruptedException {
        var request = new AsyncHttpRequest().setMethod("POST").setTargetHost(target);
        request.setHeader("Accept", "application/json");
        request.setHeader("Content-Type", "application/json");
        request.setUrl("/api/graph/hello").setBody(Map.of("person_id", 100));
        var po = PostOffice.trackable("unit.test", "3000", "TEST /api/graph/hello traversal logging");
        var event = new EventEnvelope().setTo(ASYNC_HTTP_CLIENT).setBody(request.toMap());
        var response = po.request(event, TIMEOUT).get();
        assertEquals(200, response.getStatus());

        List<String> lines = appender.messages.stream()
                .filter(line -> line.contains(" - hello (")).toList();
        // The trail starts at the root node...
        assertTrue(lines.stream().anyMatch(line -> line.startsWith("Walk to root - hello (")),
                "expected a 'Walk to root' line, got: " + lines);
        // ...reports each skill with its execution time (same wording as the dry-run console)...
        assertTrue(lines.stream().anyMatch(line ->
                        line.matches("Executed \\S+ with skill \\S+ in \\S+ ms - hello \\(.+\\)")),
                "expected an 'Executed {node} with skill {skill} in {time} ms' line, got: " + lines);
        // ...and closes with the total elapsed time.
        assertTrue(lines.stream().anyMatch(line ->
                        line.matches("Graph traversal completed in \\d+ ms - hello \\(.+\\)")),
                "expected a 'Graph traversal completed in {time} ms' line, got: " + lines);
    }

    private static class CapturingAppender extends AbstractAppender {
        private final List<String> messages = new CopyOnWriteArrayList<>();

        CapturingAppender() {
            super("graph-traversal-capture", null, null, true, Property.EMPTY_ARRAY);
        }

        @Override
        public void append(LogEvent logEvent) {
            messages.add(logEvent.getMessage().getFormattedMessage());
        }
    }
}
