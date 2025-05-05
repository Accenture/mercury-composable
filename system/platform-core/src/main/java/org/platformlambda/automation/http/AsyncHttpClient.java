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

package org.platformlambda.automation.http;

import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
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
import org.platformlambda.automation.services.HttpRouter;
import org.platformlambda.automation.util.CustomContentTypeResolver;
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
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

@EventInterceptor
public class AsyncHttpClient implements TypedLambdaFunction<EventEnvelope, Void> {
    public static final String ASYNC_HTTP_REQUEST = "async.http.request";
    public static final String ASYNC_HTTP_RESPONSE = "async.http.response";
    private static final Logger log = LoggerFactory.getLogger(AsyncHttpClient.class);
    private static final AtomicInteger initCounter = new AtomicInteger(0);
    private static final long HOUSEKEEPING_INTERVAL = 30 * 1000L;    // 30 seconds
    private static final long THIRTY_MINUTE = 30 * 60 * 1000L;
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
    private static final String GET = "GET";
    private static final String PUT = "PUT";
    private static final String POST = "POST";
    private static final String PATCH = "PATCH";
    private static final String DELETE = "DELETE";
    private static final String OPTIONS = "OPTIONS";
    private static final String HEAD = "HEAD";
    private static final String X_STREAM_ID = "x-stream-id";
    private static final String X_TTL = "x-ttl";
    private static final String STREAM_PREFIX = "stream.";
    private static final String INPUT_STREAM_SUFFIX = ".in";
    private static final String CONTENT_TYPE = "content-type";
    private static final String CONTENT_LENGTH = "content-length";
    private static final String X_CONTENT_LENGTH = "X-Content-Length";
    private static final String USER_AGENT_NAME = "async-http-client";
    private static final int DEFAULT_TTL_SECONDS = 30;  // 30 seconds
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
        String temp = reader.getProperty("app.temp.dir", "/tmp/composable/java/temp-streams");
        tempDir = new File(temp);
        if (!tempDir.exists() && tempDir.mkdirs()) {
            log.info("Temporary work directory {} created", tempDir);
        }
        if (initCounter.incrementAndGet() == 1) {
            HttpRouter.initialize();
            Platform platform = Platform.getInstance();
            platform.getVirtualThreadExecutor().submit(() -> {
                // clean up when application starts
                removeExpiredFiles();
                // then schedule clean up every 30 minutes
                platform.getVertx().setPeriodic(HOUSEKEEPING_INTERVAL, t -> removeExpiredFiles());
                log.info("Housekeeper started");
            });
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
            if (v instanceof String value) {
                sb.append(k);
                sb.append('=');
                sb.append(value);
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
            if (input.getReplyTo() != null) {
                sendResponse(input, response.setException(ex));
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

    @SuppressWarnings("rawtypes")
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
        final String rawUri;
        final String uri = util.getEncodedUri(request.getUrl());
        int hashMark = uri.lastIndexOf('#');
        final String uriWithoutHash = hashMark == -1? uri : uri.substring(0, hashMark);
        final String hashParams = hashMark == -1? null : uri.substring(hashMark+1);
        String queryString = null;
        int questionMark = uriWithoutHash.lastIndexOf('?');
        if (questionMark == -1) {
            rawUri = uriWithoutHash;
        } else {
            rawUri = uriWithoutHash.substring(0, questionMark);
            queryString = decodeUri(uriWithoutHash.substring(questionMark+1));
            request.setQueryString(queryString);
        }
        // construct target URL
        final String queryParams = queryParametersToString(request);
        // combine query parameters from query string and query parameters
        if (queryParams != null) {
            queryString = queryString == null? queryParams : queryString + "&" + queryParams;
        }
        final String uriWithHash = rawUri + (hashParams == null? "" : "#" + hashParams);
        po.annotateTrace(DESTINATION, url.getScheme() + "://" + url.getHost() + ":" + port + rawUri);
        WebClient client = getWebClient(instance, request.isTrustAllCert());
        HttpRequest<Buffer> http = client.request(httpMethod, port, host, uriWithHash).ssl(secure);
        if (queryString != null) {
            Set<String> keys = new HashSet<>();
            List<String> parts = util.split(queryString, "&");
            for (String p: parts) {
                int eq = p.indexOf('=');
                final String k;
                final String v;
                if (eq == -1) {
                    k = p;
                    v = "";
                } else {
                    k = p.substring(0, eq);
                    v = p.substring(eq+1);
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
            http.putHeader(HttpRouter.getDefaultTraceIdLabel(), traceId);
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
            final String streamId = request.getStreamRoute();
            if (streamId != null && streamId.startsWith(STREAM_PREFIX)
                    && streamId.contains(INPUT_STREAM_SUFFIX)) {
                Platform.getInstance().getVirtualThreadExecutor().submit(() ->
                        handleUpload(input, queue, request, httpRequest));
            } else {
                Object reqBody = request.getBody() == null? new byte[0] : request.getBody();
                switch (reqBody) {
                    case byte[] b -> {
                        httpRequest.putHeader(CONTENT_LENGTH, String.valueOf(b.length));
                        httpResponse = httpRequest.sendBuffer(Buffer.buffer(b));
                    }
                    case String text -> {
                        byte[] b = util.getUTF(text);
                        httpRequest.putHeader(CONTENT_LENGTH, String.valueOf(b.length));
                        httpResponse = httpRequest.sendBuffer(Buffer.buffer(b));
                    }
                    case Map map -> {
                        boolean xml = contentType != null && contentType.startsWith(APPLICATION_XML);
                        byte[] b = xml ? util.getUTF(xmlWriter.write(reqBody)) :
                                SimpleMapper.getInstance().getMapper().writeValueAsBytes(map);
                        httpRequest.putHeader(CONTENT_LENGTH, String.valueOf(b.length));
                        httpResponse = httpRequest.sendBuffer(Buffer.buffer(b));
                    }
                    case List list -> {
                        byte[] b = SimpleMapper.getInstance().getMapper().writeValueAsBytes(list);
                        httpRequest.putHeader(CONTENT_LENGTH, String.valueOf(b.length));
                        httpResponse = httpRequest.sendBuffer(Buffer.buffer(b));
                    }
                    default -> throw new IllegalArgumentException("Invalid HTTP request body");
                }
            }
        } else {
            httpResponse = httpRequest.send();
        }
        if (httpResponse != null) {
            httpResponse.onSuccess(new HttpResponseHandler(input, request, queue));
            httpResponse.onFailure(new HttpExceptionHandler(input, queue));
        }
    }

    public String decodeUri(String uri) {
        return uri != null && uri.contains("%")? URLDecoder.decode(uri, StandardCharsets.UTF_8) : uri;
    }

    private void handleUpload(EventEnvelope input, OutputStreamQueue queue,
                              AsyncHttpRequest request, HttpRequest<Void> httpRequest) {
        String streamId = request.getStreamRoute();
        String contentType = request.getHeader(CONTENT_TYPE);
        String method = request.getMethod();
        int timeout = request.getTimeoutSeconds();
        objectStream2file(streamId, timeout > 0? timeout : DEFAULT_TTL_SECONDS)
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
        response.setTo(input.getReplyTo()).setFrom(ASYNC_HTTP_REQUEST)
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
                // No auto-close because FluxConsumer is reactive and the file output stream will close at completion
                FileOutputStream out = new FileOutputStream(temp);
                FluxConsumer<Object> flux = new FluxConsumer<>(streamId, Math.max(5000L, timeoutSeconds * 1000L));
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
                if (f.isFile() && now - f.lastModified() > THIRTY_MINUTE) {
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
        private final Utility util = Utility.getInstance();
        private final CustomContentTypeResolver resolver = CustomContentTypeResolver.getInstance();
        private final EventEnvelope input;
        private final AsyncHttpRequest request;
        private final OutputStreamQueue queue;
        private final int timeoutSeconds;

        public HttpResponseHandler(EventEnvelope input, AsyncHttpRequest request, OutputStreamQueue queue) {
            this.input = input;
            this.request = request;
            this.queue = queue;
            int timeout = request.getTimeoutSeconds();
            this.timeoutSeconds = timeout > 0? timeout : DEFAULT_TTL_SECONDS;
        }

        @Override
        public void handle(HttpResponse<Void> res) {
            EventEnvelope response = new EventEnvelope();
            response.setStatus(res.statusCode());
            MultiMap headers = res.headers();
            headers.forEach(kv -> response.setHeader(kv.getKey(), kv.getValue()));
            if (input.getReplyTo() != null) {
                String resContentType = resolver.getContentType(res.getHeader(CONTENT_TYPE));
                String contentLen = res.getHeader(CONTENT_LENGTH);
                boolean renderAsBytes = "true".equals(request.getHeader(X_NO_STREAM));
                if (renderAsBytes || contentLen != null || isTextResponse(resContentType)) {
                    int len = 0;
                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                    try {
                        while (true) {
                            byte[] block = queue.read();
                            if (block.length == 0) {
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
                                if (b.length == 0) {
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
                                response.setHeader(X_STREAM_ID, publisher.getStreamId())
                                        .setHeader(X_TTL, timeoutSeconds * 1000)
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
                if (input.getReplyTo() != null) {
                    sendResponse(input, response.setException(ex).setBody(simplifyConnectionError(ex.getMessage())));
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
