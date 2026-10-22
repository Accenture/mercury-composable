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
 * What an observer learns about an invocation ({@link InvocationObserver#begin}) or an outgoing event
 * ({@link InvocationObserver#send}).
 *
 * @param route the route that runs the function, or the destination route of an outgoing event
 *              (without an {@code @origin} suffix)
 * @param from the sender's route, or null when unknown
 * @param eventId the envelope id
 * @param traceId Mercury's trace id (an observed event always carries one)
 * @param tracePath Mercury's trace path, or null
 * @param spanId the sender's span id carried by the event, or null
 * @param instance the worker instance number (1 for an outgoing event)
 */
public record InvocationInfo(String route, String from, String eventId,
                             String traceId, String tracePath, String spanId, int instance) {
}
