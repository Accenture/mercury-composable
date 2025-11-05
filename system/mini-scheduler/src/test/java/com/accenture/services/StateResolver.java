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
 * This is a sample state resolver. You must implement your own resolver to persist
 * the state of a scheduled job. For example, persist into a database or a distributed cache.
 * <p>
 * API contract:
 * 1. save:
 *      header (type = save), payload = a map of job_name, service
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
    private static final String SAVE = "save";
    private static final String EXPIRES = "expires";
    private static final String NAME = "name";
    private static final String SERVICE = "service";
    private static final String START = "start";
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
        var mapper = SimpleMapper.getInstance().getMapper();
        if (SAVE.equals(type) && input.containsKey(NAME) && input.containsKey(SERVICE)) {
            var name = String.valueOf(input.get(NAME));
            var service = String.valueOf(input.get(SERVICE));
            Map<String, Object> map = new HashMap<>();
            map.put(NAME, name);
            map.put(SERVICE, service);
            map.put(START, new Date());
            var json = mapper.writeValueAsString(map);
            File f = new File(TEMP_FOLDER, name);
            util.str2file(f, json);
            return true;
        }
        if (EXPIRES.equals(type) && headers.containsKey(NAME)) {
            var name = headers.get(NAME);
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
        throw new IllegalArgumentException("type must be save or exists");
    }
}
