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

package com.accenture.dictionary.mock;

import org.platformlambda.core.annotations.PreLoad;
import org.platformlambda.core.models.AsyncHttpRequest;
import org.platformlambda.core.models.TypedLambdaFunction;
import org.platformlambda.core.util.ConfigReader;

import java.util.Map;

@PreLoad(route = "mock.mdm.profile", instances = 50)
public class MdmProfile implements TypedLambdaFunction<AsyncHttpRequest, Object> {

    @Override
    public Object handleEvent(Map<String, String> headers, AsyncHttpRequest input, int instance) {
        final String personId;
        if (input.getMethod().equals("POST")) {
            if (input.getBody() instanceof Map<?, ?> map && map.containsKey("person_id")) {
                personId = String.valueOf(map.get("person_id"));
            } else {
                personId = null;
            }
        } else {
            personId = input.getPathParameter("id");
        }
        if (personId == null) {
            throw new IllegalArgumentException("Missing person id");
        }
        var data = new ConfigReader("classpath:/mock/profile-"+personId+".json");
        return data.getMap();
    }
}
