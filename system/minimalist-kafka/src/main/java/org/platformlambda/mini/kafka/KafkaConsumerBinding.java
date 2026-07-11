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
 * One validated {@code consumer[]} entry from {@code kafka-flow-adapter.yaml}, resolved by
 * {@link KafkaFlowAdapter#buildConsumer} and handed to {@link KafkaFlowConsumer}. Built via
 * {@link #builder()} rather than a wide constructor - a 9-parameter constructor trips static-analysis
 * parameter-count rules regardless of how many callers there are, whereas a builder's setters take one
 * argument each.
 */
public final class KafkaConsumerBinding {

    private final String topicOrPattern;
    private final boolean pattern;
    private final String flowId;
    private final String groupId;
    private final Integer partition;
    private final boolean schemaEnabled;
    private final String dlqTopic;
    private final boolean autoCommit;
    private final Integer maxPollRecords;
    private final String traceIdHeader;
    private final String correlationIdHeader;

    private KafkaConsumerBinding(Builder b) {
        this.topicOrPattern = b.topicOrPattern;
        this.pattern = b.pattern;
        this.flowId = b.flowId;
        this.groupId = b.groupId;
        this.partition = b.partition;
        this.schemaEnabled = b.schemaEnabled;
        this.dlqTopic = b.dlqTopic;
        this.autoCommit = b.autoCommit;
        this.maxPollRecords = b.maxPollRecords;
        this.traceIdHeader = b.traceIdHeader;
        this.correlationIdHeader = b.correlationIdHeader;
    }

    /** Literal topic name, or the regex pattern text when {@link #isPattern()} is true. */
    public String topicOrPattern() {
        return topicOrPattern;
    }

    /** True when {@link #topicOrPattern()} is a regex pattern (subscribed via {@code subscribe(Pattern)}). */
    public boolean isPattern() {
        return pattern;
    }

    public String flowId() {
        return flowId;
    }

    public String groupId() {
        return groupId;
    }

    /** Non-null only when this binding pins a single partition (mutually exclusive with {@link #isPattern()}). */
    public Integer partition() {
        return partition;
    }

    public boolean schemaEnabled() {
        return schemaEnabled;
    }

    /** The binding's dead-letter topic, or {@code null} when no {@code dlq-topic} was configured. */
    public String dlqTopic() {
        return dlqTopic;
    }

    /** False (default) = manual {@code commitSync} after each successful flow; true = Kafka-native auto-commit. */
    public boolean autoCommit() {
        return autoCommit;
    }

    /** Explicit {@code max-poll-records} override, or {@code null} to let the caller pick the mode's default. */
    public Integer maxPollRecords() {
        return maxPollRecords;
    }

    /**
     * Per-binding inbound trace-id header override ({@code trace.id.header}), or {@code null} to use the
     * global {@code kafka.trace.id.header}. Impedance matching for an upstream that does not send a W3C
     * {@code traceparent} - a well-formed traceparent always takes precedence.
     */
    public String traceIdHeader() {
        return traceIdHeader;
    }

    /**
     * Per-binding inbound business correlation-id header override ({@code correlation.id.header}), or
     * {@code null} to use the global {@code kafka.correlation.id.header} (default {@code cid}).
     */
    public String correlationIdHeader() {
        return correlationIdHeader;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        private String topicOrPattern;
        private boolean pattern;
        private String flowId;
        private String groupId;
        private Integer partition;
        private boolean schemaEnabled;
        private String dlqTopic;
        private boolean autoCommit;
        private Integer maxPollRecords;
        private String traceIdHeader;
        private String correlationIdHeader;

        private Builder() {
        }

        public Builder topic(String topic) {
            this.topicOrPattern = topic;
            this.pattern = false;
            return this;
        }

        public Builder topicPattern(String topicPattern) {
            this.topicOrPattern = topicPattern;
            this.pattern = true;
            return this;
        }

        public Builder flowId(String flowId) {
            this.flowId = flowId;
            return this;
        }

        public Builder groupId(String groupId) {
            this.groupId = groupId;
            return this;
        }

        public Builder partition(Integer partition) {
            this.partition = partition;
            return this;
        }

        public Builder schemaEnabled(boolean schemaEnabled) {
            this.schemaEnabled = schemaEnabled;
            return this;
        }

        public Builder dlqTopic(String dlqTopic) {
            this.dlqTopic = dlqTopic;
            return this;
        }

        public Builder autoCommit(boolean autoCommit) {
            this.autoCommit = autoCommit;
            return this;
        }

        public Builder maxPollRecords(Integer maxPollRecords) {
            this.maxPollRecords = maxPollRecords;
            return this;
        }

        public Builder traceIdHeader(String traceIdHeader) {
            this.traceIdHeader = traceIdHeader;
            return this;
        }

        public Builder correlationIdHeader(String correlationIdHeader) {
            this.correlationIdHeader = correlationIdHeader;
            return this;
        }

        public KafkaConsumerBinding build() {
            return new KafkaConsumerBinding(this);
        }
    }
}
