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

package org.platformlambda.dynatrace;

import com.accenture.adapters.FlowExecutor;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.platformlambda.automation.http.AsyncHttpClient;
import org.platformlambda.core.models.EventEnvelope;
import org.platformlambda.core.spi.InvocationObservers;
import org.platformlambda.core.system.AutoStart;
import org.platformlambda.core.system.EventEmitter;
import org.platformlambda.core.system.Platform;
import org.platformlambda.core.util.AppConfigReader;
import org.platformlambda.core.util.Utility;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The linkage through real Mercury dispatch: every hop of a request consumes a tag that the previous hop
 * produced on its own thread - across the REST edge, an RPC, Event Script's task executor and a deferred
 * Mono result. The negative control proves the check can fail.
 */
class OneAgentLinkageTest {
    private static final FakeOneAgent agent = new FakeOneAgent();
    private static final OneAgentObserver observer = new OneAgentObserver(agent.sdk, "oneagent-demo", true, false);
    private static final long TIMEOUT = 8000;

    @BeforeAll
    static void boot() throws InterruptedException {
        AutoStart.main(new String[0]);
        BlockingQueue<Boolean> ready = new ArrayBlockingQueue<>(1);
        Platform.getInstance().waitForProvider(AsyncHttpClient.ASYNC_HTTP_RESPONSE, 20).onSuccess(ready::add);
        assertEquals(Boolean.TRUE, ready.poll(20, TimeUnit.SECONDS), "REST automation did not start");
        InvocationObservers.register(observer);
    }

    @AfterAll
    static void cleanup() {
        InvocationObservers.unregister(observer);
    }

    @Test
    void theSetupWithoutAnAgentRegistersNothing() {
        // the real SDK in a JVM without OneAgent is permanently inactive: start() logs and returns
        assertDoesNotThrow(() -> new OneAgentSetup().start(new String[0]));
    }

    @Test
    void anHttpRequestIsOneConnectedChain() throws Exception {
        int port = Utility.getInstance().str2int(AppConfigReader.getInstance().getProperty("server.port", "8305"));
        HttpResponse<String> response;
        try (HttpClient client = HttpClient.newHttpClient()) {
            response = client.send(
                    HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/api/demo/hello")).GET().build(),
                    HttpResponse.BodyHandlers.ofString());
        }

        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("hello"));
        FakeOneAgent.Hop hello = awaitEnded("demo.hello");
        FakeOneAgent.Hop echo = awaitEnded("demo.echo");
        // the REST edge produced the first tag; the function's RPC produced the next one on its own thread
        assertTrue(agent.producedTags.contains(hello.tag), "demo.hello consumed no tag from the REST edge");
        assertTrue(agent.producedTags.contains(echo.tag), "demo.echo consumed no tag from demo.hello");
        assertEquals(hello.startThread, agent.tagThreads.get(echo.tag));
        assertEquals(hello.startThread, hello.endThread);
    }

    @Test
    void aFlowStaysConnectedThroughTheTaskExecutor() throws Exception {
        Utility util = Utility.getInstance();
        Map<String, Object> dataset = new HashMap<>();
        dataset.put("body", Map.of("hello", "world"));

        EventEnvelope result = FlowExecutor.getInstance()
                .request("unit.test", util.getUuid(), "TEST /api/flow", "oneagent-demo", dataset,
                        util.getUuid(), TIMEOUT)
                .get(TIMEOUT, TimeUnit.MILLISECONDS);

        assertEquals(200, result.getStatus());
        FakeOneAgent.Hop task1 = awaitEnded("demo.task.1");
        FakeOneAgent.Hop task2 = awaitEnded("demo.task.2");
        List<String> engineThreads = new java.util.ArrayList<>();
        agent.hopsOf("event.script.manager").forEach(h -> engineThreads.add(h.startThread));
        agent.hopsOf("task.executor").forEach(h -> engineThreads.add(h.startThread));
        assertFalse(agent.hopsOf("task.executor").isEmpty(), "task.executor was not observed");
        // each task's tag was produced by an observed Event Script router hop, which consumed the
        // previous step: the chain does not break at the engine's direct dispatch
        String diagnostics = "engine hops " + engineThreads + ", tag threads " + agent.tagThreads
                + ", task1 " + task1.tag + ", task2 " + task2.tag;
        assertTrue(engineThreads.contains(agent.tagThreads.get(task1.tag)), diagnostics);
        assertTrue(engineThreads.contains(agent.tagThreads.get(task2.tag)), diagnostics);
        String task1Response = agent.hopsOf("task.executor").stream()
                .map(h -> h.tag).filter(t -> task1.startThread.equals(agent.tagThreads.get(t)))
                .findFirst().orElse(null);
        assertNotNull(task1Response, "no task.executor hop consumed demo.task.1's response");
    }

    @Test
    void aDeferredResultResumesThroughAnInProcessLink() throws Exception {
        EventEnvelope response = EventEmitter.getInstance().eRequest(new EventEnvelope().setTo("demo.mono")
                .setBody("news").setTrace(Utility.getInstance().getUuid(), "TEST /api/mono"), TIMEOUT)
                .get(TIMEOUT, TimeUnit.MILLISECONDS);

        assertEquals("late news", response.getBody());
        FakeOneAgent.Hop hop = awaitEnded("demo.mono");
        List<FakeOneAgent.Call> links = awaitLinkTracerStartAndEnd();
        assertTrue(agent.callsOf("sdk").stream().anyMatch(c ->
                c.method().equals("createInProcessLink") && c.thread().equals(hop.startThread)));
        assertEquals("start", links.get(0).method());
        assertEquals("end", links.get(1).method());
    }

    @Test
    void theNegativeControlStartsANewPurePath() throws Exception {
        EventEmitter.getInstance().eRequest(new EventEnvelope().setTo("demo.unlinked").setBody("x")
                .setTrace(Utility.getInstance().getUuid(), "TEST /api/unlinked"), TIMEOUT)
                .get(TIMEOUT, TimeUnit.MILLISECONDS);

        FakeOneAgent.Hop hop = awaitEnded("demo.unlinked");
        // the sender produced a tag for it, and the hop did not consume it: a fragment, by design
        assertTrue(agent.callsOf("sdk").stream().anyMatch(c ->
                c.method().equals("traceOutgoingMessage") && "demo.unlinked".equals(c.argument())));
        assertNull(hop.tag);
    }

    private static FakeOneAgent.Hop awaitEnded(String route) {
        for (int i = 0; i < 100; i++) {
            List<FakeOneAgent.Hop> hops = agent.hopsOf(route);
            for (FakeOneAgent.Hop hop : hops) {
                if (hop.ended) {
                    return hop;
                }
            }
            Utility.getInstance().sleep(50);
        }
        throw new AssertionError("no ended hop for " + route);
    }

    private static List<FakeOneAgent.Call> awaitLinkTracerStartAndEnd() {
        for (int i = 0; i < 100; i++) {
            List<FakeOneAgent.Call> calls = agent.callsOf("link-tracer");
            if (calls.size() >= 2) {
                return calls;
            }
            Utility.getInstance().sleep(50);
        }
        throw new AssertionError("the in-process link tracer was not started and ended");
    }
}
