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

package org.platformlambda.core.mock;

import org.platformlambda.core.annotations.PreLoad;
import org.platformlambda.core.models.AsyncHttpRequest;
import org.platformlambda.core.models.EventEnvelope;
import org.platformlambda.core.models.TypedLambdaFunction;
import org.platformlambda.core.system.PostOffice;

import java.util.HashMap;
import java.util.Map;

/**
 * Simulates "application A" in an application-to-application HTTP call: a traced function that
 * invokes another REST endpoint through the "async.http.request" HTTP client. The downstream
 * endpoint ("/api/legacy/probe") echoes the trace id it continued under, so a test can assert
 * end-to-end distributed-trace continuity across the HTTP boundary.
 */
@PreLoad(route = "downstream.caller", instances = 10)
public class DownstreamCaller implements TypedLambdaFunction<AsyncHttpRequest, Object> {
    private static final long RPC_TIMEOUT = 10000;

    @Override
    public Object handleEvent(Map<String, String> headers, AsyncHttpRequest input, int instance) throws Exception {
        var po = new PostOffice(headers, instance);
        AsyncHttpRequest req = new AsyncHttpRequest();
        req.setMethod("GET").setHeader("accept", "application/json");
        req.setUrl("/api/legacy/probe").setTargetHost("http://127.0.0.1:" + input.getQueryParameter("port"));
        EventEnvelope res = po.eRequest(new EventEnvelope().setTo("async.http.request").setBody(req),
                                        RPC_TIMEOUT).get();
        Map<String, Object> result = new HashMap<>();
        if (res.getBody() instanceof Map<?, ?> probe) {
            probe.forEach((k, v) -> result.put("probe_" + k, v));
        }
        // this function's own trace context, adopted at this app's HTTP ingress
        result.put("caller_trace_id", po.getTraceId());
        return result;
    }
}
