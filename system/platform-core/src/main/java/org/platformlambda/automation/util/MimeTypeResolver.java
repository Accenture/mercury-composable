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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class MimeTypeResolver {
    private static final Logger log = LoggerFactory.getLogger(MimeTypeResolver.class);

    private static final String CONFIG_FILE = "classpath:/mime-types.yml";
    private static final ConcurrentMap<String, String> mimeTypes = new ConcurrentHashMap<>();
    private static final MimeTypeResolver instance = new MimeTypeResolver();
    private boolean initialized = false;

    @SuppressWarnings("unchecked")
    private MimeTypeResolver() {
        AppConfigReader config = AppConfigReader.getInstance();
        try {
            ConfigReader reader = new ConfigReader(CONFIG_FILE);
            Object types = reader.get("mime.types");
            if (types instanceof Map) {
                loadTypes((Map<String, Object>) types);
            } else if (types != null) {
                log.error("Unable to parse {} - mime.types should be a Map of key-values", CONFIG_FILE);
            }
        } catch (IllegalArgumentException e) {
            log.error("Unable to load mime-types.yml - {}", e.getMessage());
        }
        Object mTypes = config.get("mime.types");
        if (mTypes instanceof Map) {
            loadTypes((Map<String, Object>) mTypes);
        } else if (mTypes != null) {
            log.error("Unable to parse config - mime.types should be a Map of key-value");
        }
        // guarantee essential extensions are supported
        mimeTypes.put("json", "application/json");
        mimeTypes.put("xml", "application/xml");
        mimeTypes.put("htm", "text/html");
        mimeTypes.put("html", "text/html");
        mimeTypes.put("txt", "text/plain");
        mimeTypes.put("js", "text/javascript");
        mimeTypes.put("css", "text/css");
    }

    private void loadTypes(Map<String, Object> types) {
        types.forEach((k, v) -> mimeTypes.put(k.toLowerCase(), v.toString().toLowerCase()));
    }

    public static MimeTypeResolver getInstance() {
        return instance;
    }

    public String getMimeType(String ext) {
        return mimeTypes.get(ext);
    }

    public void init() {
        if (!initialized) {
            initialized = true;
            if (!mimeTypes.isEmpty()) {
                log.info("Loaded {} mime-types", mimeTypes.size());
            }
        }
    }
}
