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

package org.platformlambda.quartz.models;

import org.platformlambda.core.util.Utility;
import org.quartz.JobDetail;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class ScheduledJob {

    public final String name;
    public final String service;
    public final Date created = new Date();
    public final String cronSchedule;
    public String description;
    public Map<String, String> parameters = new HashMap<>();
    public Date startTime;
    public int count = 0;
    public Date stopTime;
    public Date lastExecution;
    public JobDetail job;

    public ScheduledJob(String name, String service, String cronSchedule) {
        this.name = name;
        this.service = service;
        this.cronSchedule = cronSchedule;
    }

    public ScheduledJob addParameter(String key, Object value) {
        if (key != null) {
            String v = switch (value) {
                case null -> "";
                case String s -> s;
                case Date date -> Utility.getInstance().date2str(date);
                default -> String.valueOf(value);
            };
            this.parameters.put(key, v);
        }
        return this;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> result = new HashMap<>();
        result.put("name", name);
        result.put("description", description);
        result.put("cron_schedule", cronSchedule);
        result.put("service", service);
        result.put("parameters", parameters);
        result.put("created", created);
        result.put("start_time", startTime);
        result.put("stop_time", stopTime);
        result.put("last_execution", lastExecution);
        result.put("iterations", count);
        return result;
    }
}
