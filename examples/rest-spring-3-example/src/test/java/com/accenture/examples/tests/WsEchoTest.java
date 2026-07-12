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

package com.accenture.examples.tests;

import com.accenture.examples.common.TestBase;
import org.junit.jupiter.api.Test;
import org.platformlambda.core.models.LambdaFunction;
import org.platformlambda.core.system.EventEmitter;
import org.platformlambda.core.util.AppConfigReader;
import org.platformlambda.core.util.Utility;
import org.platformlambda.core.websocket.client.PersistentWsClient;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Exercises the WsEchoDemo websocket service ("hello"): a string message is echoed verbatim and a
 * byte-array message is acknowledged with its length.
 */
class WsEchoTest extends TestBase {

    @Test
    void echoStringAndAcknowledgeBytes() throws InterruptedException {
        final Utility util = Utility.getInstance();
        final AppConfigReader config = AppConfigReader.getInstance();
        final int wsPort = util.str2int(config.getProperty("websocket.server.port", "8086"));
        final String message = "hello twin demo";
        final byte[] blob = "12345".getBytes();
        final BlockingQueue<Boolean> bench = new ArrayBlockingQueue<>(1);
        final EventEmitter po = EventEmitter.getInstance();
        final List<String> received = new ArrayList<>();
        LambdaFunction connector = (headers, input, instance) -> {
            if ("open".equals(headers.get("type"))) {
                String txPath = headers.get("tx_path");
                po.send(txPath, message);
                po.send(txPath, blob);
            }
            if ("string".equals(headers.get("type"))) {
                received.add((String) input);
                if (received.size() == 2) {
                    bench.add(true);
                }
            }
            return true;
        };
        for (int i = 0; i < 3; i++) {
            if (util.portReady("127.0.0.1", wsPort, 3000)) {
                break;
            }
            util.sleep(1000);
        }
        PersistentWsClient client = new PersistentWsClient(connector,
                Collections.singletonList("ws://127.0.0.1:" + wsPort + "/ws/hello"));
        client.start();
        Boolean done = bench.poll(10, TimeUnit.SECONDS);
        client.close();
        assertEquals(Boolean.TRUE, done, "expect both websocket responses within 10s");
        assertTrue(received.contains(message), "string message is echoed verbatim");
        assertTrue(received.contains("received " + blob.length + " bytes"),
                "byte message is acknowledged with its length");
    }
}
