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

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Holds the per-request {@link LogContext} keyed by the worker thread id.
 * <p>
 * This is the sibling of the trace registry in EventEmitter (which is keyed by
 * {@code Thread.currentThread().threadId()}): the WorkerHandler registers a context right after
 * starting tracing and removes it right after stopping tracing, so the entry exists for exactly
 * the worker's run window. The JSON appenders (which run synchronously on the same worker thread)
 * look the context up by the current thread id at log time.
 */
public class LogContextManager {
    private static final ConcurrentMap<Long, LogContext> contexts = new ConcurrentHashMap<>();

    private LogContextManager() { }

    public static void register(long threadId, LogContext context) {
        contexts.put(threadId, context);
    }

    public static LogContext get(long threadId) {
        return contexts.get(threadId);
    }

    public static void remove(long threadId) {
        contexts.remove(threadId);
    }
}
