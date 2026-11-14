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

package com.accenture.minigraph.demo;

import org.platformlambda.core.exception.AppException;
import org.platformlambda.core.util.Utility;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * The order-assistant demo's data: a few orders, their shipments and a refund ledger, held in memory.
 * <p>
 * The three tool routes of the demo graph ({@code demo.order.lookup}, {@code demo.shipment.status} and
 * {@code demo.refund.issue}) read and write this book. It stands in for the order, logistics and payment
 * systems a real assistant would reach through composable functions of its own. The data is fixed so a
 * run is reproducible: ORD-1001 was delivered, ORD-1002 and ORD-1004 were lost by the carrier, and
 * ORD-1003 is still in transit. A refund is the one write, and it is issued at most once per order.
 */
final class OrderBook {
    static final String ORDER_ID = "order_id";
    private static final String CURRENCY = "USD";
    private static final Map<String, Map<String, Object>> ORDERS = new LinkedHashMap<>();
    private static final Map<String, Map<String, Object>> SHIPMENTS = new LinkedHashMap<>();
    private static final ConcurrentMap<String, Map<String, Object>> REFUNDS = new ConcurrentHashMap<>();

    static {
        order("ORD-1001", "2026-11-03", 89.00, "delivered",
                item("HP-200", "Wireless headphones", 1, 89.00));
        shipment("ORD-1001", "Northwind Express", "NWX-5531-0071", "delivered", "2026-11-08",
                "Delivered to the front desk and signed for.");
        order("ORD-1002", "2026-11-04", 174.00, "shipped",
                item("KB-77", "Mechanical keyboard", 1, 129.00), item("HUB-4C", "USB-C hub", 1, 45.00));
        shipment("ORD-1002", "Northwind Express", "NWX-5531-0188", "lost", "2026-11-10",
                "The parcel left the sorting hub on 2026-11-06 and has not been scanned since; the carrier declared it lost.");
        order("ORD-1003", "2026-11-11", 118.00, "shipped",
                item("MS-12", "Monitor stand", 2, 59.00));
        shipment("ORD-1003", "Blue Parcel", "BP-00912-4471", "in_transit", "2026-11-13",
                "In transit; the estimated delivery date is 2026-11-16.");
        order("ORD-1004", "2026-11-05", 240.00, "shipped",
                item("DK-9", "Standing desk mat", 2, 120.00));
        shipment("ORD-1004", "Blue Parcel", "BP-00912-4502", "lost", "2026-11-12",
                "The carrier closed the trace without finding the parcel.");
    }

    private OrderBook() {
        // the book is static data
    }

    /**
     * The order id of a tool call, as the catalog's schema requires it.
     *
     * @param input the tool call's arguments
     * @return the order id
     * @throws AppException 400 when the id is missing
     */
    static String orderId(Map<String, Object> input) throws AppException {
        Object id = input.get(ORDER_ID);
        if (id == null || String.valueOf(id).isBlank()) {
            throw new AppException(400, "Missing " + ORDER_ID);
        }
        return String.valueOf(id);
    }

    /**
     * An order by its id.
     *
     * @param orderId the order id
     * @return the order: id, the date it was placed, its items, the total, the currency and the status
     * @throws AppException 404 when the order does not exist
     */
    static Map<String, Object> order(String orderId) throws AppException {
        Map<String, Object> found = ORDERS.get(orderId);
        if (found == null) {
            throw new AppException(404, "Order not found: " + orderId);
        }
        return found;
    }

    /**
     * The shipment of an order.
     *
     * @param orderId the order id
     * @return the shipment: the carrier, the tracking number, the status, the date of the last update and
     *         the carrier's note
     * @throws AppException 404 when the order does not exist
     */
    static Map<String, Object> shipment(String orderId) throws AppException {
        Map<String, Object> found = SHIPMENTS.get(orderId);
        if (found == null) {
            throw new AppException(404, "No shipment for order " + orderId);
        }
        return found;
    }

    /**
     * Issue a refund for an order, once.
     *
     * @param orderId the order id
     * @param amount the amount, above zero and up to the order's total
     * @param reason the reason the assistant gives
     * @return the refund: its id, the order, the amount, the currency and the status
     * @throws AppException 404 when the order does not exist, 400 when the amount is out of range,
     *                      409 when the order was already refunded
     */
    static Map<String, Object> refund(String orderId, double amount, String reason) throws AppException {
        Map<String, Object> ordered = order(orderId);
        double total = (Double) ordered.get("total");
        if (amount <= 0 || amount > total) {
            throw new AppException(400, "The refund must be above 0 and at most the order total of "
                    + total + " " + CURRENCY);
        }
        if (reason == null || reason.isBlank()) {
            throw new AppException(400, "Missing reason");
        }
        Map<String, Object> issued = new LinkedHashMap<>();
        issued.put("refund_id", "RF-" + Utility.getInstance().getUuid().substring(0, 8).toUpperCase());
        issued.put(ORDER_ID, orderId);
        issued.put("amount", amount);
        issued.put("currency", CURRENCY);
        issued.put("reason", reason);
        issued.put("status", "issued");
        Map<String, Object> earlier = REFUNDS.putIfAbsent(orderId, issued);
        if (earlier != null) {
            throw new AppException(409, "Order " + orderId + " was already refunded as " + earlier.get("refund_id"));
        }
        return issued;
    }

    private static Map<String, Object> item(String sku, String name, int quantity, double price) {
        Map<String, Object> line = new LinkedHashMap<>();
        line.put("sku", sku);
        line.put("name", name);
        line.put("quantity", quantity);
        line.put("price", price);
        return line;
    }

    @SafeVarargs
    private static void order(String orderId, String placedOn, double total, String status,
                              Map<String, Object>... items) {
        Map<String, Object> ordered = new LinkedHashMap<>();
        ordered.put(ORDER_ID, orderId);
        ordered.put("placed_on", placedOn);
        ordered.put("items", List.of(items));
        ordered.put("total", total);
        ordered.put("currency", CURRENCY);
        ordered.put("status", status);
        ORDERS.put(orderId, ordered);
    }

    private static void shipment(String orderId, String carrier, String tracking, String status,
                                 String updatedOn, String note) {
        Map<String, Object> shipped = new LinkedHashMap<>();
        shipped.put(ORDER_ID, orderId);
        shipped.put("carrier", carrier);
        shipped.put("tracking", tracking);
        shipped.put("status", status);
        shipped.put("updated_on", updatedOn);
        shipped.put("note", note);
        SHIPMENTS.put(orderId, shipped);
    }
}
