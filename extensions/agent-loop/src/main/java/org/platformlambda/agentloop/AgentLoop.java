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

package org.platformlambda.agentloop;

import org.platformlambda.core.exception.AppException;
import org.platformlambda.core.models.EventEnvelope;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeSet;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.function.LongSupplier;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * The agent loop (RFC-0011, section 4): one run of model turns to an outcome. Each turn goes to {@code llm.chat},
 * which keeps the transcript in the language pack's store and returns either the outcome or the model's tool
 * calls; the loop resolves each call's name to a route in the node's catalog, validates the input, applies the
 * node's approval mode, calls the route, and sends every result of the turn back in one message. It holds the
 * conversation's reference, never the transcript.
 * <p>
 * A model outcome - done, an approval, a refusal, a limit - is the envelope a decision node routes on. Only the
 * loop's own machinery throws: a bad input or catalog (before a token is spent), a resume whose decisions do not
 * match the waiting calls, or {@code llm.chat} failing after the pack's own retries.
 */
public class AgentLoop {
    /** Time kept back from the deadline so the envelope reaches the graph before the node's own timeout. */
    public static final long RESERVE_MS = 1000;
    /** The least time a model turn, or a round of tool calls, is started with. */
    public static final long MIN_STEP_MS = 1000;
    static final String TOOLS = "tools";
    static final String SYSTEM = "system";
    static final String PROMPT = "prompt";
    static final String MESSAGES = "messages";
    static final String SCHEMA = "schema";
    static final String MODEL = "model";
    static final String PARAMS = "params";
    static final String CONVERSATION = "conversation";
    static final String TURN = "turn";
    static final String APPROVAL = "approval";
    static final String DECISIONS = "decisions";
    static final String STATUS = "status";
    static final String TEXT = "text";
    static final String DATA = "data";
    static final String DECISION = "decision";
    static final String CAP = "cap";
    static final String REFUSAL = "refusal";
    static final String TURNS = "turns";
    static final String USAGE = "usage";
    static final String CALLS = "calls";
    static final String ID = "id";
    static final String NAME = "name";
    static final String INPUT = "input";
    static final String ROUTE = "route";
    static final String WAIT = "wait";
    static final String APPROVED = "approved";
    static final String NOTE = "note";
    static final String OUTPUT = "output";
    static final String ERROR = "error";
    static final String RESULTS = "results";
    static final String TTL = "ttl";
    static final String RETENTION = "retention";
    static final String TIMEOUT_MS = "timeout_ms";
    static final String DONE = "done";
    static final String TOOL_USE = "tool_use";
    static final String CAP_TURNS = "turns";
    static final String CAP_DEADLINE = "deadline";
    static final String OK = "ok";
    static final String UNKNOWN = "unknown";
    static final String INVALID = "invalid";
    static final String REFUSED = "refused";
    static final String ANSWERED = "answered";
    static final String ASK = "ask";
    static final String ANSWER = "answer";
    static final List<String> USAGE_KEYS = List.of("input_tokens", "output_tokens", "cache_creation_input_tokens",
            "cache_read_input_tokens");
    static final List<String> INPUTS = List.of(TOOLS, SYSTEM, PROMPT, MESSAGES, SCHEMA, MODEL, PARAMS,
            LoopSettings.MODE_INPUT, LoopSettings.MAX_TURNS_INPUT, LoopSettings.RESULT_LIMIT_INPUT,
            LoopSettings.TRANSCRIPT_TTL_INPUT, LoopSettings.RETENTION_INPUT, LoopSettings.TIMEOUT_INPUT, CONVERSATION,
            TURN, APPROVAL, DECISIONS);
    private static final Pattern REFERENCE = Pattern.compile("[0-9a-f]{32}");
    private static final Pattern WHOLE_NUMBER = Pattern.compile("\\d{1,9}");
    private static final String LLM_CHAT_FAILED = "llm.chat failed - ";
    private static final String OUTSIDE_CONTRACT = "llm.chat answered outside the loop contract - ";
    private static final String NO_LONGER_MATCHES = "') no longer matches the tool catalog or the mode";

    private final LoopGateway gateway;
    private final LoopSettings.Defaults defaults;
    private final LongSupplier clock;

    /**
     * @param gateway how the loop reaches {@code llm.chat} and the tools' routes
     * @param defaults the global defaults of the settings
     * @param clock the time in milliseconds, from which the deadline is measured
     */
    public AgentLoop(LoopGateway gateway, LoopSettings.Defaults defaults, LongSupplier clock) {
        this.gateway = gateway;
        this.defaults = defaults;
        this.clock = clock;
    }

    private enum Outcome { RUN, WAIT, ERROR, ANSWER }

    /**
     * One call of a turn, planned: run it, wait for a person, answer it with an error, or answer it with what a person
     * said. {@code value} is the error text for ERROR and the person's answer for ANSWER.
     */
    private record Planned(String id, String name, ToolCatalog.Row row, Object input, Outcome outcome,
                           String label, Object value) { }

    /** The run's own state; the transcript is never part of it. */
    private static final class Run {
        final LoopSettings settings;
        final ToolCatalog catalog;
        final Map<String, Object> input;
        final long deadline;
        String conversation;
        int turn;
        int turns;
        String model;
        final Map<String, Long> usage = new LinkedHashMap<>();
        final List<Map<String, Object>> calls = new ArrayList<>();

        Run(LoopSettings settings, ToolCatalog catalog, Map<String, Object> input, long deadline) {
            this.settings = settings;
            this.catalog = catalog;
            this.input = input;
            this.deadline = deadline;
            USAGE_KEYS.forEach(key -> usage.put(key, 0L));
        }
    }

    /**
     * Run the loop.
     *
     * @param input the node's input
     * @return the outcome envelope
     * @throws AppException for a failure of the loop's own machinery
     */
    public Map<String, Object> run(Map<String, Object> input) throws AppException {
        var start = clock.getAsLong();
        LoopSettings settings;
        ToolCatalog catalog;
        try {
            checkInputNames(input);
            settings = LoopSettings.resolve(input, defaults);
            catalog = ToolCatalog.parse(input.get(TOOLS), gateway::reachable);
            checkRunInputs(input);
        } catch (IllegalArgumentException e) {
            throw new AppException(400, e.getMessage());
        }
        if (!gateway.reachable(PlatformGateway.LLM_CHAT)) {
            throw new AppException(503, "llm.chat is not reachable - deploy a language pack's LLM helper and " +
                    "declare llm.chat in yaml.event.over.http");
        }
        var run = new Run(settings, catalog, input, start + settings.timeoutMs());
        List<Map<String, Object>> results = null;
        if (input.get(CONVERSATION) != null) {
            run.conversation = (String) input.get(CONVERSATION);
            run.turn = intOf(input.get(TURN));
            var planned = planResume(run);
            if (run.turn >= settings.maxTurns()) {
                return cap(run, CAP_TURNS);
            }
            if (timeLeft(run) < MIN_STEP_MS) {
                return cap(run, CAP_DEADLINE);
            }
            results = dispatch(run, planned);
        }
        return turns(run, results);
    }

    private Map<String, Object> turns(Run run, List<Map<String, Object>> answers) throws AppException {
        var results = answers;
        while (true) {
            var budget = timeLeft(run);
            if (budget < MIN_STEP_MS) {
                return cap(run, CAP_DEADLINE);
            }
            var turn = modelTurn(run, results, budget);
            if (turn.isEmpty()) {
                return cap(run, CAP_DEADLINE);
            }
            var reply = turn.get();
            var outcome = outcome(run, reply);
            if (outcome.isPresent()) {
                return outcome.get();
            }
            if (run.turn >= run.settings.maxTurns()) {
                return cap(run, CAP_TURNS);
            }
            var planned = planTurn(run, reply.get(CALLS));
            if (planned.stream().anyMatch(p -> p.outcome() == Outcome.WAIT)) {
                return approval(run, planned);
            }
            if (timeLeft(run) < MIN_STEP_MS) {
                return cap(run, CAP_DEADLINE);
            }
            results = dispatch(run, planned);
        }
    }

    /** One model turn's reply, or empty when the turn timed out: the loop's deadline, named as a cap. */
    @SuppressWarnings("unchecked")
    private Optional<Map<String, Object>> modelTurn(Run run, List<Map<String, Object>> results, long budget)
            throws AppException {
        var request = chatRequest(run, results, budget);
        var response = await(() -> gateway.chat(request, budget + RESERVE_MS / 2));
        if (response.getStatus() == 408) {
            return Optional.empty();
        }
        if (response.hasError()) {
            throw new AppException(response.getStatus(), LLM_CHAT_FAILED + errorText(response));
        }
        if (!(response.getBody() instanceof Map)) {
            throw new AppException(502, OUTSIDE_CONTRACT + "the reply is not a map");
        }
        var reply = (Map<String, Object>) response.getBody();
        noteTurn(run, reply);
        return Optional.of(reply);
    }

    /** The envelope for a model outcome, or empty for a tool_use turn. */
    private static Optional<Map<String, Object>> outcome(Run run, Map<String, Object> reply) throws AppException {
        var status = reply.get(STATUS);
        if (DONE.equals(status)) {
            return Optional.of(done(run, reply));
        }
        if (REFUSAL.equals(status)) {
            return Optional.of(envelope(run, REFUSAL).with(REFUSAL, reply.get(REFUSAL)).build());
        }
        if (CAP.equals(status)) {
            return Optional.of(envelope(run, CAP).with(CAP, reply.get(CAP)).build());
        }
        if (!TOOL_USE.equals(status)) {
            throw new AppException(502, OUTSIDE_CONTRACT + "status '" + status + "'");
        }
        return Optional.empty();
    }

    // ------------------------------------------------------------------ inputs

    private static void checkInputNames(Map<String, Object> input) {
        var unknown = input.keySet().stream().filter(key -> !INPUTS.contains(key))
                .collect(Collectors.toCollection(TreeSet::new));
        if (!unknown.isEmpty()) {
            throw new IllegalArgumentException("not an input of llm.run: " + String.join(", ", unknown) +
                    " - the inputs are " + String.join(", ", INPUTS.subList(0, INPUTS.size() - 1)) + " and " +
                    INPUTS.getLast());
        }
    }

    private static void checkRunInputs(Map<String, Object> input) {
        optional(input, SYSTEM, String.class, "system must be text");
        optional(input, SCHEMA, Map.class, "schema must be a JSON schema object");
        if (input.get(MODEL) != null && !(input.get(MODEL) instanceof String model && !model.isBlank())) {
            throw new IllegalArgumentException("model must be a model id");
        }
        optional(input, PARAMS, Map.class, "params must be a map");
        if (input.get(PARAMS) instanceof Map<?, ?> params && params.containsKey(MODEL)) {
            throw new IllegalArgumentException("params.model is not set here - use the model input");
        }
        if (input.get(PARAMS) instanceof Map<?, ?> params && params.containsKey(TIMEOUT_MS)) {
            throw new IllegalArgumentException("params.timeout_ms is set by the loop from its deadline - " +
                    "use the timeout input");
        }
        if (input.get(CONVERSATION) == null) {
            checkFirstTurn(input);
        } else {
            checkResume(input);
        }
    }

    private static void checkFirstTurn(Map<String, Object> input) {
        for (var key : List.of(TURN, APPROVAL, DECISIONS)) {
            if (input.get(key) != null) {
                throw new IllegalArgumentException(key + " needs conversation - a run continues only from " +
                        "an approval");
            }
        }
        boolean hasPrompt = input.get(PROMPT) instanceof String text && !text.isBlank();
        boolean hasMessages = input.get(MESSAGES) instanceof List<?> list && !list.isEmpty();
        if (!hasPrompt && !hasMessages) {
            throw new IllegalArgumentException("prompt or messages is required on a run's first turn");
        }
    }

    private static void checkResume(Map<String, Object> input) {
        if (!(input.get(CONVERSATION) instanceof String reference) || !REFERENCE.matcher(reference).matches()) {
            throw new IllegalArgumentException("conversation must be the 32-hex reference a run returned");
        }
        if (intOf(input.get(TURN)) < 1) {
            throw new IllegalArgumentException("turn must be the turn count the approval returned");
        }
        if (!(input.get(APPROVAL) instanceof Map<?, ?> approval) || !(approval.get(CALLS) instanceof List<?> calls)
                || calls.isEmpty()) {
            throw new IllegalArgumentException("approval must be the approval block the run returned, " +
                    "{calls: [...]} - a run continues only from an approval");
        }
        if (!(input.get(DECISIONS) instanceof List)) {
            throw new IllegalArgumentException("decisions must be a list of {id, approved, note}, one per " +
                    "waiting call");
        }
    }

    private static void optional(Map<String, Object> input, String key, Class<?> type, String message) {
        var value = input.get(key);
        if (value != null && !type.isInstance(value)) {
            throw new IllegalArgumentException(message);
        }
    }

    private static int intOf(Object value) {
        if (value instanceof Number n) {
            var d = ToolSchema.toDecimal(n);
            if (ToolSchema.isIntegral(d) && d.abs().compareTo(java.math.BigDecimal.valueOf(Integer.MAX_VALUE)) <= 0) {
                return d.intValueExact();
            }
        }
        if (value instanceof String text && WHOLE_NUMBER.matcher(text.trim()).matches()) {
            return Integer.parseInt(text.trim());
        }
        return -1;
    }

    // ------------------------------------------------------------------ the model turn

    @SuppressWarnings("unchecked")
    private Map<String, Object> chatRequest(Run run, List<Map<String, Object>> results, long budget) {
        var input = run.input;
        var request = new LinkedHashMap<String, Object>();
        if (input.get(SYSTEM) != null) {
            request.put(SYSTEM, input.get(SYSTEM));
        }
        if (run.conversation == null) {
            if (input.get(PROMPT) != null) {
                request.put(PROMPT, input.get(PROMPT));
            }
            if (input.get(MESSAGES) != null) {
                request.put(MESSAGES, input.get(MESSAGES));
            }
        }
        if (input.get(SCHEMA) != null) {
            request.put(SCHEMA, input.get(SCHEMA));
        }
        request.put(TOOLS, run.catalog.advertised());
        var params = new LinkedHashMap<String, Object>();
        if (input.get(PARAMS) instanceof Map) {
            params.putAll((Map<String, Object>) input.get(PARAMS));
        }
        if (input.get(MODEL) != null) {
            params.put(MODEL, input.get(MODEL));
        }
        params.put(TIMEOUT_MS, budget);
        request.put(PARAMS, params);
        if (run.conversation != null) {
            request.put(CONVERSATION, run.conversation);
            request.put(TURN, run.turn);
            request.put(RESULTS, results);
        }
        request.put(TTL, run.settings.transcriptTtl());
        request.put(RETENTION, run.settings.retention());
        return request;
    }

    @SuppressWarnings("unchecked")
    private static void noteTurn(Run run, Map<String, Object> reply) throws AppException {
        if (!(reply.get(CONVERSATION) instanceof String reference) || reference.isEmpty()) {
            throw new AppException(502, OUTSIDE_CONTRACT + "no conversation");
        }
        var turn = intOf(reply.get(TURN));
        if (turn < 1) {
            throw new AppException(502, OUTSIDE_CONTRACT + "no turn count");
        }
        run.conversation = reference;
        run.turn = turn;
        run.turns++;
        if (reply.get(MODEL) instanceof String model) {
            run.model = model;
        }
        if (reply.get(USAGE) instanceof Map) {
            var usage = (Map<String, Object>) reply.get(USAGE);
            for (var key : USAGE_KEYS) {
                if (usage.get(key) instanceof Number n) {
                    run.usage.merge(key, n.longValue(), Long::sum);
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> done(Run run, Map<String, Object> reply) {
        var envelope = envelope(run, DONE);
        if (reply.containsKey(DATA)) {
            var data = reply.get(DATA);
            envelope.with(DATA, data);
            // the business decision of section 7.2, lifted when the node's output schema declares it
            if (run.input.get(SCHEMA) instanceof Map<?, ?> schema && schema.get("properties") instanceof Map<?, ?> props
                    && props.containsKey(DECISION) && data instanceof Map<?, ?> map && map.containsKey(DECISION)) {
                envelope.with(DECISION, ((Map<String, Object>) data).get(DECISION));
            }
        } else {
            envelope.with(TEXT, reply.get(TEXT));
        }
        return envelope.build();
    }

    // ------------------------------------------------------------------ the calls of a turn

    @SuppressWarnings("unchecked")
    private static List<Planned> planTurn(Run run, Object raw) throws AppException {
        if (!(raw instanceof List<?> calls) || calls.isEmpty()) {
            throw new AppException(502, OUTSIDE_CONTRACT + "tool_use without calls");
        }
        var planned = new ArrayList<Planned>();
        var ids = new HashSet<String>();
        for (var item : calls) {
            if (!(item instanceof Map) || !(((Map<String, Object>) item).get(ID) instanceof String id)
                    || id.isEmpty() || !ids.add(id)) {
                throw new AppException(502, OUTSIDE_CONTRACT + "a call without a unique id");
            }
            var call = (Map<String, Object>) item;
            var name = call.get(NAME) instanceof String text ? text : String.valueOf(call.get(NAME));
            planned.add(evaluate(run, id, name, call.get(INPUT)));
        }
        return planned;
    }

    /** The steps of section 4 for one call: resolve the name, validate the input, check approval. */
    private static Planned evaluate(Run run, String id, String name, Object input) {
        var row = run.catalog.get(name);
        if (row == null) {
            return new Planned(id, name, null, input, Outcome.ERROR, UNKNOWN, "unknown tool '" + name + "'");
        }
        var violations = new ArrayList<String>();
        row.schema().validate(INPUT, input, violations);
        if (!violations.isEmpty()) {
            return new Planned(id, name, row, input, Outcome.ERROR, INVALID,
                    "invalid input for '" + name + "': " + ToolSchema.report(violations));
        }
        var mode = run.settings.mode();
        if (row.ask()) {
            // a question for a person waits in every mode that has a person to ask
            return LoopSettings.UNATTENDED.equals(mode) ?
                    new Planned(id, name, row, input, Outcome.ERROR, REFUSED,
                            "'" + name + "' asks a person, and this run has nobody to ask") :
                    new Planned(id, name, row, input, Outcome.WAIT, null, null);
        }
        if (row.write() && LoopSettings.UNATTENDED.equals(mode)) {
            return new Planned(id, name, row, input, Outcome.ERROR, REFUSED,
                    "'" + name + "' is a write, and this run is unattended");
        }
        if (LoopSettings.MANUAL.equals(mode) || (row.write() && LoopSettings.WRITE.equals(mode))) {
            return new Planned(id, name, row, input, Outcome.WAIT, null, null);
        }
        return new Planned(id, name, row, input, Outcome.RUN, null, null);
    }

    private static Map<String, Object> approval(Run run, List<Planned> planned) {
        var calls = new ArrayList<Map<String, Object>>();
        for (var p : planned) {
            var call = new LinkedHashMap<String, Object>();
            call.put(ID, p.id());
            call.put(NAME, p.name());
            if (p.row() != null && p.row().route() != null) {
                call.put(ROUTE, p.row().route());
            }
            call.put(INPUT, p.input());
            call.put(WAIT, p.outcome() == Outcome.WAIT);
            if (p.row() != null && p.row().ask()) {
                call.put(ASK, true);
            }
            calls.add(call);
        }
        var block = new LinkedHashMap<String, Object>();
        block.put(CALLS, calls);
        return envelope(run, APPROVAL).with(APPROVAL, block).build();
    }

    /**
     * Section 7.1: the calls come from the restored approval block, never from the resume request, so the call
     * that runs is the call that was shown. Every waiting call needs one decision; anything else throws.
     */
    @SuppressWarnings("unchecked")
    private static List<Planned> planResume(Run run) throws AppException {
        var shown = (List<Object>) ((Map<String, Object>) run.input.get(APPROVAL)).get(CALLS);
        var waiting = new LinkedHashMap<String, Map<String, Object>>();
        var ids = new HashSet<String>();
        for (int i = 0; i < shown.size(); i++) {
            var call = shownCall(shown.get(i), i);
            if (!ids.add((String) call.get(ID))) {
                throw shapeError(i);
            }
            if (Boolean.TRUE.equals(call.get(WAIT))) {
                waiting.put((String) call.get(ID), call);
            }
        }
        var decisions = decisions(run, waiting);
        var planned = new ArrayList<Planned>();
        for (int i = 0; i < shown.size(); i++) {
            planned.add(resumeCall(run, i, (Map<String, Object>) shown.get(i), decisions));
        }
        return planned;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> shownCall(Object item, int i) throws AppException {
        if (item instanceof Map && ((Map<String, Object>) item).get(ID) instanceof String
                && ((Map<String, Object>) item).get(WAIT) instanceof Boolean
                && ((Map<String, Object>) item).get(NAME) instanceof String
                && (((Map<String, Object>) item).get(ASK) == null
                    || ((Map<String, Object>) item).get(ASK) instanceof Boolean)) {
            return (Map<String, Object>) item;
        }
        throw shapeError(i);
    }

    private static AppException shapeError(int i) {
        return new AppException(400, "approval.calls[" + i + "] must be a call the run returned: " +
                "{id, name, route, input, wait}, its id unique");
    }

    private static AppException mismatch(int i, String id) {
        return new AppException(400, "approval.calls[" + i + "] ('" + id + NO_LONGER_MATCHES);
    }

    /** A shown call as it runs on resume: checked against the catalog and the mode, then decided. */
    private static Planned resumeCall(Run run, int i, Map<String, Object> call,
                                      Map<String, Map<String, Object>> decisions) throws AppException {
        var id = (String) call.get(ID);
        var name = (String) call.get(NAME);
        var row = run.catalog.get(name);
        var route = call.get(ROUTE);
        boolean shownAsk = Boolean.TRUE.equals(call.get(ASK));
        boolean sameRow = row == null ? route == null && !shownAsk :
                Objects.equals(row.route(), route) && row.ask() == shownAsk;
        var fresh = evaluate(run, id, name, call.get(INPUT));
        boolean wait = Boolean.TRUE.equals(call.get(WAIT));
        if (!sameRow || wait != (fresh.outcome() == Outcome.WAIT)) {
            throw mismatch(i, id);
        }
        if (!wait) {
            return fresh;
        }
        var decision = decisions.get(id);
        if (row.ask()) {
            return new Planned(id, name, row, call.get(INPUT), Outcome.ANSWER, ANSWERED, decision.get(ANSWER));
        }
        if (Boolean.TRUE.equals(decision.get(APPROVED))) {
            return new Planned(id, name, row, call.get(INPUT), Outcome.RUN, null, null);
        }
        var note = decision.get(NOTE) instanceof String text && !text.isBlank() ? ": " + text : "";
        return new Planned(id, name, row, call.get(INPUT), Outcome.ERROR, REFUSED, "refused by the reviewer" + note);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Map<String, Object>> decisions(Run run, Map<String, Map<String, Object>> waiting)
            throws AppException {
        var given = (List<Object>) run.input.get(DECISIONS);
        var byId = new HashMap<String, Map<String, Object>>();
        for (int i = 0; i < given.size(); i++) {
            var where = "decisions[" + i + "]";
            if (!(given.get(i) instanceof Map<?, ?> item) || !(item.get(ID) instanceof String id)) {
                throw new AppException(400, where + " must be {id, approved, note}, or {id, answer} for a question");
            }
            var decision = (Map<String, Object>) given.get(i);
            var call = waiting.get(id);
            if (call == null) {
                throw new AppException(400, where + ".id '" + id + "' is not a call waiting for approval");
            }
            if (Boolean.TRUE.equals(call.get(ASK))) {
                checkAnswer(where, id, decision);
            } else {
                checkApproval(where, decision);
            }
            if (byId.put(id, decision) != null) {
                throw new AppException(400, where + ".id '" + id + "' is decided twice");
            }
        }
        for (var id : waiting.keySet()) {
            if (!byId.containsKey(id)) {
                throw new AppException(400, "no decision for the waiting call '" + id + "'");
            }
        }
        return byId;
    }

    /** A question's decision is exactly {id, answer}: the person's answer is the call's result. */
    private static void checkAnswer(String where, String id, Map<String, Object> decision) throws AppException {
        if (decision.get(ANSWER) == null || decision.containsKey(APPROVED) || decision.containsKey(NOTE)) {
            throw new AppException(400, where + " answers the question in call '" + id + "': {id, answer}");
        }
    }

    /** A tool call's decision is {id, approved, note}: approved true or false, the note optional text. */
    private static void checkApproval(String where, Map<String, Object> decision) throws AppException {
        if (!(decision.get(APPROVED) instanceof Boolean) || decision.containsKey(ANSWER)) {
            throw new AppException(400, where + " must be {id, approved, note} with approved true or false");
        }
        var note = decision.get(NOTE);
        if (note != null && !(note instanceof String)) {
            throw new AppException(400, where + ".note must be text");
        }
    }

    /**
     * Run the turn's calls concurrently, each bounded by the time left, and answer every call in call order.
     */
    private List<Map<String, Object>> dispatch(Run run, List<Planned> planned) throws AppException {
        var budget = timeLeft(run);
        // every request is issued here, on the worker thread, before any is awaited
        List<CompletableFuture<EventEnvelope>> futures = planned.stream().map(p -> p.outcome() == Outcome.RUN ?
                safely(() -> gateway.call(p.row().route(), p.input(), budget)) : null).toList();
        var results = new ArrayList<Map<String, Object>>();
        for (int i = 0; i < planned.size(); i++) {
            var p = planned.get(i);
            var result = new LinkedHashMap<String, Object>();
            result.put(ID, p.id());
            String label;
            if (p.outcome() == Outcome.RUN) {
                var response = join(futures.get(i));
                if (response.hasError()) {
                    label = ERROR;
                    result.put(ERROR, "error " + response.getStatus() + ": " + errorText(response));
                } else {
                    label = OK;
                    result.put(OUTPUT, capped(response.getBody(), run.settings.resultLimit()));
                }
            } else if (p.outcome() == Outcome.ANSWER) {
                label = p.label();
                result.put(OUTPUT, capped(p.value(), run.settings.resultLimit()));
            } else {
                label = p.label();
                result.put(ERROR, p.value());
            }
            results.add(result);
            var entry = new LinkedHashMap<String, Object>();
            entry.put(ID, p.id());
            entry.put(NAME, p.name());
            if (p.row() != null && p.row().route() != null) {
                entry.put(ROUTE, p.row().route());
            }
            entry.put(STATUS, label);
            run.calls.add(entry);
        }
        return results;
    }

    /**
     * A result over the limit is cut and marked as cut. Text is measured as it is; anything else as its compact
     * JSON text. Characters are code points.
     */
    static Object capped(Object body, int limit) {
        if (body == null) {
            return "";
        }
        var text = body instanceof String s ? s : Json.compact(body);
        int length = text.codePointCount(0, text.length());
        if (length <= limit) {
            return body;
        }
        return text.substring(0, text.offsetByCodePoints(0, limit)) + "\n[the result was cut at " + limit + " of " +
                length + " characters]";
    }

    /** An error response's message as text; a structured error as its compact JSON. */
    private static String errorText(EventEnvelope response) {
        var error = response.getError();
        return error instanceof String text ? text : Json.compact(error);
    }

    // ------------------------------------------------------------------ time and transport

    private long timeLeft(Run run) {
        return run.deadline - clock.getAsLong() - RESERVE_MS;
    }

    private static CompletableFuture<EventEnvelope> safely(Supplier<CompletableFuture<EventEnvelope>> request) {
        try {
            return request.get().exceptionally(AgentLoop::failure);
        } catch (RuntimeException e) {
            return CompletableFuture.completedFuture(failure(e));
        }
    }

    private static EventEnvelope await(Supplier<CompletableFuture<EventEnvelope>> request) throws AppException {
        return join(safely(request));
    }

    private static EventEnvelope join(CompletableFuture<EventEnvelope> future) throws AppException {
        try {
            return future.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AppException(500, "llm.run was interrupted");
        } catch (ExecutionException e) {
            return failure(e);
        }
    }

    /** A request that failed to send, or failed without a response, as an error response. */
    static EventEnvelope failure(Throwable e) {
        var cause = (e instanceof CompletionException || e instanceof ExecutionException) && e.getCause() != null ?
                e.getCause() : e;
        int status = switch (cause) {
            case AppException app -> app.getStatus();
            case TimeoutException ignored -> 408;
            // the platform's routing error: no function serves the route now
            case IllegalArgumentException ignored -> 404;
            default -> 500;
        };
        return new EventEnvelope().setStatus(status).setBody(String.valueOf(cause.getMessage()));
    }

    // ------------------------------------------------------------------ the envelope

    private static Map<String, Object> cap(Run run, String reason) {
        return envelope(run, CAP).with(CAP, reason).build();
    }

    private static Envelope envelope(Run run, String status) {
        return new Envelope(run, status);
    }

    /** The outcome envelope of section 6, its fields in the order the spec lists them. */
    private static final class Envelope {
        private final Map<String, Object> map = new LinkedHashMap<>();
        private final Run run;

        Envelope(Run run, String status) {
            this.run = run;
            map.put(STATUS, status);
        }

        Envelope with(String key, Object value) {
            map.put(key, value);
            return this;
        }

        /** The outcome fields, then the run's references and counts. */
        Map<String, Object> build() {
            if (run.conversation != null) {
                map.put(CONVERSATION, run.conversation);
                map.put(TURN, run.turn);
            }
            map.put(TURNS, run.turns);
            map.put(USAGE, new LinkedHashMap<>(run.usage));
            map.put(CALLS, new ArrayList<>(run.calls));
            if (run.model != null) {
                map.put(MODEL, run.model);
            }
            return map;
        }
    }
}
