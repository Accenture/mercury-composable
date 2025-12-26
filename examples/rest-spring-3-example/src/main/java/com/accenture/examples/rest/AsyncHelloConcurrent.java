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
import org.platformlambda.core.annotations.KernelThreadRunner;
import org.platformlambda.core.exception.AppException;
import org.platformlambda.core.models.EventEnvelope;
import org.platformlambda.core.system.PostOffice;
import org.platformlambda.core.util.Utility;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.*;

/**
 * This demonstrates non-blocking fork-n-join using future results
 */
@KernelThreadRunner
@RestController
public class AsyncHelloConcurrent {

    @GetMapping(value = "/api/hello/concurrent", produces={"application/json"})
    public Mono<Map<String, Object>> hello(HttpServletRequest request) {
        String traceId = Utility.getInstance().getUuid();
        PostOffice po = new PostOffice("hello.world.endpoint", traceId, "GET /api/hello/concurrent");
        Map<String, Object> forward = new HashMap<>();
        Enumeration<String> headers = request.getHeaderNames();
        while (headers.hasMoreElements()) {
            String key = headers.nextElement();
            forward.put(key, request.getHeader(key));
        }
        int total = 10;
        List<EventEnvelope> parallelEvents = new ArrayList<>();
        for (int i=0; i < total; i++) {
            EventEnvelope event = new EventEnvelope();
            event.setTo("hello.world");
            event.setBody(forward);
            event.setHeader("request", "#"+(i+1));
            parallelEvents.add(event);
        }
        return Mono.create(callback -> {
            po.asyncRequest(parallelEvents, 3000)
                .onSuccess(events -> {
                    Map<String, Object> results = new HashMap<>();
                    int n = 0;
                    for (EventEnvelope evt: events) {
                        n++;
                        Map<String, Object> singleResult = new HashMap<>();
                        singleResult.put("status", evt.getStatus());
                        singleResult.put("headers", evt.getHeaders());
                        singleResult.put("body", evt.getBody());
                        singleResult.put("seq", evt.getCorrelationId());
                        singleResult.put("execution_time", evt.getExecutionTime());
                        singleResult.put("round_trip", evt.getRoundTrip());
                        results.put("result-"+n, singleResult);
                    }
                    callback.success(results);
                })
                .onFailure(ex -> callback.error(new AppException(408, ex.getMessage())));
        });
    }
}
