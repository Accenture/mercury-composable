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

package com.accenture.examples.rest;

import jakarta.servlet.http.HttpServletRequest;
import org.platformlambda.core.exception.AppException;
import org.platformlambda.core.models.EventEnvelope;
import org.platformlambda.core.system.PostOffice;
import org.platformlambda.core.util.Utility;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

@RestController
public class AsyncHelloWorld {
    private static final AtomicInteger seq = new AtomicInteger(0);

    @GetMapping(value = "/api/hello/world", produces={"application/json"})
    public Mono<Map<String, Object>> hello(HttpServletRequest request) {
        String traceId = Utility.getInstance().getUuid();
        PostOffice po = new PostOffice("hello.world.endpoint", traceId, "GET /api/hello/world");
        Map<String, Object> forward = new HashMap<>();
        Enumeration<String> headers = request.getHeaderNames();
        while (headers.hasMoreElements()) {
            String key = headers.nextElement();
            forward.put(key, request.getHeader(key));
        }
        // As a demo, just put the incoming HTTP headers as a payload and a parameter showing the sequence counter.
        // The echo service will return both.
        int n = seq.incrementAndGet();
        EventEnvelope req = new EventEnvelope().setTo("hello.world").setBody(forward).setHeader("seq", n);
        return Mono.create(callback ->
                po.eRequest(req, 3000, false)
                    .thenAccept(response -> {
                        if (response.hasError()) {
                            var error = response.getError() instanceof String text?
                                            text : String.valueOf(response.getError());
                            callback.error(new AppException(response.getStatus(), error));
                        } else {
                            Map<String, Object> result = new HashMap<>();
                            result.put("status", response.getStatus());
                            result.put("headers", response.getHeaders());
                            result.put("body", response.getBody());
                            result.put("execution_time", response.getExecutionTime());
                            result.put("round_trip", response.getRoundTrip());
                            callback.success(result);
                        }
                    })
        );
    }
}
