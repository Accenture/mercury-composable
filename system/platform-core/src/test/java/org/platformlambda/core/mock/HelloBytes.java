/*

    Copyright 2018-2024 Accenture Technology

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
import org.platformlambda.core.system.EventPublisher;
import org.platformlambda.core.util.Utility;

import java.util.Map;

@PreLoad(route="hello.bytes")
public class HelloBytes implements TypedLambdaFunction<AsyncHttpRequest, EventEnvelope> {
    private static final String APPLICATION_OCTET_STREAM = "application/octet-stream";

    private static final String CONTENT_TYPE = "content-type";
    private static final String X_STREAM_ID = "x-stream-id";
    private static final String X_TTL = "x-ttl";
    private static final long EXPIRY = 10000;

    @Override
    public EventEnvelope handleEvent(Map<String, String> headers, AsyncHttpRequest input, int instance) {
        EventPublisher publisher = new EventPublisher(EXPIRY);
        publisher.publish(Utility.getInstance().getUTF("hello world 0123456789"));
        publisher.publishCompletion();
        return new EventEnvelope().setHeader(X_STREAM_ID, publisher.getStreamId())
                .setHeader(X_TTL, EXPIRY).setHeader(CONTENT_TYPE, APPLICATION_OCTET_STREAM);
    }
}
