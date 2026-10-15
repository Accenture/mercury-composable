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
import org.platformlambda.core.models.TypedLambdaFunction;
import org.platformlambda.core.system.PostOffice;
import org.platformlambda.core.util.MultiLevelMap;
import org.platformlambda.core.util.Utility;
import org.platformlambda.postgres.support.PgRequest;
import org.platformlambda.postgres.support.PgTransactionBuilder;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 * The orders demo - every method of the PgRequest helper on one REST surface.
 * <p>
 * PgRequest wraps the "postgres.service" events (see DemoOrderEvents for the raw form):
 * <ul>
 *     <li>POST /api/demo/orders/{profile_id} - insertRows: all the orders in ONE multi-row INSERT</li>
 *     <li>POST /api/demo/orders/{profile_id}/with-history - PgTransactionBuilder: the orders AND their
 *         history rows, two tables in one transaction, all or none</li>
 *     <li>GET /api/demo/orders/{profile_id} - query with a positional parameter;
 *         with ?status=a,b - query with named parameters, one of them a list</li>
 *     <li>PUT /api/demo/orders/{profile_id}/status - batch: the same UPDATE for many ids in one transaction</li>
 *     <li>PUT /api/demo/orders/{profile_id}/cancel/{order_id} - transaction: two different statements, all or none</li>
 * </ul>
 */
@PreLoad(route = "v1.demo.orders", instances = 20)
public class DemoOrderEndpoint implements TypedLambdaFunction<AsyncHttpRequest, Object> {
    private static final long TIMEOUT = 8000;
    private static final String TABLE = "demo_order";
    private static final String PROFILE_ID = "profile_id";
    private static final String ORDER_ID = "order_id";
    private static final String STATUS = "status";
    private static final String UPDATED = "updated";
    private static final String ORDERS = "orders";
    private static final String IDS = "ids";
    private static final String OPEN = "open";
    private static final List<String> COLUMNS = List.of("id", PROFILE_ID, "item", "quantity", "amount",
                                                        STATUS, "created", UPDATED);
    private static final String HISTORY_TABLE = "demo_order_history";
    private static final List<String> HISTORY_COLUMNS = List.of(ORDER_ID, "event", "created");
    private static final String SQL_BY_PROFILE =
            "SELECT * FROM demo_order WHERE profile_id = $1 ORDER BY created, id";
    private static final String SQL_BY_PROFILE_AND_STATUS =
            "SELECT * FROM demo_order WHERE profile_id = :profile_id AND status IN (:status_list) ORDER BY created, id";
    private static final String SQL_SET_STATUS =
            "UPDATE demo_order SET status = $1, updated = $2 WHERE id = $3 AND profile_id = $4";
    private static final String SQL_CANCEL =
            "UPDATE demo_order SET status = 'cancelled', updated = $1 WHERE id = $2 AND profile_id = $3";
    private static final String SQL_CANCEL_HISTORY =
            "INSERT INTO demo_order_history (order_id, event, created)" +
            " SELECT id, 'cancelled', $1 FROM demo_order WHERE id = $2 AND profile_id = $3";

    @Override
    public Object handleEvent(Map<String, String> headers, AsyncHttpRequest input, int instance)
            throws ExecutionException, InterruptedException {
        // a function creates its own PostOffice from the incoming headers,
        // so the distributed trace continues into the database calls
        var po = new PostOffice(headers, instance);
        var sql = new PgRequest(TIMEOUT);
        var profileId = input.getPathParameter(PROFILE_ID);
        if (profileId == null) {
            throw new AppException(400, "Missing profile_id");
        }
        var orderId = input.getPathParameter(ORDER_ID);
        var method = input.getMethod();
        var withHistory = input.getUrl().endsWith("/with-history");
        if ("POST".equals(method)) {
            return withHistory ? createOrdersWithHistory(po, sql, profileId, input.getBody())
                               : createOrders(po, sql, profileId, input.getBody());
        }
        if ("GET".equals(method)) {
            return listOrders(po, sql, profileId, input.getQueryParameter(STATUS));
        }
        if ("PUT".equals(method) && orderId == null) {
            return setStatus(po, sql, profileId, input.getBody());
        }
        if ("PUT".equals(method)) {
            return cancelOrder(po, sql, profileId, orderId);
        }
        throw new AppException(405, "Method not allowed");
    }

    /**
     * insertRows - the body's orders become ONE multi-row INSERT statement (one round trip, one commit).
     * The helper validates the table and column names, binds every value as a parameter and splits a very
     * large set into statements of at most 1000 parameters inside one transaction.
     */
    private Map<String, Object> createOrders(PostOffice po, PgRequest sql, String profileId, Object body)
            throws ExecutionException, InterruptedException {
        var rows = orderRows(profileId, body, new Timestamp(System.currentTimeMillis()));
        int inserted = sql.insertRows(po, TABLE, COLUMNS, rows);
        return Map.of("inserted", inserted, IDS, orderIds(rows));
    }

    /**
     * PgTransactionBuilder - the orders AND one "created" history row per order, two tables in ONE
     * transaction: if the history insert failed, the orders would roll back too. The result carries one
     * row count per step, in the order the steps were added.
     */
    private Map<String, Object> createOrdersWithHistory(PostOffice po, PgRequest sql, String profileId, Object body)
            throws ExecutionException, InterruptedException {
        var now = new Timestamp(System.currentTimeMillis());
        var rows = orderRows(profileId, body, now);
        var ids = orderIds(rows);
        var events = ids.stream().map(id -> List.<Object>of(id, "created", now)).toList();
        var tx = new PgTransactionBuilder()
                .insertRows(TABLE, COLUMNS, rows)
                .insertRows(HISTORY_TABLE, HISTORY_COLUMNS, events);
        var counts = sql.transaction(po, tx);
        return Map.of("inserted", counts.getFirst(), "history", counts.get(1), IDS, ids);
    }

    private static List<List<Object>> orderRows(String profileId, Object body, Timestamp now) {
        var util = Utility.getInstance();
        if (!(body instanceof Map<?, ?> map) || !(map.get(ORDERS) instanceof List<?> orders) || orders.isEmpty()) {
            throw new AppException(400, "Body must contain a non-empty 'orders' list");
        }
        var rows = new ArrayList<List<Object>>();
        for (var o : orders) {
            if (!(o instanceof Map<?, ?> order) || order.get("item") == null ||
                    !(order.get("quantity") instanceof Number quantity) ||
                    !(order.get("amount") instanceof Number amount)) {
                throw new AppException(400, "Each order needs item, quantity and amount");
            }
            // the amount is bound as a double - NUMERIC(12, 2) rounds it to cents on the way in
            rows.add(List.of(util.getUuid(), profileId, String.valueOf(order.get("item")), quantity.intValue(),
                             amount.doubleValue(), OPEN, now, now));
        }
        return rows;
    }

    private static List<String> orderIds(List<List<Object>> rows) {
        return rows.stream().map(row -> String.valueOf(row.getFirst())).toList();
    }

    /**
     * query - a positional parameter ($1) for the plain read, named parameters (:name) when a status filter
     * is given; the filter is a list, which the statement classes expand into the IN (...) clause.
     * A NUMERIC column arrives as a BigDecimal and travels to the caller as a plain string ("274.49").
     */
    private List<Map<String, Object>> listOrders(PostOffice po, PgRequest sql, String profileId, String status)
            throws ExecutionException, InterruptedException {
        if (status == null || status.isBlank()) {
            return sql.query(po, SQL_BY_PROFILE, profileId);
        }
        var statusList = List.of(status.split(","));
        return sql.query(po, SQL_BY_PROFILE_AND_STATUS, Map.of(PROFILE_ID, profileId, "status_list", statusList));
    }

    /**
     * batch - the same UPDATE statement once per id, executed in one transaction on one connection:
     * one commit for the whole set, and the count for each statement comes back in order.
     */
    private Map<String, Object> setStatus(PostOffice po, PgRequest sql, String profileId, Object body)
            throws ExecutionException, InterruptedException {
        var mm = new MultiLevelMap(body instanceof Map<?, ?> map ? castMap(map) : Map.of());
        var status = mm.getElement(STATUS);
        if (!(status instanceof String newStatus) || !(mm.getElement(IDS) instanceof List<?> ids) || ids.isEmpty()) {
            throw new AppException(400, "Body must contain 'status' and a non-empty 'ids' list");
        }
        var now = new Timestamp(System.currentTimeMillis());
        var parameterList = ids.stream()
                .map(id -> List.<Object>of(newStatus, now, String.valueOf(id), profileId)).toList();
        var counts = sql.batch(po, SQL_SET_STATUS, parameterList);
        return Map.of(UPDATED, counts);
    }

    /**
     * transaction - two different statements, all or none: the order is marked cancelled and the event is
     * recorded in the history table. If the second statement failed, the first would roll back.
     * Both statements select by id AND profile_id, so an unknown order updates nothing and answers 404.
     */
    private Map<String, Object> cancelOrder(PostOffice po, PgRequest sql, String profileId, String orderId)
            throws ExecutionException, InterruptedException {
        var now = new Timestamp(System.currentTimeMillis());
        var statements = List.of(SQL_CANCEL, SQL_CANCEL_HISTORY);
        var parameters = List.<List<Object>>of(List.of(now, orderId, profileId), List.of(now, orderId, profileId));
        var counts = sql.transaction(po, statements, parameters);
        if (counts.getFirst() == 0) {
            throw new AppException(404, "Order " + orderId + " not found for profile " + profileId);
        }
        return Map.of(UPDATED, counts);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> castMap(Map<?, ?> map) {
        return (Map<String, Object>) map;
    }
}
