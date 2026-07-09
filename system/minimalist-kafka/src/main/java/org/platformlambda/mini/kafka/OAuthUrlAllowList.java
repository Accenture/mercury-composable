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

import org.platformlambda.core.util.Utility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Registers OAuth 2.0 token endpoint URLs on the JVM-wide allow-list that the Kafka client enforces.
 *
 * <p>Since Apache Kafka hardened its SASL/OAUTHBEARER support, any HTTP(S) URL used to fetch a token -
 * the transport's {@code sasl.oauthbearer.token.endpoint.url} <b>and</b> the Confluent Schema Registry
 * client's {@code bearer.auth.issuer.endpoint.url} (its OAuth provider validates through the same Kafka
 * {@code ConfigurationUtils.validateUrl}) - must appear in the
 * {@value #ALLOWED_URLS_PROPERTY} <b>system property</b>, or client construction fails with a
 * {@code ConfigException}. Field applications previously had to call {@code System.setProperty} by hand;
 * this helper does it automatically for every token URL found in the
 * {@code kafka-producer/consumer/schema-registry.properties} templates.</p>
 *
 * <p><b>Merge, never clobber:</b> a URL is appended to whatever the property already holds (set by the
 * operator on the command line or by another library), deduplicated, comma-separated. Registration must
 * happen <i>before</i> the Kafka client or Schema Registry client is constructed - the template loaders
 * call it at property-assembly time, which precedes construction.</p>
 */
public final class OAuthUrlAllowList {
    private static final Logger log = LoggerFactory.getLogger(OAuthUrlAllowList.class);

    /** The system property (not a client config) read by Kafka's OAuth URL validation. */
    public static final String ALLOWED_URLS_PROPERTY = "org.apache.kafka.sasl.oauthbearer.allowed.urls";

    // virtual-thread friendly: a ReentrantLock does not pin the carrier thread like 'synchronized'
    private static final ReentrantLock LOCK = new ReentrantLock();

    private OAuthUrlAllowList() {}

    /**
     * Add a token endpoint URL to the JVM allow-list, preserving and deduplicating existing entries.
     * A {@code null}/blank URL is ignored, so callers can pass an optional config value directly.
     *
     * @param url the OAuth 2.0 token endpoint URL to allow (e.g. an Azure AD {@code /oauth2/v2.0/token})
     */
    public static void register(String url) {
        if (url == null || url.isBlank()) {
            return;
        }
        LOCK.lock();
        try {
            Set<String> entries = new LinkedHashSet<>();
            String existing = System.getProperty(ALLOWED_URLS_PROPERTY);
            if (existing != null && !existing.isBlank()) {
                entries.addAll(Utility.getInstance().split(existing, ", "));
            }
            if (entries.add(url.trim())) {
                System.setProperty(ALLOWED_URLS_PROPERTY, String.join(",", entries));
                log.info("OAuth token endpoint allow-listed: {}", url.trim());
            }
        } finally {
            LOCK.unlock();
        }
    }
}
