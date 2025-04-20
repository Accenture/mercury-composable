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

package org.platformlambda.automation.services;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerResponse;
import org.platformlambda.automation.config.RoutingEntry;
import org.platformlambda.automation.models.AsyncContextHolder;
import org.platformlambda.automation.models.HeaderInfo;
import org.platformlambda.automation.util.SimpleHttpUtility;
import org.platformlambda.core.annotations.EventInterceptor;
import org.platformlambda.core.models.EventEnvelope;
import org.platformlambda.core.models.TypedLambdaFunction;
import org.platformlambda.core.serializers.SimpleMapper;
import org.platformlambda.core.serializers.SimpleXmlWriter;
import org.platformlambda.core.system.FluxConsumer;
import org.platformlambda.core.util.Utility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;

/**
 * This is reserved for system use.
 * DO NOT use this directly in your application code.
 */
@EventInterceptor
public class AsyncHttpResponse implements TypedLambdaFunction<EventEnvelope, Void> {
    private static final Logger log = LoggerFactory.getLogger(AsyncHttpResponse.class);

    private static final SimpleXmlWriter xmlWriter = new SimpleXmlWriter();
    private static final String APPLICATION_JSON = "application/json";
    private static final String APPLICATION_XML = "application/xml";
    private static final String TEXT_HTML = "text/html";
    private static final String TEXT_PLAIN = "text/plain";
    private static final String HEAD = "HEAD";
    private static final String X_STREAM_ID = "x-stream-id";
    private static final String X_TTL = "x-ttl";
    private static final String STREAM_PREFIX = "stream.";
    private static final String INPUT_STREAM_SUFFIX = ".in";
    private static final String SET_COOKIE = "Set-Cookie";
    private static final String CONTENT_TYPE = "Content-Type";
    private static final String CONTENT_LEN = "Content-Length";
    private static final String HTML_START = "<html><body><pre>\n";
    private static final String HTML_END = "\n</pre></body></html>";
    private static final String RESULT = "result";
    private static final String ACCEPT_ANY = "*/*";

    private final ConcurrentMap<String, AsyncContextHolder> contexts;

    public AsyncHttpResponse(ConcurrentMap<String, AsyncContextHolder> contexts) {
        this.contexts = contexts;
    }

    @Override
    public Void handleEvent(Map<String, String> headers, EventEnvelope event, int instance) throws IOException {
        Utility util = Utility.getInstance();
        SimpleHttpUtility httpUtil = SimpleHttpUtility.getInstance();
        String requestId = event.getCorrelationId();
        if (requestId != null) {
            AsyncContextHolder holder = contexts.get(requestId);
            if (holder != null) {
                holder.touch();
                HttpServerResponse response = holder.request.response();
                if (event.getStatus() != 200) {
                    response.setStatusCode(event.getStatus());
                }
                boolean httpHead = HEAD.equals(holder.method);
                String streamTimeout = null;
                String streamId = null;
                String contentType = httpHead? "?" : null;
                Map<String, String> resHeaders = new HashMap<>();
                if (!event.getHeaders().isEmpty()) {
                    Map<String, String> evtHeaders = event.getHeaders();
                    for (Map.Entry<String, String> kv : evtHeaders.entrySet()) {
                        String h = kv.getKey();
                        String key = h.toLowerCase();
                        String value = evtHeaders.get(h);
                        /*
                         * 1. "stream" and "timeout" are reserved as stream ID and read timeout in seconds
                         * 2. "trace_id" and "trace_path" should be dropped from HTTP response headers
                         */
                        if (X_STREAM_ID.equals(key) && value.startsWith(STREAM_PREFIX) &&
                                value.contains(INPUT_STREAM_SUFFIX)) {
                            streamId = value;
                        } else if (X_TTL.equalsIgnoreCase(key)) {
                            streamTimeout = value;
                        } else if (CONTENT_TYPE.equalsIgnoreCase(key)) {
                            if (!httpHead) {
                                contentType = value.toLowerCase();
                                response.putHeader(CONTENT_TYPE, contentType);
                            }
                        } else if (SET_COOKIE.equalsIgnoreCase(key)) {
                            httpUtil.setCookies(response, value);
                        } else {
                            resHeaders.put(key, value);
                        }
                    }
                }
                if (holder.resHeaderId != null) {
                    HeaderInfo hi = RoutingEntry.getInstance().getResponseHeaderInfo(holder.resHeaderId);
                    resHeaders = httpUtil.filterHeaders(hi, resHeaders);
                }
                for (Map.Entry<String, String> kv : resHeaders.entrySet()) {
                    String prettyHeader = httpUtil.getHeaderCase(kv.getKey());
                    if (prettyHeader != null) {
                        response.putHeader(prettyHeader, resHeaders.get(kv.getKey()));
                    }
                }
                if (contentType == null) {
                    String accept = holder.accept;
                    if (accept == null) {
                        contentType = "?";
                        // content-type header will not be provided
                    } else if (accept.contains(TEXT_HTML)) {
                        contentType = TEXT_HTML;
                        response.putHeader(CONTENT_TYPE, TEXT_HTML);
                    } else if (accept.contains(APPLICATION_XML)) {
                        contentType = APPLICATION_XML;
                        response.putHeader(CONTENT_TYPE, APPLICATION_XML);
                    } else if (accept.contains(APPLICATION_JSON) || accept.contains(ACCEPT_ANY)) {
                        contentType = APPLICATION_JSON;
                        response.putHeader(CONTENT_TYPE, APPLICATION_JSON);
                    } else {
                        contentType = TEXT_PLAIN;
                        response.putHeader(CONTENT_TYPE, TEXT_PLAIN);
                    }
                }
                // is this an exception?
                int status = event.getStatus();
                /*
                 * status range 100: used for HTTP protocol handshake
                 * status range 200: normal responses
                 * status range 300: redirection or unchanged content
                 * status ranges 400 and 500: HTTP exceptions
                 */
                if (status >= 400 && event.getHeaders().isEmpty() && event.getRawBody() instanceof String rawBody) {
                    String message = rawBody.trim();
                    // make sure it does not look like JSON or XML
                    if (!message.startsWith("{") && !message.startsWith("[") && !message.startsWith("<")) {
                        httpUtil.sendError(requestId, holder.request, status, message);
                        return null;
                    }
                }
                // Except HEAD method, HTTP response may have a body
                if (!httpHead) {
                    Object responseBody = event.getRawBody();
                    if (responseBody == null && streamId != null) {
                        // output is a stream?
                        response.setChunked(true);
                        FluxConsumer<Object> flux = new FluxConsumer<>(streamId,
                                                        getReadTimeout(streamTimeout, holder.timeout));
                        flux.consume(data -> {
                            if (data instanceof byte[] b && b.length > 0) {
                                response.write(Buffer.buffer(b));
                            }
                            if (data instanceof String text && !text.isEmpty()) {
                                response.write(text);
                            }
                        }, e -> {
                            log.error("Closing stream {} - {}", util.getSimpleRoute(flux.getStreamId()), e.getMessage());
                            HttpRouter.closeContext(requestId);
                            response.end();
                        }, () ->{
                            HttpRouter.closeContext(requestId);
                            response.end();
                        });
                        return null;

                    } else if (responseBody instanceof Map) {
                        if (contentType.startsWith(TEXT_HTML)) {
                            byte[] start = util.getUTF(HTML_START);
                            byte[] payload = SimpleMapper.getInstance().getMapper().writeValueAsBytes(responseBody);
                            byte[] end = util.getUTF(HTML_END);
                            response.putHeader(CONTENT_LEN, String.valueOf(start.length+payload.length+end.length));
                            response.write(HTML_START);
                            response.write(Buffer.buffer(payload));
                            response.write(HTML_END);
                        } else if (contentType.startsWith(APPLICATION_XML)) {
                            byte[] payload = util.getUTF(xmlWriter.write(RESULT, responseBody));
                            response.putHeader(CONTENT_LEN, String.valueOf(payload.length));
                            response.write(Buffer.buffer(payload));
                        } else {
                            byte[] payload = SimpleMapper.getInstance().getMapper().writeValueAsBytes(responseBody);
                            response.putHeader(CONTENT_LEN, String.valueOf(payload.length));
                            response.write(Buffer.buffer(payload));
                        }
                    } else if (responseBody instanceof List) {
                        if (contentType.startsWith(TEXT_HTML)) {
                            byte[] start = util.getUTF(HTML_START);
                            byte[] payload = SimpleMapper.getInstance().getMapper().writeValueAsBytes(responseBody);
                            byte[] end = util.getUTF(HTML_END);
                            response.putHeader(CONTENT_LEN, String.valueOf(start.length+payload.length+end.length));
                            response.write(HTML_START);
                            response.write(Buffer.buffer(payload));
                            response.write(HTML_END);
                        } else if (contentType.startsWith(APPLICATION_XML)) {
                            // xml must be delivered as a map
                            Map<String, Object> map = new HashMap<>();
                            map.put(RESULT, responseBody);
                            byte[] payload = util.getUTF(xmlWriter.write(RESULT, map));
                            response.putHeader(CONTENT_LEN, String.valueOf(payload.length));
                            response.write(Buffer.buffer(payload));
                        } else {
                            byte[] payload = SimpleMapper.getInstance().getMapper().writeValueAsBytes(responseBody);
                            response.putHeader(CONTENT_LEN, String.valueOf(payload.length));
                            response.write(Buffer.buffer(payload));
                        }
                    } else if (responseBody instanceof String str) {
                        byte[] payload = util.getUTF(str);
                        response.putHeader(CONTENT_LEN, String.valueOf(payload.length));
                        response.write(Buffer.buffer(payload));
                    } else if (responseBody instanceof byte[] payload) {
                        response.putHeader(CONTENT_LEN, String.valueOf(payload.length));
                        response.write(Buffer.buffer(payload));
                    } else if (responseBody != null) {
                        byte[] payload = util.getUTF(responseBody.toString());
                        response.putHeader(CONTENT_LEN, String.valueOf(payload.length));
                        response.write(Buffer.buffer(payload));
                    }
                }
                HttpRouter.closeContext(requestId);
                response.end();
            }
        }
        return null;
    }

    private long getReadTimeout(String timeoutOverride, long contextTimeout) {
        if (timeoutOverride == null) {
            return contextTimeout;
        }
        // convert to milliseconds
        long timeout = Utility.getInstance().str2long(timeoutOverride) * 1000;
        return timeout > 0? timeout : contextTimeout;
    }

}
