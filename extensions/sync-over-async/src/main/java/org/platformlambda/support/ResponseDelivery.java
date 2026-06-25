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

package org.platformlambda.support;

/**
 * Sink for a response consumed from the Kafka response topic. The production wiring is
 * {@code ReturnRouteCoordinator::deliver}, which routes the payload back to the originating pod.
 */
@FunctionalInterface
public interface ResponseDelivery {

    /**
     * @param correlationId the correlation-id carried by the response.
     * @param payload the response payload (already decoded to text), possibly {@code null}.
     * @return {@code true} if a caller was waiting and received the response; {@code false} if the
     *         response was orphaned (no return route - e.g. the originating pod is gone or timed out).
     */
    boolean deliver(String correlationId, String payload);
}
