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

/**
 * How a {@link KafkaFlowConsumer} handles a flow-processing failure: retry a bounded number of times,
 * then route the un-processable message to a <b>per-topic</b> dead-letter topic before committing, so a
 * poison message is neither lost nor reprocessed forever.
 *
 * <p>The DLQ is strictly per source topic ({@code <topic><dlqSuffix>}) - a shared/global DLQ is an
 * anti-pattern, because mixing many source schemas into one topic makes reprocessing (the whole point of a
 * DLQ) hard. Only the suffix is configurable, since conventions vary across orgs ({@code .dlq},
 * {@code -dlq}, {@code -DLQ}, Spring's {@code .DLT}).</p>
 *
 * @param maxRetries          extra attempts after the first failure (0 = no retry, straight to DLQ).
 * @param backoffMs           pause between attempts in milliseconds (0 = retry immediately).
 * @param dlqSuffix           suffix appended to the source topic to form its DLQ topic (e.g. {@code .dlq});
 *                            a blank suffix falls back to {@code .dlq} so the DLQ can never equal the source.
 * @param deadLetterPublisher the shared publisher used to park exhausted messages on the DLQ topic;
 *                            may be {@code null}, in which case the message is not committed (it redelivers).
 */
public record RetryPolicy(int maxRetries, long backoffMs, String dlqSuffix,
                          KafkaRequestPublisher deadLetterPublisher) {
}
