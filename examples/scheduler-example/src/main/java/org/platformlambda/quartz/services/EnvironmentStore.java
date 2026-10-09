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

package org.platformlambda.quartz.services;

import org.platformlambda.core.annotations.PreLoad;
import org.platformlambda.core.models.TypedLambdaFunction;
import org.platformlambda.core.serializers.SimpleMapper;
import org.platformlambda.core.util.Utility;
import org.platformlambda.scheduler.ActiveEnvironment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * This is just a sample store of the active environment (RFC-0008).
 * <p>
 * For production, you must implement your own store that persists the active environment name into
 * a data store shared by every instance of the site - a database such as PostgreSQL or MongoDB, or a
 * distributed cache - because Kubernetes restarts a pod without notice and an in-memory value would
 * reset to the {@code scheduler.active.environment} property. Keep the route name
 * {@code v1.environment.store}, or set {@code scheduler.environment.store} to yours.
 * <p>
 * API contract:
 * 1. get:
 *      header (type = get)
 *      return the persisted environment name as text, or nothing when none was persisted yet
 * <p>
 * 2. set:
 *      header (type = set, environment = name, operator = who)
 *      return true
 */
// S5443 (publicly writable directory): /tmp/scheduler-environment IS the sample store by design -
// throwaway local data; a production store persists to a database or distributed cache
@SuppressWarnings("java:S5443")
@PreLoad(route = ActiveEnvironment.DEFAULT_STORE)
public class EnvironmentStore implements TypedLambdaFunction<Map<String, Object>, Object> {
    private static final Logger log = LoggerFactory.getLogger(EnvironmentStore.class);
    private static final Utility util = Utility.getInstance();
    private static final String TEMP_FOLDER = "/tmp/scheduler-environment";
    private static final String ACTIVE = "active";
    private static final String OPERATOR = "operator";
    private static final String TIME = "time";
    private static final File RECORD = new File(TEMP_FOLDER, ACTIVE);

    public EnvironmentStore() {
        File dir = new File(TEMP_FOLDER);
        if (!dir.exists() && dir.mkdirs()) {
            log.info("{} created", dir);
        }
    }

    @Override
    public Object handleEvent(Map<String, String> headers, Map<String, Object> input, int instance) {
        var type = headers.get(ActiveEnvironment.TYPE);
        if (ActiveEnvironment.GET.equals(type)) {
            return read();
        }
        if (ActiveEnvironment.SET.equals(type)) {
            var name = headers.get(ActiveEnvironment.ENVIRONMENT_HEADER);
            if (name == null || name.isBlank()) {
                throw new IllegalArgumentException("Missing environment header");
            }
            write(name.trim(), headers.getOrDefault(ActiveEnvironment.OPERATOR, "unknown"));
            return true;
        }
        throw new IllegalArgumentException("type must be get or set");
    }

    @SuppressWarnings("unchecked")
    private String read() {
        if (!RECORD.exists()) {
            return null;
        }
        var map = SimpleMapper.getInstance().getMapper().readValue(util.file2str(RECORD), Map.class);
        return map == null ? null : (String) map.get(ACTIVE);
    }

    private void write(String name, String operator) {
        Map<String, Object> record = new HashMap<>();
        record.put(ACTIVE, name);
        record.put(OPERATOR, operator);
        record.put(TIME, new Date());
        util.str2file(RECORD, SimpleMapper.getInstance().getMapper().writeValueAsString(record));
        log.info("Active environment {} persisted by {}", name, operator);
    }
}
