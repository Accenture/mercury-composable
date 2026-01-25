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

package com.accenture.db2.services;

import com.accenture.db2.models.Db2QueryStatement;
import com.accenture.db2.models.Db2TransactionStatement;
import com.accenture.db2.models.Db2UpdateStatement;
import org.platformlambda.core.annotations.PreLoad;
import org.platformlambda.core.models.EventEnvelope;
import org.platformlambda.core.models.TypedLambdaFunction;
import org.platformlambda.core.serializers.SimpleMapper;
import org.platformlambda.core.util.AppConfigReader;
import org.platformlambda.postgres.models.HealthCheck;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Timestamp;
import java.util.*;

@PreLoad(route = Db2MockService.ROUTE, instances = 200)
public class Db2MockService implements TypedLambdaFunction<EventEnvelope, Object> {
    private static final Logger log = LoggerFactory.getLogger(Db2MockService.class);
    public static final String ROUTE = "db2.mock.service";
    private static final String ROW_UPDATED = "row_updated";
    private static final String UPDATED = "updated";
    private static final String DB2_QUERY_CLASS = Db2QueryStatement.class.getName();
    private static final String DB2_UPDATE_CLASS = Db2UpdateStatement.class.getName();
    private static final String DB2_TRANSACTION_CLASS = Db2TransactionStatement.class.getName();

    private HealthCheck healthCheck = new HealthCheck();

    public Db2MockService() {
        var config = AppConfigReader.getInstance();
        var debugQuery = "debug".equalsIgnoreCase(config.getProperty("logging.level.io.r2dbc.postgresql.QUERY"));
        var debugParams = "debug".equalsIgnoreCase(config.getProperty("logging.level.io.r2dbc.postgresql.PARAM"));
        if (debugQuery) {
            log.warn("*** logging.level.io.r2dbc.postgresql.QUERY=DEBUG - DO NOT enable this feature in production.");
        }
        if (debugParams) {
            log.warn("*** logging.level.io.r2dbc.postgresql.PARAM=DEBUG - DO NOT enable this feature in production.");
        }
    }

    @Override
    public Object handleEvent(Map<String, String> headers, EventEnvelope input, int instance) {
        Object clazz = restorePoJo(input);
        if (clazz instanceof Db2QueryStatement query) {
            log.info("EXECUTE {}", query.getStatement());
            if (query.getOriginalParameter(1) instanceof String id) {
                return healthCheck.id != null && healthCheck.id.equals(id) ?
                        List.of(healthCheck) : Collections.emptyList();
            }
            // for unit test, echo the SQL query with list values
            if (query.getOriginalParameter(1) == null || query.getOriginalParameter(1) instanceof Timestamp) {
                return List.of(Map.of("sql", query.getStatement()));
            }
        }
        if (clazz instanceof Db2UpdateStatement update) {
            log.info("EXECUTE {}", update.getStatement());
            if (update.getStatement().startsWith("INSERT INTO")) {
                Map<String, Object> map = new HashMap<>();
                map.put("id", update.getOriginalParameter(1));
                map.put("app_name", update.getOriginalParameter(2));
                map.put("app_instance", update.getOriginalParameter(3));
                map.put("created", update.getOriginalParameter(4));
                map.put("updated", update.getOriginalParameter(5));
                healthCheck = SimpleMapper.getInstance().getMapper().readValue(map, HealthCheck.class);
            }
            return Map.of(ROW_UPDATED, 1, "class", update);
        }
        if (clazz instanceof Db2TransactionStatement transaction) {
            log.info("EXECUTE {}", transaction.getStatements().getFirst().getStatement());
            return Map.of(UPDATED, 1, "class", transaction);
        }
        throw new IllegalArgumentException("Unknown statement type");
    }

    private Object restorePoJo(EventEnvelope input) {
        var type = input.getType();
        if (DB2_QUERY_CLASS.equals(type)) {
            return input.getBody(Db2QueryStatement.class);
        }
        if (DB2_UPDATE_CLASS.equals(type)) {
            return input.getBody(Db2UpdateStatement.class);
        }
        if (DB2_TRANSACTION_CLASS.equals(type)) {
            return input.getBody(Db2TransactionStatement.class);
        }
        return Optional.empty();
    }
}
