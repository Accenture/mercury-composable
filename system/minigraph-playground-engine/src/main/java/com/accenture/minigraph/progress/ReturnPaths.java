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

package com.accenture.minigraph.progress;

import org.platformlambda.core.util.AppConfigReader;
import org.platformlambda.core.util.Utility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * The routes an application declares as return paths ({@code return.path.routes}, RFC-0011 section 8.4). A caller
 * names its return route in an event's {@code x-return-path}, and the graph engine sends progress events and the
 * final result there only when the application declares the route: otherwise a caller could make the engine send
 * events to any route it can name, a store or a write operation included.
 * <p>
 * An entry that ends with {@code *} matches a route prefix, and any other entry matches one route exactly - the rule
 * {@code invocation.observer.include} applies to route names. A bare {@code *}, a {@code *} anywhere but at the end,
 * and an entry whose fixed part is not a valid route name are refused at startup, each logged by name; a refused
 * entry declares nothing.
 */
public final class ReturnPaths {
    private static final Logger log = LoggerFactory.getLogger(ReturnPaths.class);
    public static final String RETURN_PATH_ROUTES = "return.path.routes";
    private static final String WILDCARD = "*";
    private static final AtomicReference<ReturnPaths> CONFIGURED = new AtomicReference<>();
    private final List<String> exact = new ArrayList<>();
    private final List<String> prefixes = new ArrayList<>();
    private final List<String> refusals = new ArrayList<>();

    ReturnPaths(String text) {
        var util = Utility.getInstance();
        for (var entry : util.split(text == null? "" : text, ", ")) {
            var refusal = refusal(entry);
            if (refusal != null) {
                refusals.add(RETURN_PATH_ROUTES + " entry '" + entry + "' refused - " + refusal);
            } else if (entry.endsWith(WILDCARD)) {
                prefixes.add(entry.substring(0, entry.length() - 1));
            } else {
                exact.add(entry);
            }
        }
    }

    private static String refusal(String entry) {
        if (WILDCARD.equals(entry)) {
            return "a bare '*' would declare every route";
        }
        var star = entry.indexOf(WILDCARD);
        if (star >= 0 && star != entry.length() - 1) {
            return "'*' may only end an entry";
        }
        var fixed = star >= 0? entry.substring(0, star) : entry;
        // a prefix may end on a whole segment (order.status.*), so its trailing separator is not part of the name
        var name = star >= 0 && fixed.endsWith(".")? fixed.substring(0, fixed.length() - 1) : fixed;
        if (!Utility.getInstance().validServiceName(name) || !name.contains(".")) {
            return "'" + name + "' is not a valid route name";
        }
        return null;
    }

    /**
     * The application's declared return paths, read once from {@code return.path.routes}. Each refused entry is
     * logged when the list is first read, which the graph compiler does at startup.
     *
     * @return the declared return paths
     */
    public static ReturnPaths getInstance() {
        var current = CONFIGURED.get();
        if (current == null) {
            current = new ReturnPaths(AppConfigReader.getInstance().getProperty(RETURN_PATH_ROUTES, ""));
            if (CONFIGURED.compareAndSet(null, current)) {
                current.refusals.forEach(log::error);
                if (!current.exact.isEmpty() || !current.prefixes.isEmpty()) {
                    log.info("Return paths declared - routes {}, prefixes {}", current.exact, current.prefixes);
                }
            } else {
                current = CONFIGURED.get();
            }
        }
        return current;
    }

    /**
     * Whether the application declares a route as a return path.
     *
     * @param route the route a caller named
     * @return true when an entry declares it
     */
    public boolean isDeclared(String route) {
        if (route == null || !Utility.getInstance().validServiceName(route)) {
            return false;
        }
        return exact.contains(route) || prefixes.stream().anyMatch(route::startsWith);
    }

    /**
     * The refused entries, each message naming the entry and why.
     *
     * @return the refusals, in the order of the list
     */
    public List<String> refusals() {
        return List.copyOf(refusals);
    }
}
