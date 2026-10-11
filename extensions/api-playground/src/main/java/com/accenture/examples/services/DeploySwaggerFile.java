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
import org.platformlambda.core.models.TypedLambdaFunction;
import org.platformlambda.core.serializers.SimpleMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Deploys a document into the specs folder: {@code PUT /api/specs/{filename}} with the document as
 * text. An existing file of that name answers 409 unless {@code ?overwrite=true}, so the page can
 * ask the user before replacing it. See {@link SpecFiles#write}.
 */
@PreLoad(route = "v1.deploy.swagger.file", instances=10)
public class DeploySwaggerFile implements TypedLambdaFunction<AsyncHttpRequest, Map<String, Object>> {

    @Override
    public Map<String, Object> handleEvent(Map<String, String> headers, AsyncHttpRequest input, int instance)
            throws AppException, IOException {
        String filename = input.getPathParameter("filename");
        if (filename == null) {
            throw new IllegalArgumentException("Missing filename in path parameter");
        }
        boolean overwrite = "true".equalsIgnoreCase(input.getQueryParameter("overwrite"));
        SpecFiles files = SpecFiles.getInstance();
        boolean replaced = files.write(filename, text(input.getBody()), overwrite);
        Map<String, Object> result = new HashMap<>();
        result.put("deployed", filename);
        result.put("overwritten", replaced);
        result.put("folder", files.folderPath());
        return result;
    }

    private static String text(Object body) {
        return switch (body) {
            case null -> null;
            case String s -> s;
            case byte[] b -> new String(b, StandardCharsets.UTF_8);
            case Map<?, ?> m -> SimpleMapper.getInstance().getMapper().writeValueAsString(m);
            case List<?> l -> SimpleMapper.getInstance().getMapper().writeValueAsString(l);
            default -> String.valueOf(body);
        };
    }
}
