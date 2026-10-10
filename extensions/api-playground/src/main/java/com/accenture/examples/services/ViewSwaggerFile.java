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

package com.accenture.examples.services;

import org.platformlambda.core.annotations.PreLoad;
import org.platformlambda.core.exception.AppException;
import org.platformlambda.core.models.AsyncHttpRequest;
import org.platformlambda.core.models.EventEnvelope;
import org.platformlambda.core.models.TypedLambdaFunction;

import java.io.IOException;
import java.util.Map;

/**
 * Serves one OpenAPI document by name - the bundled example demo.yaml or a file of the optional
 * {@code api.playground.apps} folder - as text with a content type Swagger UI accepts.
 */
@PreLoad(route = "v1.view.swagger.file", instances=10)
public class ViewSwaggerFile implements TypedLambdaFunction<AsyncHttpRequest, EventEnvelope> {

    private static final String CONTENT_TYPE = "Content-Type";

    @Override
    public EventEnvelope handleEvent(Map<String, String> headers, AsyncHttpRequest input, int instance)
            throws AppException, IOException {
        String filename = input.getPathParameter("filename");
        if (filename == null) {
            throw new IllegalArgumentException("Missing filename in path parameter");
        }
        SpecFiles files = SpecFiles.getInstance();
        String content = files.read(filename);
        if (content == null) {
            throw new AppException(404, "File not found");
        }
        return new EventEnvelope().setHeader(CONTENT_TYPE, files.contentType(filename)).setBody(content);
    }
}
