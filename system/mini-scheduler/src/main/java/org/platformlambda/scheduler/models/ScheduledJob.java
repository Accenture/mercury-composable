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
    /** The time zone the cron statement is read in: the job's own, or the JVM's default when the job names none. */
    public final ZoneId zone;
    /**
     * The schedule the state resolver records, for information: the cron statement, followed by " @" and the time zone
     * when the job names one (e.g. "0 0 9 * * ? @America/New_York"). A job that names none sends the statement alone.
     */
    public final String schedule;
    public final Map<String, Object> parameters = new HashMap<>();

    /**
     * A job whose cron statement is read in the given time zone, so a region's daylight saving moves its UTC time.
     * A null zone reads the statement in the JVM's default time zone.
     */
    public ScheduledJob(String name, String service, String resolver, String cronSchedule, ZoneId zone) {
        this.name = name;
        this.service = service;
        this.resolver = resolver;
        this.cronSchedule = cronSchedule;
        this.zone = zone == null ? ZoneId.systemDefault() : zone;
        this.schedule = zone == null ? cronSchedule : cronSchedule + " @" + zone.getId();
    }

    public void addParameter(String key, Object value) {
        if (key != null && value != null) {
            this.parameters.put(key, value);
        }
    }
}
