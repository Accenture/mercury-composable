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
import org.platformlambda.core.models.LambdaFunction;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Lists the OpenAPI files bundled under resources/sample/yaml. The files are enumerated with a
 * classpath pattern because a classpath directory has no listing inside a packaged jar - reading
 * {@code /sample/yaml} as a stream works from an IDE's exploded classpath and returns nothing
 * from the executable jar.
 */
@PreLoad(route = "v1.list.swagger.files", instances=10)
public class CountSwaggerFiles implements LambdaFunction {

    private static final String SWAGGER_FILES = "classpath*:sample/yaml/*";

    @Override
    public Object handleEvent(Map<String, String> headers, Object input, int instance) throws IOException {
        Resource[] resources = new PathMatchingResourcePatternResolver().getResources(SWAGGER_FILES);
        List<String> fileList = new ArrayList<>();
        for (Resource resource : resources) {
            String filename = resource.getFilename();
            if (filename != null && !filename.isEmpty() && resource.isReadable()) {
                fileList.add(filename);
            }
        }
        if (fileList.isEmpty()) {
            throw new IllegalArgumentException("Missing swagger files in resources/sample/yaml");
        }
        Collections.sort(fileList);
        Map<String, Object> result = new HashMap<>();
        result.put("time", new Date());
        result.put("total", fileList.size());
        result.put("list", fileList);
        return result;
    }
}
