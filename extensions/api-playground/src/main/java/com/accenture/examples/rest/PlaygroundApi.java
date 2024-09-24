/*

    Copyright 2018-2024 Accenture Technology

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

package com.accenture.examples.rest;

import org.platformlambda.core.exception.AppException;
import org.platformlambda.core.util.AppConfigReader;
import org.platformlambda.core.util.Utility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.io.File;
import java.util.*;

@RestController
public class PlaygroundApi {
    private static final Logger log = LoggerFactory.getLogger(PlaygroundApi.class);

    private static final String YAML = ".yaml";
    private static final String JSON = ".json";
    private static final String TOTAL = "total";
    private static final String LIST = "list";
    private static final String TIME = "time";
    private static final String FILE_PREFIX = "file:";
    private static final String CLASSPATH_PREFIX = "classpath:";
    private static final String API_PLAYGROUND_APPS = "api.playground.apps";
    private static boolean ready = false;
    private static File dir;

    public PlaygroundApi() {
        if (dir == null) {
            AppConfigReader config = AppConfigReader.getInstance();
            String location = config.getProperty(API_PLAYGROUND_APPS, "/tmp/api-playground");
            if (location.startsWith(FILE_PREFIX)) {
                location = location.substring(FILE_PREFIX.length());
            }
            if (location.startsWith(CLASSPATH_PREFIX)) {
                log.error("{} must be a folder in the local file system", API_PLAYGROUND_APPS);
            }
            File f = new File(location);
            if (f.exists() && f.isDirectory()) {
                dir = f;
                ready = true;
            } else {
                log.error("{} contains invalid file path {}", API_PLAYGROUND_APPS, f);
            }
        }
    }
    @GetMapping(value = "/api/specs", produces = {"application/json"})
    public Mono<ResponseEntity<Map<String, Object>>> listFiles() {
        return Mono.create(callback -> {
            if (!ready) {
                callback.error(new AppException(503, "API playground not ready"));
            } else {
                Map<String, Object> result = new HashMap<>();
                List<String> fileList = new ArrayList<>();
                File[] files = dir.listFiles();
                if (files != null) {
                    for (File f : files) {
                        if (f.getName().endsWith(YAML) || f.getName().endsWith(JSON)) {
                            fileList.add(f.getName());
                        }
                    }
                    if (fileList.size() > 1) {
                        Collections.sort(fileList);
                    }
                    result.put(TOTAL, fileList.size());
                    result.put(TIME, new Date());
                    result.put(LIST, fileList);
                    callback.success(ResponseEntity.status(200).body(result));
                }
            }
        });
    }

    @GetMapping(value = "/api/specs/{id}", produces = {"text/plain"})
    public Mono<ResponseEntity<String>> getSpecs(@PathVariable("id") String id) {
        return Mono.create(callback -> {
            if (!ready) {
                callback.error(new AppException(503, "API playground not ready"));
            } else {
                String path = safePath(id);
                if (path == null) {
                    callback.error(new AppException(503, "Path parameter 'id' must not contain path traversal text"));
                } else {
                    File f = new File(dir, path);
                    if (f.exists()) {
                        callback.success(ResponseEntity.status(200).body(Utility.getInstance().file2str(f)));
                    } else {
                        callback.error(new AppException(404, "File not found"));
                    }
                }
            }
        });
    }

    private String safePath(String path) {
        return path.startsWith("../") || path.contains("/../")? null : path;
    }

}
