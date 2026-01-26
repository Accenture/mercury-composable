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

package org.platformlambda.postgres.services;

import org.platformlambda.postgres.models.PgUpdateStatement;
import org.platformlambda.postgres.support.PgRequest;
import org.platformlambda.core.annotations.PreLoad;
import org.platformlambda.core.exception.AppException;
import org.platformlambda.core.models.EventEnvelope;
import org.platformlambda.core.models.LambdaFunction;
import org.platformlambda.core.system.Platform;
import org.platformlambda.core.system.PostOffice;
import org.platformlambda.core.util.AppConfigReader;
import org.platformlambda.core.util.Utility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;

@PreLoad(route = "postgres.health", instances = 10)
public class PgHealth implements LambdaFunction {
    private static final Logger log = LoggerFactory.getLogger(PgHealth.class);
    private static final String SQL_READ = "SELECT * FROM health_check WHERE id = :id";
    private static final String SQL_INSERT = "INSERT INTO health_check " +
                                                "(id, app_name, app_instance, created, updated)" +
                                                " VALUES ($1, $2, $3, $4, $5)";
    private static final String SQL_UPDATE = "UPDATE health_check SET updated = :updated WHERE id = :id";
    private static final String SQL_CLEAN = "DELETE FROM health_check WHERE updated < :updated";
    private static final String TYPE = "type";
    private static final String HEALTH = "health";
    private static final String INFO = "info";
    private static final AtomicBoolean firstRun = new AtomicBoolean(true);
    private static final long TIMEOUT = 5000;
    private static final long CLEAN_INTERVAL = 30 * 60 * 1000L;  // 30 minutes
    private static final long EXPIRY_TIMER = 30 * 60 * 1000L;

    private final String host;

    public PgHealth() {
        AppConfigReader config = AppConfigReader.getInstance();
        host = config.getProperty("postgres.host", "unknown");
    }

    @Override
    public Object handleEvent(Map<String, String> headers, Object body, int instance)
            throws ExecutionException, InterruptedException {
        if (INFO.equals(headers.get(TYPE))) {
            Map<String, Object> result = new HashMap<>();
            result.put("service", "postgres");
            result.put("href", host);
            return result;
        }
        if (HEALTH.equals(headers.get(TYPE))) {
            var sb = new StringBuilder();
            var platform = Platform.getInstance();
            var id = platform.getOrigin();
            var appName = platform.getName();
            var appInstance = appName + "-" + id;
            var po = new PostOffice(headers, instance);
            var now = System.currentTimeMillis();
            var readReq = new PgRequest(TIMEOUT);
            var readResult = readReq.query(po, SQL_READ, Map.of("id", id));
            if (readResult.isEmpty()) {
                // insert one if no health record
                var timestamp = new Timestamp(now);
                var request = new PgRequest(TIMEOUT);
                var count = request.update(po, SQL_INSERT, id, appName, appInstance, timestamp, timestamp);
                var diff = Math.abs(System.currentTimeMillis() - now);
                if (count == 1) {
                    sb.append("Insert (").append(diff).append(" ms)");
                    log.info("Created health check record for {}, {}", id, platform.getName());
                    // if first run, schedule event to remove outdated health records
                    if (firstRun.get()) {
                        firstRun.set(false);
                        var interval = Utility.getInstance().elapsedTime(CLEAN_INTERVAL);
                        log.info("Housekeeping interval: {}", interval);
                        // immediately check outdated health records
                        removeOutdatedHealthRecords(true);
                        // then schedule periodic check
                        platform.getVertx().setPeriodic(CLEAN_INTERVAL, t -> removeOutdatedHealthRecords(false));
                    }
                }
            } else {
                var diff = Math.abs(System.currentTimeMillis() - now);
                sb.append("Read (").append(diff).append(" ms)");
            }
            var writeReq = new PgUpdateStatement(SQL_UPDATE);
            writeReq.bindParameter("id", id);
            writeReq.bindParameter("updated", new Timestamp(System.currentTimeMillis()));
            var writeResult = po.request(new EventEnvelope().setTo(PgService.ROUTE).setBody(writeReq), TIMEOUT).get();
            if (writeResult.hasError()) {
                throw new AppException(writeResult.getStatus(), String.valueOf(writeResult.getError()));
            }
            sb.append(", Write (").append(writeResult.getRoundTrip()).append(" ms)");
            return sb.toString();
        }
        return false;
    }

    private void removeOutdatedHealthRecords(boolean first) {
        var po = new PostOffice(Map.of(), 1);
        var request = new PgRequest(TIMEOUT);
        try {
            var deleted = request.update(po, SQL_CLEAN,
                                Map.of("updated", new Timestamp(System.currentTimeMillis() - EXPIRY_TIMER)));
            if (deleted > 0) {
                log.info("Removed {} outdated health records", deleted);
            } else if (first) {
                log.info("No outdated health records found");
            }
        } catch (ExecutionException | InterruptedException e) {
            log.error("Unable to remove outdated health records", e);
        }
    }
}
