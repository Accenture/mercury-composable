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

package com.accenture.services;

import org.platformlambda.core.annotations.PreLoad;
import org.platformlambda.core.models.TypedLambdaFunction;
import org.platformlambda.core.serializers.SimpleMapper;
import org.platformlambda.core.util.Utility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * This is just a sample state resolver.
 * <p>
 * For production, you must implement your own resolver to persist the state of a scheduled job.
 * For example, persist the job status into a database or a distributed cache.
 * <p>
 * API contract:
 * 1. start:
 *      header (type = start, name = job_name, schedule = job_cron_statement,
 *              service = task where task is a function route or a flow name)
 *      body (job_parameters)
 *      return true
 * <p>
 * 2. end:
 *      header (type = end, name = job_name)
 *      body (not used)
 *      return true
 * <p>
 * 2. expires:
 *      header (type = expires, name = job_name)
 *      return true or false
 */
@PreLoad(route="v1.state.resolver")
public class StateResolver implements TypedLambdaFunction<Map<String, Object>, Boolean> {
    private static final Logger log = LoggerFactory.getLogger(StateResolver.class);
    private static final Utility util = Utility.getInstance();
    private static final String TEMP_FOLDER = "/tmp/scheduler-states";
    private static final String TYPE = "type";
    private static final String EXPIRES = "expires";
    private static final String NAME = "name";
    private static final String SERVICE = "service";
    private static final String PARAMS = "parameters";
    private static final String SCHEDULE = "schedule";
    private static final String START = "start";
    private static final String END = "end";
    private static final String ELAPSED = "elapsed";
    private static final String NONE = "none";
    private static final long TEN_SECONDS = 10 * 1000;

    public StateResolver() {
        File dir = new File(TEMP_FOLDER);
        if (!dir.exists()) {
            boolean created = dir.mkdirs();
            if (created) {
                log.info("{} created", dir);
            }
        }
    }

    @Override
    public Boolean handleEvent(Map<String, String> headers, Map<String, Object> input, int instance) {
        var type = headers.get(TYPE);
        var name = headers.getOrDefault(NAME, "none");
        var service = headers.getOrDefault(SERVICE, "none");
        var schedule = headers.get(SCHEDULE);
        if (START.equals(type) && !name.equals(NONE) && !service.equals(NONE)) {
            // startTask(String name, String service, String schedule, Map<String, Object> input
            startTask(name, service, schedule, input);
            return true;
        }
        if (END.equals(type) && !name.equals(NONE)) {
            endTask(name);
            return true;
        }
        if (EXPIRES.equals(type) && !name.equals(NONE)) {
            return checkExpiry(name);
        }
        throw new IllegalArgumentException("type must be start, end or expires");
    }

    private void startTask(String name, String service, String schedule, Map<String, Object> input) {
        var mapper = SimpleMapper.getInstance().getMapper();
        // create status record with start time
        Map<String, Object> map = new HashMap<>();
        map.put(NAME, name);
        map.put(SERVICE, service);
        map.put(START, new Date());
        if (input != null) {
            map.put(PARAMS, input);
        }
        if (schedule != null) {
            map.put(SCHEDULE, schedule);
        }
        var json = mapper.writeValueAsString(map);
        File f = new File(TEMP_FOLDER, name);
        util.str2file(f, json);
    }

    @SuppressWarnings("unchecked")
    private void endTask(String name) {
        var mapper = SimpleMapper.getInstance().getMapper();
        File f = new File(TEMP_FOLDER, name);
        if (f.exists()) {
            // update status record with end time
            var json = util.file2str(f);
            var map = mapper.readValue(json, Map.class);
            var now = System.currentTimeMillis();
            var start = util.str2date(String.valueOf(map.get(START)));
            var milliseconds = now - start.getTime();
            map.put(END, new Date(now));
            map.put(ELAPSED, util.elapsedTime(milliseconds));
            util.str2file(f, mapper.writeValueAsString(map));
        }
    }

    private Boolean checkExpiry(String name) {
        var mapper = SimpleMapper.getInstance().getMapper();
        File f = new File(TEMP_FOLDER, name);
        if (f.exists()) {
            var json = util.file2str(f);
            var map = mapper.readValue(json, Map.class);
            if (map.containsKey(NAME) && map.containsKey(SERVICE) && map.containsKey(START)) {
                var now = System.currentTimeMillis();
                var start = util.str2date(String.valueOf(map.get(START)));
                boolean expired = now - start.getTime() > TEN_SECONDS;
                if (expired) {
                    log.info("{} is good to go because previous iteration has expired", name);
                }
                return expired;
            }
        } else {
            log.info("{} is good to go", name);
        }
        return true;
    }
}
