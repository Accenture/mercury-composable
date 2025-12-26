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

package com.accenture.tasks;

import org.platformlambda.core.annotations.PreLoad;
import org.platformlambda.core.models.TypedLambdaFunction;
import org.platformlambda.core.util.Utility;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@PreLoad(route="file.vault")
public class FileVaultReadWrite implements TypedLambdaFunction<Map<String, Object>, Map<String, Object>> {

    @Override
    public Map<String, Object> handleEvent(Map<String, String> headers, Map<String, Object> input, int instance) {
        Object text = input.get("text");
        Object binary = input.get("binary");
        Object json = input.get("json");
        Map<String, Object> result = new HashMap<>();
        result.put("json", json);
        result.put("list", List.of("hello", "world"));
        if (text instanceof String && binary instanceof byte[] b) {
            String str = Utility.getInstance().getUTF(b);
            if (str.equals(text)) {
                result.put("text", text);
                result.put("matched", true);
            } else {
                result.put("matched", false);
                result.put("text", "Input text and binary values do not match");
            }
            // set a test field with binary value
            result.put("binary", Utility.getInstance().getUTF("binary"));
        } else {
            result.put("error", "Input must be a map of text and binary key values");
        }
        Object textRes = input.get("text_resource");
        Object binaryRes = input.get("binary_resource");
        if (textRes instanceof String && binaryRes instanceof byte[] b) {
            String str = Utility.getInstance().getUTF(b);
            if (str.equals(textRes)) {
                result.put("text_resource", textRes);
                result.put("matched", true);
            } else {
                result.put("matched", false);
                result.put("text_resource", "Input text and binary values do not match");
            }
        } else {
            result.put("error", "Input must be a map of text and binary key values");
        }
        return result;
    }
}
