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

import com.mongodb.ErrorCategory;
import com.mongodb.MongoWriteException;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.model.Updates;
import org.bson.Document;
import org.platformlambda.core.annotations.PreLoad;
import org.platformlambda.core.models.TypedLambdaFunction;
import org.platformlambda.quartz.db.SchedulerDatabase;
import org.platformlambda.scheduler.models.ScheduledJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.Map;

/**
 * The example's state resolver, on MongoDB, for one instance or many: "expires" claims the job in one conditional
 * write, so exactly one instance runs each firing. It is the worked example in "Running more than one instance"
 * in the mini-scheduler README, as an application runs it.
 * <p>
 * Each job is one document in the "job_state" collection, with the job's name as its _id, which MongoDB always
 * indexes uniquely. The parameters, with "_origin" among them, are a sub-document.
 * <p>
 * API contract:
 * 1. start:
 *      header (type = start, name = job_name, schedule = job_cron_statement,
 *              service = task where task is a function route or a flow name)
 *      (schedule is for information: the cron statement, followed by " @" and the time zone when the job
 *       names one, e.g. "0 0 9 * * ? @America/New_York")
 *      body (job_parameters, plus "_origin", the instance's origin id - a key that starts with "_" is the system's)
 *      return true
 * <p>
 * 2. end:
 *      header (type = end, name = job_name)
 *      body (not used)
 *      return true
 * <p>
 * 3. expires:
 *      header (type = expires, name = job_name)
 *      body (job_parameters, plus "_origin")
 *      return true or false
 *      (true claims the job for the instance that "_origin" names. A question not answered within 3 seconds
 *       is asked again, up to 3 times, with the same "_origin", so the resolver answers true again to the
 *       instance that holds the claim)
 */
@PreLoad(route="v1.state.resolver")
public class StateResolver implements TypedLambdaFunction<Map<String, Object>, Boolean> {
    private static final Logger log = LoggerFactory.getLogger(StateResolver.class);
    private static final String TYPE = "type";
    private static final String EXPIRES = "expires";
    private static final String NAME = "name";
    private static final String SERVICE = "service";
    private static final String SCHEDULE = "schedule";
    private static final String PARAMETERS = "parameters";
    private static final String START = "start";
    private static final String END = "end";
    private static final String ID = "_id";
    private static final String NONE = "none";
    // the window: more than 18 seconds plus the database's latency (9 s of deferred start, 9 s of retries), and
    // less than the job's period minus 9 seconds, so a job runs at most every 39 seconds
    private static final long WINDOW = 30_000;

    @Override
    public Boolean handleEvent(Map<String, String> headers, Map<String, Object> input, int instance) {
        var type = headers.get(TYPE);
        var name = headers.getOrDefault(NAME, NONE);
        var service = headers.getOrDefault(SERVICE, NONE);
        var parameters = input == null ? Map.<String, Object>of() : input;
        if (START.equals(type) && !name.equals(NONE) && !service.equals(NONE)) {
            start(name, service, headers.get(SCHEDULE), parameters);
            return true;
        }
        if (END.equals(type) && !name.equals(NONE)) {
            end(name);
            return true;
        }
        if (EXPIRES.equals(type) && !name.equals(NONE)) {
            if (!(parameters.get(ScheduledJob.ORIGIN) instanceof String)) {
                throw new IllegalArgumentException("expires needs the " + ScheduledJob.ORIGIN + " parameter");
            }
            var claimed = claim(name, parameters);
            if (claimed) {
                log.info("Job {} claimed by this instance", name);
            } else {
                log.info("Job {} is claimed by another instance within the window", name);
            }
            return claimed;
        }
        throw new IllegalArgumentException("type must be start, end or expires");
    }

    private static MongoCollection<Document> states() {
        return SchedulerDatabase.jobStates();
    }

    /** "expires": claim the job for the instance that "_origin" names. */
    private boolean claim(String name, Map<String, Object> parameters) {
        var now = new Date();
        var filter = Filters.and(Filters.eq(ID, name), Filters.or(
                Filters.lt(START, new Date(now.getTime() - WINDOW)),
                Filters.eq("parameters._origin", parameters.get(ScheduledJob.ORIGIN))));
        var update = Updates.combine(Updates.set(PARAMETERS, new Document(parameters)),
                Updates.set(START, now), Updates.unset(END));
        try {
            states().updateOne(filter, update, new UpdateOptions().upsert(true));
            return true;
        } catch (MongoWriteException e) {
            if (e.getError().getCategory() == ErrorCategory.DUPLICATE_KEY) {
                // the filter missed, so the upsert's insert met the peer's document for this job
                return false;
            }
            throw e;
        }
    }

    /** "start": the run that the claim allowed, or an operator's manual run, which skips the claim. */
    private void start(String name, String service, String schedule, Map<String, Object> parameters) {
        states().updateOne(Filters.eq(ID, name), Updates.combine(Updates.set(SERVICE, service),
                Updates.set(SCHEDULE, schedule), Updates.set(PARAMETERS, new Document(parameters)),
                Updates.set(START, new Date()), Updates.unset(END)), new UpdateOptions().upsert(true));
    }

    /** "end": the run has finished. */
    private void end(String name) {
        states().updateOne(Filters.eq(ID, name), Updates.set(END, new Date()));
    }
}
