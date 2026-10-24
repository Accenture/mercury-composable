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

package org.platformlambda.core.spi;

/**
 * One observed invocation, open on the thread that began it. The engine closes it on that same thread,
 * once: {@link #error} (when the function failed), then {@link #end}. Scopes of several observers close
 * in reverse order of opening, so a tracer that requires LIFO closing on its thread gets it.
 * <p>
 * The engine holds a scope as a local variable and passes it down the call, never in a thread-local or a
 * thread-keyed map, so later work on another thread reaches it by reference, through {@link #detach}.
 */
public interface InvocationScope {

    /**
     * The function failed. Called before {@link #end}.
     *
     * @param message the error message the caller receives
     */
    default void error(String message) {
        // an observer that records no failure details needs no override
    }

    /**
     * The function returned. Close the scope on this thread.
     */
    void end();

    /**
     * The function returned a reactive result (a Mono) that completes later, usually on another thread.
     * Called on the scope's own thread while it is still open, before {@link #end}. The engine resumes
     * the continuation exactly once on the completion thread: when the result arrives, fails, or is
     * cancelled on timeout.
     *
     * @return a continuation, or null when the observer has nothing to resume
     */
    default InvocationContinuation detach() {
        return null;
    }
}
