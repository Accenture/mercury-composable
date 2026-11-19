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

import java.time.Instant;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A stand-in for {@code person.ask}, the main flow of RFC-0014 served by a language pack's ask-person extension:
 * it checks the message a hold path sends - the kind, the graph's name and description, the correlation id, and
 * for a wait the turn, the waiting calls and the suspension's ttl - records it with the graph context the task
 * node sends, and answers as the extension does: a ticket, the resume link and the expiry for a wait, and what was
 * delivered; {@code delivered} alone for an advice. {@link #DOWN} makes it fail like a pack that is not running,
 * so the graphs' handlers are exercised: the hold stands without a link, and a resumed run goes on.
 */
@PreLoad(route = "person.ask", instances = 10)
public class MockAskPerson implements TypedLambdaFunction<Map<String, Object>, Map<String, Object>> {
    public static final String REPLY_URL = "https://ask.example.test";
    public static final String CHANNEL = "agent.question.inbox";
    /** Every message taken, in order, each with the {@code x-graph-id} and {@code x-node} headers it arrived with. */
    public static final List<Map<String, Object>> MESSAGES = new CopyOnWriteArrayList<>();
    /** True makes the function fail with a 503, as a pack that is down would. */
    public static final AtomicBoolean DOWN = new AtomicBoolean(false);
    private static final AtomicInteger TICKETS = new AtomicInteger();
    private static final Set<String> KINDS = Set.of("approval", "question", "advice");
    private static final Pattern DURATION = Pattern.compile("^(\\d+)\\s*(ms|s|m|h|d)?$");
    private static final Map<String, Long> UNIT_SECONDS = Map.of("ms", 0L, "s", 1L, "m", 60L, "h", 3600L, "d", 86400L);

    /** The messages of one transaction, in the order they were taken. */
    public static List<Map<String, Object>> messagesFor(String correlationId) {
        return MESSAGES.stream().filter(m -> correlationId.equals(m.get("correlation_id"))).toList();
    }

    @Override
    public Map<String, Object> handleEvent(Map<String, String> headers, Map<String, Object> input, int instance)
            throws AppException {
        if (DOWN.get()) {
            throw new AppException(503, "person.ask is down");
        }
        Object kind = input.get("kind");
        if (!(kind instanceof String k) || !KINDS.contains(k)) {
            throw new AppException(400, "kind must be one of approval, question, advice");
        }
        for (String name : List.of("graph_id", "description", "correlation_id", "summary")) {
            if (!(input.get(name) instanceof String text) || text.isBlank()) {
                throw new AppException(400, name + " is required");
            }
        }
        Map<String, Object> result = new HashMap<>();
        result.put("delivered", List.of(Map.of("route", CHANNEL, "ok", true, "status", 200)));
        if (!"advice".equals(kind)) {
            if (!(input.get("turn") instanceof Number)) {
                throw new AppException(400, "turn is required for an approval or a question");
            }
            if (!(input.get("calls") instanceof List<?> calls) || calls.isEmpty()) {
                throw new AppException(400, "calls must be a non-empty list of the waiting calls, each with an id");
            }
            long seconds = seconds(input.get("ttl"));
            String ticket = "t-" + TICKETS.incrementAndGet();
            result.put("ticket", ticket);
            result.put("link", REPLY_URL + "/ask/" + ticket);
            result.put("expires_at", Instant.now().plusSeconds(seconds).toString());
        } else if (input.containsKey("calls") || input.containsKey("ttl")) {
            throw new AppException(400, "an advice carries no calls and no ttl");
        }
        Map<String, Object> record = new LinkedHashMap<>(input);
        record.put("x-graph-id", headers.get("x-graph-id"));
        record.put("x-node", headers.get("x-node"));
        MESSAGES.add(record);
        return result;
    }

    private static long seconds(Object ttl) throws AppException {
        String text = ttl instanceof Number n ? String.valueOf(n.longValue()) : ttl instanceof String s ? s.trim() : null;
        Matcher matcher = text == null ? null : DURATION.matcher(text);
        if (matcher == null || !matcher.matches()) {
            throw new AppException(400, "ttl must be a duration such as 30m, 12h or 2d (seconds, s, m, h, d) > 0");
        }
        String unit = matcher.group(2) == null ? "s" : matcher.group(2);
        long seconds = Long.parseLong(matcher.group(1)) * UNIT_SECONDS.get(unit);
        if (seconds <= 0) {
            throw new AppException(400, "ttl must be a duration such as 30m, 12h or 2d (seconds, s, m, h, d) > 0");
        }
        return seconds;
    }
}
