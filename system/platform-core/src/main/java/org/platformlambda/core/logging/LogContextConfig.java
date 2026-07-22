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

package org.platformlambda.core.logging;

import org.platformlambda.core.util.AppConfigReader;
import org.platformlambda.core.util.ConfigReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Resolves the log-context template. The feature is ON by default: platform-core ships a built-in
 * {@code default-log-context.yaml} carrying the standard trace context (cid, traceId, tracePath,
 * spanId, parentSpanId, service, timestamp).
 * <p>
 * An application may replace the template by providing its own {@code app-log-context.yaml} on the
 * classpath, or opt out entirely with {@code app.log.context=false} in application.properties.
 * When enabled, the JSON appenders ({@code JsonAppender}, {@code CompactAppender}) add a
 * {@code context} block to each structured log line.
 * <p>
 * The template maps an output key to either:
 * <ul>
 *   <li>a reserved {@code $token} (cid, traceId, tracePath, spanId, parentSpanId, service, utc),
 *       resolved live per log line from the current {@link LogContext}; or</li>
 *   <li>a constant (a literal, or a {@code ${ENV:default}} substitution resolved once here at load).</li>
 * </ul>
 * Loading is via the standard {@link ConfigReader}, so environment-variable substitution and YAML
 * parsing behave like every other config file. Key order is not preserved and does not matter —
 * log aggregators reorder keys on display.
 * <p>
 * Initialization is lazy and thread-safe (initialization-on-demand holder), independent of platform
 * boot order. Touching {@link AppConfigReader} first guarantees the base config is registered so
 * {@code ${ENV:default}} resolves even when an appender initializes early.
 */
public class LogContextConfig {
    private static final Logger log = LoggerFactory.getLogger(LogContextConfig.class);
    private static final String CONFIG_FILE = "classpath:/app-log-context.yaml";
    private static final String DEFAULT_CONFIG_FILE = "classpath:/default-log-context.yaml";
    private static final String FEATURE_FLAG = "app.log.context";
    private static final String CONTEXT = "context";
    private static final String TOKEN_PREFIX = "$";
    private static final String ENV_PREFIX = "${";

    // Eager singleton (same pattern as Utility) - thread-safe via class initialization, which the JVM
    // defers until the class is first referenced (the first log line through a JSON appender).
    private static LogContextConfig instance = new LogContextConfig(loadConfigFile());

    public static LogContextConfig getInstance() {
        return instance;
    }

    /**
     * Replace the singleton instance. Reserved for unit tests that need to exercise the
     * enabled/disabled feature paths deterministically.
     *
     * @param override config instance, or null to reset by reloading the config file
     */
    static void setInstanceForTest(LogContextConfig override) {
        instance = override != null ? override : new LogContextConfig(loadConfigFile());
    }

    private final boolean enabled;
    // output key -> reserved token name (without the leading '$'), resolved live per log line
    private final Map<String, String> tokens = new HashMap<>();
    // output key -> constant value (env-resolved or literal), fixed at load
    private final Map<String, Object> constants = new HashMap<>();

    /**
     * Build a config from a loaded reader (or null when the optional file is absent).
     * Package-private so tests can construct a config directly from an in-memory ConfigReader.
     *
     * @param reader a ConfigReader over app-log-context.yaml, or null to leave the feature disabled
     */
    LogContextConfig(ConfigReader reader) {
        boolean on = false;
        if (reader != null) {
            Object section = reader.getMap().get(CONTEXT);
            if (section instanceof Map<?, ?> raw) {
                for (Object k : raw.keySet()) {
                    parseEntry(reader, String.valueOf(k));
                }
                on = !tokens.isEmpty() || !constants.isEmpty();
            } else {
                log.warn("Log context config has no '{}' section - feature disabled", CONTEXT);
            }
        }
        this.enabled = on;
        if (on) {
            log.info("Application log context enabled with {} context key-value(s)", tokens.size() + constants.size());
        }
    }

    private static ConfigReader loadConfigFile() {
        // ensure the base config is registered so ${ENV:default} substitution works regardless of timing
        AppConfigReader config = AppConfigReader.getInstance();
        if ("false".equals(config.getProperty(FEATURE_FLAG, "true"))) {
            log.info("Application log context disabled by {}=false", FEATURE_FLAG);
            return null;
        }
        try {
            return new ConfigReader(CONFIG_FILE);
        } catch (IllegalArgumentException notFound) {
            // no application override - fall back to the built-in default so the feature is on out of the box
            try {
                return new ConfigReader(DEFAULT_CONFIG_FILE);
            } catch (IllegalArgumentException missing) {
                log.warn("Built-in {} missing - log context feature disabled", DEFAULT_CONFIG_FILE);
                return null;
            }
        }
    }

    private void parseEntry(ConfigReader reader, String outputKey) {
        // ConfigReader resolves ${ENV:default} on the leaf value
        Object resolved = reader.get(CONTEXT + "." + outputKey);
        String value = resolved == null ? null : String.valueOf(resolved);
        if (value != null && value.startsWith(TOKEN_PREFIX) && !value.startsWith(ENV_PREFIX)) {
            String tokenName = value.substring(TOKEN_PREFIX.length());
            if (!LogContext.RESERVED_KEYS.contains(tokenName)) {
                throw new IllegalArgumentException(
                        "Invalid log context token '" + value + "' for key '" + outputKey
                                + "' - allowed tokens: " + LogContext.RESERVED_KEYS);
            }
            tokens.put(outputKey, tokenName);
        } else if (value != null) {
            // constant - env-resolved value or literal; an unset ${VAR} with no default resolves to null and is dropped
            constants.put(outputKey, value);
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Build the context map for one log line. Keys whose value resolves to null are omitted
     * (never emitted as "null").
     *
     * @param context the current request's log context
     * @param logTimeMillis the log event time (for the per-line utc timestamp)
     * @return context key-values to embed under the log "context" field
     */
    public Map<String, Object> render(LogContext context, long logTimeMillis) {
        Map<String, Object> out = new HashMap<>();
        tokens.forEach((outputKey, tokenName) -> {
            Object value = context.token(tokenName, logTimeMillis);
            if (value != null) {
                out.put(outputKey, value);
            }
        });
        out.putAll(constants);
        context.getCustomKeys().forEach((key, value) -> {
            if (value != null) {
                out.put(key, value);
            }
        });
        return out;
    }
}
