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

package org.platformlambda.core.mock;

import org.platformlambda.core.annotations.PreLoad;
import org.platformlambda.core.models.EventEnvelope;
import org.platformlambda.core.models.TypedLambdaFunction;
import org.platformlambda.core.system.Platform;
import org.platformlambda.core.system.PostOffice;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

@PreLoad(route="hello.level.1", instances=10)
public class MultiLevelTrace implements TypedLambdaFunction<EventEnvelope, Object> {
    @Override
    public Object handleEvent(Map<String, String> headers, EventEnvelope input, int instance) throws Exception {
        var po = new PostOffice(headers, instance);
        var result = new HashMap<String, Object>();
        result.put("headers", headers);
        result.put("body", input.getBody());
        result.put("instance", instance);
        result.put("origin", Platform.getInstance().getOrigin());
        result.put("route_one", po.getRoute());
        result.put("trace_id", po.getTraceId());
        result.put("trace_path", po.getTracePath());
        // annotate trace
        po.annotateTrace("some_key", "some value");
        po.annotateTrace("another_key", "another_value");
        var request = new EventEnvelope().setTo("hello.level.2").setBody("test");
        return Mono.create(callback ->
                po.asyncRequest(request, 5000)
                    .onSuccess(res -> {
                        result.put("level-2", res.getBody());
                        callback.success(new EventEnvelope().setBody(result));
                    }));
    }
}
