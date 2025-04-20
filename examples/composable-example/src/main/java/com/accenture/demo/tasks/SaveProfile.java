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

package com.accenture.demo.tasks;

import org.platformlambda.core.annotations.PreLoad;
import org.platformlambda.core.models.TypedLambdaFunction;
import org.platformlambda.core.serializers.SimpleMapper;
import org.platformlambda.core.util.MultiLevelMap;
import org.platformlambda.core.util.Utility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.List;
import java.util.Map;

@PreLoad(route="v1.save.profile", instances=10)
public class SaveProfile implements TypedLambdaFunction<Map<String, Object>, Void> {
    private static final Logger log = LoggerFactory.getLogger(SaveProfile.class);
    private static final  Utility util = Utility.getInstance();
    private static final String TEMP_DATA_STORE = "/tmp/store";
    private static final String JSON_EXT = ".json";
    private static final String REQUIRED_FIELDS = "required_fields";

    /**
     * To make this function generic, we use Map as input instead of the Profile class.
     *
     * @param headers containing the required_fields parameter
     * @param input dataset
     * @param instance of this function
     * @return nothing
     */
    @Override
    public Void handleEvent(Map<String, String> headers, Map<String, Object> input, int instance) {
        if (!input.containsKey("id")) {
            throw new IllegalArgumentException("Missing id in profile");
        }
        String requiredFields = headers.get(REQUIRED_FIELDS);
        if (requiredFields == null) {
            throw new IllegalArgumentException("Missing required_fields");
        }
        var dataset = new MultiLevelMap(input);
        List<String> fields = util.split(requiredFields, ", ");
        for (String f: fields) {
            if (!dataset.exists(f)) {
                throw new IllegalArgumentException("Missing " + f);
            }
        }
        // save only fields that are in the interface contract
        var filtered = new MultiLevelMap();
        for (String f: fields) {
            filtered.setElement(f, dataset.getElement(f));
        }
        var mapper = SimpleMapper.getInstance().getMapper();
        String json = mapper.writeValueAsString(filtered.getMap());
        File folder = new File(TEMP_DATA_STORE);
        if (!folder.exists() && folder.mkdirs()) {
            log.info("Temporary key folder {} created", folder);
        }
        String id = String.valueOf(input.get("id"));
        File file = new File(folder, id+JSON_EXT);
        util.str2file(file, json);
        log.info("Profile {} saved", id);
        // this task does not have any output
        return null;
    }
}
