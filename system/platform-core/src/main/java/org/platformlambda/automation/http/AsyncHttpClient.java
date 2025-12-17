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

import io.netty.channel.ChannelOption;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import reactor.core.publisher.Mono;
import reactor.netty.ByteBufFlux;
import reactor.netty.http.Http11SslContextSpec;
import reactor.netty.http.client.HttpClient;
import io.netty.handler.codec.http.HttpMethod;
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

import javax.net.ssl.SSLException;
import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

@EventInterceptor
public class AsyncHttpClient implements TypedLambdaFunction<EventEnvelope, Void> {
    public static final String ASYNC_HTTP_REQUEST = "async.http.request";
    public static final String ASYNC_HTTP_RESPONSE = "async.http.response";
    private static final Logger log = LoggerFactory.getLogger(AsyncHttpClient.class);
    private static final AtomicBoolean loaded = new AtomicBoolean(false);
    private static final long HOUSEKEEPING_INTERVAL = 30 * 1000L;    // 30 seconds
    private static final long THIRTY_MINUTE = 30 * 60 * 1000L;
    private static final SimpleXmlParser xmlReader = new SimpleXmlParser();
    private static final SimpleXmlWriter xmlWriter = new SimpleXmlWriter();
    private static final String MULTIPART_FORM_DATA = "multipart/form-data";
    private static final String APPLICATION_JSON = "application/json";
    private static final String APPLICATION_XML = "application/xml";
    private static final String X_RAW_XML = "x-raw-xml";
    private static final String X_NO_STREAM = "x-small-payload-as-bytes";
    private static final String APPLICATION_JAVASCRIPT = "application/javascript";
    private static final String TEXT_PREFIX = "text/";
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
    private static final String CONTENT_TYPE = "content-type";
    private static final String CONTENT_LENGTH = "content-length";
    private static final String X_CONTENT_LENGTH = "x-content-length";
    private static final String USER_AGENT = "user-agent";
    private static final String USER_AGENT_NAME = "async-http-client";
    private static final int DEFAULT_TTL_SECONDS = 30;  // 30 seconds
    /*
     * Some headers are ignored because they may interfere with the underlying HttpClient
     */
    private static final String[] HEADERS_TO_IGNORE = { CONTENT_LENGTH, USER_AGENT, X_STREAM_ID,
                                                        "content-encoding", "transfer-encoding", "host", "connection",
                                                        "upgrade-insecure-requests", "accept-encoding",
                                                        "sec-fetch-mode", "sec-fetch-site", "sec-fetch-user" };
    private final File tempDir;
    private final int connectTimeout;

    public AsyncHttpClient() {
        Utility util = Utility.getInstance();
        AppConfigReader config = AppConfigReader.getInstance();
        var timeout = config.getProperty("http.client.connection.timeout", "5000");
        connectTimeout = Math.max(2000, util.str2int(timeout));
        String temp = config.getProperty("app.temp.dir", "/tmp/composable/java/temp-streams");
        tempDir = new File(temp);
        if (!tempDir.exists() && tempDir.mkdirs()) {
            log.info("Temporary work directory {} created", tempDir);
        }
        if (!loaded.get()) {
            loaded.set(true);
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
        throw new AppException(405, "Method not allowed");
    }

    private void processRequest(Map<String, String> headers, EventEnvelope input, int instance)
            throws AppException, URISyntaxException, SSLException {
        PostOffice po = PostOffice.trackable(headers, instance);
        AsyncHttpRequest request = new AsyncHttpRequest(input.getBody());
        HttpMetadata md = new HttpMetadata();
        validateUrl(request, md);
        String uri = normalizeUrl(request, md);
        po.annotateTrace(DESTINATION, request.getTargetHost() + md.rawUri);
        HttpClient client = HttpClient.create()
                            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectTimeout)
                            .headers(h -> updateHttpHeaders(po, request, h));
        int timeout = request.getTimeoutSeconds();
        client.responseTimeout(Duration.ofSeconds(timeout));
        if (md.secure) {
            if (request.isTrustAllCert()) {
                Http11SslContextSpec http11Context = Http11SslContextSpec.forClient()
                    .configure(builder -> builder.trustManager(InsecureTrustManagerFactory.INSTANCE));
                SslContext sslContext = http11Context.sslContext();
                client = client.secure(spec -> spec.sslContext(sslContext));
            } else {
                client.secure();
            }
        }
        var sender = client.request(md.httpMethod).uri(request.getTargetHost() + uri);
        // get request body if any
        String method = request.getMethod();
        if (POST.equals(method) || PUT.equals(method) || PATCH.equals(method)) {
            final var streams = request.getStreamRoutes();
            if (streams.isEmpty()) {
                sendHttpBody(sender, input, request);
            } else {
                uploadFiles(sender, input, request);
            }
        } else {
            var httpResponse = new HttpResponseHandler(input, request, sender);
            httpResponse.process();
        }
    }

    private void uploadFiles(HttpClient.RequestSender sender, EventEnvelope input, AsyncHttpRequest request) {
        String contentType = request.getHeader(CONTENT_TYPE);
        String method = request.getMethod();
        int timeout = request.getTimeoutSeconds();
        final var streams = request.getStreamRoutes();
        objectStreams2files(streams, timeout > 0? timeout : DEFAULT_TTL_SECONDS)
                .onSuccess(files -> {
                    final HttpClient.ResponseReceiver<?> receiver;
                    if (contentType != null && contentType.startsWith(MULTIPART_FORM_DATA) &&
                            POST.equals(method) && request.isValidStreams()) {
                        // support one or more files to upload
                        var fileNames = request.getFileNames();
                        var uploadTags = request.getUploadTags();
                        var contentTypes = request.getFileContentTypes();
                        receiver = sender.sendForm((clientRequest, form) -> {
                            form.multipart(true);
                            int i = 0;
                            for (File f: files) {
                                form.file(uploadTags.get(i), fileNames.get(i), f, contentTypes.get(i));
                                i++;
                            }
                        });
                    } else {
                        receiver = sender.send(ByteBufFlux.fromPath(files.getFirst().toPath()));
                    }
                    var httpResponse = new HttpResponseHandler(input, request, receiver);
                    httpResponse.process();
                })
                .onFailure(e -> sendErrorResponse(input, e));
    }

    private void sendErrorResponse(EventEnvelope input, Throwable e) {
        EventEnvelope response = new EventEnvelope();
        if (input.getReplyTo() != null) {
            sendResponse(input, response.setException(e).setBody(e.getMessage()));
        } else {
            log.error("Unhandled exception", e);
        }
    }

    private void sendHttpBody(HttpClient.RequestSender sender, EventEnvelope input, AsyncHttpRequest request) {
        Object reqBody = request.getBody() == null? new byte[0] : request.getBody();
        final byte[] bytes;
        Utility util = Utility.getInstance();
        String contentType = request.getHeader(CONTENT_TYPE);
        switch (reqBody) {
            case byte[] b -> bytes = b;
            case String text -> bytes = util.getUTF(text);
            case Map<?, ?> map -> {
                boolean xml = contentType != null && contentType.startsWith(APPLICATION_XML);
                bytes = xml ? util.getUTF(xmlWriter.write(reqBody)) :
                        SimpleMapper.getInstance().getMapper().writeValueAsBytes(map);
            }
            case List<?> list -> bytes = SimpleMapper.getInstance().getMapper().writeValueAsBytes(list);
            default -> throw new IllegalArgumentException("Invalid HTTP request body");
        }
        var receiver = sender.send(ByteBufFlux.fromInbound((Mono.just(bytes))));
        var httpResponse = new HttpResponseHandler(input, request, receiver);
        httpResponse.process();
    }

    private void validateUrl(AsyncHttpRequest request, HttpMetadata md) throws URISyntaxException {
        md.httpMethod = getMethod(request.getMethod());
        String targetHost = request.getTargetHost();
        if (targetHost == null) {
            throw new IllegalArgumentException("Missing target host. e.g. https://hostname");
        }
        md.url = new URI(targetHost);
        String protocol = md.url.getScheme();
        if ("http".equals(protocol)) {
            md.secure = false;
        } else if ("https".equals(protocol)) {
            md.secure = true;
        } else {
            throw new IllegalArgumentException("Protocol must be http or https");
        }
        md.host = md.url.getHost().trim();
        if (md.host.isEmpty()) {
            throw new IllegalArgumentException("Unable to resolve target host as domain or IP address");
        }
        md.port = md.url.getPort();
        String path = md.url.getPath();
        if (!path.isEmpty()) {
            throw new IllegalArgumentException("Target host must not contain URI path");
        }
    }

    private String normalizeUrl(AsyncHttpRequest request, HttpMetadata md) {
        Utility util = Utility.getInstance();
        final String uri = request.getUrl();
        int hashMark = uri.lastIndexOf('#');
        final String uriWithoutHash = hashMark == -1? uri : uri.substring(0, hashMark);
        final String hashParams = hashMark == -1? null : uri.substring(hashMark+1);
        md.queryString = null;
        int questionMark = uriWithoutHash.lastIndexOf('?');
        if (questionMark == -1) {
            md.rawUri = uriWithoutHash;
        } else {
            md.rawUri = uriWithoutHash.substring(0, questionMark);
            md.queryString = decodeUri(uriWithoutHash.substring(questionMark+1));
            request.setQueryString(md.queryString);
        }
        // construct target URL
        final String queryParams = queryParametersToString(request);
        // combine query parameters from query string and query parameters
        if (queryParams != null) {
            md.queryString = md.queryString == null? queryParams : md.queryString + "&" + queryParams;
        }
        // render path parameters
        var pathParameters = request.getPathParameters();
        for (var entry : pathParameters.entrySet()) {
            var key = "{"+entry.getKey()+"}";
            if (md.rawUri.contains(key)) {
                md.rawUri = md.rawUri.replace(key, entry.getValue());
            }
        }
        // reconstruct full URI
        var sb = new StringBuilder();
        sb.append(md.rawUri);
        if (md.queryString != null) {
            sb.append('?');
            sb.append(md.queryString);
        }
        if (hashParams != null) {
            sb.append('#');
            sb.append(hashParams);
        }
        return util.getEncodedUri(sb.toString());
    }

    private void updateHttpHeaders(PostOffice po, AsyncHttpRequest request, HttpHeaders http) {
        // set user-agent for this HTTP client
        http.set(USER_AGENT, USER_AGENT_NAME);
        // set content-length, including zero, if needed
        var method = request.getMethod();
        if (request.isContentLengthDefined() && request.getStreamRoutes().isEmpty() &&
                (POST.equals(method) || PUT.equals(method) || PATCH.equals(method))) {
            http.set(CONTENT_LENGTH, request.getContentLength());
        }
        Map<String, String> reqHeaders = request.getHeaders();
        // convert authentication session info into HTTP request headers
        Map<String, String> sessionInfo = request.getSessionInfo();
        reqHeaders.putAll(sessionInfo);
        for (Map.Entry<String, String> kv: reqHeaders.entrySet()) {
            if (permittedHttpHeader(kv.getKey())) {
                http.set(kv.getKey(), kv.getValue());
            }
        }
        // propagate X-Trace-Id when forwarding the HTTP request
        String traceId = po.getTraceId();
        if (traceId != null) {
            http.set(HttpRouter.getDefaultTraceIdLabel(), traceId);
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
            http.set(COOKIE, sb.substring(0, sb.length()-2));
        }
    }

    public String decodeUri(String uri) {
        return uri != null && uri.contains("%")? URLDecoder.decode(uri, StandardCharsets.UTF_8) : uri;
    }

    private void sendResponse(EventEnvelope input, EventEnvelope response) {
        response.setTo(input.getReplyTo()).setFrom(ASYNC_HTTP_REQUEST)
                .setCorrelationId(input.getCorrelationId())
                .setTrace(input.getTraceId(), input.getTracePath());
        EventEmitter.getInstance().send(response);
    }

    private boolean permittedHttpHeader(String header) {
        for (String h: HEADERS_TO_IGNORE) {
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

    private Future<List<File>> objectStreams2files(List<String> streams, int timeoutSeconds) {
        return Future.future(promise -> {
            final List<File> files = new ArrayList<>();
            final List<BufferedOutputStream> fileStreams = new ArrayList<>();
            streams.forEach(id -> files.add(getTempFile(id)));
            files.forEach(f -> {
                try {
                    fileStreams.add(new BufferedOutputStream(new FileOutputStream(f)));
                } catch (FileNotFoundException e) {
                    promise.fail(e);
                }
            });
            final AtomicInteger received = new AtomicInteger(0);
            final AtomicInteger index = new AtomicInteger(0);
            for (String id: streams) {
                final int i = index.getAndIncrement();
                BufferedOutputStream out = fileStreams.get(i);
                FluxConsumer<Object> flux = new FluxConsumer<>(id, Math.max(5000L, timeoutSeconds * 1000L));
                flux.consume(data -> saveFileBlock(data, out),
                e -> closeFileStreams(promise, fileStreams, e),
                () -> {
                    if (received.incrementAndGet() == streams.size()) {
                        closeFileStreams(promise, fileStreams, files);
                    }
                });
            }
        });
    }

    private void saveFileBlock(Object data, BufferedOutputStream out) {
        try {
            if (data instanceof byte[] b && b.length > 0) {
                out.write(b);
            }
            if (data instanceof String text && !text.isEmpty()) {
                out.write(Utility.getInstance().getUTF(text));
            }
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
    }

    private void closeFileStreams(Promise<List<File>> promise, List<BufferedOutputStream> fileStreams, Throwable e) {
        Throwable error = null;
        for (BufferedOutputStream out: fileStreams) {
            try {
                out.close();
            } catch (IOException ex) {
                error = ex;
            }
        }
        promise.fail(error == null? e : error);
    }

    private void closeFileStreams(Promise<List<File>> promise, List<BufferedOutputStream> fileStreams, List<File> files) {
        Throwable error = null;
        for (BufferedOutputStream out: fileStreams) {
            try {
                out.close();
            } catch (IOException ex) {
                error = ex;
            }
        }
        if (error != null) {
            promise.fail(error);
        } else {
            promise.complete(files);
        }
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

    private class HttpResponseHandler {
        private final Utility util = Utility.getInstance();
        private final CustomContentTypeResolver resolver = CustomContentTypeResolver.getInstance();
        private final EventEnvelope response = new EventEnvelope();
        private final EventEnvelope input;
        private final AsyncHttpRequest request;
        private final HttpClient.ResponseReceiver<?> http;
        private final int timeoutSeconds;

        public HttpResponseHandler(EventEnvelope input, AsyncHttpRequest request, HttpClient.ResponseReceiver<?> http) {
            this.input = input;
            this.request = request;
            this.http = http;
            int timeout = request.getTimeoutSeconds();
            this.timeoutSeconds = timeout > 0? timeout : DEFAULT_TTL_SECONDS;
        }

        public void process() {
            var noContent = new AtomicBoolean(true);
            http.responseSingle((httpResponse, buffer) -> {
                response.setStatus(httpResponse.status().code());
                var httpHeaders = httpResponse.responseHeaders();
                httpHeaders.forEach(kv -> response.setHeader(kv.getKey(), kv.getValue()));
                return buffer.asInputStream();
            }).subscribe(stream -> {
                noContent.set(false);
                if (input.getReplyTo() != null) {
                    String resContentType = resolver.getContentType(response.getHeader(CONTENT_TYPE));
                    String len = response.getHeader(CONTENT_LENGTH);
                    boolean renderAsBytes = "true".equals(request.getHeader(X_NO_STREAM));
                    if (renderAsBytes || len != null || isTextResponse(resContentType)) {
                        sendFixedLengthResponse(resContentType, response, resStreamToBytes(response, stream, len));
                    } else {
                        Platform.getInstance().getVirtualThreadExecutor().submit(() ->
                                sendStreamResponse(response, stream));
                    }
                }
            }, e -> {
                noContent.set(false);
                sendErrorResponse(input, e);
            }, () -> {
                if (noContent.get()) {
                    sendResponse(input, response);
                }
            });
        }

        private byte[] resStreamToBytes(EventEnvelope response, InputStream stream, String contentLen) {
            byte[] bytes = Utility.getInstance().stream2bytes(stream);
            // if content-length is not provide, add x-content-length header
            if (contentLen == null) {
                response.setHeader(X_CONTENT_LENGTH, bytes.length);
            }
            return bytes;
        }

        private void sendStreamResponse(EventEnvelope response, InputStream stream) {
            EventPublisher publisher = null;
            try {
                int total = 0;
                int len;
                byte[] buffer = new byte[1024];
                while ((len = stream.read(buffer, 0, buffer.length)) != -1) {
                    if (publisher == null) {
                        publisher = new EventPublisher(timeoutSeconds * 1000L);
                    }
                    total += len;
                    publisher.publish(buffer, 0, len);
                }
                if (publisher != null) {
                    response.setHeader(X_STREAM_ID, publisher.getStreamId())
                            .setHeader(X_TTL, timeoutSeconds * 1000)
                            .setHeader(X_CONTENT_LENGTH, total);
                    publisher.publishCompletion();
                }
                sendResponse(input, response);
            } catch (IOException e) {
                throw new IllegalArgumentException(e);
            }
        }

        private void sendFixedLengthResponse(String resContentType, EventEnvelope response, byte[] b) {
            if (resContentType != null) {
                if (resContentType.startsWith(APPLICATION_JSON)) {
                    sendJsonResponse(response, b);
                } else if (resContentType.startsWith(APPLICATION_XML)) {
                    sendXmlResponse(request, response, b);
                } else if (resContentType.startsWith(TEXT_PREFIX) ||
                        resContentType.startsWith(APPLICATION_JAVASCRIPT)) {
                    sendResponse(input, response.setBody(util.getUTF(b)));
                } else {
                    sendResponse(input, response.setBody(b));
                }
            } else {
                sendResponse(input, response.setBody(b));
            }
        }

        private void sendJsonResponse(EventEnvelope response, byte[] b) {
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
        }

        private void sendXmlResponse(AsyncHttpRequest request, EventEnvelope response, byte[] b) {
            String text = util.getUTF(b);
            String trimmed = text.trim();
            boolean rawXml = "true".equals(request.getHeader(X_RAW_XML));
            if (rawXml) {
                sendResponse(input, response.setBody(text));
            } else {
                try {
                    sendResponse(input, response.setBody(trimmed.isEmpty() ? new HashMap<>() : xmlReader.parse(text)));
                } catch (Exception e) {
                    sendResponse(input, response.setBody(text));
                }
            }
        }

        private boolean isTextResponse(String contentType) {
            return  contentType != null && (
                    contentType.startsWith(APPLICATION_JSON) || contentType.startsWith(APPLICATION_XML) ||
                    contentType.startsWith(TEXT_PREFIX) || contentType.startsWith(APPLICATION_JAVASCRIPT));
        }
    }

    private static class HttpMetadata {
        URI url;
        String rawUri;
        String queryString;
        HttpMethod httpMethod;
        boolean secure;
        String host;
        int port;
    }
}
