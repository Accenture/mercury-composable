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

package org.platformlambda.services;

import org.platformlambda.core.annotations.PreLoad;
import org.platformlambda.core.models.EventEnvelope;
import org.platformlambda.core.models.TypedLambdaFunction;
import org.platformlambda.core.system.Platform;
import org.platformlambda.core.system.PostOffice;
import org.platformlambda.core.util.Utility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Public echo function - the standing Event-over-HTTP interop target of this
 * example app, mirroring the Rust port's hello-world echo so the two example
 * apps can demonstrate cross-language "Event over HTTP" against each other
 * out of the box. It echoes the request body and headers, and honors an
 * optional integer body key "sleep_ms" (delay before replying, capped at 10s)
 * so a caller can exercise its RPC-timeout path.<p>
 *
 * This function has two route names for event-over-http calls from
 * the composable-example. There are two REST endpoints in the composable-example
 * for this purpose. One for making an event-over-http call using programmatic means
 * and the other using declarative means.<p>
 *
 * For interop, the "hello-world" and "hello-flow" examples in the Rust version
 * are the counterpart of the lambda-example and composable-example.<p>
 *
 * The Rust's hello-flow example can call the Java's lambda-example by changing
 * the "peer.demo.host" and "peer.demo.port" to point to the Java version.
 * Similarly, the Java's composable-example can also change the two parameters
 * in application.properties to talk to the Rust version.<p>
 *
 * The default settings are "peer.demo.host=127.0.0.1" and "peer.demo.port={$rest.server.port}"
 */
@PreLoad(route="hello.world, hello.declarative", instances=10, isPrivate = false)
public class HelloWorld implements TypedLambdaFunction<EventEnvelope, Map<String, Object>> {
    private static final Logger log = LoggerFactory.getLogger(HelloWorld.class);

    @Override
    public Map<String, Object> handleEvent(Map<String, String> headers, EventEnvelope input, int instance) {
        var po = new PostOffice(headers, instance);
        log.info("echo #{} got a request", instance);
        if (input.getBody() instanceof Map<?, ?> map && map.get("sleep_ms") != null) {
            long ms = Utility.getInstance().str2long(String.valueOf(map.get("sleep_ms")));
            Utility.getInstance().sleep(Math.min(ms, 10000));
        }
        Map<String, Object> result = new HashMap<>();
        // echo back the input payload
        result.put("body", input.getBody());
        // Note that 'headers' = input.getHeaders() plus metadata for tracing
        // (metadata: my_route, my_trace_id, my_trace_path and my_correlation_id).
        // Since we want to echo back the original request headers, we use the input.getHeaders() method.
        result.put("headers", input.getHeaders());
        result.put("instance", instance);
        result.put("origin", Platform.getInstance().getOrigin());
        // forward event to hello.pojo so we can see the span-id of "hello.world" propagated to "hello.pojo"
        po.send(new EventEnvelope().setTo("hello.pojo").setHeader("id", 1));
        return result;
    }
}
