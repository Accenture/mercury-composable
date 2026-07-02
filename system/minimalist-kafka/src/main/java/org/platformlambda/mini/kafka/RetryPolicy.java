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
 * then route the un-processable message to the binding's configured dead-letter topic before committing,
 * so a poison message is neither lost nor reprocessed forever.
 *
 * <p>The DLQ topic itself is a per-{@link KafkaConsumerBinding} concern ({@code dlq-topic} in
 * {@code kafka-flow-adapter.yaml}), not part of this shared/global policy - this record only carries the
 * retry/backoff shape and the publisher used to write to whichever DLQ topic the binding configured.</p>
 *
 * @param maxRetries          extra attempts after the first failure (0 = no retry, straight to DLQ).
 * @param backoffMs           pause between attempts in milliseconds (0 = retry immediately).
 * @param deadLetterPublisher the shared publisher used to park exhausted messages on a DLQ topic; may be
 *                            {@code null}, in which case a dead-letter write is skipped and the message is
 *                            dropped with a logged {@code ERROR} (the offset still commits, to avoid a
 *                            redelivery storm - see {@link KafkaFlowConsumer#writeToDeadLetter}).
 */
public record RetryPolicy(int maxRetries, long backoffMs, KafkaRequestPublisher deadLetterPublisher) {
}
