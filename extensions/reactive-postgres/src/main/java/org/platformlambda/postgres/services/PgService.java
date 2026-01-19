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
import org.platformlambda.postgres.models.PgTransactionStatement;
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
import org.springframework.r2dbc.connection.R2dbcTransactionManager;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

@PreLoad(route = PgService.ROUTE, instances = 200)
public class PgService implements TypedLambdaFunction<EventEnvelope, Object> {
    private static final Logger log = LoggerFactory.getLogger(PgService.class);
    public static final String ROUTE = "postgres.service";
    private static final String ROW_UPDATED = "row_updated";
    private static final String UPDATED = "updated";
    private static final String PG_QUERY_CLASS = PgQueryStatement.class.getName();
    private static final String PG_UPDATE_CLASS = PgUpdateStatement.class.getName();
    private static final String PG_TRANSACTION_CLASS = PgTransactionStatement.class.getName();

    private DatabaseClient client;
    private R2dbcTransactionManager transactionManager;

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

    @Autowired
    public void setTransactionManager(R2dbcTransactionManager transactionManager) {
        this.transactionManager = transactionManager;
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
            return sql.fetch().rowsUpdated().map(count -> Map.of(ROW_UPDATED, count.intValue()));
        }
        if (clazz instanceof PgTransactionStatement transaction) {
            var statements = transaction.getStatements();
            if (statements.isEmpty()) {
                throw new IllegalArgumentException("Missing transaction statements");
            }
            final AtomicReference<List<Integer>> counter = new AtomicReference<>();
            counter.set(new ArrayList<>());
            var firstOne = statements.getFirst();
            var sql = bindParamsToStatement(client.sql(firstOne.getStatement()), firstOne)
                        .fetch().rowsUpdated().map(count -> {
                            counter.get().add(count.intValue());
                            return Map.of(UPDATED, counter.get());
                    });
            for (int i=1; i<statements.size(); i++) {
                sql = sql.then(bindParamsToStatement(client.sql(statements.get(i).getStatement()), statements.get(i))
                        .fetch().rowsUpdated().map(count -> {
                            counter.get().add(count.intValue());
                            return Map.of(UPDATED, counter.get());
                        })
                );
            }
            var action = sql.then(Mono.just(Map.of(UPDATED, counter.get())));
            return action.as(TransactionalOperator.create(transactionManager)::transactional);
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
        if (PG_TRANSACTION_CLASS.equals(type)) {
            return input.getBody(PgTransactionStatement.class);
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
            sql = sql.bind(entry.getKey(), query.getOriginalParameter(entry.getKey()));
        }
        for (var entry : namedNulls.entrySet()) {
            sql = sql.bindNull(entry.getKey(), query.getNullClass(entry.getKey()));
        }
        for (var entry : parameters.entrySet()) {
            sql = sql.bind(entry.getKey(), query.getOriginalParameter(entry.getKey()));
        }
        for (var entry : nullParams.entrySet()) {
            sql = sql.bindNull(entry.getKey(), query.getNullClass(entry.getKey()));
        }
        return sql;
    }
}
