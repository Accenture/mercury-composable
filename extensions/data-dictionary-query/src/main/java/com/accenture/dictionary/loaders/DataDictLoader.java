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

package com.accenture.dictionary.loaders;

import com.accenture.dictionary.models.DataDictionary;
import org.platformlambda.core.util.ConfigReader;

import java.util.List;

public class DataDictLoader {
    private static final String PROTOCOL_INDICATOR = "://";
    private static final DataDictLoader INSTANCE = new DataDictLoader();

    private DataDictLoader() {
        // singleton
    }

    public static DataDictLoader getInstance() {
        return INSTANCE;
    }

    public DataDictionary loadDataDict(String dataId,  ConfigReader config) {
        var id = config.getProperty("dictionary.id", "");
        var target = config.getProperty("dictionary.target", "");
        var input = config.get("dictionary.input");
        var output = config.get("dictionary.output");
        if (!id.isEmpty() && target.contains(PROTOCOL_INDICATOR) &&
                input instanceof List<?> inputList && output instanceof List<?> outputList &&
                !inputList.isEmpty() && !outputList.isEmpty()) {
            validateProtocol(id, target);
            var dataDict = new DataDictionary(id, target);
            inputList.forEach(d -> dataDict.addInput(String.valueOf(d)));
            outputList.forEach(d -> dataDict.addOutput(String.valueOf(d)));
            return dataDict;
        } else {
            throw new IllegalArgumentException("Invalid syntax in "+dataId+
                        " - check dictionary.id, target, input, and output");
        }
    }

    private void validateProtocol(String id, String target) {
        int sep = target.indexOf(PROTOCOL_INDICATOR);
        if (sep != -1) {
            var protocol = target.substring(0, sep).trim();
            var service = target.substring(sep + PROTOCOL_INDICATOR.length()).trim();
            if (!protocol.isEmpty() && !service.isEmpty()) {
                return;
            }
        }
        throw new IllegalArgumentException("Invalid target '"+target+"' in data dictionary item "+id);
    }
}
