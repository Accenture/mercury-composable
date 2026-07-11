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

package org.platformlambda.twin.kafka;

import com.sun.net.httpserver.HttpServer;
import org.platformlambda.core.serializers.SimpleMapper;
import org.platformlambda.core.util.Utility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A self-contained, in-JVM Confluent-compatible Schema Registry for tests - a tiny JDK {@link HttpServer}
 * on a random localhost port, no external server and no network beyond loopback. It implements the few
 * endpoints the Confluent JSON Schema serdes call:
 *
 * <ul>
 *   <li>{@code POST /subjects/{subject}/versions} - register, returns {@code {"id": N}} (content-based,
 *       global, deduplicated; also assigns a per-subject version)</li>
 *   <li>{@code GET /subjects/{subject}/versions/{version}} - subject+version metadata ({@code latest} or an
 *       integer), used to resolve a subject/version to a global id</li>
 *   <li>{@code GET /schemas/ids/{id}} - returns {@code {"schema": ..., "schemaType": ...}}</li>
 *   <li>{@code POST /subjects/{subject}} - schema lookup, returns the registered entity or 404</li>
 * </ul>
 *
 * Any unmatched path is logged + answered 404, so a missing endpoint surfaces loudly during test
 * development. Mirrors the contract of {@code helpers/schema-registry-standalone}.
 *
 * <p><b>Optional OAuth 2.0 mode</b> (the two-arg constructor): the registry then enforces
 * {@code Authorization: Bearer} with a token it issued, and serves a client-credentials token endpoint at
 * {@code POST /oauth/token} (client id/secret via HTTP Basic or form fields) that mints an HS256-signed
 * JWT with the {@code sub}/{@code iat}/{@code exp}/{@code scope} claims the Confluent client-side
 * validator expects. This exercises the full {@code bearer.auth.credentials.source=OAUTHBEARER} flow -
 * token fetch, caching, and the Authorization header on every registry call - without an external IdP.</p>
 */
public class EmbeddedSchemaRegistry implements AutoCloseable {
    private static final Logger log = LoggerFactory.getLogger(EmbeddedSchemaRegistry.class);
    private static final String SCHEMA = "schema";
    private static final String SCHEMA_TYPE = "schemaType";
    private static final String AVRO = "AVRO";
    // fixed port (predictable: if it is in use the test fails fast rather than picking a surprise port)
    private static final int PORT = 17081;

    private static final String LATEST = "latest";
    private static final String TOKEN_PATH = "/oauth/token";
    private static final long TOKEN_LIFETIME_SECONDS = 3600;

    private final HttpServer server;
    private final AtomicInteger idGenerator = new AtomicInteger(1);
    private final ConcurrentMap<String, Integer> contentToId = new ConcurrentHashMap<>();
    private final ConcurrentMap<Integer, Map<String, Object>> idToSchema = new ConcurrentHashMap<>();
    // subject -> (version -> global id); versions are per-subject, ids are global/content-addressed.
    private final ConcurrentMap<String, ConcurrentMap<Integer, Integer>> subjectVersions = new ConcurrentHashMap<>();
    // OAuth mode (null clientId = auth off, the original behavior)
    private final String clientId;
    private final String clientSecret;
    private final Set<String> issuedTokens = ConcurrentHashMap.newKeySet();
    private final AtomicInteger tokenRequests = new AtomicInteger(0);

    public EmbeddedSchemaRegistry() throws IOException {
        this(null, null);
    }

    /**
     * Start in OAuth 2.0 mode: registry endpoints require a bearer token issued by this instance's
     * {@code POST /oauth/token} client-credentials endpoint.
     *
     * @param clientId     the only client id the token endpoint accepts
     * @param clientSecret the matching client secret (also the HS256 signing key)
     * @throws IOException if the server cannot bind
     */
    public EmbeddedSchemaRegistry(String clientId, String clientSecret) throws IOException {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", PORT), 0);
        server.createContext("/", this::handle);
        server.start();
        log.info("Embedded schema registry on {}{}", baseUrl(), clientId == null ? "" : " (OAuth 2.0 enforced)");
    }

    public String baseUrl() {
        return "http://127.0.0.1:" + server.getAddress().getPort();
    }

    /** @return the client-credentials token endpoint URL (OAuth mode) */
    public String tokenEndpointUrl() {
        return baseUrl() + TOKEN_PATH;
    }

    /** @return how many times the token endpoint issued a token (for cache assertions) */
    public int tokenRequests() {
        return tokenRequests.get();
    }

    @Override
    public void close() {
        server.stop(0);
    }

    /**
     * Route by method + path to the matching Confluent endpoint handler:
     * <ul>
     *   <li>{@code POST /subjects/{subject}/versions} - {@link #register}</li>
     *   <li>{@code POST /subjects/{subject}} - {@link #lookup} (lookup a schema under a subject)</li>
     *   <li>{@code GET /schemas/ids/{id}} - {@link #getById}</li>
     * </ul>
     * Anything else is logged + answered 404.
     *
     * @param exchange the HTTP exchange to route and respond to
     * @throws IOException if writing the response fails
     */
    private void handle(com.sun.net.httpserver.HttpExchange exchange) throws IOException {
        try {
            String method = exchange.getRequestMethod();
            String path = exchange.getRequestURI().getPath();
            if (clientId != null && TOKEN_PATH.equals(path) && "POST".equals(method)) {
                issueToken(exchange);
                return;
            }
            if (clientId != null && !bearerTokenAccepted(exchange)) {
                respond(exchange, 401, Map.of("error_code", 401, "message", "Bearer token required"));
                return;
            }
            String[] p = path.split("/");
            if ("POST".equals(method) && p.length == 4 && "subjects".equals(p[1]) && "versions".equals(p[3])) {
                register(exchange, decode(p[2]));
            } else if ("GET".equals(method) && p.length == 5 && "subjects".equals(p[1]) && "versions".equals(p[3])) {
                getVersion(exchange, decode(p[2]), p[4]);
            } else if ("POST".equals(method) && p.length == 3 && "subjects".equals(p[1])) {
                lookup(exchange, decode(p[2]));
            } else if ("GET".equals(method) && p.length == 4 && "schemas".equals(p[1]) && "ids".equals(p[2])) {
                getById(exchange, Utility.getInstance().str2int(p[3]));
            } else {
                log.warn("Embedded registry: unhandled {} {}", method, path);
                respond(exchange, 404, Map.of("error_code", 404, "message", "Not found: " + path));
            }
        } catch (RuntimeException e) {
            respond(exchange, 500, Map.of("error_code", 500, "message", String.valueOf(e.getMessage())));
        }
    }

    private void register(com.sun.net.httpserver.HttpExchange exchange, String subject) throws IOException {
        Map<String, Object> body = readBody(exchange);
        String schema = String.valueOf(body.get(SCHEMA));
        String type = body.containsKey(SCHEMA_TYPE) ? String.valueOf(body.get(SCHEMA_TYPE)) : AVRO;
        int id = contentToId.computeIfAbsent(type + ":" + schema, k -> {
            int newId = idGenerator.getAndIncrement();
            Map<String, Object> entry = new HashMap<>();
            entry.put(SCHEMA, schema);
            entry.put(SCHEMA_TYPE, type);
            idToSchema.put(newId, entry);
            return newId;
        });
        assignVersion(subject, id);
        respond(exchange, 200, Map.of("id", id));
    }

    /** Assign (or find, if idempotent) the per-subject version for {@code id}. */
    private void assignVersion(String subject, int id) {
        ConcurrentMap<Integer, Integer> versions = subjectVersions.computeIfAbsent(subject, k -> new ConcurrentHashMap<>());
        synchronized (versions) {
            for (Map.Entry<Integer, Integer> e : versions.entrySet()) {
                if (e.getValue().equals(id)) {
                    return;
                }
            }
            versions.put(versions.isEmpty() ? 1 : Collections.max(versions.keySet()) + 1, id);
        }
    }

    /** {@code GET /subjects/{subject}/versions/{version}} - metadata used to resolve subject+version to an id. */
    private void getVersion(com.sun.net.httpserver.HttpExchange exchange, String subject, String versionParam)
            throws IOException {
        ConcurrentMap<Integer, Integer> versions = subjectVersions.get(subject);
        if (versions == null || versions.isEmpty()) {
            respond(exchange, 404, Map.of("error_code", 40401, "message", "Subject '" + subject + "' not found"));
            return;
        }
        Integer version = LATEST.equalsIgnoreCase(versionParam) ? Collections.max(versions.keySet())
                : (Utility.getInstance().isDigits(versionParam) ? Utility.getInstance().str2int(versionParam) : null);
        Integer id = version == null ? null : versions.get(version);
        if (id == null) {
            respond(exchange, 404, Map.of("error_code", 40402, "message", "Version '" + versionParam + "' not found"));
            return;
        }
        Map<String, Object> entry = idToSchema.get(id);
        Map<String, Object> entity = new HashMap<>();
        entity.put("subject", subject);
        entity.put("version", version);
        entity.put("id", id);
        entity.put(SCHEMA, entry.get(SCHEMA));
        if (!AVRO.equals(entry.get(SCHEMA_TYPE))) {
            entity.put(SCHEMA_TYPE, entry.get(SCHEMA_TYPE));
        }
        respond(exchange, 200, entity);
    }

    private void lookup(com.sun.net.httpserver.HttpExchange exchange, String subject) throws IOException {
        Map<String, Object> body = readBody(exchange);
        String schema = String.valueOf(body.get(SCHEMA));
        String type = body.containsKey(SCHEMA_TYPE) ? String.valueOf(body.get(SCHEMA_TYPE)) : AVRO;
        Integer id = contentToId.get(type + ":" + schema);
        if (id == null) {
            respond(exchange, 404, Map.of("error_code", 40403, "message", "Schema not found"));
            return;
        }
        Map<String, Object> entity = new HashMap<>();
        entity.put("subject", subject);
        entity.put("version", 1);
        entity.put("id", id);
        entity.put(SCHEMA, schema);
        if (!AVRO.equals(type)) {
            entity.put(SCHEMA_TYPE, type);
        }
        respond(exchange, 200, entity);
    }

    private void getById(com.sun.net.httpserver.HttpExchange exchange, int id) throws IOException {
        Map<String, Object> entry = idToSchema.get(id);
        if (entry == null) {
            respond(exchange, 404, Map.of("error_code", 40403, "message", "Schema " + id + " not found"));
            return;
        }
        Map<String, Object> response = new HashMap<>();
        response.put(SCHEMA, entry.get(SCHEMA));
        // Confluent omits schemaType for AVRO (the default) and includes it otherwise.
        if (!AVRO.equals(entry.get(SCHEMA_TYPE))) {
            response.put(SCHEMA_TYPE, entry.get(SCHEMA_TYPE));
        }
        respond(exchange, 200, response);
    }

    /**
     * {@code POST /oauth/token} - the client-credentials token endpoint. Accepts the client id/secret as
     * HTTP Basic (what Kafka's HTTP JWT retriever sends) or as {@code client_id}/{@code client_secret}
     * form fields, and answers the standard token response with an HS256-signed JWT.
     */
    private void issueToken(com.sun.net.httpserver.HttpExchange exchange) throws IOException {
        String form = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        Map<String, String> params = new HashMap<>();
        for (String pair : form.split("&")) {
            int eq = pair.indexOf('=');
            if (eq > 0) {
                params.put(decode(pair.substring(0, eq)), decode(pair.substring(eq + 1)));
            }
        }
        String id = params.get("client_id");
        String secret = params.get("client_secret");
        String basic = exchange.getRequestHeaders().getFirst("Authorization");
        if (basic != null && basic.startsWith("Basic ")) {
            String[] credentials = new String(Base64.getDecoder().decode(basic.substring(6)),
                    StandardCharsets.UTF_8).split(":", 2);
            if (credentials.length == 2) {
                id = credentials[0];
                secret = credentials[1];
            }
        }
        if (!clientId.equals(id) || !clientSecret.equals(secret)) {
            respond(exchange, 401, Map.of("error", "invalid_client"));
            return;
        }
        String token = mintJwt(params.getOrDefault("scope", "registry"));
        issuedTokens.add(token);
        tokenRequests.incrementAndGet();
        respond(exchange, 200, Map.of("access_token", token, "token_type", "Bearer",
                "expires_in", TOKEN_LIFETIME_SECONDS), "application/json");
    }

    /** Mint an HS256 JWT carrying the sub/iat/exp/scope claims the Confluent client validator reads. */
    private String mintJwt(String scope) {
        long now = System.currentTimeMillis() / 1000;
        String header = base64Url("{\"alg\":\"HS256\",\"typ\":\"JWT\"}".getBytes(StandardCharsets.UTF_8));
        String payload = base64Url(SimpleMapper.getInstance().getCompactGson().toJson(Map.of(
                "sub", clientId, "iat", now, "exp", now + TOKEN_LIFETIME_SECONDS, "scope", scope))
                .getBytes(StandardCharsets.UTF_8));
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(clientSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            String signature = base64Url(mac.doFinal((header + "." + payload).getBytes(StandardCharsets.UTF_8)));
            return header + "." + payload + "." + signature;
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Unable to sign test JWT", e);
        }
    }

    private static String base64Url(byte[] data) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(data);
    }

    private boolean bearerTokenAccepted(com.sun.net.httpserver.HttpExchange exchange) {
        String authorization = exchange.getRequestHeaders().getFirst("Authorization");
        return authorization != null && authorization.startsWith("Bearer ")
                && issuedTokens.contains(authorization.substring(7));
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> readBody(com.sun.net.httpserver.HttpExchange exchange) throws IOException {
        byte[] bytes = exchange.getRequestBody().readAllBytes();
        if (bytes.length == 0) {
            return Map.of();
        }
        return SimpleMapper.getInstance().getMapper().readValue(new String(bytes, StandardCharsets.UTF_8), Map.class);
    }

    private static void respond(com.sun.net.httpserver.HttpExchange exchange, int status, Map<String, Object> body)
            throws IOException {
        respond(exchange, status, body, "application/vnd.schemaregistry.v1+json");
    }

    private static void respond(com.sun.net.httpserver.HttpExchange exchange, int status, Map<String, Object> body,
                                String contentType) throws IOException {
        byte[] out = SimpleMapper.getInstance().getCompactGson().toJson(body).getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("content-type", contentType);
        exchange.sendResponseHeaders(status, out.length);
        exchange.getResponseBody().write(out);
        exchange.close();
    }

    private static String decode(String value) {
        return java.net.URLDecoder.decode(value, StandardCharsets.UTF_8);
    }
}
