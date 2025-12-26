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

package org.platformlambda.quartz.rest;

import org.platformlambda.core.annotations.PreLoad;
import org.platformlambda.core.models.AsyncHttpRequest;
import org.platformlambda.core.models.EventEnvelope;
import org.platformlambda.core.models.TypedLambdaFunction;
import org.platformlambda.core.serializers.SimpleMapper;
import org.platformlambda.core.system.PostOffice;
import org.platformlambda.core.util.Utility;
import org.platformlambda.scheduler.JobLoader;
import org.platformlambda.scheduler.models.ScheduledJob;
import org.platformlambda.scheduler.services.JobExecutor;

import java.io.File;
import java.util.*;

@PreLoad(route="v1.schedule.admin", instances=10)
public class ScheduleAdmin implements TypedLambdaFunction<AsyncHttpRequest, EventEnvelope> {
    private static final Utility util = Utility.getInstance();
    private static final String CONTENT_TYPE = "content-type";
    private static final String APPLICATION_JSON = "application/json";
    private static final String TEMP = "/tmp/scheduler-states";
    private static final String START = "start";
    private static final String END = "end";
    private static final File TEMP_FOLDER = new File(TEMP);

    @Override
    public EventEnvelope handleEvent(Map<String, String> headers, AsyncHttpRequest input, int instance) {
        var name = input.getPathParameter("name");
        if (name == null) {
            return getJobList();
        } else {
            ScheduledJob job = JobLoader.getJob(name);
            if (job == null) {
                throw new IllegalArgumentException("Job name is invalid");
            }
            if ("POST".equals(input.getMethod())) {
                return runJob(new PostOffice(headers, instance), input);
            } else {
                File f = new File(TEMP_FOLDER, name);
                if (f.exists()) {
                    var result = getJob(util.file2str(f));
                    return new EventEnvelope().setBody(result).setHeader(CONTENT_TYPE, APPLICATION_JSON);
                } else {
                    throw new IllegalArgumentException("Job not found");
                }
            }
        }
    }

    private EventEnvelope getJobList() {
        List<Map<String, Object>> jobs = new ArrayList<>();
        // list all scheduled jobs
        File[] files = TEMP_FOLDER.listFiles();
        if (files != null) {
            for (File f : files) {
                var map = f.exists() ? getJobTime(util.file2str(f)) : new HashMap<String, Object>();
                map.put("name", f.getName());
                jobs.add(map);
            }
        }
        Map<String, Object> result = new HashMap<>();
        result.put("jobs", jobs);
        result.put("message", "GET /api/schedule/{name}");
        result.put("time", new Date());
        return new EventEnvelope().setBody(result).setHeader(CONTENT_TYPE, APPLICATION_JSON);
    }

    @SuppressWarnings("unchecked")
    private EventEnvelope runJob(PostOffice po, AsyncHttpRequest input) {
        if (input.getBody() instanceof Map) {
            var name = input.getPathParameter("name");
            var data = (Map<String, Object>) input.getBody();
            var operator = data.get("operator");
            if (operator != null) {
                po.send(new EventEnvelope().setTo(JobExecutor.JOB_EXECUTOR)
                            .setHeader("job", name).setHeader("operator", operator));
                Map<String, Object> result = new HashMap<>();
                result.put("job", name);
                result.put("message", "Job started");
                result.put("time", new Date());
                return new EventEnvelope().setBody(result).setHeader(CONTENT_TYPE, APPLICATION_JSON);
            }
        }
        throw new IllegalArgumentException("Missing 'operator' parameter in request payload");
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> getJob(String json) {
        return SimpleMapper.getInstance().getMapper().readValue(json, Map.class);
    }

    private Map<String, Object> getJobTime(String json) {
        var map = getJob(json);
        Map<String, Object> result = new HashMap<>();
        if (map.containsKey(START)) {
            result.put(START, map.get(START));
        }
        if (map.containsKey(END)) {
            result.put(END, map.get(END));
        }
        return result;
    }
}
