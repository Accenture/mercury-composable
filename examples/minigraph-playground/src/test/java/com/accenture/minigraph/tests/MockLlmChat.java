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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicReference;
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
 *     catalog's one tool and answers with the validated code; for the three graphs on the built-in agent tools
 *     (RFC-0013) it plays a research assistant that crawls a documentation section and writes notes, an analyst
 *     that extracts a document's parties, obligations, rules and deadlines into the knowledge graph, and an
 *     answerer that reads the graph's schema, queries it and cites the sources; for the plan-assistant graph it plays a
 *     planning assistant that drafts the options and asks the owner through the ask row (RFC-0014), then writes the plan
 *     for the option the owner picked.</li>
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
    private static final String CRAWL_CALL = "n1";
    private static final String WRITE_CALL = "n2";
    private static final String EXTRACT_CALL = "g1";
    private static final String INGEST_CALL = "g2";
    private static final String SCHEMA_CALL = "q1";
    private static final String QUERY_CALL = "q2";
    private static final String ASK_CALL = "p1";
    private static final String ASK_TOOL = "ask_owner";
    private static final List<String> PLANS = List.of("A: one release behind a feature flag",
            "B: two releases, the data migration first", "C: a canary in one region before the rest");
    private static final Pattern FIELD = Pattern.compile("(Section|Topic|Document):\\s*([^;\\n]+)");
    private static final Pattern PARTIES = Pattern.compile("\\*\\*Parties:\\*\\*\\s*(.+)");
    private static final Pattern PARTY = Pattern.compile("([^,]+?)\\s*\\(([^)]+)\\)");
    private static final Pattern FACT = Pattern.compile("^- (OBL-\\d+|RULE-\\d+|\\d{4}-\\d{2}-\\d{2}): (.+)$", Pattern.MULTILINE);
    private static final Pattern HEADING = Pattern.compile("^# (.+)$", Pattern.MULTILINE);
    private static final String DIAGRAM_CODE = "flowchart TD\n  A[Request] --> B{Review}\n  B -->|approve| C[Approved]\n  B -->|reject| D[Rejected]";
    private static final ConcurrentMap<String, Conversation> CONVERSATIONS = new ConcurrentHashMap<>();
    private static final AtomicReference<Map<String, String>> LAST_GRAPH_CONTEXT = new AtomicReference<>(Map.of());

    /**
     * The graph context the most recent loop turn carried: {@code x-graph-id} and {@code x-node}, which the engine's
     * task node sends and the loop copies onto each turn (RFC-0012 section 8).
     */
    public static Map<String, String> lastGraphContext() {
        return LAST_GRAPH_CONTEXT.get();
    }

    /** What the scripted assistant remembers of one conversation: the record's turn count and the facts. */
    private static final class Conversation {
        private final Object lock = new Object();
        private int turn;
        private boolean diagram;
        private String kind;
        private String section;
        private String topic;
        private String document;
        private List<Map<String, Object>> pages;
        private String crawlError;
        private Map<String, Object> written;
        private String writeError;
        private String extracted;
        private String extractError;
        private Map<String, Object> ingested;
        private String ingestError;
        private Map<String, Object> counts;
        private Map<String, Object> schema;
        private String schemaError;
        private List<List<Object>> rows;
        private String queryError;
        private Map<String, Object> render;
        private String renderError;
        private String orderId;
        private Map<String, Object> order;
        private String orderError;
        private Map<String, Object> shipment;
        private String shipmentError;
        private Map<String, Object> refund;
        private String refundError;
        private String task;
        private Object answer;
        private String answerError;
    }

    @Override
    public Map<String, Object> handleEvent(Map<String, String> headers, Map<String, Object> input, int instance)
            throws AppException {
        if (input.get("tools") != null) {
            var context = new HashMap<String, String>();
            for (var name : List.of("x-graph-id", "x-node")) {
                if (headers.get(name) != null) {
                    context.put(name, headers.get(name));
                }
            }
            LAST_GRAPH_CONTEXT.set(Map.copyOf(context));
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
        synchronized (conversation.lock) {
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
                conversation.kind = hasTool(input.get("tools"), "web_crawl") ? "notes"
                        : hasTool(input.get("tools"), "graph_ingest") ? "ingest"
                        : hasTool(input.get("tools"), "graph_query") ? "query"
                        : hasTool(input.get("tools"), ASK_TOOL) ? "plan" : null;
                firstTurn(conversation, String.valueOf(input.get("prompt")), reply);
            } else if (conversation.diagram) {
                laterDiagramTurn(conversation, reply);
            } else if ("notes".equals(conversation.kind)) {
                laterNotesTurn(conversation, reply);
            } else if ("ingest".equals(conversation.kind)) {
                laterIngestTurn(conversation, reply);
            } else if ("query".equals(conversation.kind)) {
                laterQueryTurn(conversation, reply);
            } else if ("plan".equals(conversation.kind)) {
                laterPlanTurn(conversation, reply);
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

    private static String field(String prompt, String name) {
        Matcher matcher = FIELD.matcher(prompt);
        while (matcher.find()) {
            if (name.equals(matcher.group(1))) {
                return matcher.group(2).trim();
            }
        }
        return null;
    }

    private static String slug(String text) {
        return text.toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
    }

    private static void firstTurn(Conversation conversation, String prompt, Map<String, Object> reply) {
        if ("notes".equals(conversation.kind)) {
            // the research assistant reads the section first - a bounded crawl under its path
            conversation.section = field(prompt, "Section");
            conversation.topic = field(prompt, "Topic");
            if (conversation.section == null || conversation.topic == null) {
                done(reply, "Please name the documentation section and the topic.");
                return;
            }
            String include = conversation.section.replaceFirst("^https?://[^/]+", "");
            calls(reply, List.of(call(CRAWL_CALL, "web_crawl", Map.of("url", conversation.section,
                    "max_pages", 5, "include", include.isEmpty() ? "/" : include))));
            return;
        }
        if ("ingest".equals(conversation.kind)) {
            conversation.document = field(prompt, "Document");
            if (conversation.document == null) {
                done(reply, "Please name the document to ingest.");
                return;
            }
            calls(reply, List.of(call(EXTRACT_CALL, "file_extract", Map.of("path", conversation.document))));
            return;
        }
        if ("query".equals(conversation.kind)) {
            conversation.topic = prompt;
            calls(reply, List.of(call(SCHEMA_CALL, "graph_schema", Map.of())));
            return;
        }
        if ("plan".equals(conversation.kind)) {
            // the planning assistant drafts the options and asks the owner - the ask row waits like a marked write
            conversation.task = prompt;
            calls(reply, List.of(call(ASK_CALL, ASK_TOOL, Map.of(
                    "question", "Which plan should '" + prompt + "' follow?", "options", PLANS))));
            return;
        }
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

    /** The notes assistant: the crawl's pages become a notes file, written under the topic's slug; the write waits. */
    private static void laterNotesTurn(Conversation conversation, Map<String, Object> reply) {
        if (conversation.crawlError != null) {
            done(reply, "I could not read the section " + conversation.section + ": " + conversation.crawlError + ".");
        } else if (conversation.written != null) {
            done(reply, "I wrote the notes on " + conversation.topic + " to " + conversation.written.get("path") + " ("
                    + conversation.written.get("bytes") + " bytes) after reading " + conversation.pages.size()
                    + " pages of " + conversation.section + ". They cover the configuration keys and the runtime overrides.");
        } else if (conversation.writeError != null) {
            done(reply, "I could not write the notes: " + conversation.writeError + ". In short, the section covers the "
                    + "configuration keys and the -D overrides.");
        } else {
            StringBuilder notes = new StringBuilder("# Notes on " + conversation.topic + "\n");
            for (Map<String, Object> page : conversation.pages) {
                notes.append("\n## ").append(page.get("title")).append("\n\n").append(page.get("text"))
                        .append("\n\nSource: ").append(page.get("url")).append("\n");
            }
            calls(reply, List.of(call(WRITE_CALL, "file_write", Map.of(
                    "path", "notes/" + slug(conversation.topic) + ".md", "content", notes.toString()))));
        }
    }

    /** The analyst: the document's facts, extracted by the corpus's shape, become one idempotent ingest. */
    private static void laterIngestTurn(Conversation conversation, Map<String, Object> reply) {
        if (conversation.extractError != null) {
            done(reply, "I could not read " + conversation.document + ": " + conversation.extractError + ".");
        } else if (conversation.ingested != null) {
            done(reply, "Ingested " + conversation.ingested.get("nodes") + " nodes and " + conversation.ingested.get("relationships")
                    + " relationships from " + conversation.document + ": " + conversation.counts.get("parties") + " parties, "
                    + conversation.counts.get("obligations") + " obligations, " + conversation.counts.get("rules") + " rules and "
                    + conversation.counts.get("deadlines") + " deadlines, each with the document as its source.");
        } else if (conversation.ingestError != null) {
            done(reply, "I could not ingest " + conversation.document + ": " + conversation.ingestError + ".");
        } else {
            calls(reply, List.of(call(INGEST_CALL, "graph_ingest", extraction(conversation))));
        }
    }

    /** The answerer: the schema first, then one read-only query for the label the question names, then the answer with sources. */
    private static void laterQueryTurn(Conversation conversation, Map<String, Object> reply) {
        if (conversation.schemaError != null) {
            done(reply, "I could not read the graph's schema: " + conversation.schemaError + ".");
        } else if (conversation.rows != null) {
            if (conversation.rows.isEmpty()) {
                done(reply, "I found nothing about that in the knowledge graph.");
                return;
            }
            StringBuilder answer = new StringBuilder("From the knowledge graph:");
            for (List<Object> row : conversation.rows) {
                if (row.getFirst() instanceof Map<?, ?> node && node.get("properties") instanceof Map<?, ?> properties) {
                    // a fact has a text; a party a name; a document a title
                    Object fact = properties.get("text") != null ? properties.get("text")
                            : properties.get("name") != null ? properties.get("name") : properties.get("title");
                    answer.append("\n- ").append(fact).append(" (source: ").append(properties.get("source")).append(")");
                }
            }
            done(reply, answer.toString());
        } else if (conversation.queryError != null) {
            done(reply, "The query failed: " + conversation.queryError + ".");
        } else if (conversation.schema != null && conversation.schema.get("labels") instanceof List<?> labels && labels.isEmpty()) {
            done(reply, "The knowledge graph holds nothing yet. Ingest a document first.");
        } else {
            String question = conversation.topic.toLowerCase();
            String label = question.contains("deadline") ? "Deadline" : question.contains("rule") ? "Rule"
                    : question.contains("part") ? "Party" : "Obligation";
            calls(reply, List.of(call(QUERY_CALL, "graph_query", Map.of(
                    "query", "MATCH (n:" + label + ") RETURN n ORDER BY n.key", "parameters", Map.of()))));
        }
    }

    /** The corpus's shape as an ingest: the document, its parties, and one node per obligation, rule and deadline. */
    private static Map<String, Object> extraction(Conversation conversation) {
        String text = conversation.extracted;
        String path = conversation.document;
        String documentKey = "document:" + path;
        Matcher heading = HEADING.matcher(text);
        String title = heading.find() ? heading.group(1).trim() : path;
        List<Map<String, Object>> nodes = new ArrayList<>();
        List<Map<String, Object>> relationships = new ArrayList<>();
        nodes.add(node(documentKey, "Document", Map.of("title", title, "path", path)));
        int parties = 0;
        Matcher partyLine = PARTIES.matcher(text);
        if (partyLine.find()) {
            Matcher party = PARTY.matcher(partyLine.group(1).replace(" and ", ", "));
            while (party.find()) {
                String key = "party:" + slug(party.group(1));
                nodes.add(node(key, "Party", Map.of("name", party.group(1).trim(), "role", party.group(2).trim())));
                relationships.add(relationship(key, documentKey, "PARTY_TO"));
                parties++;
            }
        }
        int obligations = 0;
        int rules = 0;
        int deadlines = 0;
        String stem = slug(path.replaceFirst("\\.[a-z]+$", ""));
        Matcher fact = FACT.matcher(text);
        while (fact.find()) {
            String id = fact.group(1);
            String label = id.startsWith("OBL") ? "Obligation" : id.startsWith("RULE") ? "Rule" : "Deadline";
            String key = label.toLowerCase() + ":" + stem + "-" + slug(id);
            Map<String, Object> properties = new LinkedHashMap<>();
            properties.put("text", fact.group(2).trim());
            if ("Deadline".equals(label)) {
                properties.put("date", id);
            } else {
                properties.put("ref", id);
            }
            nodes.add(node(key, label, properties));
            relationships.add(relationship(key, documentKey, "STATED_IN"));
            switch (label) {
                case "Obligation" -> obligations++;
                case "Rule" -> rules++;
                default -> deadlines++;
            }
        }
        conversation.counts = Map.of("parties", parties, "obligations", obligations, "rules", rules, "deadlines", deadlines);
        Map<String, Object> ingest = new LinkedHashMap<>();
        ingest.put("nodes", nodes);
        ingest.put("relationships", relationships);
        ingest.put("source", path);
        return ingest;
    }

    private static Map<String, Object> node(String key, String label, Map<String, Object> properties) {
        Map<String, Object> node = new LinkedHashMap<>();
        node.put("key", key);
        node.put("label", label);
        node.put("properties", properties);
        return node;
    }

    private static Map<String, Object> relationship(String from, String to, String type) {
        Map<String, Object> relationship = new LinkedHashMap<>();
        relationship.put("from", from);
        relationship.put("to", to);
        relationship.put("type", type);
        return relationship;
    }

    /** The planning assistant: the owner's answer is the ask call's result, and the plan follows the option picked. */
    private static void laterPlanTurn(Conversation conversation, Map<String, Object> reply) {
        if (conversation.answerError != null) {
            done(reply, "The owner did not answer: " + conversation.answerError + ". I will wait for the decision.");
        } else {
            done(reply, "Plan for '" + conversation.task + "' - the owner chose " + conversation.answer + ". Steps: "
                    + "1. agree the cut-over window with the team; 2. prepare the rollback; 3. ship as chosen; "
                    + "4. watch the error budget for a day; 5. close the task.");
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
                    case CRAWL_CALL -> {
                        conversation.pages = output != null && output.get("pages") instanceof List<?> pages ?
                                (List<Map<String, Object>>) pages : List.of();
                        conversation.crawlError = error;
                    }
                    case WRITE_CALL -> { conversation.written = output; conversation.writeError = error; }
                    case EXTRACT_CALL -> {
                        conversation.extracted = output == null ? null : String.valueOf(output.get("text"));
                        conversation.extractError = error;
                    }
                    case INGEST_CALL -> { conversation.ingested = output; conversation.ingestError = error; }
                    case SCHEMA_CALL -> { conversation.schema = output; conversation.schemaError = error; }
                    case ASK_CALL -> { conversation.answer = result.get(OUTPUT); conversation.answerError = error; }
                    case QUERY_CALL -> {
                        conversation.rows = output != null && output.get("rows") instanceof List<?> rows ?
                                (List<List<Object>>) rows : List.of();
                        conversation.queryError = error;
                    }
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
