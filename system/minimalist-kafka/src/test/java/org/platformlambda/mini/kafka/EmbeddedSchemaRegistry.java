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

import com.sun.net.httpserver.HttpServer;
import org.platformlambda.core.serializers.SimpleMapper;
import org.platformlambda.core.util.Utility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
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
 *       global, deduplicated)</li>
 *   <li>{@code GET /schemas/ids/{id}} - returns {@code {"schema": ..., "schemaType": ...}}</li>
 *   <li>{@code POST /subjects/{subject}} - schema lookup, returns the registered entity or 404</li>
 * </ul>
 *
 * Any unmatched path is logged + answered 404, so a missing endpoint surfaces loudly during test
 * development. Mirrors the contract of {@code helpers/schema-registry-standalone}.
 */
public class EmbeddedSchemaRegistry implements AutoCloseable {
    private static final Logger log = LoggerFactory.getLogger(EmbeddedSchemaRegistry.class);
    private static final String SCHEMA = "schema";
    private static final String SCHEMA_TYPE = "schemaType";
    private static final String AVRO = "AVRO";

    private final HttpServer server;
    private final AtomicInteger idGenerator = new AtomicInteger(1);
    private final ConcurrentMap<String, Integer> contentToId = new ConcurrentHashMap<>();
    private final ConcurrentMap<Integer, Map<String, Object>> idToSchema = new ConcurrentHashMap<>();

    public EmbeddedSchemaRegistry() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", this::handle);
        server.start();
        log.info("Embedded schema registry on {}", baseUrl());
    }

    public String baseUrl() {
        return "http://127.0.0.1:" + server.getAddress().getPort();
    }

    @Override
    public void close() {
        server.stop(0);
    }

    private void handle(com.sun.net.httpserver.HttpExchange exchange) throws IOException {
        try {
            String method = exchange.getRequestMethod();
            String path = exchange.getRequestURI().getPath();
            String[] p = path.split("/");
            // /subjects/{subject}/versions  (register)
            if ("POST".equals(method) && p.length == 4 && "subjects".equals(p[1]) && "versions".equals(p[3])) {
                register(exchange);
            // /subjects/{subject}  (lookup a schema under a subject)
            } else if ("POST".equals(method) && p.length == 3 && "subjects".equals(p[1])) {
                lookup(exchange, decode(p[2]));
            // /schemas/ids/{id}
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

    private void register(com.sun.net.httpserver.HttpExchange exchange) throws IOException {
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
        respond(exchange, 200, Map.of("id", id));
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
        byte[] out = SimpleMapper.getInstance().getCompactGson().toJson(body).getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("content-type", "application/vnd.schemaregistry.v1+json");
        exchange.sendResponseHeaders(status, out.length);
        exchange.getResponseBody().write(out);
        exchange.close();
    }

    private static String decode(String value) {
        return java.net.URLDecoder.decode(value, StandardCharsets.UTF_8);
    }
}
