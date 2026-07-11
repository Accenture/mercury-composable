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
import org.platformlambda.core.models.TypedLambdaFunction;
import org.platformlambda.core.system.PostOffice;

import java.util.HashMap;
import java.util.Map;

/**
 * Test probe for the REST automation header-name overrides: echoes the resolved trace-id and business
 * correlation-id as observed by the target function, so a test can assert that a per-endpoint
 * 'trace.id.header' / 'correlation.id.header' override in rest.yaml captured the caller's custom headers.
 */
@PreLoad(route = "header.probe", instances = 10)
public class HeaderProbe implements TypedLambdaFunction<AsyncHttpRequest, Object> {

    @Override
    public Object handleEvent(Map<String, String> headers, AsyncHttpRequest input, int instance) {
        var po = PostOffice.trackable(headers, instance);
        Map<String, Object> result = new HashMap<>();
        result.put("traceId", po.getTraceId());
        result.put("cid", po.getMyCorrelationId());
        return result;
    }
}
