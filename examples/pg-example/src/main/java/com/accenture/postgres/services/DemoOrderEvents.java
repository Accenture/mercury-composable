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

package com.accenture.postgres.services;

import org.platformlambda.core.annotations.PreLoad;
import org.platformlambda.core.exception.AppException;
import org.platformlambda.core.models.AsyncHttpRequest;
import org.platformlambda.core.models.EventEnvelope;
import org.platformlambda.core.models.TypedLambdaFunction;
import org.platformlambda.core.system.PostOffice;
import org.platformlambda.postgres.models.PgQueryStatement;
import org.platformlambda.postgres.models.PgTransactionStatement;
import org.platformlambda.postgres.models.PgUpdateStatement;
import org.platformlambda.postgres.services.PgService;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 * The orders demo, the event form - statements sent to the "postgres.service" route with the PostOffice.
 * <p>
 * This is what the PgRequest helper does underneath: a PgQueryStatement answers a list of rows (each a map
 * of column name to value), a PgUpdateStatement answers {"row_updated": n}, and a PgTransactionStatement
 * answers {"updated": [n1, n2, ...]} after running its statements in one transaction. Use this form when
 * a function wants the asynchronous flavours of the PostOffice (asyncRequest, a future, parallel requests
 * to several routes) instead of the helper's blocking convenience.
 * <ul>
 *     <li>GET /api/demo/orders/{profile_id}/summary - a PgQueryStatement with aggregates</li>
 *     <li>DELETE /api/demo/orders/{profile_id}/purge - a PgTransactionStatement of two PgUpdateStatements</li>
 * </ul>
 */
@PreLoad(route = "v1.demo.orders.events", instances = 20)
public class DemoOrderEvents implements TypedLambdaFunction<AsyncHttpRequest, Object> {
    private static final long TIMEOUT = 8000;
    private static final String SQL_SUMMARY =
            "SELECT count(*) AS orders, coalesce(sum(amount), 0) AS total," +
            " count(*) FILTER (WHERE status = 'cancelled') AS cancelled FROM demo_order WHERE profile_id = $1";
    private static final String SQL_DELETE_HISTORY =
            "DELETE FROM demo_order_history WHERE order_id IN (SELECT id FROM demo_order WHERE profile_id = $1)";
    private static final String SQL_DELETE_ORDERS = "DELETE FROM demo_order WHERE profile_id = $1";
    private static final String PROFILE_ID = "profile_id";

    @Override
    public Object handleEvent(Map<String, String> headers, AsyncHttpRequest input, int instance) throws ExecutionException, InterruptedException {
        var po = new PostOffice(headers, instance);
        var profileId = input.getPathParameter(PROFILE_ID);
        if (profileId == null) {
            throw new AppException(400, "Missing profile_id");
        }
        if ("GET".equals(input.getMethod())) {
            return summary(po, profileId);
        }
        if ("DELETE".equals(input.getMethod())) {
            return purge(po, profileId);
        }
        throw new AppException(405, "Method not allowed");
    }

    /**
     * A query statement as an event. The reply body is a list of rows; count(*) arrives as a Long and
     * sum(amount) as a BigDecimal that travels as a plain string, so the caller keeps every cent.
     */
    private Object summary(PostOffice po, String profileId) throws ExecutionException, InterruptedException {
        var query = new PgQueryStatement(SQL_SUMMARY);
        query.bindParameters(profileId);
        var result = po.request(new EventEnvelope().setTo(PgService.ROUTE).setBody(query), TIMEOUT).get();
        if (result.hasError()) {
            throw new AppException(result.getStatus(), String.valueOf(result.getBody()));
        }
        if (result.getBody() instanceof List<?> rows && !rows.isEmpty()) {
            return rows.getFirst();
        }
        return Map.of();
    }

    /**
     * A transaction statement as an event: the history rows go first because they reference the orders,
     * then the orders - all or none. The reply body carries the count of each statement in order.
     */
    private Object purge(PostOffice po, String profileId) throws ExecutionException, InterruptedException {
        var history = new PgUpdateStatement(SQL_DELETE_HISTORY);
        history.bindParameters(profileId);
        var orders = new PgUpdateStatement(SQL_DELETE_ORDERS);
        orders.bindParameters(profileId);
        var transaction = new PgTransactionStatement();
        transaction.addStatement(history);
        transaction.addStatement(orders);
        var result = po.request(new EventEnvelope().setTo(PgService.ROUTE).setBody(transaction), TIMEOUT).get();
        if (result.hasError()) {
            throw new AppException(result.getStatus(), String.valueOf(result.getBody()));
        }
        if (result.getBody() instanceof Map<?, ?> map && map.get("updated") instanceof List<?> counts) {
            return Map.of("history_deleted", counts.get(0), "orders_deleted", counts.get(1));
        }
        throw new AppException(500, "Unexpected reply from " + PgService.ROUTE);
    }
}
