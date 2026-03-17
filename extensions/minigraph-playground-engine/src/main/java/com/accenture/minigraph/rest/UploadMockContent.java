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

package com.accenture.minigraph.rest;

import com.accenture.minigraph.services.GraphCommandService;
import org.platformlambda.core.annotations.PreLoad;
import org.platformlambda.core.models.AsyncHttpRequest;
import org.platformlambda.core.models.EventEnvelope;
import org.platformlambda.core.models.TypedLambdaFunction;

import java.util.List;
import java.util.Map;

@PreLoad(route = "upload.mock.content", instances = 10)
public class UploadMockContent implements TypedLambdaFunction<AsyncHttpRequest, Object> {

    @Override
    public Object handleEvent(Map<String, String> headers, AsyncHttpRequest input, int instance) {
        var id = input.getPathParameter("id");
        if (input.getBody() instanceof Map || input.getBody() instanceof List) {
            if (GraphCommandService.uploadContent(id, input.getBody())) {
                return new EventEnvelope().setHeader("Content-Type", "application/json")
                        .setBody(Map.of("message", "Content uploaded", "type", "upload"));
            } else {
                throw new IllegalArgumentException("Session "+id+" is expired or invalid");
            }
        }
        throw new IllegalArgumentException("Input is not a valid JSON payload that represents a Map or List");
    }
}
