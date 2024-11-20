/*

    Copyright 2018-2024 Accenture Technology

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

package org.platformlambda.automation.http;

import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.Promise;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.file.AsyncFile;
import io.vertx.core.file.FileSystem;
import io.vertx.core.file.OpenOptions;
import io.vertx.core.http.HttpMethod;
import io.vertx.ext.web.client.HttpRequest;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.ext.web.codec.BodyCodec;
import io.vertx.ext.web.multipart.MultipartForm;
import org.platformlambda.automation.models.OutputStreamQueue;
import org.platformlambda.automation.services.ServiceGateway;
import org.platformlambda.core.annotations.EventInterceptor;
import org.platformlambda.core.exception.AppException;
import org.platformlambda.core.models.AsyncHttpRequest;
import org.platformlambda.core.models.EventEnvelope;
import org.platformlambda.core.models.TypedLambdaFunction;
import org.platformlambda.core.serializers.SimpleMapper;
import org.platformlambda.core.serializers.SimpleXmlParser;
import org.platformlambda.core.serializers.SimpleXmlWriter;
import org.platformlambda.core.system.*;
import org.platformlambda.core.util.AppConfigReader;
import org.platformlambda.core.util.Utility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

@EventInterceptor
public class AsyncHttpClient implements TypedLambdaFunction<EventEnvelope, Void> {
    private static final Logger log = LoggerFactory.getLogger(AsyncHttpClient.class);
    private static final AtomicInteger initCounter = new AtomicInteger(0);
    private static final AtomicBoolean housekeeperNotRunning = new AtomicBoolean(true);
    private static final long HOUSEKEEPING_INTERVAL = 30 * 1000L;    // 30 seconds
    private static final long TEN_MINUTE = 10 * 60 * 1000L;
    private static final SimpleXmlParser xmlReader = new SimpleXmlParser();
    private static final SimpleXmlWriter xmlWriter = new SimpleXmlWriter();
    private static final ConcurrentMap<String, WebClient> webClients = new ConcurrentHashMap<>();
    private static final OpenOptions READ_THEN_DELETE = new OpenOptions().setRead(true).setDeleteOnClose(true);
    private static final String APPLICATION_OCTET_STREAM = "application/octet-stream";
    private static final String MULTIPART_FORM_DATA = "multipart/form-data";
    private static final String APPLICATION_JSON = "application/json";
    private static final String APPLICATION_XML = "application/xml";
    private static final String X_RAW_XML = "x-raw-xml";
    private static final String X_NO_STREAM = "x-small-payload-as-bytes";
    private static final String APPLICATION_JAVASCRIPT = "application/javascript";
    private static final String TEXT_PREFIX = "text/";
    private static final String REGULAR_FACTORY = "regular.";
    private static final String TRUST_ALL_FACTORY = "trust_all.";
    private static final String COOKIE = "cookie";
    private static final String DESTINATION = "destination";
    private static final String HTTP_RELAY = "async.http.request";
    private static final String GET = "GET";
    private static final String PUT = "PUT";
    private static final String POST = "POST";
    private static final String PATCH = "PATCH";
    private static final String DELETE = "DELETE";
    private static final String OPTIONS = "OPTIONS";
    private static final String HEAD = "HEAD";
    private static final String STREAM = "stream";
    private static final String STREAM_PREFIX = "stream.";
    private static final String INPUT_STREAM_SUFFIX = ".in";
    private static final String CONTENT_TYPE = "content-type";
    private static final String CONTENT_LENGTH = "content-length";
    private static final String X_CONTENT_LENGTH = "X-Content-Length";
    private static final String TIMEOUT = "timeout";
    private static final String USER_AGENT_NAME = "async-http-client";
    /*
     * Some headers must be dropped because they are not relevant for HTTP relay
     * e.g. "content-encoding" and "transfer-encoding" will break HTTP response rendering.
     */
    private static final String[] MUST_DROP_HEADERS = { "content-encoding", "transfer-encoding", "host", "connection",
                                                        "upgrade-insecure-requests", "accept-encoding", "user-agent",
                                                        "sec-fetch-mode", "sec-fetch-site", "sec-fetch-user" };
    private static WebClientOptions optionsTrustAll;
    private static WebClientOptions optionsVerifySSL;
    private final File tempDir;

    public AsyncHttpClient() {
        // create temp upload directory
        AppConfigReader reader = AppConfigReader.getInstance();
        String temp = reader.getProperty("app.temp.dir", "/tmp/temp_files_to_delete");
        tempDir = new File(temp);
        if (!tempDir.exists() && tempDir.mkdirs()) {
            log.info("Temporary work directory {} created", tempDir);
        }
        if (initCounter.incrementAndGet() == 1) {
            ServiceGateway.initialize();
            Platform.getInstance().getVertx().setPeriodic(HOUSEKEEPING_INTERVAL, t -> removeExpiredFiles());
            log.info("Housekeeper started");
        }
        if (initCounter.get() > 10000) {
            initCounter.set(10);
        }
        // this is a brief blocking call because WebClient will read version information from the library JAR
        if (optionsTrustAll == null) {
            optionsTrustAll = getClientOptions(true);
        }
        if (optionsVerifySSL == null) {
            optionsVerifySSL = getClientOptions(false);
        }
    }

    private WebClientOptions getClientOptions(boolean trustAll) {
        WebClientOptions options = new WebClientOptions().setUserAgent(USER_AGENT_NAME).setKeepAlive(true);
        options.setMaxHeaderSize(12 * 1024).setConnectTimeout(10000);
        if (trustAll) {
            options.setTrustAll(true);
        }
        return options;
    }

    private WebClient getWebClient(int instance, boolean trustAll) {
        String key = (trustAll? TRUST_ALL_FACTORY : REGULAR_FACTORY) + instance;
        if (webClients.containsKey(key)) {
            return webClients.get(key);
        }
        WebClientOptions options = trustAll? optionsTrustAll : optionsVerifySSL;
        WebClient client = WebClient.create(Platform.getInstance().getVertx(), options);
        log.debug("Loaded HTTP web client {}", key);
        webClients.put(key, client);
        return client;
    }

    @SuppressWarnings("unchecked")
    private String queryParametersToString(AsyncHttpRequest request) {
        StringBuilder sb = new StringBuilder();
        Map<String, Object> params = request.getQueryParameters();
        if (params.isEmpty()) {
            return null;
        }
        for (Map.Entry<String, Object> kv: params.entrySet()) {
            String k = kv.getKey();
            Object v = kv.getValue();
            if (v instanceof String) {
                sb.append(k);
                sb.append('=');
                sb.append(v);
                sb.append('&');
            }
            if (v instanceof List) {
                List<String> list = (List<String>) v;
                for (String item: list) {
                    sb.append(k);
                    sb.append('=');
                    sb.append(item);
                    sb.append('&');
                }
            }
        }
        if (sb.isEmpty()) {
            return null;
        }
        return sb.substring(0, sb.length()-1);
    }

    @Override
    public Void handleEvent(Map<String, String> headers, EventEnvelope input, int instance) {
        try {
            processRequest(headers, input, instance);
        } catch (Exception ex) {
            EventEnvelope response = new EventEnvelope();
            response.setException(ex).setBody(ex.getMessage());
            if (input.getReplyTo() != null) {
                if (ex instanceof AppException e) {
                    response.setStatus(e.getStatus());
                } else if (ex instanceof IllegalArgumentException) {
                    response.setStatus(400);
                } else {
                    response.setStatus(500);
                }
                sendResponse(input, response);
            } else {
                log.error("Unhandled exception", ex);
            }
        }
        return null;
    }

    private HttpMethod getMethod(String method) {
        if (GET.equals(method)) {
            return HttpMethod.GET;
        }
        if (HEAD.equals(method)) {
            return HttpMethod.HEAD;
        }
        if (PUT.equals(method)) {
            return HttpMethod.PUT;
        }
        if (POST.equals(method)) {
            return HttpMethod.POST;
        }
        if (PATCH.equals(method)) {
            return HttpMethod.PATCH;
        }
        if (DELETE.equals(method)) {
            return HttpMethod.DELETE;
        }
        if (OPTIONS.equals(method)) {
            return HttpMethod.OPTIONS;
        }
        return null;
    }

    private void processRequest(Map<String, String> headers, EventEnvelope input, int instance)
            throws AppException, URISyntaxException {
        PostOffice po = new PostOffice(headers, instance);
        Utility util = Utility.getInstance();
        AsyncHttpRequest request = new AsyncHttpRequest(input.getBody());
        String method = request.getMethod();
        HttpMethod httpMethod = getMethod(method);
        if (httpMethod == null) {
            throw new AppException(405, "Method not allowed");
        }
        String targetHost = request.getTargetHost();
        if (targetHost == null) {
            throw new IllegalArgumentException("Missing target host. e.g. https://hostname");
        }
        final boolean secure;
        URI url = new URI(targetHost);
        String protocol = url.getScheme();
        if ("http".equals(protocol)) {
            secure = false;
        } else if ("https".equals(protocol)) {
            secure = true;
        } else {
            throw new IllegalArgumentException("Protocol must be http or https");
        }
        String host = url.getHost().trim();
        if (host.isEmpty()) {
            throw new IllegalArgumentException("Unable to resolve target host as domain or IP address");
        }
        int port = url.getPort();
        if (port < 0) {
            port = secure? 443 : 80;
        }
        String path = url.getPath();
        if (!path.isEmpty()) {
            throw new IllegalArgumentException("Target host must not contain URI path");
        }
        // normalize URI and query string
        final String uri;
        if (request.getUrl().contains("?")) {
            // when there are more than one query separator, drop the middle portion.
            int sep1 = request.getUrl().indexOf('?');
            int sep2 = request.getUrl().lastIndexOf('?');
            uri = encodeUri(util.getSafeDisplayUri(request.getUrl().substring(0, sep1)));
            String q = request.getUrl().substring(sep2+1).trim();
            if (!q.isEmpty()) {
                request.setQueryString(q);
            }
        } else {
            uri = encodeUri(util.getSafeDisplayUri(request.getUrl()));
        }
        // construct target URL
        String qs = request.getQueryString();
        String queryParams = queryParametersToString(request);
        if (queryParams != null) {
            qs = qs == null? queryParams : qs + "&" + queryParams;
        }
        String uriWithQuery = uri + (qs == null? "" : "?" + qs);
        po.annotateTrace(DESTINATION, url.getScheme() + "://" + url.getHost() + ":" + port + uriWithQuery);
        WebClient client = getWebClient(instance, request.isTrustAllCert());
        HttpRequest<Buffer> http = client.request(httpMethod, port, host, uri).ssl(secure);
        if (qs != null) {
            Set<String> keys = new HashSet<>();
            List<String> parts = util.split(qs, "&");
            for (String kv: parts) {
                int eq = kv.indexOf('=');
                final String k;
                final String v;
                if (eq > 0) {
                    k = kv.substring(0, eq);
                    v = kv.substring(eq+1);
                } else {
                    k = kv;
                    v = "";
                }
                if (keys.contains(k)) {
                    http.addQueryParam(k, v);
                } else {
                    http.setQueryParam(k, v);
                    keys.add(k);
                }
            }
        }
        // optional read timeout
        int timeout = request.getTimeoutSeconds();
        if (timeout > 0) {
            http.timeout(timeout * 1000L);
        }
        Map<String, String> reqHeaders = request.getHeaders();
        // convert authentication session info into HTTP request headers
        Map<String, String> sessionInfo = request.getSessionInfo();
        reqHeaders.putAll(sessionInfo);
        for (Map.Entry<String, String> kv: reqHeaders.entrySet()) {
            if (allowedHeader(kv.getKey())) {
                http.putHeader(kv.getKey(), kv.getValue());
            }
        }
        // propagate X-Trace-Id when forwarding the HTTP request
        String traceId = po.getTraceId();
        if (traceId != null) {
            http.putHeader(ServiceGateway.getDefaultTraceIdLabel(), traceId);
        }
        // set cookies if any
        Map<String, String> cookies  = request.getCookies();
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> kv: cookies.entrySet()) {
            sb.append(kv.getKey());
            sb.append('=');
            sb.append(URLEncoder.encode(kv.getValue(), StandardCharsets.UTF_8));
            sb.append("; ");
        }
        if (!sb.isEmpty()) {
            // remove the ending separator
            http.putHeader(COOKIE, sb.substring(0, sb.length()-2));
        }
        OutputStreamQueue queue = new OutputStreamQueue();
        HttpRequest<Void> httpRequest = http.as(BodyCodec.pipe(queue));
        Future<HttpResponse<Void>> httpResponse = null;
        // get request body if any
        String contentType = request.getHeader(CONTENT_TYPE);
        if (POST.equals(method) || PUT.equals(method) || PATCH.equals(method)) {
            Object reqBody = request.getBody();
            if (reqBody instanceof byte[] b) {
                httpRequest.putHeader(CONTENT_LENGTH, String.valueOf(b.length));
                httpResponse = httpRequest.sendBuffer(Buffer.buffer(b));
            }
            if (reqBody instanceof String text) {
                byte[] b = util.getUTF(text);
                httpRequest.putHeader(CONTENT_LENGTH, String.valueOf(b.length));
                httpResponse = httpRequest.sendBuffer(Buffer.buffer(b));
            }
            if (reqBody instanceof Map) {
                boolean xml = contentType != null && contentType.startsWith(APPLICATION_XML);
                byte[] b = xml? util.getUTF(xmlWriter.write(reqBody)) :
                            SimpleMapper.getInstance().getMapper().writeValueAsBytes(reqBody);
                httpRequest.putHeader(CONTENT_LENGTH, String.valueOf(b.length));
                httpResponse = httpRequest.sendBuffer(Buffer.buffer(b));
            }
            if (reqBody instanceof List) {
                byte[] b = SimpleMapper.getInstance().getMapper().writeValueAsBytes(reqBody);
                httpRequest.putHeader(CONTENT_LENGTH, String.valueOf(b.length));
                httpResponse = httpRequest.sendBuffer(Buffer.buffer(b));
            }
            final String streamId = request.getStreamRoute();
            if (reqBody == null && streamId != null && streamId.startsWith(STREAM_PREFIX)
                    && streamId.contains(INPUT_STREAM_SUFFIX)) {
                Platform.getInstance().getVirtualThreadExecutor().submit(() ->
                        handleUpload(input, queue, request, httpRequest));
            }
        } else {
            httpResponse = httpRequest.send();
        }
        if (httpResponse != null) {
            httpResponse.onSuccess(new HttpResponseHandler(input, request, queue));
            httpResponse.onFailure(new HttpExceptionHandler(input, queue));
        }
    }

    private String encodeUri(String uri) {
        Utility util = Utility.getInstance();
        List<String> parts = util.split(uri, "/");
        StringBuilder sb = new StringBuilder();
        for (String p: parts) {
            sb.append('/');
            sb.append(URLEncoder.encode(p, StandardCharsets.UTF_8).replace("+", "%20"));
        }
        return sb.isEmpty() ? "/" : sb.toString();
    }

    private void handleUpload(EventEnvelope input, OutputStreamQueue queue,
                              AsyncHttpRequest request, HttpRequest<Void> httpRequest) {
        String streamId = request.getStreamRoute();
        String contentType = request.getHeader(CONTENT_TYPE);
        String method = request.getMethod();
        objectStream2file(streamId, request.getTimeoutSeconds())
            .onSuccess(temp -> {
                final Future<HttpResponse<Void>> future;
                int contentLen = request.getContentLength();
                if (contentLen > 0) {
                    String filename = request.getFileName();
                    if (contentType != null && contentType.startsWith(MULTIPART_FORM_DATA) &&
                            POST.equals(method) && filename != null) {
                        MultipartForm form = MultipartForm.create()
                                .binaryFileUpload(request.getUploadTag(), filename, temp.getPath(),
                                                    APPLICATION_OCTET_STREAM);
                        future = httpRequest.sendMultipartForm(form);
                    } else {
                        FileSystem fs = Platform.getInstance().getVertx().fileSystem();
                        AsyncFile file = fs.openBlocking(temp.getPath(), READ_THEN_DELETE);
                        future = httpRequest.sendStream(file);
                    }
                } else {
                    future = httpRequest.send();
                }
                future.onSuccess(new HttpResponseHandler(input, request, queue))
                        .onFailure(new HttpExceptionHandler(input, queue));
            })
            .onFailure(new HttpExceptionHandler(input, queue));
    }

    private void sendResponse(EventEnvelope input, EventEnvelope response) {
        response.setTo(input.getReplyTo()).setFrom(HTTP_RELAY)
                .setCorrelationId(input.getCorrelationId())
                .setTrace(input.getTraceId(), input.getTracePath());
        try {
            EventEmitter.getInstance().send(response);
        } catch (IOException e) {
            log.error("Unable to deliver response to {} - {}", input.getReplyTo(), e.getMessage());
        }
    }

    private boolean allowedHeader(String header) {
        for (String h: MUST_DROP_HEADERS) {
            if (header.equalsIgnoreCase(h)) {
                return false;
            }
        }
        return true;
    }

    private File getTempFile(String streamId) {
        int at = streamId.indexOf('@');
        return new File(tempDir, at > 0? streamId.substring(0, at) : streamId);
    }

    private Future<File> objectStream2file(String streamId, int timeoutSeconds) {
        return Future.future(promise -> {
            Utility util = Utility.getInstance();
            File temp = getTempFile(streamId);
            try {
                FileOutputStream out = new FileOutputStream(temp);
                long timeout = Math.max(5000, timeoutSeconds * 1000L);
                FluxConsumer<Object> flux = new FluxConsumer<>(streamId, timeout);
                flux.consume(data -> {
                    try {
                        if (data instanceof byte[] b && b.length > 0) {
                            out.write(b);
                        }
                        if (data instanceof String text && !text.isEmpty()) {
                            out.write(util.getUTF(text));
                        }
                    } catch (IOException e) {
                        // ok to ignore
                    }
                }, promise::fail, () -> {
                    try {
                        out.close();
                    } catch (IOException e) {
                        // ok to ignore
                    }
                    promise.complete(temp);
                });
            } catch (IOException e) {
                promise.fail(e);
            }
        });
    }

    private void removeExpiredFiles() {
        if (housekeeperNotRunning.compareAndSet(true, false)) {
            Platform.getInstance().getVirtualThreadExecutor().submit(() -> {
                try {
                    checkExpiredFiles();
                } finally {
                    housekeeperNotRunning.set(true);
                }
            });
        }
    }

    private void checkExpiredFiles() {
        /*
         * The temporary directory is used as a buffer for binary HTTP payload (including multipart file upload).
         * They are removed immediately after relay.
         *
         * This housekeeper is designed as a "catch-all" mechanism to enable zero-maintenance.
         */
        long now = System.currentTimeMillis();
        List<File> expired = new ArrayList<>();
        File[] files = tempDir.listFiles();
        if (files != null && files.length > 0) {
            for (File f: files) {
                if (f.isFile() && now - f.lastModified() > TEN_MINUTE) {
                    expired.add(f);
                }
            }
            for (File f: expired) {
                try {
                    Files.deleteIfExists(f.toPath());
                    log.warn("Removing expired file {}", f);
                } catch (IOException e) {
                    log.error("Unable to delete expired file {} - {}", f, e.getMessage());
                }
            }
        }
    }

    private class HttpResponseHandler implements Handler<HttpResponse<Void>> {

        private final EventEnvelope input;
        private final AsyncHttpRequest request;
        private final OutputStreamQueue queue;
        private final int timeoutSeconds;

        public HttpResponseHandler(EventEnvelope input, AsyncHttpRequest request, OutputStreamQueue queue) {
            this.input = input;
            this.request = request;
            this.queue = queue;
            this.timeoutSeconds = Math.max(8, request.getTimeoutSeconds());
        }

        @Override
        public void handle(HttpResponse<Void> res) {
            Utility util = Utility.getInstance();
            EventEnvelope response = new EventEnvelope();
            response.setStatus(res.statusCode());
            MultiMap headers = res.headers();
            headers.forEach(kv -> response.setHeader(kv.getKey(), kv.getValue()));
            if (input.getReplyTo() != null) {
                String resContentType = res.getHeader(CONTENT_TYPE);
                String contentLen = res.getHeader(CONTENT_LENGTH);
                boolean renderAsBytes = "true".equals(request.getHeader(X_NO_STREAM));
                if (renderAsBytes || contentLen != null || isTextResponse(resContentType)) {
                    int len = 0;
                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                    try {
                        while (true) {
                            byte[] block = queue.read();
                            if (block == null) {
                                break;
                            } else {
                                try {
                                    out.write(block);
                                    len += block.length;
                                } catch (IOException e) {
                                    // ok to ignore
                                }
                            }
                        }
                    } finally {
                        queue.close();
                    }
                    if (renderAsBytes || contentLen != null) {
                        response.setHeader(X_CONTENT_LENGTH, len);
                    }
                    byte[] b = out.toByteArray();
                    if (resContentType != null) {
                        if (resContentType.startsWith(APPLICATION_JSON)) {
                            // response body is assumed to be JSON
                            String text = util.getUTF(b).trim();
                            if (text.isEmpty()) {
                                sendResponse(input, response.setBody(new HashMap<>()));
                            } else {
                                if (text.startsWith("{") && text.endsWith("}")) {
                                    sendResponse(input, response.setBody(
                                            SimpleMapper.getInstance().getMapper().readValue(text, Map.class)));
                                } else if (text.startsWith("[") && text.endsWith("]")) {
                                    sendResponse(input, response.setBody(
                                            SimpleMapper.getInstance().getMapper().readValue(text, List.class)));
                                } else {
                                    sendResponse(input, response.setBody(text));
                                }
                            }
                        } else if (resContentType.startsWith(APPLICATION_XML)) {
                            // response body is assumed to be XML
                            String text = util.getUTF(b);
                            String trimmed = text.trim();
                            boolean rawXml = "true".equals(request.getHeader(X_RAW_XML));
                            if (rawXml) {
                                sendResponse(input, response.setBody(text));
                            } else {
                                try {
                                    sendResponse(input, response.setBody(
                                            trimmed.isEmpty() ? new HashMap<>() : xmlReader.parse(text)));
                                } catch (Exception e) {
                                    sendResponse(input, response.setBody(text));
                                }
                            }
                        } else if (resContentType.startsWith(TEXT_PREFIX) ||
                                resContentType.startsWith(APPLICATION_JAVASCRIPT)) {
                            /*
                             * For API targetHost, the content-types are usually JSON or XML.
                             * HTML, CSS and JS are the best effort static file contents.
                             */
                            sendResponse(input, response.setBody(util.getUTF(b)));
                        } else {
                            sendResponse(input, response.setBody(b));
                        }
                    } else {
                        sendResponse(input, response.setBody(b));
                    }
                } else {
                    Platform.getInstance().getVirtualThreadExecutor().submit(() -> {
                        int len = 0;
                        EventPublisher publisher = null;
                        try {
                            while (true) {
                                byte[] b = queue.read();
                                if (b == null) {
                                    break;
                                } else {
                                    if (publisher == null) {
                                        publisher = new EventPublisher(timeoutSeconds * 1000L);
                                    }
                                    len += b.length;
                                    publisher.publish(b);
                                }
                            }
                            if (publisher != null) {
                                response.setHeader(STREAM, publisher.getStreamId())
                                        .setHeader(TIMEOUT, timeoutSeconds * 1000)
                                        .setHeader(X_CONTENT_LENGTH, len);
                                publisher.publishCompletion();
                            }
                            sendResponse(input, response);
                        } finally {
                            queue.close();
                        }
                    });
                }
            }
        }

        private boolean isTextResponse(String contentType) {
            return  contentType != null && (
                    contentType.startsWith(APPLICATION_JSON) || contentType.startsWith(APPLICATION_XML) ||
                    contentType.startsWith(TEXT_PREFIX) || contentType.startsWith(APPLICATION_JAVASCRIPT));
        }
    }

    private class HttpExceptionHandler implements Handler<Throwable> {

        private final EventEnvelope input;
        private final OutputStreamQueue queue;

        public HttpExceptionHandler(EventEnvelope input, OutputStreamQueue queue) {
            this.input = input;
            this.queue = queue;
        }

        @Override
        public void handle(Throwable ex) {
            try {
                EventEnvelope response = new EventEnvelope();
                response.setException(ex).setBody(simplifyConnectionError(ex.getMessage()));
                if (input.getReplyTo() != null) {
                    if (ex instanceof AppException e) {
                        response.setStatus(e.getStatus());
                    } else if (ex instanceof IllegalArgumentException) {
                        response.setStatus(400);
                    } else {
                        response.setStatus(500);
                    }
                    sendResponse(input, response);
                } else {
                    log.error("Unhandled exception", ex);
                }
            } finally {
                queue.close();
            }
        }

        private String simplifyConnectionError(String error) {
            if (error.startsWith("Connection refused:")) {
                int colon = error.indexOf(':');
                int slash = error.indexOf('/');
                if (slash != -1) {
                    return error.substring(0, colon) + ": " + error.substring(slash+1);
                }
            }
            return error;
        }
    }

}
