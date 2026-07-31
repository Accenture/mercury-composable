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

package org.platformlambda.mini.kafka;

import org.platformlambda.core.util.MultiLevelMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Compiled second-level routing rules for one consumer binding - the {@code flows} alternative to a
 * binding's single {@code flow}. Each rule inspects one key-value of the inbound record and picks the
 * target flow or function per message:
 *
 * <pre>
 * flows:
 *   - 'input.header.type(order) -&gt; flow://order-flow'
 *   - 'input.header.type(order-*) -&gt; flow://order-variant-flow'
 *   - 'input.header.type(regex: ^shipment-(eu|us)$) -&gt; flow://shipment-flow'
 *   - 'input.body.event.kind(refund) -&gt; task://v1.refund.processor'
 *   - 'default -&gt; flow://catch-all-flow'
 * </pre>
 *
 * <p><b>Rule syntax.</b> {@code <selector>(<matcher>) -> <target>} plus the mandatory
 * {@code default -> <target>} fallback. The selector is {@code input.header.<name>} (a Kafka record
 * header; the header <i>name</i> lookup is case-insensitive because Kafka preserves the producer's
 * wire casing) or {@code input.body} followed by a dot-bracket composite path - a Map body via
 * {@code input.body.order.type}, a TOP-LEVEL List body via {@code input.body[0].type}, and any nesting
 * of the two. The matcher has three modes: exact ({@code type(order)}, case-sensitive), wildcard when
 * the value contains {@code *} ({@code type(order-*)}), and regex only via the explicit {@code regex:}
 * prefix ({@code type(regex: ^a|b$)}) - regex is the exception, not the norm. Wildcard and regex
 * matchers use full-string matching (the {@code topic-pattern} precedent).</p>
 *
 * <p><b>Evaluation.</b> Order matters: the first matching rule wins, in declaration order. A missing
 * header/key, a byte[] body for an {@code input.body} rule, or a non-String value is a non-match -
 * never an error. When no rule matches, the {@code default} target is used. Body lookups run under a
 * synthetic {@code body} root (see {@link #bodyPath}), which both makes a top-level List addressable
 * and keeps MultiLevelMap's {@code $}-JsonPath escape hatch (whose parser can throw) structurally
 * unreachable - '{@code $}'-prefixed keys are ordinary literal segments here.</p>
 *
 * <p><b>Targets.</b> {@code flow://<flow-id>} dispatches to an Event Script flow exactly as direct
 * {@code flow} routing does; {@code task://<route>} invokes a registered function directly (see
 * {@link KafkaFlowConsumer} for the dispatch contract). Any other scheme is rejected at compile time,
 * the same way CompileFlows rejects a {@code ://} that is not {@code flow://}.</p>
 *
 * <p>Compiled once at startup by {@link KafkaFlowAdapter} (fail-fast on any malformed rule) and then
 * read-only on the consumer's poll thread.</p>
 */
public final class RoutingRuleSet {

    private static final Logger log = LoggerFactory.getLogger(RoutingRuleSet.class);
    private static final String HEADER_SELECTOR = "input.header.";
    private static final String BODY_PREFIX = "input.body";
    private static final String BODY_SELECTOR = BODY_PREFIX + ".";
    private static final String BODY_INDEX_SELECTOR = BODY_PREFIX + "[";
    // synthetic root the record body is evaluated under - makes a top-level List addressable and keeps
    // every lookup path away from MultiLevelMap's '$'-JsonPath dispatch (full-path prefix check only)
    private static final String BODY_ROOT = "body";
    private static final String DEFAULT_RULE = "default";
    private static final String ARROW = "->";
    private static final String REGEX_PREFIX = "regex:";
    private static final String FLOW_PROTOCOL = "flow://";
    private static final String TASK_PROTOCOL = "task://";
    private static final String ROUTING_RULE = "routing rule '";

    /**
     * One routing destination: an Event Script flow ({@code task} false, {@code destination} = flow-id)
     * or a direct function invocation ({@code task} true, {@code destination} = route name).
     */
    public record Target(boolean task, String destination) {

        static Target flow(String flowId) {
            return new Target(false, flowId);
        }

        /** Display form for logs and error messages, e.g. {@code flow 'order-flow'} or {@code task 'v1.refund'}. */
        public String label() {
            return (task ? "task '" : "flow '") + destination + "'";
        }
    }

    private enum SelectorType { HEADER, BODY }

    /**
     * One compiled rule: {@code pattern} is null in exact mode, {@code exact} is null in pattern mode.
     * {@code key} is the header name (HEADER) or the precomputed lookup path under the synthetic
     * {@code body} root (BODY), e.g. {@code body.event.kind} / {@code body[0].type}.
     */
    private record Rule(SelectorType selector, String key, String exact, Pattern pattern, Target target) {

        boolean matches(String value) {
            return exact != null ? exact.equals(value) : pattern.matcher(value).matches();
        }
    }

    private final List<Rule> rules;
    private final Target defaultTarget;

    private RoutingRuleSet(List<Rule> rules, Target defaultTarget) {
        this.rules = Collections.unmodifiableList(rules);
        this.defaultTarget = defaultTarget;
    }

    /**
     * Compile a binding's {@code flows} rule list. Fail-fast: any malformed rule, a missing or duplicate
     * {@code default}, or an invalid regex is an {@link IllegalArgumentException} at startup.
     *
     * @param ruleStrings the raw rule strings in declaration order
     * @return the compiled, immutable rule set
     */
    public static RoutingRuleSet compile(List<String> ruleStrings) {
        if (ruleStrings == null || ruleStrings.isEmpty()) {
            throw new IllegalArgumentException("'flows' must be a non-empty list of routing rules");
        }
        List<Rule> rules = new ArrayList<>();
        Target defaultTarget = null;
        for (String raw : ruleStrings) {
            String rule = raw == null ? "" : raw.trim();
            int arrow = rule.indexOf(ARROW);
            if (arrow == -1) {
                throw new IllegalArgumentException(ROUTING_RULE + raw + "' must use the syntax "
                        + "'<selector>(<matcher>) -> <target>' or 'default -> <target>'");
            }
            Target target = parseTarget(rule.substring(arrow + ARROW.length()).trim(), raw);
            String lhs = rule.substring(0, arrow).trim();
            if (DEFAULT_RULE.equals(lhs)) {
                if (defaultTarget != null) {
                    throw new IllegalArgumentException("only one 'default' routing rule is allowed");
                }
                defaultTarget = target;
            } else {
                rules.add(parseRule(lhs, target, raw));
            }
        }
        if (defaultTarget == null) {
            throw new IllegalArgumentException("'flows' must contain a 'default -> <target>' routing rule");
        }
        return new RoutingRuleSet(rules, defaultTarget);
    }

    /** Parse the {@code <selector>(<matcher>)} left-hand side of one rule. */
    private static Rule parseRule(String lhs, Target target, String raw) {
        int open = lhs.indexOf('(');
        if (open < 1 || !lhs.endsWith(")")) {
            throw new IllegalArgumentException(ROUTING_RULE + raw
                    + "' must use the syntax '<selector>(<matcher>) -> <target>'");
        }
        String selector = lhs.substring(0, open).trim();
        String matcher = lhs.substring(open + 1, lhs.length() - 1).trim();
        final SelectorType type;
        final String key;
        if (selector.startsWith(HEADER_SELECTOR)) {
            type = SelectorType.HEADER;
            key = selector.substring(HEADER_SELECTOR.length());
            if (key.isEmpty()) {
                throw new IllegalArgumentException(ROUTING_RULE + raw + "' is missing a header name");
            }
        } else if (selector.startsWith(BODY_SELECTOR) || selector.startsWith(BODY_INDEX_SELECTOR)) {
            type = SelectorType.BODY;
            key = bodyPath(selector, raw);
        } else {
            throw new IllegalArgumentException(ROUTING_RULE + raw + "' selector must be "
                    + "'input.header.<name>', 'input.body.<key>' or 'input.body[<index>]...'");
        }
        if (matcher.isEmpty()) {
            throw new IllegalArgumentException(ROUTING_RULE + raw + "' is missing a matcher value");
        }
        return newRule(type, key, matcher, target, raw);
    }

    /**
     * Precompute a body rule's lookup path: the record body (Map or List) is evaluated under the
     * synthetic {@code body} root, so a top-level List is addressable with the same dot-bracket
     * convention ({@code input.body[0].type} -> {@code body[0].type}, {@code input.body.event.kind} ->
     * {@code body.event.kind}) - and no lookup path can start with {@code $}, which is what keeps
     * MultiLevelMap's JsonPath escape hatch (whose parser can throw at record time) out of reach.
     */
    private static String bodyPath(String selector, String raw) {
        // the remainder starts with '.' or '[' by construction of the caller's prefix checks
        String relative = selector.substring(BODY_PREFIX.length());
        if (relative.length() < 2) {
            throw new IllegalArgumentException(ROUTING_RULE + raw + "' is missing a body key");
        }
        return BODY_ROOT + relative;
    }

    /** Resolve the matcher mode: explicit {@code regex:} prefix > wildcard (contains {@code *}) > exact. */
    private static Rule newRule(SelectorType type, String key, String matcher, Target target, String raw) {
        if (matcher.startsWith(REGEX_PREFIX)) {
            String expression = matcher.substring(REGEX_PREFIX.length()).trim();
            if (expression.isEmpty()) {
                throw new IllegalArgumentException(ROUTING_RULE + raw + "' has an empty regex expression");
            }
            try {
                return new Rule(type, key, null, Pattern.compile(expression), target);
            } catch (PatternSyntaxException e) {
                throw new IllegalArgumentException(ROUTING_RULE + raw + "' regex is invalid: " + e.getMessage(), e);
            }
        }
        if (matcher.indexOf('*') >= 0) {
            return new Rule(type, key, null, wildcardToPattern(matcher), target);
        }
        return new Rule(type, key, matcher, null, target);
    }

    /** Convert a wildcard matcher to an anchored pattern: literal segments quoted, each {@code *} = any run. */
    private static Pattern wildcardToPattern(String wildcard) {
        StringBuilder sb = new StringBuilder();
        int start = 0;
        for (int i = 0; i < wildcard.length(); i++) {
            if (wildcard.charAt(i) == '*') {
                if (i > start) {
                    sb.append(Pattern.quote(wildcard.substring(start, i)));
                }
                sb.append(".*");
                start = i + 1;
            }
        }
        if (start < wildcard.length()) {
            sb.append(Pattern.quote(wildcard.substring(start)));
        }
        // DOTALL: '*' matches ANY run of characters, including line breaks inside a free-text body value
        return Pattern.compile(sb.toString(), Pattern.DOTALL);
    }

    /** Parse a rule's {@code flow://<flow-id>} or {@code task://<route>} target. */
    private static Target parseTarget(String target, String raw) {
        if (target.startsWith(FLOW_PROTOCOL) && target.length() > FLOW_PROTOCOL.length()) {
            return new Target(false, target.substring(FLOW_PROTOCOL.length()));
        }
        if (target.startsWith(TASK_PROTOCOL) && target.length() > TASK_PROTOCOL.length()) {
            return new Target(true, target.substring(TASK_PROTOCOL.length()));
        }
        throw new IllegalArgumentException(ROUTING_RULE + raw
                + "' target must be 'flow://<flow-id>' or 'task://<route>'");
    }

    /**
     * Select the target for one record: the first matching rule in declaration order, else the default.
     * A non-match (missing header/key, non-Map body, non-String value) never errors.
     *
     * @param headers the record's UTF-8 decoded headers (original wire casing)
     * @param body    the record's body: byte[], or a Map when schema/serializer decoding applied
     * @return the selected target (never null - the default is mandatory)
     */
    public Target select(Map<String, String> headers, Object body) {
        MultiLevelMap bodyMap = body instanceof Map || body instanceof List
                ? new MultiLevelMap(Map.of(BODY_ROOT, body)) : null;
        for (Rule rule : rules) {
            final String value;
            try {
                value = rule.selector() == SelectorType.HEADER
                        ? headerValue(headers, rule.key()) : bodyValue(bodyMap, rule.key());
            } catch (RuntimeException e) {
                // the never-throws contract: a failed lookup is a non-match, never an error - this
                // safety net protects the poll thread from any surprise in a path/value evaluation
                log.debug("Routing rule lookup for '{}' failed; treated as a non-match", rule.key(), e);
                continue;
            }
            if (value != null && rule.matches(value)) {
                return rule.target();
            }
        }
        return defaultTarget;
    }

    /**
     * Case-insensitive header NAME lookup - Kafka preserves the producer's wire casing, so a rule must
     * not depend on it. Header VALUES stay case-sensitive.
     */
    private static String headerValue(Map<String, String> headers, String name) {
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            if (name.equalsIgnoreCase(entry.getKey())) {
                return entry.getValue();
            }
        }
        return null;
    }

    /** A byte[] body or a non-String value is a non-match by design (route on headers instead). */
    private static String bodyValue(MultiLevelMap body, String path) {
        return body != null && body.getElement(path) instanceof String s ? s : null;
    }

    /** Number of rules excluding the default (for the adapter's binding log line). */
    public int size() {
        return rules.size();
    }

    /** Every target this rule set can route to, including the default - for startup validation. */
    public List<Target> allTargets() {
        List<Target> all = new ArrayList<>();
        rules.forEach(rule -> all.add(rule.target()));
        all.add(defaultTarget);
        return all;
    }
}
