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

package org.platformlambda.flow.services;

import org.platformlambda.core.annotations.PreLoad;
import org.platformlambda.core.models.TypedLambdaFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

@PreLoad(route="v1.csv.processor")
public class DemoCsvProcessor implements TypedLambdaFunction<Map<String, Object>, Object> {
    private static final Logger log = LoggerFactory.getLogger(DemoCsvProcessor.class);

    @Override
    public Object handleEvent(Map<String, String> headers, Map<String, Object> input, int instance) {
        String filename = headers.get("filename");
        String row = headers.get("row");
        if (filename != null && row != null) {
            log.info("Received row-{} of file '{}' - {}", row, filename, input);
        }
        return "done";
    }
}
