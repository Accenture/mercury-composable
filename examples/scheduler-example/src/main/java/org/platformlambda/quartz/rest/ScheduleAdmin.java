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

import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Projections;
import org.bson.Document;
import org.platformlambda.core.annotations.PreLoad;
import org.platformlambda.core.models.AsyncHttpRequest;
import org.platformlambda.core.models.EventEnvelope;
import org.platformlambda.core.models.TypedLambdaFunction;
import org.platformlambda.core.system.PostOffice;
import org.platformlambda.core.util.Utility;
import org.platformlambda.quartz.db.SchedulerDatabase;
import org.platformlambda.scheduler.JobLoader;
import org.platformlambda.scheduler.models.ScheduledJob;
import org.platformlambda.scheduler.services.JobExecutor;

import java.util.*;

@PreLoad(route="v1.schedule.admin", instances=10)
public class ScheduleAdmin implements TypedLambdaFunction<AsyncHttpRequest, EventEnvelope> {
    private static final String CONTENT_TYPE = "content-type";
    private static final String APPLICATION_JSON = "application/json";
    private static final String ID = "_id";
    private static final String NAME = "name";
    private static final String SERVICE = "service";
    private static final String SCHEDULE = "schedule";
    private static final String START = "start";
    private static final String END = "end";
    private static final String PARAMETERS = "parameters";

    @Override
    public EventEnvelope handleEvent(Map<String, String> headers, AsyncHttpRequest input, int instance) {
        var name = input.getPathParameter(NAME);
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
                var rec = SchedulerDatabase.jobStates().find(Filters.eq(ID, name)).first();
                if (rec == null) {
                    throw new IllegalArgumentException("Job not found");
                }
                return new EventEnvelope().setBody(getJob(rec)).setHeader(CONTENT_TYPE, APPLICATION_JSON);
            }
        }
    }

    private EventEnvelope getJobList() {
        List<Map<String, Object>> jobs = new ArrayList<>();
        // the time of each job's last run
        for (var rec : SchedulerDatabase.jobStates().find().projection(Projections.include(START, END))) {
            Map<String, Object> map = new HashMap<>();
            map.put(NAME, rec.get(ID));
            putIfPresent(map, START, rec.get(START));
            putIfPresent(map, END, rec.get(END));
            jobs.add(map);
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
            var name = input.getPathParameter(NAME);
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

    /**
     * A job's record as the dashboard shows it, with the elapsed time of a finished run. A parameter key that starts
     * with "_" belongs to the system - the scheduler adds "_origin" to the parameters the state resolver stores - so
     * it is filtered out when read.
     */
    private static Map<String, Object> getJob(Document rec) {
        Map<String, Object> result = new HashMap<>();
        result.put(NAME, rec.get(ID));
        putIfPresent(result, SERVICE, rec.get(SERVICE));
        putIfPresent(result, SCHEDULE, rec.get(SCHEDULE));
        if (rec.get(PARAMETERS) instanceof Map<?, ?> parameters) {
            var shown = new HashMap<String, Object>();
            parameters.forEach((key, value) -> {
                if (!String.valueOf(key).startsWith(ScheduledJob.RESERVED_PREFIX)) {
                    shown.put(String.valueOf(key), value);
                }
            });
            result.put(PARAMETERS, shown);
        }
        var start = rec.getDate(START);
        var end = rec.getDate(END);
        putIfPresent(result, START, start);
        putIfPresent(result, END, end);
        if (start != null && end != null) {
            result.put("elapsed", Utility.getInstance().elapsedTime(end.getTime() - start.getTime()));
        }
        return result;
    }

    private static void putIfPresent(Map<String, Object> map, String key, Object value) {
        if (value != null) {
            map.put(key, value);
        }
    }
}
