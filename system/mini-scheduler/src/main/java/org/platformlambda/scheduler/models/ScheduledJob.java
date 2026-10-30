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

package org.platformlambda.scheduler.models;

import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;

public class ScheduledJob {

    public final String name;
    public final String service;
    public final String cronSchedule;
    public final String resolver;
    public final ZoneId zone;
    public final Map<String, Object> parameters = new HashMap<>();

    /** A job whose cron statement is read in the given time zone, so a region's daylight saving moves its UTC time. */
    public ScheduledJob(String name, String service, String resolver, String cronSchedule, ZoneId zone) {
        this.name = name;
        this.service = service;
        this.resolver = resolver;
        this.cronSchedule = cronSchedule;
        this.zone = zone;
    }

    public void addParameter(String key, Object value) {
        if (key != null && value != null) {
            this.parameters.put(key, value);
        }
    }
}
