/*

    Copyright 2018-2025 Accenture Technology

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

package org.platformlambda.core.websocket.server;

import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import org.platformlambda.core.models.EventEnvelope;
import org.platformlambda.core.serializers.SimpleMapper;
import org.platformlambda.core.serializers.SimpleXmlWriter;
import org.platformlambda.core.services.ActuatorServices;
import org.platformlambda.core.system.Platform;
import org.platformlambda.core.system.EventEmitter;
import org.platformlambda.core.util.Utility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.buffer.Buffer;

import java.util.*;

/**
 * This is reserved for system use.
 * DO NOT use this directly in your application code.
 * <p>
 * HTTP admin endpoints for info, health, env, shutdown, suspend and resume
 * to be available with the same port when websocket server is deployed.
 * i.e. when user defined websocket server using WebSocketService is found.
 */
public class MinimalistHttpHandler implements Handler<HttpServerRequest> {
    private static final Logger log = LoggerFactory.getLogger(MinimalistHttpHandler.class);
    private static final SimpleXmlWriter xml = new SimpleXmlWriter();
    private static final String TYPE = "type";
    private static final String ACCEPT = "Accept";
    private static final String ACCEPT_CONTENT = ACCEPT.toLowerCase();
    private static final String APPLICATION_JSON = "application/json";
    private static final String APPLICATION_XML = "application/xml";
    private static final String GET = "GET";
    private static final String DATE = "Date";
    private static final String CONTENT_TYPE = "Content-Type";
    private static final String CONTENT_LENGTH = "Content-Length";
    private static final String TEXT_PLAIN = "text/plain";
    private static final String STATUS = "status";
    private static final String MESSAGE = "message";
    private static final String PATH = "path";
    private static final String KEEP_ALIVE = "keep-alive";
    private static final String CONNECTION_HEADER = "Connection";
    private static final String[] INFO_SERVICE = {"/info", "info"};
    private static final String[] INFO_LIB = {"/info/lib", "lib"};
    private static final String[] INFO_ROUTES = {"/info/routes", "routes"};
    private static final String[] HEALTH_SERVICE = {"/health", "health"};
    private static final String[] ENV_SERVICE = {"/env", "env"};
    private static final String[] LIVENESSPROBE = {"/livenessprobe", "livenessprobe"};
    private static final String[][] ADMIN_ENDPOINTS = {INFO_SERVICE, INFO_LIB, INFO_ROUTES,
            HEALTH_SERVICE, ENV_SERVICE, LIVENESSPROBE};

    @Override
    public void handle(HttpServerRequest request) {
        EventEmitter po = EventEmitter.getInstance();
        Utility util = Utility.getInstance();
        HttpServerResponse response = request.response();
        response.putHeader(DATE, util.getHtmlDate(new Date()));
        // default content type is JSON
        response.putHeader(CONTENT_TYPE, APPLICATION_JSON);
        String connectionType = request.getHeader(CONNECTION_HEADER);
        if (KEEP_ALIVE.equals(connectionType)) {
            response.putHeader(CONNECTION_HEADER, KEEP_ALIVE);
        }
        final String uri = util.getDecodedUri(request.path());
        String method = request.method().name();
        boolean processed = false;
        if (GET.equals(method)) {
            String type = getAdminEndpointType(uri);
            if (type != null) {
                EventEnvelope event = new EventEnvelope().setHeader(TYPE, type);
                event.setTo(ActuatorServices.ACTUATOR_SERVICES);
                String accept = request.getHeader(ACCEPT);
                event.setHeader(ACCEPT_CONTENT, accept != null? accept : APPLICATION_JSON);
                po.asyncRequest(event, 30000)
                    .onSuccess(result -> {
                        final String contentType = result.getHeaders()
                                                    .getOrDefault(CONTENT_TYPE.toLowerCase(), APPLICATION_JSON);
                        final Object data = result.getRawBody();
                        final byte[] b;
                        if (TEXT_PLAIN.equals(contentType) && data instanceof String str) {
                            response.putHeader(CONTENT_TYPE, TEXT_PLAIN);
                            b = util.getUTF(str);
                        } else {
                            if (APPLICATION_XML.equals(contentType)) {
                                response.putHeader(CONTENT_TYPE, APPLICATION_XML);
                                if (data instanceof Map) {
                                    b = util.getUTF(xml.write(data));
                                } else {
                                    b = util.getUTF(data == null? "" : data.toString());
                                }
                            } else {
                                if (data instanceof Map) {
                                    b = SimpleMapper.getInstance().getMapper().writeValueAsBytes(data);
                                } else {
                                    b = util.getUTF(data == null? "" : data.toString());
                                }
                            }
                        }
                        response.putHeader(CONTENT_LENGTH, String.valueOf(b.length));
                        response.setStatusCode(result.getStatus());
                        response.write(Buffer.buffer(b));
                        response.end();
                    })
                    .onFailure(e -> sendError(response, uri, 408, e.getMessage()));
                processed = true;
            }
        }
        if (!processed) {
            if ("/".equals(uri)) {
                Map<String, Object> instruction = new HashMap<>();
                List<String> endpoints = new ArrayList<>();
                instruction.put(MESSAGE, "Minimalist HTTP server supports these admin endpoints");
                instruction.put("endpoints", endpoints);
                for (String[] service: ADMIN_ENDPOINTS) {
                    endpoints.add(service[0]);
                }
                instruction.put("name", Platform.getInstance().getName());
                instruction.put("time", new Date());
                sendResponse("info", response, uri, 200, instruction);
            } else {
                sendError(response, uri, 404, "Resource not found");
            }
        }
    }

    private void sendError(HttpServerResponse response, String uri, int status, Object message) {
        sendResponse("error", response, uri, status, message);
    }

    @SuppressWarnings("unchecked")
    private void sendResponse(String type, HttpServerResponse response, String uri, int status, Object message) {
        final Map<String, Object> error;
        if (message instanceof Map) {
            error = (Map<String, Object>) message;
        } else {
            error = new HashMap<>();
            error.put(TYPE, type);
            error.put(STATUS, status);
            error.put(MESSAGE, message);
            error.put(PATH, uri);
        }
        byte[] b = SimpleMapper.getInstance().getMapper().writeValueAsBytes(error);
        response.putHeader(CONTENT_LENGTH, String.valueOf(b.length));
        response.setStatusCode(status);
        response.write(Buffer.buffer(b));
        response.end();
    }

    private String getAdminEndpointType(String path) {
        for (String[] service: ADMIN_ENDPOINTS) {
            if (path.equals(service[0])) {
                return service[1];
            }
        }
        return null;
    }
}
