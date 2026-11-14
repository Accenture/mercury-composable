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

package com.accenture.minigraph.tests;

import org.platformlambda.core.annotations.PreLoad;
import org.platformlambda.core.exception.AppException;
import org.platformlambda.core.models.TypedLambdaFunction;
import org.platformlambda.core.util.Utility;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A scripted stand-in for the LLM helper's {@code llm.chat} route, so the AI demo graphs run in the
 * test scope without tokens or credentials.
 * <p>
 * In production the route resolves to the LLM helper app (mercury-python or mercury-nodejs) through
 * declarative Event-over-HTTP; here the same route name resolves to this mock - the route-decoupling
 * contract is what makes an LLM node mockable by configuration. Two contracts are served:
 * <ul>
 * <li>a plain chat (the support-triage graph): the reply mirrors the {@code llm.chat} contract (data,
 *     usage, model, stop_reason) with a deterministic verdict derived from the prompt;</li>
 * <li>a loop turn (the order-assistant and diagram-assistant graphs, a request with {@code tools}): the reply follows the loop
 *     contract of RFC-0011 section 3.2 - {@code tool_use} with the calls, or {@code done} with the text,
 *     a conversation reference and a turn count the next turn must echo, a 409 when it does not - and
 *     the script plays a careful assistant: it looks the order up and checks the shipment, proposes a
 *     refund only for a lost shipment, and answers from the tool results it was given, including a
 *     refund that failed or was refused; for the diagram assistant it drafts a flowchart, validates it through the
 *     catalog's one tool and answers with the validated code.</li>
 * </ul>
 */
@PreLoad(route = "llm.chat", instances = 10)
public class MockLlmChat implements TypedLambdaFunction<Map<String, Object>, Map<String, Object>> {
    private static final Pattern ORDER_ID = Pattern.compile("ORD-\\d+");
    private static final String ORDER_ID_KEY = "order_id";
    private static final String MODEL = "mock-llm";
    private static final String STATUS = "status";
    private static final String OUTPUT = "output";
    private static final String ERROR = "error";
    private static final String LOOKUP_CALL = "c1";
    private static final String SHIPMENT_CALL = "c2";
    private static final String REFUND_CALL = "c3";
    private static final String RENDER_CALL = "d1";
    private static final String RENDER_TOOL = "render_diagram";
    private static final String DIAGRAM_CODE = "flowchart TD\n  A[Request] --> B{Review}\n  B -->|approve| C[Approved]\n  B -->|reject| D[Rejected]";
    private static final ConcurrentMap<String, Conversation> CONVERSATIONS = new ConcurrentHashMap<>();

    /** What the scripted assistant remembers of one conversation: the record's turn count and the facts. */
    private static final class Conversation {
        private int turn;
        private boolean diagram;
        private Map<String, Object> render;
        private String renderError;
        private String orderId;
        private Map<String, Object> order;
        private String orderError;
        private Map<String, Object> shipment;
        private String shipmentError;
        private Map<String, Object> refund;
        private String refundError;
    }

    @Override
    public Map<String, Object> handleEvent(Map<String, String> headers, Map<String, Object> input, int instance)
            throws AppException {
        if (input.get("tools") != null) {
            return loopTurn(input);
        }
        String prompt = String.valueOf(input.get("prompt"));
        String label = prompt.contains("crash")? "bug" :
                        prompt.contains("wish") || prompt.contains("feature")? "feature" : "question";
        return Map.of(
                "data", Map.of("label", label, "reason", "mock verdict for the token-free test"),
                "usage", Map.of("input_tokens", 42, "output_tokens", 7),
                "model", MODEL,
                "stop_reason", "end_turn");
    }

    private Map<String, Object> loopTurn(Map<String, Object> input) throws AppException {
        Utility util = Utility.getInstance();
        String reference = input.get("conversation") == null ? null : String.valueOf(input.get("conversation"));
        Conversation conversation;
        if (reference == null) {
            reference = util.getUuid();
            conversation = new Conversation();
            CONVERSATIONS.put(reference, conversation);
        } else {
            conversation = CONVERSATIONS.get(reference);
            if (conversation == null) {
                throw new AppException(404, "Conversation not found: " + reference);
            }
        }
        int expected = input.get("turn") == null ? 0 : util.str2int(String.valueOf(input.get("turn")));
        synchronized (conversation) {
            if (expected != conversation.turn) {
                throw new AppException(409, "The record holds turn " + conversation.turn + ", not " + expected);
            }
            conversation.turn++;
            if (input.get("results") instanceof List<?> results) {
                remember(conversation, results);
            }
            Map<String, Object> reply = new HashMap<>();
            if (conversation.turn == 1) {
                conversation.diagram = hasTool(input.get("tools"), RENDER_TOOL);
                firstTurn(conversation, String.valueOf(input.get("prompt")), reply);
            } else if (conversation.diagram) {
                laterDiagramTurn(conversation, reply);
            } else {
                laterTurn(conversation, reply);
            }
            reply.put("conversation", reference);
            reply.put("turn", conversation.turn);
            reply.put("model", MODEL);
            reply.put("usage", Map.of("input_tokens", 500, "output_tokens", 40,
                    "cache_creation_input_tokens", 0, "cache_read_input_tokens", 0));
            return reply;
        }
    }

    private static boolean hasTool(Object tools, String name) {
        if (tools instanceof List<?> rows) {
            for (Object row : rows) {
                if (row instanceof Map<?, ?> map && name.equals(map.get("name"))) {
                    return true;
                }
            }
        }
        return false;
    }

    private static void firstTurn(Conversation conversation, String prompt, Map<String, Object> reply) {
        if (conversation.diagram) {
            // the diagram assistant drafts the code and validates it before answering
            calls(reply, List.of(call(RENDER_CALL, RENDER_TOOL, Map.of(
                    "diagramCode", DIAGRAM_CODE, "title", prompt.length() > 40 ? prompt.substring(0, 40) : prompt))));
            return;
        }
        Matcher matcher = ORDER_ID.matcher(prompt);
        if (matcher.find()) {
            conversation.orderId = matcher.group();
            Map<String, Object> arguments = Map.of(ORDER_ID_KEY, conversation.orderId);
            calls(reply, List.of(call(LOOKUP_CALL, "order_lookup", arguments),
                                 call(SHIPMENT_CALL, "shipment_status", arguments)));
        } else {
            done(reply, "Please give me your order number. It starts with ORD-.");
        }
    }

    private static void laterTurn(Conversation conversation, Map<String, Object> reply) {
        String id = conversation.orderId;
        if (conversation.orderError != null) {
            done(reply, "I could not find order " + id + ": " + conversation.orderError + ".");
        } else if (conversation.shipmentError != null) {
            done(reply, "I found order " + id + " but not its shipment: " + conversation.shipmentError + ".");
        } else if (conversation.refund != null) {
            done(reply, "I have issued refund " + conversation.refund.get("refund_id") + " of "
                    + conversation.refund.get("amount") + " " + conversation.refund.get("currency") + " for order "
                    + id + " because the carrier lost the shipment. It reaches your statement within five business days.");
        } else if (conversation.refundError != null) {
            done(reply, "I could not issue the refund for order " + id + ": " + conversation.refundError + ".");
        } else if ("lost".equals(conversation.shipment.get(STATUS))) {
            calls(reply, List.of(call(REFUND_CALL, "issue_refund", Map.of(ORDER_ID_KEY, id,
                    "amount", conversation.order.get("total"),
                    "reason", "The carrier lost the shipment of order " + id))));
        } else {
            done(reply, "Your order " + id + " is " + conversation.shipment.get(STATUS) + ". "
                    + conversation.shipment.get("note"));
        }
    }

    private static void laterDiagramTurn(Conversation conversation, Map<String, Object> reply) {
        if (conversation.renderError != null) {
            done(reply, "I could not validate the diagram: " + conversation.renderError + ".");
        } else if (conversation.render != null && Boolean.TRUE.equals(conversation.render.get("valid"))) {
            done(reply, "Here is the validated diagram:\n```mermaid\n" + conversation.render.get("diagramCode")
                    + "\n```\nIt shows the steps from the request to the decision. The rendered image is stored under the reference "
                    + conversation.render.get("reference") + ".");
        } else {
            done(reply, "The renderer reported the diagram invalid and I could not fix it.");
        }
    }

    @SuppressWarnings("unchecked")
    private static void remember(Conversation conversation, List<?> results) {
        for (Object entry : results) {
            if (entry instanceof Map<?, ?> result) {
                String id = String.valueOf(result.get("id"));
                Map<String, Object> output = result.get(OUTPUT) instanceof Map<?, ?> map ?
                                                (Map<String, Object>) map : null;
                String error = result.containsKey(ERROR) ? String.valueOf(result.get(ERROR)) : null;
                switch (id) {
                    case RENDER_CALL -> { conversation.render = output; conversation.renderError = error; }
                    case LOOKUP_CALL -> { conversation.order = output; conversation.orderError = error; }
                    case SHIPMENT_CALL -> { conversation.shipment = output; conversation.shipmentError = error; }
                    case REFUND_CALL -> { conversation.refund = output; conversation.refundError = error; }
                    default -> throw new IllegalArgumentException("Unexpected call id " + id);
                }
            }
        }
    }

    private static Map<String, Object> call(String id, String name, Map<String, Object> arguments) {
        Map<String, Object> call = new HashMap<>();
        call.put("id", id);
        call.put("name", name);
        call.put("input", arguments);
        return call;
    }

    private static void calls(Map<String, Object> reply, List<Map<String, Object>> calls) {
        reply.put(STATUS, "tool_use");
        reply.put("calls", new ArrayList<>(calls));
        reply.put("stop_reason", "tool_use");
    }

    private static void done(Map<String, Object> reply, String text) {
        reply.put(STATUS, "done");
        reply.put("text", text);
        reply.put("stop_reason", "end_turn");
    }
}
