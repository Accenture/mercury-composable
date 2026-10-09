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

package org.platformlambda.scheduler;

import org.platformlambda.core.models.EventEnvelope;
import org.platformlambda.core.system.PostOffice;
import org.platformlambda.core.util.AppConfigReader;
import org.platformlambda.core.util.Utility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicReference;

/**
 * The active environment of an active-active deployment (RFC-0008). A scheduler deployed in two sites,
 * Production and DR, must fire its jobs in ONE of them: {@code scheduler.environment} names this
 * instance's environment, {@code scheduler.active.environment} names the one whose scheduler runs the
 * jobs at startup (both default to {@code prod}), and this instance is <b>active</b> when the two match
 * and on <b>standby</b> otherwise - a scheduled job is skipped with one log line, an operator's manual
 * run is still honoured.
 * <p>
 * The operations team moves the active environment around a maintenance window through the
 * application's REST endpoint, which calls {@link #activate}. The value is persisted through the
 * function named by {@code scheduler.environment.store} (default {@code v1.environment.store}), a
 * function the application implements against its own data store - the example's file-backed
 * {@code EnvironmentStore} is the template - because a pod restarts without notice and an in-memory
 * value would reset to the property. Its contract: header {@code type=get} answers the persisted name as
 * text (empty when none); header {@code type=set} with {@code environment} and {@code operator} persists
 * the name and answers {@code true}. When the store function is not registered, the value lives in memory
 * only and the scheduler says so at startup. The store is read before every scheduled job, so every
 * instance of every site follows one call, and the persisted value wins over the property.
 */
public final class ActiveEnvironment {
    private static final Logger log = LoggerFactory.getLogger(ActiveEnvironment.class);
    public static final String ENVIRONMENT = "scheduler.environment";
    public static final String ACTIVE_ENVIRONMENT = "scheduler.active.environment";
    public static final String ENVIRONMENT_STORE = "scheduler.environment.store";
    public static final String DEFAULT_ENVIRONMENT = "prod";
    public static final String DEFAULT_STORE = "v1.environment.store";
    /** The store contract's header names and values. */
    public static final String TYPE = "type";
    public static final String GET = "get";
    public static final String SET = "set";
    public static final String ENVIRONMENT_HEADER = "environment";
    public static final String OPERATOR = "operator";
    private static final String ACTIVE = "active";
    private static final String MODE = "mode";
    private static final String STANDBY = "standby";
    private static final String PERSISTENT = "persistent";
    private static final String STORE = "store";
    private static final long STORE_TIMEOUT = 5000;
    private static final ActiveEnvironment INSTANCE = new ActiveEnvironment();

    private final String environment;
    private final String defaultActive;
    private final String store;
    private final AtomicReference<String> active = new AtomicReference<>();

    private ActiveEnvironment() {
        var config = AppConfigReader.getInstance();
        this.environment = name(config.getProperty(ENVIRONMENT, DEFAULT_ENVIRONMENT), ENVIRONMENT);
        this.defaultActive = name(config.getProperty(ACTIVE_ENVIRONMENT, DEFAULT_ENVIRONMENT), ACTIVE_ENVIRONMENT);
        this.store = config.getProperty(ENVIRONMENT_STORE, DEFAULT_STORE).trim();
        this.active.set(defaultActive);
    }

    public static ActiveEnvironment getInstance() {
        return INSTANCE;
    }

    /** This instance's environment ({@code scheduler.environment}). */
    public String getEnvironment() {
        return environment;
    }

    /** The active environment as this instance last read or set it. */
    public String getActive() {
        return active.get();
    }

    /** The route of the persistence function ({@code scheduler.environment.store}). */
    public String getStore() {
        return store;
    }

    /** True when this instance's environment is the active one, by the last read or set value. */
    public boolean isActive() {
        return environment.equalsIgnoreCase(active.get());
    }

    /** True when the store function is registered, so the active environment survives a restart. */
    public boolean isPersistent() {
        return postOffice().exists(store);
    }

    /**
     * Read the store and answer whether this instance should run scheduled jobs now. Without a store,
     * or when the store fails to answer, the last known value decides and the failure is logged.
     *
     * @return true when this instance is active
     */
    public boolean isActiveNow() {
        refresh();
        return isActive();
    }

    /**
     * Read the persisted active environment; the persisted name, when present, wins over the property.
     *
     * @return the active environment after the read
     */
    public String refresh() {
        if (isPersistent()) {
            try {
                var result = postOffice().request(new EventEnvelope().setTo(store).setHeader(TYPE, GET), STORE_TIMEOUT).get();
                if (result.getBody() instanceof String name && !name.isBlank()) {
                    active.set(name.trim());
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("Active environment not read from {} - interrupted; keeping {}", store, active.get());
            } catch (ExecutionException e) {
                var cause = e.getCause() != null ? e.getCause() : e;
                log.warn("Active environment not read from {} - {}; keeping {}", store, cause.getMessage(), active.get());
            }
        }
        return active.get();
    }

    /**
     * Set the active environment: persisted first when there is a store, then kept in memory.
     *
     * @param name the environment that runs the scheduled jobs from now on, e.g. {@code prod} or {@code DR}
     * @param operator who asked, for the store's record and the log
     * @return the status after the change ({@link #status()})
     * @throws IllegalArgumentException for a blank or malformed name
     * @throws IllegalStateException when the store refuses or fails to persist the name
     */
    public Map<String, Object> activate(String name, String operator) {
        var target = name(name, ACTIVE);
        var who = operator == null || operator.isBlank() ? "unknown" : operator.trim();
        if (isPersistent()) {
            persist(target, who);
        }
        active.set(target);
        log.info("Active environment set to {} by {} - this instance ({}) is {}", target, who, environment,
                isActive() ? ACTIVE : STANDBY);
        return status();
    }

    private void persist(String target, String who) {
        var event = new EventEnvelope().setTo(store).setHeader(TYPE, SET)
                .setHeader(ENVIRONMENT_HEADER, target).setHeader(OPERATOR, who);
        try {
            var result = postOffice().request(event, STORE_TIMEOUT).get();
            if (result.getStatus() != 200 || !Boolean.TRUE.equals(result.getBody())) {
                throw new IllegalStateException("Active environment not persisted by " + store + " - " + result.getBody());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Active environment not persisted by " + store + " - interrupted");
        } catch (ExecutionException e) {
            var cause = e.getCause() != null ? e.getCause() : e;
            throw new IllegalStateException("Active environment not persisted by " + store + " - " + cause.getMessage());
        }
    }

    /** The status an operations dashboard shows: environment, active, mode, persistent, store. */
    public Map<String, Object> status() {
        var result = new LinkedHashMap<String, Object>();
        result.put(ENVIRONMENT_HEADER, environment);
        result.put(ACTIVE, active.get());
        result.put(MODE, isActive() ? ACTIVE : STANDBY);
        result.put(PERSISTENT, isPersistent());
        result.put(STORE, store);
        return result;
    }

    /** Drop the in-memory value, as a restart does; the next {@link #refresh()} reads the store again. */
    public void reset() {
        active.set(defaultActive);
    }

    /** An environment name is one word of letters, digits, hyphens and underscores. */
    private static String name(String value, String what) {
        var text = value == null ? "" : value.trim();
        if (text.isEmpty() || !text.chars().allMatch(c -> Character.isLetterOrDigit(c) || c == '-' || c == '_')) {
            throw new IllegalArgumentException(what + " must be one word of letters, digits, hyphens and underscores");
        }
        return text;
    }

    private static PostOffice postOffice() {
        return new PostOffice("scheduler.environment", Utility.getInstance().getUuid(), "ACTIVE environment");
    }
}
