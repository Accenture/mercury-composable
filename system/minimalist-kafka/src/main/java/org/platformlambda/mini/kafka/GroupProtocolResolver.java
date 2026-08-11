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

import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.DescribeFeaturesOptions;
import org.apache.kafka.clients.admin.FeatureMetadata;
import org.apache.kafka.clients.admin.FinalizedVersionRange;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

/**
 * Resolves the adapter-level {@code group.protocol=auto} consumer template value into a concrete
 * Kafka {@code group.protocol} ({@code consumer} or {@code classic}) by probing the cluster's
 * finalized {@code group.version} feature flag - the flag that gates the KIP-848 consumer
 * rebalance protocol.
 *
 * <p><b>How the probe works.</b> {@link Admin#describeFeatures()} rides the {@code ApiVersions}
 * handshake that every Kafka client performs on connect: the broker answers it before
 * authentication completes and never applies an ACL to it (version negotiation must work before a
 * session exists), so the probe needs no grant beyond the connection credentials already present
 * in the consumer template. The <i>finalized</i> {@code group.version} is controller-managed and
 * cluster-wide - it only turns on when every broker supports it - so it is authoritative even
 * during a rolling broker upgrade.</p>
 *
 * <p><b>Resolution rules.</b> Finalized {@code group.version >= 1} resolves to {@code consumer};
 * an absent or zero feature level, or any probe failure (older broker, unreachable cluster, a
 * Kafka-compatible endpoint that does not report features), resolves to {@code classic} - the
 * safe default on any Kafka. One probe per {@code bootstrap.servers} per JVM; the decision and
 * its evidence are stated in the startup log. An explicit {@code consumer} or {@code classic} in
 * the template is passed through verbatim, no probe.</p>
 *
 * <p><b>Conflict guard.</b> {@code session.timeout.ms}, {@code heartbeat.interval.ms} and
 * {@code partition.assignment.strategy} are client-side tuning that cannot be set together with
 * {@code group.protocol=consumer} (the Kafka client fails fast with a {@code ConfigException} -
 * their roles move to broker-side group configuration under KIP-848). When the template sets any
 * of them, {@code auto} resolves to {@code classic} with a warning naming the conflicting keys:
 * respecting the operator's explicit tuning is safer than silently discarding it.</p>
 */
final class GroupProtocolResolver {
    private static final Logger log = LoggerFactory.getLogger(GroupProtocolResolver.class);

    private static final String AUTO = "auto";
    private static final String CONSUMER = "consumer";
    private static final String CLASSIC = "classic";
    private static final String GROUP_VERSION_FEATURE = "group.version";
    /** Client-side tuning that cannot be combined with {@code group.protocol=consumer}. */
    private static final List<String> CONSUMER_PROTOCOL_CONFLICTS = List.of(
            ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG,
            ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG,
            ConsumerConfig.PARTITION_ASSIGNMENT_STRATEGY_CONFIG);
    /** One resolution per cluster per JVM, keyed by the template's bootstrap.servers. */
    private static final ConcurrentMap<String, String> RESOLVED = new ConcurrentHashMap<>();
    static int probeTimeoutMs = 10_000;     // visible for testing

    private GroupProtocolResolver() {}

    /**
     * Resolve {@code group.protocol=auto} in place. Any other value (or none) is left untouched.
     *
     * @param p the consumer properties assembled from the template
     */
    static void resolve(Properties p) {
        String protocol = p.getProperty(ConsumerConfig.GROUP_PROTOCOL_CONFIG);
        if (protocol == null || !AUTO.equalsIgnoreCase(protocol.trim())) {
            return;
        }
        List<String> conflicts = CONSUMER_PROTOCOL_CONFLICTS.stream()
                .filter(p::containsKey).toList();
        if (!conflicts.isEmpty()) {
            p.setProperty(ConsumerConfig.GROUP_PROTOCOL_CONFIG, CLASSIC);
            log.warn("group.protocol=auto resolved to classic - {} cannot be used with the consumer "
                    + "rebalance protocol; remove the setting(s) to let auto upgrade", conflicts);
            return;
        }
        String bootstrap = p.getProperty(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG);
        if (bootstrap == null || bootstrap.isBlank()) {
            p.setProperty(ConsumerConfig.GROUP_PROTOCOL_CONFIG, CLASSIC);
            log.warn("group.protocol=auto resolved to classic - no bootstrap.servers to probe");
            return;
        }
        p.setProperty(ConsumerConfig.GROUP_PROTOCOL_CONFIG,
                RESOLVED.computeIfAbsent(bootstrap, b -> probe(p, b)));
    }

    /**
     * One-time feature probe against the cluster, using only the template's connection and
     * security settings (filtered to the Admin client's own config surface, so consumer-only
     * keys are not passed along).
     */
    private static String probe(Properties template, String bootstrap) {
        Properties adminProps = new Properties();
        template.stringPropertyNames().stream()
                .filter(AdminClientConfig.configNames()::contains)
                .forEach(k -> adminProps.setProperty(k, template.getProperty(k)));
        adminProps.setProperty(AdminClientConfig.REQUEST_TIMEOUT_MS_CONFIG, String.valueOf(probeTimeoutMs));
        adminProps.setProperty(AdminClientConfig.DEFAULT_API_TIMEOUT_MS_CONFIG, String.valueOf(probeTimeoutMs));
        Admin admin = Admin.create(adminProps);
        try {
            FeatureMetadata meta = admin.describeFeatures(
                            new DescribeFeaturesOptions().timeoutMs(probeTimeoutMs))
                    .featureMetadata().get(probeTimeoutMs + 1000L, TimeUnit.MILLISECONDS);
            FinalizedVersionRange groupVersion = meta.finalizedFeatures().get(GROUP_VERSION_FEATURE);
            if (groupVersion != null && groupVersion.maxVersionLevel() >= 1) {
                log.info("Kafka cluster ({}) finalizes group.version={} - group.protocol=auto resolved "
                        + "to consumer (KIP-848 consumer rebalance protocol)",
                        bootstrap, groupVersion.maxVersionLevel());
                return CONSUMER;
            }
            log.info("Kafka cluster ({}) does not finalize group.version - group.protocol=auto "
                    + "resolved to classic", bootstrap);
            return CLASSIC;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Kafka feature probe interrupted ({}) - group.protocol=auto resolved to classic",
                    bootstrap);
            return CLASSIC;
        } catch (Exception e) {
            log.warn("Unable to probe Kafka cluster features ({}) - group.protocol=auto resolved to "
                    + "classic: {}", bootstrap, e.getMessage());
            return CLASSIC;
        } finally {
            admin.close(Duration.ofSeconds(5));
        }
    }

    /** Forget cached cluster resolutions - for tests that probe the same bootstrap twice. */
    static void clearCachedResolutions() {
        RESOLVED.clear();
    }
}
