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

package com.accenture.dictionary.tasks;

import com.accenture.dictionary.base.DictionaryLambdaFunction;
import org.platformlambda.core.annotations.PreLoad;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

@PreLoad(route="v1.show.data.dict", instances=50)
public class ShowDataSpecs extends DictionaryLambdaFunction {
    private static final Logger log = LoggerFactory.getLogger(ShowDataSpecs.class);

    @Override
    public Object handleEvent(Map<String, String> headers, Map<String, Object> input, int instance) {
        var id = headers.get("data");
        if (id == null) {
            throw new IllegalArgumentException("Missing data dictionary ID in header");
        }
        log.info("Showing data dictionary {}", id);
        // load question specs
        var questionSpecs = getDataDict(id);
        if (questionSpecs == null) {
            throw new IllegalArgumentException("Data dictionary specs '" + id + "' not found");
        }
        return questionSpecs;
    }
}


