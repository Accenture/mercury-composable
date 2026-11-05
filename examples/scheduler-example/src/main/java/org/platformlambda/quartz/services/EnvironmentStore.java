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

import com.mongodb.client.model.Filters;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.model.Updates;
import org.platformlambda.core.annotations.PreLoad;
import org.platformlambda.core.models.TypedLambdaFunction;
import org.platformlambda.quartz.db.SchedulerDatabase;
import org.platformlambda.scheduler.ActiveEnvironment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.Map;

/**
 * The example's store of the active environment (RFC-0008), on MongoDB.
 * <p>
 * The active environment is persisted in a data store shared by every instance of every site, because Kubernetes
 * restarts a pod without notice and an in-memory value would reset. The scheduler reads this store at every
 * scheduled point. It is one document in the "environment" collection: the active environment's name, the
 * operator who set it and when. Keep the route name {@code v1.environment.store}, or set
 * {@code scheduler.environment.store} to yours.
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
@PreLoad(route = ActiveEnvironment.DEFAULT_STORE)
public class EnvironmentStore implements TypedLambdaFunction<Map<String, Object>, Object> {
    private static final Logger log = LoggerFactory.getLogger(EnvironmentStore.class);
    private static final String ID = "_id";
    private static final String RECORD = "active-environment";
    private static final String ACTIVE = "active";
    private static final String OPERATOR = "operator";
    private static final String TIME = "time";

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

    private String read() {
        var rec = SchedulerDatabase.environment().find(Filters.eq(ID, RECORD)).first();
        return rec == null ? null : rec.getString(ACTIVE);
    }

    private void write(String name, String operator) {
        SchedulerDatabase.environment().updateOne(Filters.eq(ID, RECORD), Updates.combine(Updates.set(ACTIVE, name),
                Updates.set(OPERATOR, operator), Updates.set(TIME, new Date())), new UpdateOptions().upsert(true));
        log.info("Active environment {} persisted by {}", name, operator);
    }
}
