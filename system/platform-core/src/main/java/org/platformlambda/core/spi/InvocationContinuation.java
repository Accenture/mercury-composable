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
 * The handle a scope hands over when its function's reactive result completes later on another thread
 * (see {@link InvocationScope#detach}). It is process-local: it is held by reference and never
 * serialized.
 */
public interface InvocationContinuation {

    /**
     * Open a scope on the current thread - the completion thread - for the delivery of the deferred
     * result. The engine calls it exactly once and closes the returned scope on the same thread.
     *
     * @return the scope for the completion, or null when there is nothing to observe
     */
    InvocationScope resume();
}
