/*

    Copyright 2018-2025 Accenture Technology

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

package org.platformlambda.common;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import org.junit.jupiter.api.BeforeAll;
import org.platformlambda.automation.http.AsyncHttpClient;
import org.platformlambda.automation.service.MockHelloWorld;
import org.platformlambda.core.models.AsyncHttpRequest;
import org.platformlambda.core.models.EventEnvelope;
import org.platformlambda.core.services.LongRunningRpcSimulator;
import org.platformlambda.core.system.*;
import org.platformlambda.core.util.AppConfigReader;
import org.platformlambda.core.util.Utility;
import org.platformlambda.core.websocket.server.MinimalistHttpHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class TestBase {
    private static final Logger log = LoggerFactory.getLogger(TestBase.class);
    protected static final String HELLO_WORLD = "hello.world";
    protected static final String HELLO_MOCK = "hello.mock";
    protected static final String LONG_RUNNING_RPC = "long.running.rpc";
    protected static final String SLOW_RPC_FUNCTION = "slow.rpc.function";
    protected static final String HELLO_LIST = "hello.list";
    protected static final String CLOUD_CONNECTOR_HEALTH = "cloud.connector.health";
    protected static final int MINIMALIST_HTTP_PORT = 8020;
    protected static final String APP_ID = Utility.getInstance().getDateUuid()+"-"+System.getProperty("user.name");
    private static final String SERVICE_LOADED = "http.service.loaded";
    private static final int WAIT_INTERVAL = 300;
    protected static int port;
    protected static String localHost;

    private static final AtomicInteger startCounter = new AtomicInteger(0);

    @BeforeAll
    public static void setup() throws IOException, InterruptedException {
        if (startCounter.incrementAndGet() == 1) {
            Platform.setAppId(APP_ID);
            Utility util = Utility.getInstance();
            AppConfigReader config = AppConfigReader.getInstance();
            port = util.str2int(config.getProperty("server.port", "8100"));
            localHost = "http://127.0.0.1:" + port;
            AppStarter.runAsSpringBootApp();
            AutoStart.main(new String[0]);
            AppStarter.runMainApp();
            ServerPersonality.getInstance().setType(ServerPersonality.Type.REST);
            blockingWait(AsyncHttpClient.ASYNC_HTTP_RESPONSE, 20);
            blockingWait(CLOUD_CONNECTOR_HEALTH, 20);
            // you can convert a private function to public when needed
            blockingWait(HELLO_WORLD, 5);
            log.info("Mock cloud ready");
            Platform platform = Platform.getInstance();
            platform.registerPrivate(HELLO_MOCK, new MockHelloWorld(), 10);
            platform.registerKotlinPrivate(LONG_RUNNING_RPC, new LongRunningRpcSimulator(), 15);
            // test registering the same function with an alias route name
            platform.registerKotlin(SLOW_RPC_FUNCTION, new LongRunningRpcSimulator(), 10);
            // hello.list is a special function to test returning result set as a list
            platform.registerPrivate(HELLO_LIST, (headers, input, instance) ->
                    Collections.singletonList(input), 5);
            platform.makePublic(HELLO_MOCK);
            // load minimalist HTTP server
            Vertx vertx = Vertx.vertx();
            HttpServerOptions options = new HttpServerOptions().setTcpKeepAlive(true);
            HttpServer server = vertx.createHttpServer(options);
            server.requestHandler(new MinimalistHttpHandler());
            server.listen(MINIMALIST_HTTP_PORT)
                    .onSuccess(service -> {
                        try {
                            platform.registerPrivate(SERVICE_LOADED, (headers, input, instance) -> true, 1);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    })
                    .onFailure(ex -> {
                        log.error("Unable to start - {}", ex.getMessage());
                        System.exit(-1);
                    });
            blockingWait(SERVICE_LOADED, 20);
            EventEmitter po = EventEmitter.getInstance();
            log.info("Journal ready? {}", po.isJournalEnabled());
            int n = 0;
            while (!po.isJournalEnabled()) {
                Thread.sleep(WAIT_INTERVAL);
                n++;
                log.info("Waiting for journal engine to get ready. Elapsed {} ms", n * WAIT_INTERVAL);
            }
            log.info("Event over HTTP config ready? {}", po.isEventHttpConfigEnabled());
            n = 0;
            while (!po.isEventHttpConfigEnabled()) {
                Thread.sleep(WAIT_INTERVAL);
                n++;
                log.info("Waiting for Event over HTTP config to get ready. Elapsed {} ms", n * WAIT_INTERVAL);
            }
            // render REST endpoint again with correct config file because the first pass is intentionally to have errors
            System.setProperty("yaml.rest.automation", "classpath:/rest.yaml, classpath:/event-api.yaml");
            AppStarter.renderRestEndpoints();
            System.setProperty("yaml.rest.automation", "classpath:/rest.yaml");
            AppStarter.renderRestEndpoints();
        }
    }

    private static void blockingWait(String provider, int seconds) throws InterruptedException {
        BlockingQueue<Boolean> bench = new ArrayBlockingQueue<>(1);
        Future<Boolean> status = Platform.getInstance().waitForProvider(provider, seconds);
        status.onSuccess(bench::add);
        if (!Boolean.TRUE.equals(bench.poll(seconds, TimeUnit.SECONDS))) {
            log.error("{} provider not available in {} seconds", provider, seconds);
        }
    }

    protected EventEnvelope httpGet(String host, String path, Map<String, String> headers)
            throws IOException, InterruptedException {
        // BlockingQueue should only be used in unit test
        final BlockingQueue<EventEnvelope> bench = new ArrayBlockingQueue<>(1);
        EventEmitter po = EventEmitter.getInstance();
        AsyncHttpRequest req = new AsyncHttpRequest().setMethod("GET").setTargetHost(host).setUrl(path);
        if (headers != null) {
            for (Map.Entry<String, String> kv: headers.entrySet()) {
                req.setHeader(kv.getKey(), kv.getValue());
            }
        }
        EventEnvelope event = new EventEnvelope().setTo(AsyncHttpClient.ASYNC_HTTP_REQUEST).setBody(req);
        Future<EventEnvelope> res = po.asyncRequest(event, 10000);
        res.onSuccess(bench::add);
        return bench.poll(10, TimeUnit.SECONDS);
    }

    protected EventEnvelope httpPost(String host, String path,
                                     Map<String, String> headers, Map<String, Object> body)
            throws IOException, InterruptedException {
        // BlockingQueue should only be used in unit test
        final BlockingQueue<EventEnvelope> bench = new ArrayBlockingQueue<>(1);
        EventEmitter po = EventEmitter.getInstance();
        AsyncHttpRequest req = new AsyncHttpRequest().setMethod("POST")
                                    .setTargetHost(host).setUrl(path).setBody(body);
        if (headers != null) {
            for (Map.Entry<String, String> kv: headers.entrySet()) {
                req.setHeader(kv.getKey(), kv.getValue());
            }
        }
        EventEnvelope event = new EventEnvelope().setTo(AsyncHttpClient.ASYNC_HTTP_REQUEST).setBody(req);
        Future<EventEnvelope> res = po.asyncRequest(event, 10000);
        res.onSuccess(bench::add);
        return bench.poll(10, TimeUnit.SECONDS);
    }
}
