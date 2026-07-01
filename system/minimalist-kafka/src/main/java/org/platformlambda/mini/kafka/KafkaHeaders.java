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

    /**
     * Optional for {@code simple.kafka.notification}: the registry <b>subject</b> whose schema serializes the
     * body into the Confluent wire format. The subject + {@link #VERSION} are resolved to a global schema id
     * (and type) via the Schema Registry client; the caller never needs to know the id. Absent ⇒ the body is
     * published as raw byte[] (the default minimalist behavior).
     */
    public static final String SUBJECT = "subject";

    /**
     * Optional companion to {@link #SUBJECT}: the subject version to resolve - {@code "latest"} (the default
     * when omitted) or a positive integer. The schema type is resolved from the registry, not supplied.
     */
    public static final String VERSION = "version";

    /** Default {@link #VERSION} when a {@link #SUBJECT} is given without one. */
    public static final String DEFAULT_VERSION = "latest";

    private KafkaHeaders() {}
}
