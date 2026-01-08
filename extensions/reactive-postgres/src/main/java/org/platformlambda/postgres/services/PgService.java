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

import org.platformlambda.postgres.models.PgQueryStatement;
import org.platformlambda.postgres.models.PgUpdateStatement;
import org.platformlambda.postgres.models.SqlPreparedStatement;
import org.platformlambda.postgres.support.RowParser;
import io.r2dbc.spi.Row;
import org.platformlambda.core.annotations.PreLoad;
import org.platformlambda.core.models.EventEnvelope;
import org.platformlambda.core.models.TypedLambdaFunction;
import org.platformlambda.core.util.AppConfigReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.r2dbc.core.DatabaseClient;

import java.util.Map;
import java.util.Optional;

@PreLoad(route = PgService.ROUTE, instances = 200)
public class PgService implements TypedLambdaFunction<EventEnvelope, Object> {
    private static final Logger log = LoggerFactory.getLogger(PgService.class);
    public static final String ROUTE = "postgres.service";
    private static final String PG_QUERY_CLASS = PgQueryStatement.class.getName();
    private static final String PG_UPDATE_CLASS = PgUpdateStatement.class.getName();

    private DatabaseClient client;

    public PgService() {
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

    @Autowired
    public void setDatabaseClient(DatabaseClient client) {
        this.client = client;
    }

    @Override
    public Object handleEvent(Map<String, String> headers, EventEnvelope input, int instance) {
        Object clazz = restorePoJo(input);
        if (clazz instanceof PgQueryStatement query) {
            var sql = bindParamsToStatement(client.sql(query.getStatement()), query);
            return sql.map(r -> {
                if (r instanceof Row row) {
                    return RowParser.toMap(row);
                } else {
                    // this should never happen
                    throw new IllegalArgumentException("database record object is not a Row");
                }
            }).all().collectList();
        }
        if (clazz instanceof PgUpdateStatement update) {
            var sql = bindParamsToStatement(client.sql(update.getStatement()), update);
            return sql.fetch().rowsUpdated().map(count -> Map.of("row_updated", count));
        }
        throw new IllegalArgumentException("Unknown statement type");
    }

    private Object restorePoJo(EventEnvelope input) {
        var type = input.getType();
        if (PG_QUERY_CLASS.equals(type)) {
            return input.getBody(PgQueryStatement.class);
        }
        if (PG_UPDATE_CLASS.equals(type)) {
            return input.getBody(PgUpdateStatement.class);
        }
        return Optional.empty();
    }

    private DatabaseClient.GenericExecuteSpec bindParamsToStatement(DatabaseClient.GenericExecuteSpec sql,
                                                                    SqlPreparedStatement query) {
        var namedParms = query.getNamedParams();
        var namedNulls = query.getNamedNulls();
        var parameters = query.getParameters();
        var nullParams = query.getNullParams();
        for (var entry : namedParms.entrySet()) {
            log.info("Binding named param: {}", entry.getKey());
            sql = sql.bind(entry.getKey(), query.getOriginalParameter(entry.getKey()));
        }
        for (var entry : namedNulls.entrySet()) {
            sql = sql.bindNull(entry.getKey(), query.getNullClass(entry.getKey()));
        }
        for (var entry : parameters.entrySet()) {
            log.info("Binding parameter: {}", entry.getKey());
            sql = sql.bind(entry.getKey(), query.getOriginalParameter(entry.getKey()));
        }
        for (var entry : nullParams.entrySet()) {
            sql = sql.bindNull(entry.getKey(), query.getNullClass(entry.getKey()));
        }
        return sql;
    }
}
