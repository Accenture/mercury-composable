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

package org.platformlambda.adapter.flow;

import org.platformlambda.adapter.flow.services.CsvFlowAdapter;
import org.platformlambda.core.annotations.MainApplication;
import org.platformlambda.core.models.EntryPoint;
import org.platformlambda.core.util.AppConfigReader;
import org.platformlambda.core.util.ConfigReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

@MainApplication
public class FlowAdapterLoader implements EntryPoint {
    private static final Logger log = LoggerFactory.getLogger(FlowAdapterLoader.class);

    private static final String CSV_FLOW_ADAPTER = "adapter.flow.csv";
    private static final String FILE_PROTOCOL = "file:/";

    @SuppressWarnings("unchecked")
    @Override
    public void start(String[] args) throws Exception {
        AppConfigReader config = AppConfigReader.getInstance();
        String yamlPath = config.getProperty("yaml.csv.flow.adapter", "classpath:/csv-flow-adapter.yml");
        ConfigReader reader = new ConfigReader(yamlPath);
        Object entries = reader.get(CSV_FLOW_ADAPTER);
        if (entries instanceof List) {
            List<Object> list = (List<Object>) entries;
            for (int i=0; i < list.size(); i++) {
                Object entry = reader.get(CSV_FLOW_ADAPTER + "[" + i +"]");
                if (entry instanceof Map) {
                    String staging = reader.getProperty(CSV_FLOW_ADAPTER + "[" + i +"].staging");
                    String archive = reader.getProperty(CSV_FLOW_ADAPTER + "[" + i +"].archive");
                    String flowId = reader.getProperty(CSV_FLOW_ADAPTER + "[" + i +"].flow");
                    if (staging != null && !staging.isEmpty() && archive != null && !archive.isEmpty() &&
                            flowId != null && !flowId.isEmpty()) {
                        if (staging.startsWith(FILE_PROTOCOL) && archive.startsWith(FILE_PROTOCOL)) {
                            CsvFlowAdapter adapter = new CsvFlowAdapter(staging, archive, flowId);
                            adapter.start();
                        } else {
                            log.warn("Skipping entry-{} - protocol {} not implemented", i+1, staging);
                        }
                    } else {
                        log.error("Skipping entry-{} - please check staging, archive and flow parameters", i+1);
                    }

                } else {
                    log.error("Skipping entry-{} because it is not a map", i+1);
                }
            }

        } else {
            log.error("Unable to start flow adapter - Config '{}' should be a list of maps", CSV_FLOW_ADAPTER);
        }
    }
}
