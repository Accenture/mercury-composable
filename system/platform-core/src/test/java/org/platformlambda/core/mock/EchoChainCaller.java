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

import org.platformlambda.core.models.AsyncHttpRequest;
import org.platformlambda.core.models.EventEnvelope;
import org.platformlambda.core.models.TypedLambdaFunction;
import org.platformlambda.core.annotations.PreLoad;
import org.platformlambda.core.system.PostOffice;

import java.util.Map;

/**
 * A traced function that calls the "/api/hello/world" echo endpoint through the "async.http.request"
 * HTTP client and returns the echo's view of the request it received. Because the echo reflects the
 * raw HTTP headers, a test can assert exactly what AsyncHttpClient stamped on the outgoing call -
 * e.g. that the W3C trace context travels under BOTH the standard "traceparent" and a custom
 * http.traceparent.header name.
 */
@PreLoad(route = "echo.chain.caller", instances = 10)
public class EchoChainCaller implements TypedLambdaFunction<AsyncHttpRequest, Object> {
    private static final long RPC_TIMEOUT = 10000;

    @Override
    public Object handleEvent(Map<String, String> headers, AsyncHttpRequest input, int instance) throws Exception {
        var po = new PostOffice(headers, instance);
        AsyncHttpRequest req = new AsyncHttpRequest();
        req.setMethod("GET").setHeader("accept", "application/json");
        req.setUrl("/api/hello/world").setTargetHost("http://127.0.0.1:" + input.getQueryParameter("port"));
        EventEnvelope res = po.eRequest(new EventEnvelope().setTo("async.http.request").setBody(req),
                                        RPC_TIMEOUT).get();
        return res.getBody();
    }
}
