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

import org.platformlambda.core.util.AppConfigReader;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * The loop node's settings (RFC-0011, section 6.1). Each is a constant input mapping on the node, so it is visible
 * on the graph and certified with it; a global default for each lives in the application's configuration.
 *
 * @param mode the approval mode: write (the default), auto, manual or unattended
 * @param maxTurns the conversation's turn limit, a proxy for the input tokens the run accumulates
 * @param resultLimit the largest tool result, in characters of JSON text
 * @param transcriptTtl how long a running or waiting transcript lives, as sent to {@code llm.chat}
 * @param retention how long a finished run's transcript is kept, as sent to {@code llm.chat}
 * @param timeoutMs the loop's deadline, from its start
 */
public record LoopSettings(String mode, int maxTurns, int resultLimit, String transcriptTtl, String retention,
                           long timeoutMs) {
    /** A tool marked {@code write: true} waits for a person; every other tool runs. The default. */
    public static final String WRITE = "write";
    /** Every tool runs, a marked write included: the node is designed for automation. */
    public static final String AUTO = "auto";
    /** Every tool call waits for a person: a supervised run. */
    public static final String MANUAL = "manual";
    /** A marked write goes back to the model as a refusal and never waits: a run with nobody to ask. */
    public static final String UNATTENDED = "unattended";
    static final String MODE_INPUT = "mode";
    private static final List<String> MODES = List.of(WRITE, AUTO, MANUAL, UNATTENDED);
    static final String MAX_TURNS_INPUT = "max_turns";
    static final String RESULT_LIMIT_INPUT = "result_limit";
    static final String TRANSCRIPT_TTL_INPUT = "transcript_ttl";
    static final String RETENTION_INPUT = "retention";
    static final String TIMEOUT_INPUT = "timeout";
    private static final Pattern DURATION = Pattern.compile("(\\d+)([smhd]?)");
    private static final Pattern DIGITS = Pattern.compile("\\d+");
    private static final String DURATION_RULE = " must be a duration such as 30m, 12h or 2d (seconds, s, m, h, d)";

    /**
     * The global defaults, from the application's configuration.
     *
     * @param maxTurns {@code llm.run.max.turns}, default 10
     * @param resultLimit {@code llm.run.result.limit}, default 65536
     * @param transcriptTtl {@code llm.run.transcript.ttl}, default 7d
     * @param retention {@code llm.run.transcript.retention}, default 0
     * @param timeout {@code llm.run.timeout}, default 25s - below the graph's default {@code model.ttl} of 30 seconds
     */
    public record Defaults(String maxTurns, String resultLimit, String transcriptTtl, String retention,
                           String timeout) {
        public static final String MAX_TURNS_KEY = "llm.run.max.turns";
        public static final String RESULT_LIMIT_KEY = "llm.run.result.limit";
        public static final String TRANSCRIPT_TTL_KEY = "llm.run.transcript.ttl";
        public static final String RETENTION_KEY = "llm.run.transcript.retention";
        public static final String TIMEOUT_KEY = "llm.run.timeout";

        /**
         * The built-in defaults of section 6.1.
         *
         * @return the defaults
         */
        public static Defaults builtIn() {
            return new Defaults("10", "65536", "7d", "0", "25s");
        }

        /**
         * The defaults from the application's configuration, the built-in ones where a key is not set.
         *
         * @return the defaults
         */
        public static Defaults fromConfig() {
            var config = AppConfigReader.getInstance();
            var builtIn = builtIn();
            return new Defaults(config.getProperty(MAX_TURNS_KEY, builtIn.maxTurns()),
                    config.getProperty(RESULT_LIMIT_KEY, builtIn.resultLimit()),
                    config.getProperty(TRANSCRIPT_TTL_KEY, builtIn.transcriptTtl()),
                    config.getProperty(RETENTION_KEY, builtIn.retention()),
                    config.getProperty(TIMEOUT_KEY, builtIn.timeout()));
        }
    }

    /**
     * Resolve the settings: a node input wins over the global default. Each value is checked, and a bad one names
     * the node input, or the configuration key when the default is bad.
     *
     * @param input the node's input
     * @param defaults the global defaults
     * @return the settings
     * @throws IllegalArgumentException naming the first bad value
     */
    public static LoopSettings resolve(Map<String, Object> input, Defaults defaults) {
        var mode = input.get(MODE_INPUT);
        if (mode == null) {
            mode = WRITE;
        }
        if (!MODES.contains(mode)) {
            throw new IllegalArgumentException("mode must be write, auto, manual or unattended");
        }
        int maxTurns = positive(MAX_TURNS_INPUT, input.get(MAX_TURNS_INPUT), Defaults.MAX_TURNS_KEY,
                defaults.maxTurns());
        int resultLimit = positive(RESULT_LIMIT_INPUT, input.get(RESULT_LIMIT_INPUT), Defaults.RESULT_LIMIT_KEY,
                defaults.resultLimit());
        var ttl = duration(TRANSCRIPT_TTL_INPUT, input.get(TRANSCRIPT_TTL_INPUT), Defaults.TRANSCRIPT_TTL_KEY,
                defaults.transcriptTtl(), 1);
        var retention = duration(RETENTION_INPUT, input.get(RETENTION_INPUT), Defaults.RETENTION_KEY,
                defaults.retention(), 0);
        var timeout = duration(TIMEOUT_INPUT, input.get(TIMEOUT_INPUT), Defaults.TIMEOUT_KEY, defaults.timeout(), 1);
        return new LoopSettings((String) mode, maxTurns, resultLimit, ttl, retention, seconds(timeout) * 1000L);
    }

    private static int positive(String name, Object given, String key, String fallback) {
        var where = given == null ? key : name;
        var value = given == null ? fallback : given;
        BigDecimal n = null;
        if (value instanceof Number number) {
            n = ToolSchema.toDecimal(number);
        } else if (value instanceof String text && DIGITS.matcher(text.trim()).matches()) {
            n = new BigDecimal(text.trim());
        }
        if (n == null || !ToolSchema.isIntegral(n) || n.signum() <= 0
                || n.compareTo(BigDecimal.valueOf(Integer.MAX_VALUE)) > 0) {
            throw new IllegalArgumentException(where + " must be a whole number >= 1");
        }
        return n.intValueExact();
    }

    /** A duration as the language packs read it: seconds, or a number with s, m, h or d. Returned as text. */
    private static String duration(String name, Object given, String key, String fallback, int minimum) {
        var where = given == null ? key : name;
        var value = given == null ? fallback : given;
        String text = null;
        if (value instanceof String s) {
            text = s.trim();
        } else if (value instanceof Number number && ToolSchema.isIntegral(ToolSchema.toDecimal(number))) {
            text = ToolSchema.render(ToolSchema.toDecimal(number));
        }
        if (text == null || !DURATION.matcher(text).matches() || seconds(text) < minimum) {
            throw new IllegalArgumentException(where + DURATION_RULE + " >= " + minimum);
        }
        return text;
    }

    static long seconds(String duration) {
        var m = DURATION.matcher(duration);
        // twelve digits are enough for any duration; a longer one is refused rather than overflowed
        if (!m.matches() || m.group(1).length() > 12) {
            return -1;
        }
        long n = Long.parseLong(m.group(1));
        return switch (m.group(2)) {
            case "m" -> n * 60;
            case "h" -> n * 3600;
            case "d" -> n * 86400;
            default -> n;
        };
    }
}
