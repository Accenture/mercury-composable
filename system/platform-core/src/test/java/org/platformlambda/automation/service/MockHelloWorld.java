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

package org.platformlambda.automation.service;

import org.platformlambda.core.annotations.KernelThreadRunner;
import org.platformlambda.core.models.AsyncHttpRequest;
import org.platformlambda.core.models.EventEnvelope;
import org.platformlambda.core.models.TypedLambdaFunction;
import org.platformlambda.core.serializers.SimpleMapper;

import java.io.IOException;
import java.util.Map;

/**
 * KernelThreadRunner is added for demonstration purpose only.
 * <p>
 * Normally KernelThread should be reserved for computational intensive functions
 * or legacy code that cannot be refactored into non-blocking operation.
 */
@KernelThreadRunner
public class MockHelloWorld implements TypedLambdaFunction<AsyncHttpRequest, Object> {
    private static final String X_STREAM_ID = "x-stream-id";
    private static final String X_TTL = "x-ttl";
    private static final String CONTENT_TYPE = "content-type";
    private static final String CUSTOM_JSON = "application/vnd.my.org-v2.1+json; charset=utf-8";
    private static final String APPLICATION_XML = "application/xml";

    @Override
    public Object handleEvent(Map<String, String> headers, AsyncHttpRequest input, int instance) throws IOException {
        if ("HEAD".equals(input.getMethod())) {
            var cookies = input.getCookies();
            EventEnvelope result = new EventEnvelope().setHeader("X-Response", "HEAD request received")
                                        .setHeader("Content-Length", 100);
            if (!cookies.isEmpty()) {
                result.setHeader("x-cookies", SimpleMapper.getInstance().getCompactGson().toJson(cookies));
            }
            if (input.getHeader("x-hello") != null) {
                result.setHeader("x-hello", input.getHeader("x-hello"));
            }
            // set multiple cookies
            result.setHeader("Set-Cookie", "first=cookie");
            result.setHeader("Set-Cookie", "second=one");
            return result;
        }
        if (input.getStreamRoute() != null) {
            var result = new EventEnvelope().setBody(input.getBody())
                                .setHeader("content-type", "application/octet-stream");

            String streamId = input.getHeader(X_STREAM_ID);
            String ttl = input.getHeader(X_TTL);
            if (streamId != null && ttl != null) {
                result.setHeader(X_STREAM_ID, streamId).setHeader(X_TTL, ttl);
            }
            return result;
        } else if (input.getBody() instanceof byte[]) {
            return new EventEnvelope().setBody(input.getBody())
                    .setHeader("content-type", "application/octet-stream");
        } else {
            if (APPLICATION_XML.equals(input.getHeader(CONTENT_TYPE))) {
                return input.toMap();
            } else {
                return new EventEnvelope().setHeader(CONTENT_TYPE, CUSTOM_JSON).setBody(input.toMap());
            }
        }
    }
}
