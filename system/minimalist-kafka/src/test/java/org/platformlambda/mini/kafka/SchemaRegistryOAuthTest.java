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

import io.confluent.kafka.schemaregistry.client.rest.exceptions.RestClientException;
import io.confluent.kafka.schemaregistry.json.JsonSchema;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.platformlambda.core.util.ConfigReader;
import org.platformlambda.core.util.common.ConfigBase;
import org.platformlambda.mini.kafka.schema.ResolvedSchema;
import org.platformlambda.mini.kafka.schema.SchemaCodec;
import org.platformlambda.mini.kafka.schema.SchemaType;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * OAuth 2.0 for the Schema Registry protocol - the full client-credentials flow against the in-JVM
 * {@link EmbeddedSchemaRegistry} running in OAuth mode: the {@code schema-registry.properties} template
 * pass-through carries {@code bearer.auth.*} to the Confluent client, the token endpoint URL is
 * auto-registered on the JVM allow-list ({@link OAuthUrlAllowList}), a bearer token is fetched with the
 * client id/secret and cached, and every registry call carries {@code Authorization: Bearer}.
 */
class SchemaRegistryOAuthTest {

    private static final String CLIENT_ID = "test-client";
    private static final String CLIENT_SECRET = "test-secret";
    private static final String OAUTH_TEMPLATE = "classpath:/schema-registry-oauth-test.properties";
    private static final String JSON_SCHEMA = "{\"type\":\"object\",\"properties\":{\"hello\":{\"type\":\"string\"}}}";

    private String savedAllowList;

    /** Minimal, fully-controlled {@link ConfigBase} backed by a flat key-value map. */
    private static final class MapConfig implements ConfigBase {
        private final Map<String, Object> map;
        MapConfig(Map<String, Object> map) { this.map = map; }
        @Override public Object get(String key) { return map.get(key); }
        @Override public Object get(String key, Object defaultValue, String... loop) { return map.getOrDefault(key, defaultValue); }
        @Override public String getProperty(String key) { Object v = map.get(key); return v == null ? null : String.valueOf(v); }
        @Override public String getProperty(String key, String defaultValue) { String v = getProperty(key); return v == null ? defaultValue : v; }
        @Override public boolean exists(String key) { return map.containsKey(key); }
        @Override public boolean isEmpty() { return map.isEmpty(); }
        @Override public boolean isBaseConfig() { return false; }
        @Override public Map<String, Object> getMap() { return map; }
        @Override public Map<String, Object> getCompositeKeyValues() { return getMap(); }
    }

    @BeforeEach
    void saveAllowList() {
        savedAllowList = System.getProperty(OAuthUrlAllowList.ALLOWED_URLS_PROPERTY);
    }

    @AfterEach
    void restoreAllowList() {
        if (savedAllowList == null) {
            System.clearProperty(OAuthUrlAllowList.ALLOWED_URLS_PROPERTY);
        } else {
            System.setProperty(OAuthUrlAllowList.ALLOWED_URLS_PROPERTY, savedAllowList);
        }
    }

    @Test
    void allowListMergesAndDeduplicates() {
        System.setProperty(OAuthUrlAllowList.ALLOWED_URLS_PROPERTY, "https://existing.example.com/token");
        OAuthUrlAllowList.register("https://login.microsoftonline.com/tenant/oauth2/v2.0/token");
        assertEquals("https://existing.example.com/token,https://login.microsoftonline.com/tenant/oauth2/v2.0/token",
                System.getProperty(OAuthUrlAllowList.ALLOWED_URLS_PROPERTY),
                "a new URL is appended to the operator's hand-set list, never clobbering it");
        OAuthUrlAllowList.register("https://login.microsoftonline.com/tenant/oauth2/v2.0/token");
        OAuthUrlAllowList.register("  ");
        OAuthUrlAllowList.register(null);
        assertEquals("https://existing.example.com/token,https://login.microsoftonline.com/tenant/oauth2/v2.0/token",
                System.getProperty(OAuthUrlAllowList.ALLOWED_URLS_PROPERTY),
                "duplicates and blank/null input are no-ops");
    }

    @Test
    void defaultTemplateIsEmptyPassThrough() {
        // the shipped schema-registry.properties is all comments -> no client parameters, current behavior kept
        Map<String, Object> passThrough = KafkaClientConfig.schemaRegistryProperties(new ConfigReader());
        assertTrue(passThrough.isEmpty(), "shipped template has no active entries");
    }

    @Test
    void oauthClientCredentialsEndToEnd() throws Exception {
        try (EmbeddedSchemaRegistry registry = new EmbeddedSchemaRegistry(CLIENT_ID, CLIENT_SECRET)) {
            Map<String, Object> appConfig = new HashMap<>();
            appConfig.put("schema.registry.properties", OAUTH_TEMPLATE);
            SchemaCodec codec = SchemaCodec.fromConfig(new MapConfig(appConfig), registry.baseUrl());
            assertNotNull(codec);
            // loading the template auto-registered the token endpoint on the JVM allow-list -
            // without this, the Confluent client's OAuth provider would refuse the issuer URL
            String allowList = System.getProperty(OAuthUrlAllowList.ALLOWED_URLS_PROPERTY);
            assertNotNull(allowList);
            assertTrue(allowList.contains(registry.tokenEndpointUrl()),
                    "token endpoint auto-registered on " + OAuthUrlAllowList.ALLOWED_URLS_PROPERTY);
            // register + resolve + fetch all authenticate with Authorization: Bearer
            String subject = "oauth-hello-value";
            int id = codec.client().register(subject, new JsonSchema(JSON_SCHEMA));
            ResolvedSchema resolved = codec.resolve(subject, "latest");
            assertEquals(id, resolved.id(), "authenticated resolve returns the registered global id");
            assertEquals(SchemaType.JSON, resolved.type());
            assertTrue(registry.tokenRequests() >= 1, "a bearer token was fetched from the token endpoint");
            assertTrue(registry.tokenRequests() <= 2,
                    "the token is cached by the Confluent client, not re-fetched per registry call");
        }
    }

    @Test
    void unauthenticatedClientIsRejectedByOAuthRegistry() throws Exception {
        try (EmbeddedSchemaRegistry registry = new EmbeddedSchemaRegistry(CLIENT_ID, CLIENT_SECRET)) {
            // default (empty) template -> no bearer auth on the client
            SchemaCodec codec = SchemaCodec.fromConfig(new ConfigReader(), registry.baseUrl());
            assertNotNull(codec);
            RestClientException denied = assertThrows(RestClientException.class,
                    () -> codec.client().register("no-auth-value", new JsonSchema(JSON_SCHEMA)));
            assertEquals(401, denied.getStatus(), "registry rejects requests without a bearer token");
        }
    }
}
