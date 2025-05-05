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

package org.platformlambda.automation.util;

import org.platformlambda.core.util.AppConfigReader;
import org.platformlambda.core.util.ConfigReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class CustomContentTypeResolver {
    private static final Logger log = LoggerFactory.getLogger(CustomContentTypeResolver.class);

    private static final String CONFIG_FILE = "classpath:/custom-content-types.yml";
    private static final ConcurrentMap<String, String> customContentTypes = new ConcurrentHashMap<>();
    private static final CustomContentTypeResolver instance = new CustomContentTypeResolver();
    private boolean initialized = false;

    public static CustomContentTypeResolver getInstance() {
        return instance;
    }

    @SuppressWarnings("unchecked")
    private CustomContentTypeResolver() {
        AppConfigReader config = AppConfigReader.getInstance();
        try {
            ConfigReader reader = new ConfigReader(CONFIG_FILE);
            Object types = reader.get("custom.content.types");
            if (types instanceof List) {
                loadTypes((List<Object>) types);
            } else if (types != null) {
                log.error("Unable to parse {} - custom.content.types should be a List of type mappings", CONFIG_FILE);
            }
        } catch (IOException e) {
            // ok to ignore because custom content type feature is optional
        }
        Object ct = config.get("custom.content.types");
        if (ct instanceof List) {
            loadTypes((List<Object>) ct);
        } else if (ct != null) {
            log.error("Unable to parse config - custom.content.types should be a List of type mappings");
        }
    }

    private void loadTypes(List<Object> list) {
        list.forEach(entry -> {
            String line = String.valueOf(entry);
            int sep = line.lastIndexOf("->");
            if (sep == -1) {
                log.error("Unable to parse content-type entry '{}' - syntax 'x -> y'", line);
            } else {
                String k = line.substring(0, sep).trim();
                String v = line.substring(sep + 2).trim();
                if (v.isEmpty()) {
                    log.error("Unable to parse content-type entry '{}' - missing content-type", line);
                } else {
                    customContentTypes.put(k.toLowerCase(), v.toLowerCase());
                }
            }
        });
    }

    public String getContentType(String contentType) {
        if (contentType != null) {
            int sep = contentType.indexOf(';');
            String ct = sep == -1? contentType.trim() : contentType.substring(0, sep).trim();
            String customType = customContentTypes.get(ct.toLowerCase());
            return customType != null? customType : ct;
        } else {
            return null;
        }
    }

    public void init() {
        if (!initialized) {
            initialized = true;
            if (!customContentTypes.isEmpty()) {
                log.info("Loaded {} custom content-types", customContentTypes.size());
            }
        }
    }
}
