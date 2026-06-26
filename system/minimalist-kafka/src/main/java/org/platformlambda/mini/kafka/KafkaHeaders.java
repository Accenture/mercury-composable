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
 * Header names used by the minimalist Kafka building blocks. The W3C {@code traceparent} header is not
 * declared here - the framework's {@code org.platformlambda.core.util.W3cTrace.TRACEPARENT} is used.
 */
public final class KafkaHeaders {

    /** Destination topic for {@code simple.kafka.notification} (required routing header). */
    public static final String TOPIC = "topic";

    /** Target partition for {@code simple.kafka.notification} (optional routing header). */
    public static final String PARTITION = "partition";

    /** Correlation-id convention: carried as a Kafka header and used as the flow's correlation-id. */
    public static final String CORRELATION_ID = "cid";

    private KafkaHeaders() {}
}
