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

package org.platformlambda.automation.http;

import io.netty.channel.ChannelOption;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.netty.ByteBufFlux;
import reactor.netty.http.Http11SslContextSpec;
import reactor.netty.http.client.HttpClient;
import io.netty.handler.codec.http.HttpMethod;
import org.platformlambda.automation.services.EventStreamRenderer;
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
import org.msgpack.core.MessagePackException;
import org.platformlambda.core.util.AppConfigReader;
import org.platformlambda.core.util.Utility;
import org.platformlambda.core.util.W3cTrace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLException;
import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

@EventInterceptor
public class AsyncHttpClient implements TypedLambdaFunction<EventEnvelope, Void> {
    public static final String ASYNC_HTTP_REQUEST = "async.http.request";
    public static final String ASYNC_HTTP_RESPONSE = "async.http.response";
    // ordered reply-lane family for streaming responses: a route pool of single-instance
    // lanes (async.http.response.stream.{n}) - a streaming request checks out one dedicated
    // lane for its lifetime, so its segment order is guaranteed while different
    // requests stream concurrently through their own lanes
    public static final String ASYNC_HTTP_RESPONSE_STREAM_POOL = "async.http.response.stream";
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
    private static final String ACCEPT = "accept";
    private static final String TEXT_EVENT_STREAM = "text/event-stream";
    // the Event-over-HTTP relay marks its request event so the SSE consumption
    // switches to the envelope-mode wire dialect
    private static final String X_EVENT_API = "x-event-api";
    private static final String STREAM_RELAY = "stream";
    private static final String ENVELOPE = EventStreamWriter.ENVELOPE;
    private static final String TYPE = "type";
    private static final String ERROR = "error";
    private static final String MESSAGE = "message";
    private static final String CONTENT_TYPE = "content-type";
    private static final String CONTENT_LENGTH = "content-length";
    private static final String X_CONTENT_LENGTH = "x-content-length";
    private static final String USER_AGENT = "user-agent";
    private static final String USER_AGENT_NAME = "async-http-client";
    private static final String BODY_TAG = "    body: ";
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
    private final boolean relaxedHeaderSize;

    public AsyncHttpClient() {
        Utility util = Utility.getInstance();
        AppConfigReader config = AppConfigReader.getInstance();
        var timeout = config.getProperty("http.client.connection.timeout", "5000");
        connectTimeout = Math.max(2000, util.str2int(timeout));
        relaxedHeaderSize = "true".equals(config.getProperty("oversize.http.response.header", "false"));
        String temp = config.getProperty("async.http.temp", "/tmp/async-http-temp");
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
        validateUrl(request);
        String uri = request.getFinalizedUrl();
        po.annotateTrace(DESTINATION, request.getTargetHost() + getRawUrl(uri));
        if (log.isDebugEnabled()) {
            logHttpRequest(request, uri);
        }
        HttpClient client = HttpClient.create()
                            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectTimeout)
                            .headers(h -> updateHttpHeaders(po, request, h, input.getSpanId()));
        // override default of 8 KB to 16 KB - use this with caution
        if (relaxedHeaderSize) {
            client = client.httpResponseDecoder(spec -> spec.maxHeaderSize(16 * 1024));
        }
        // one extra second of grace over the request TTL so a peer that spends
        // its whole TTL and then replies (e.g. an Event-over-HTTP 408 sent AT
        // the deadline) is still readable; the caller's own RPC timeout - not
        // this wire-level read timeout - governs the user-visible deadline.
        // A progressive SSE candidate is exempt: a healthy stream may outlive
        // any fixed total - the SSE relay enforces a per-read idle allowance
        if (!isEventStreamCandidate(input, request)) {
            client = client.responseTimeout(Duration.ofSeconds(request.getTimeoutSeconds() + 1L));
        }
        if (request.isSecure()) {
            if (request.isTrustAllCert()) {
                Http11SslContextSpec http11Context = Http11SslContextSpec.forClient()
                    .configure(builder -> builder.trustManager(InsecureTrustManagerFactory.INSTANCE));
                SslContext sslContext = http11Context.sslContext();
                client = client.secure(spec -> spec.sslContext(sslContext));
            } else {
                client = client.secure();
            }
        }
        var sender = client.request(getMethod(request.getMethod())).uri(request.getTargetHost() + uri);
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

    private String getRawUrl(String uri) {
        return uri.contains("?") ? uri.substring(0, uri.lastIndexOf("?")) : uri;
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

    private void validateUrl(AsyncHttpRequest request) throws URISyntaxException {
        String targetHost = request.getTargetHost();
        if (targetHost == null) {
            throw new IllegalArgumentException("Missing target host. e.g. https://hostname");
        }
        var url = new URI(targetHost);
        String protocol = url.getScheme();
        if ("http".equals(protocol)) {
            request.setSecure(false);
        } else if ("https".equals(protocol)) {
            request.setSecure(true);
        } else {
            throw new IllegalArgumentException("Protocol must be http or https");
        }
        var host = url.getHost().trim();
        if (host.isEmpty()) {
            throw new IllegalArgumentException("Unable to resolve target host as domain or IP address");
        }
        String path = url.getPath();
        if (!path.isEmpty()) {
            throw new IllegalArgumentException("Target host must not contain URI path");
        }
    }

    private void updateHttpHeaders(PostOffice po, AsyncHttpRequest request, HttpHeaders http, String spanId) {
        // set user-agent for this HTTP client
        http.set(USER_AGENT, USER_AGENT_NAME);
        setContentLengthHeader(request, http);
        applyRequestAndSessionHeaders(request, http);
        applyTraceHeaders(po, http, spanId);
        applyBusinessCorrelationId(po, request, http);
        setCookies(request, http);
    }

    private void setContentLengthHeader(AsyncHttpRequest request, HttpHeaders http) {
        // set content-length, including zero, if needed
        var method = request.getMethod();
        if (request.isContentLengthDefined() && request.getStreamRoutes().isEmpty() &&
                (POST.equals(method) || PUT.equals(method) || PATCH.equals(method))) {
            http.set(CONTENT_LENGTH, request.getContentLength());
        }
    }

    private void applyRequestAndSessionHeaders(AsyncHttpRequest request, HttpHeaders http) {
        Map<String, String> reqHeaders = request.getHeaders();
        // convert authentication session info into HTTP request headers
        reqHeaders.putAll(request.getSessionInfo());
        for (Map.Entry<String, String> kv: reqHeaders.entrySet()) {
            if (permittedHttpHeader(kv.getKey())) {
                http.set(kv.getKey(), kv.getValue());
            }
        }
    }

    private void applyTraceHeaders(PostOffice po, HttpHeaders http, String spanId) {
        // Trace headers (X-Trace-Id / W3C "traceparent") copied from the request above are left intact when
        // this call is not being traced: an explicitly developer-set trace header is an intentional act (e.g.
        // handing a trace context to a 3rd-party system, or forwarding an upstream trace) and must propagate.
        // When this call IS traced, the framework's own current trace context takes precedence below so the
        // downstream span chains to this caller's span. Since a traced request's context is itself adopted
        // from the upstream X-Trace-Id/traceparent at ingress, the upstream trace still propagates.
        String traceId = po.getTraceId();
        if (traceId != null) {
            // use the configured trace-id header name (http.trace.id.header, default X-Trace-Id)
            http.set(HttpRouter.getTraceIdHeader(), traceId);
        }
        String traceparent = W3cTrace.format(traceId, spanId);
        if (traceparent != null) {
            http.set(W3cTrace.TRACEPARENT, traceparent);
            // when a custom traceparent header name is configured (http.traceparent.header), stamp the
            // same value under that name too, so the W3C trace context survives an intermediary that
            // strips the standard header
            String customTraceparent = HttpRouter.getTraceparentHeader();
            if (!W3cTrace.TRACEPARENT.equalsIgnoreCase(customTraceparent)) {
                http.set(customTraceparent, traceparent);
            }
        }
    }

    private void applyBusinessCorrelationId(PostOffice po, AsyncHttpRequest request, HttpHeaders http) {
        // propagate the business correlation-id downstream (unless the caller set the header explicitly)
        String businessCorrelationId = po.getMyCorrelationId();
        if (businessCorrelationId != null) {
            String cidHeader = HttpRouter.getCorrelationIdHeader();
            if (cidHeader != null && request.getHeader(cidHeader) == null) {
                http.set(cidHeader, businessCorrelationId);
            }
        }
    }

    private void setCookies(AsyncHttpRequest request, HttpHeaders http) {
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

    /**
     * A request is a progressive-SSE candidate when the caller explicitly accepts
     * text/event-stream AND supplied a reply_to (a multi-shot-capable consumer).
     * Everything else keeps the buffered single-shot behavior.
     *
     * @param input the request event
     * @param request the HTTP request dataset
     * @return true when the response may be consumed progressively
     */
    private static boolean isEventStreamCandidate(EventEnvelope input, AsyncHttpRequest request) {
        if (input.getReplyTo() == null) {
            return false;
        }
        String accept = request.getHeader(ACCEPT);
        return accept != null && accept.contains(TEXT_EVENT_STREAM);
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

    private void logHttpRequest(AsyncHttpRequest request, String uri) {
        var sb = new StringBuilder();
        sb.append("\n>>> ").append(request.getMethod()).append(' ')
          .append(request.getTargetHost()).append(uri).append('\n');
        request.getHeaders().forEach((k, v) ->
                sb.append("    ").append(k).append(": ").append(v).append('\n'));
        Object body = request.getBody();
        if (body instanceof byte[] b) {
            sb.append(BODY_TAG+"[").append(b.length).append(" bytes]\n");
        } else if (body instanceof Map || body instanceof List) {
            sb.append(BODY_TAG)
              .append(SimpleMapper.getInstance().getMapper().writeValueAsString(body)).append('\n');
        } else if (body instanceof String text && !text.isEmpty()) {
            sb.append(BODY_TAG).append(text).append('\n');
        }
        log.debug("{}", sb);
    }

    private class HttpResponseHandler {
        private static final ExecutorService executor = Platform.getInstance().getVirtualThreadExecutor();
        private final Utility util = Utility.getInstance();
        private final CustomContentTypeResolver resolver = CustomContentTypeResolver.getInstance();
        private final EventEnvelope response = new EventEnvelope();
        private final EventEnvelope input;
        private final AsyncHttpRequest request;
        private final HttpClient.ResponseReceiver<?> http;
        private final int timeoutSeconds;
        // the Event-over-HTTP relay path consumes the peer's envelope-mode wire dialect
        private final boolean envelopeRelay;

        public HttpResponseHandler(EventEnvelope input, AsyncHttpRequest request, HttpClient.ResponseReceiver<?> http) {
            this.input = input;
            this.request = request;
            this.http = http;
            int timeout = request.getTimeoutSeconds();
            this.timeoutSeconds = timeout > 0? timeout : DEFAULT_TTL_SECONDS;
            this.envelopeRelay = STREAM_RELAY.equals(input.getHeader(X_EVENT_API));
        }

        public void process() {
            if (isEventStreamCandidate(input, request)) {
                processEventStreamCapable();
                return;
            }
            var noContent = new AtomicBoolean(true);
            http.responseSingle((httpResponse, buffer) -> {
                response.setStatus(httpResponse.status().code());
                var httpHeaders = httpResponse.responseHeaders();
                httpHeaders.forEach(kv -> response.setHeader(kv.getKey(), kv.getValue()));
                return buffer.asInputStream();
            }).subscribeOn(Schedulers.fromExecutor(executor)).subscribe(stream -> {
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

        /**
         * The caller opted into SSE (Accept + reply_to). If the upstream answers with
         * text/event-stream, consume it progressively: one x-event-stream data envelope
         * per SSE event to the caller's reply_to, then eof - the same producer contract
         * the HTTP edge consumes. A non-SSE response falls back to the buffered
         * single-shot rendering, exactly as before.
         */
        private void processEventStreamCapable() {
            var relay = new SseRelay();
            var fallback = new ByteArrayOutputStream();
            var streaming = new AtomicBoolean(false);
            Disposable connection = http.response((httpResponse, content) -> {
                response.setStatus(httpResponse.status().code());
                var httpHeaders = httpResponse.responseHeaders();
                httpHeaders.forEach(kv -> response.setHeader(kv.getKey(), kv.getValue()));
                String resContentType = resolver.getContentType(response.getHeader(CONTENT_TYPE));
                if (resContentType != null && resContentType.startsWith(TEXT_EVENT_STREAM)) {
                    streaming.set(true);
                    relay.start();
                }
                return content.asByteArray();
            }).subscribeOn(Schedulers.fromExecutor(executor)).subscribe(
                chunk -> {
                    if (streaming.get()) {
                        relay.onChunk(chunk);
                    } else {
                        fallback.writeBytes(chunk);
                    }
                },
                e -> {
                    if (streaming.get()) {
                        relay.onTransportError(e);
                    } else {
                        sendErrorResponse(input, e);
                    }
                },
                () -> {
                    if (streaming.get()) {
                        relay.onComplete();
                    } else {
                        renderBuffered(fallback.toByteArray());
                    }
                });
            relay.setConnection(connection);
        }

        /**
         * Buffered fallback of the SSE-candidate path (the upstream did not stream):
         * render exactly as the single-shot path would
         */
        private void renderBuffered(byte[] b) {
            if (envelopeRelay) {
                // the peer answered single-shot (a non-streaming target, or an edge
                // error) - deliver the decoded reply with the classic callback semantics
                deliverToCallback(decodeRelayReply(b));
                return;
            }
            String resContentType = resolver.getContentType(response.getHeader(CONTENT_TYPE));
            String len = response.getHeader(CONTENT_LENGTH);
            boolean renderAsBytes = "true".equals(request.getHeader(X_NO_STREAM));
            if (renderAsBytes || len != null || isTextResponse(resContentType)) {
                if (len == null) {
                    response.setHeader(X_CONTENT_LENGTH, b.length);
                }
                sendFixedLengthResponse(resContentType, response, b);
            } else if (b.length > 0) {
                Platform.getInstance().getVirtualThreadExecutor().submit(() ->
                        sendStreamResponse(response, new ByteArrayInputStream(b)));
            } else {
                sendResponse(input, response);
            }
        }

        /**
         * Decode a single-shot Event-over-HTTP reply: a serialized envelope normally,
         * with the classic tolerant handling of an edge-level REST error body
         * and of a payload that is not a packed envelope at all
         *
         * @param b the buffered response body
         * @return the reply envelope for the callback
         */
        private EventEnvelope decodeRelayReply(byte[] b) {
            if (b.length == 0) {
                return new EventEnvelope().setStatus(response.getStatus());
            }
            try {
                return new EventEnvelope(b);
            } catch (IllegalArgumentException | MessagePackException e) {
                EventEnvelope restError = restErrorReply(b);
                return restError != null? restError : new EventEnvelope().setStatus(400)
                        .setBody("Did you configure rest.yaml correctly? Invalid result set - " + e.getMessage());
            }
        }

        /**
         * An edge-level REST error ({"status": n, "message": ..., "type": "error"})
         * arrives as JSON, not as a packed envelope - unwrap it exactly as the
         * classic relay does
         *
         * @param b the buffered response body
         * @return the unwrapped error envelope, or null when it is not a REST error
         */
        @SuppressWarnings("unchecked")
        private EventEnvelope restErrorReply(byte[] b) {
            if (response.getStatus() >= 400) {
                try {
                    Map<String, Object> data = SimpleMapper.getInstance().getMapper().readValue(b, Map.class);
                    if (ERROR.equals(data.get(TYPE)) && data.get(MESSAGE) instanceof String text) {
                        return new EventEnvelope().setStatus(response.getStatus()).setBody(text);
                    }
                } catch (Exception e) {
                    // not a REST error body
                }
            }
            return null;
        }

        /**
         * Forward one decoded or synthesized event to the original caller's reply
         * route with the original correlation id (the relay rewrites addressing)
         *
         * @param reply the event to forward
         */
        private void deliverToCallback(EventEnvelope reply) {
            reply.setTo(input.getReplyTo())
                    .setFrom(input.getFrom() == null? ASYNC_HTTP_REQUEST : input.getFrom())
                    .setReplyTo(null)
                    .setCorrelationId(input.getCorrelationId())
                    .setTrace(input.getTraceId(), input.getTracePath());
            EventEmitter.getInstance().send(reply);
        }

        /**
         * Progressive SSE consumption (raw mode): an incremental frame parser feeding
         * x-event-stream envelopes to the caller's reply_to. The request TTL is the
         * per-read idle allowance (any upstream bytes - keep-alive comments included -
         * reset it); idle expiry and mid-stream transport errors fail the stream
         * in-band, matching the edge contract.
         */
        private class SseRelay {
            // the reply-lane lesson applies here too: the chunk consumer and the
            // idle timer run on different threads - one lock serializes them
            private final ReentrantLock lock = new ReentrantLock();
            private final ByteArrayOutputStream pending = new ByteArrayOutputStream();
            private final List<String> dataLines = new ArrayList<>();
            private final long idleMs = timeoutSeconds * 1000L;
            private String eventName = null;
            private boolean headSent = false;
            private volatile boolean closed = false;
            private volatile long lastActivity = System.currentTimeMillis();
            private long timer = -1;
            private Disposable connection;

            public void start() {
                lastActivity = System.currentTimeMillis();
                armIdleTimer(idleMs);
            }

            public void setConnection(Disposable connection) {
                this.connection = connection;
            }

            private void armIdleTimer(long delayMs) {
                timer = Platform.getInstance().getVertx().setTimer(Math.max(100, delayMs), t -> {
                    if (closed) {
                        return;
                    }
                    long quiet = System.currentTimeMillis() - lastActivity;
                    if (quiet >= idleMs) {
                        onIdleTimeout();
                    } else {
                        armIdleTimer(idleMs - quiet);
                    }
                });
            }

            public void onChunk(byte[] chunk) {
                // any upstream bytes prove liveness - comments included
                lastActivity = System.currentTimeMillis();
                lock.lock();
                try {
                    if (closed) {
                        return;
                    }
                    pending.writeBytes(chunk);
                    drainCompleteLines();
                } finally {
                    lock.unlock();
                }
            }

            /**
             * Extract complete lines from the pending buffer - a newline is a
             * single byte, so byte-level splitting is UTF-8 safe
             */
            private void drainCompleteLines() {
                byte[] buffer = pending.toByteArray();
                int start = 0;
                for (int i = 0; i < buffer.length; i++) {
                    if (buffer[i] == '\n') {
                        int end = i > start && buffer[i - 1] == '\r' ? i - 1 : i;
                        onLine(new String(buffer, start, end - start, StandardCharsets.UTF_8));
                        start = i + 1;
                    }
                }
                pending.reset();
                if (start < buffer.length) {
                    pending.write(buffer, start, buffer.length - start);
                }
            }

            /**
             * One SSE line - the caller holds the lock
             */
            private void onLine(String line) {
                if (line.isEmpty()) {
                    // blank line dispatches the pending event (SSE specification)
                    if (!dataLines.isEmpty()) {
                        emitData(String.join("\n", dataLines), eventName);
                    }
                    dataLines.clear();
                    eventName = null;
                } else if (line.charAt(0) != ':') {
                    // a comment line (leading colon) is consumed, never forwarded
                    int colon = line.indexOf(':');
                    String field = colon == -1 ? line : line.substring(0, colon);
                    String value = colon == -1 ? "" : line.substring(colon + 1);
                    if (!value.isEmpty() && value.charAt(0) == ' ') {
                        value = value.substring(1);
                    }
                    switch (field) {
                        case "data" -> dataLines.add(value);
                        case "event" -> eventName = value;
                        default -> { /* id, retry and unknown fields are ignored */ }
                    }
                }
            }

            private void emitData(String text, String name) {
                if (envelopeRelay) {
                    emitRelayFrame(text, name);
                    return;
                }
                EventEnvelope segment = new EventEnvelope()
                        .setHeader(EventStreamWriter.X_EVENT_STREAM, EventStreamWriter.DATA)
                        .setBody(text);
                if (name != null && !name.isEmpty()) {
                    segment.setHeader(EventStreamWriter.X_EVENT_NAME, name);
                }
                if (!headSent) {
                    headSent = true;
                    // head control rides the first envelope: upstream status + SSE type
                    segment.setStatus(response.getStatus());
                    segment.setHeader(CONTENT_TYPE, TEXT_EVENT_STREAM);
                }
                sendSegment(segment);
            }

            /**
             * Envelope-mode dialect (the Event-over-HTTP relay): an "envelope" frame
             * carries one base64-encoded serialized EventEnvelope - the head, the
             * terminals and non-text segments; any other frame is a raw text segment.
             * A decoded terminal (eof or exception) ends the logical stream - trailing
             * frames are discarded. The caller holds the lock.
             *
             * @param text the frame's data text
             * @param name the frame's event name, if any
             */
            private void emitRelayFrame(String text, String name) {
                if (closed) {
                    return;
                }
                if (ENVELOPE.equals(name)) {
                    deliverEnvelopeFrame(text);
                } else if (!headSent) {
                    // the dialect guarantees an envelope frame first (conformance guard)
                    failInBand(500, "Invalid event stream - missing envelope head");
                } else {
                    deliverRawFrame(text, name);
                }
            }

            /**
             * Decode one envelope frame and forward it; a decoded terminal
             * (eof or exception) ends the logical stream
             *
             * @param text the frame's base64-encoded serialized envelope
             */
            private void deliverEnvelopeFrame(String text) {
                final EventEnvelope decoded;
                try {
                    decoded = new EventEnvelope(util.base64ToBytes(text));
                } catch (IllegalArgumentException | MessagePackException e) {
                    failInBand(500, "Invalid event stream - malformed envelope frame");
                    return;
                }
                headSent = true;
                deliverToCallback(decoded);
                String signal = EventStreamRenderer.getSignal(decoded);
                if (EventStreamWriter.EOF.equals(signal) || EventStreamWriter.EXCEPTION.equals(signal)) {
                    closed = true;
                    cancelIdleTimer();
                    if (connection != null && !connection.isDisposed()) {
                        connection.dispose();
                    }
                }
            }

            /**
             * Synthesize a data envelope from one raw text frame and forward it
             *
             * @param text the frame's data text
             * @param name the frame's event name, if any
             */
            private void deliverRawFrame(String text, String name) {
                EventEnvelope segment = new EventEnvelope()
                        .setHeader(EventStreamWriter.X_EVENT_STREAM, EventStreamWriter.DATA)
                        .setBody(text);
                if (name != null && !name.isEmpty()) {
                    segment.setHeader(EventStreamWriter.X_EVENT_NAME, name);
                }
                deliverToCallback(segment);
            }

            public void onComplete() {
                if (envelopeRelay) {
                    // the dialect ends with a decoded terminal - a bare transport end
                    // is a truncation; after a terminal this is a silent no-op
                    failInBand(500, "Event stream ended without eof");
                    return;
                }
                lock.lock();
                try {
                    if (closed) {
                        return;
                    }
                    // an incomplete trailing event is discarded (SSE specification)
                    closed = true;
                    cancelIdleTimer();
                    EventEnvelope eof = new EventEnvelope()
                            .setHeader(EventStreamWriter.X_EVENT_STREAM, EventStreamWriter.EOF);
                    if (!headSent) {
                        headSent = true;
                        eof.setStatus(response.getStatus()).setHeader(CONTENT_TYPE, TEXT_EVENT_STREAM);
                    }
                    sendSegment(eof);
                } finally {
                    lock.unlock();
                }
            }

            public void onTransportError(Throwable e) {
                failInBand(500, e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage());
            }

            private void onIdleTimeout() {
                failInBand(408, "Timeout for " + (idleMs / 1000) + " seconds");
                if (connection != null && !connection.isDisposed()) {
                    connection.dispose();
                }
            }

            private void failInBand(int status, String message) {
                lock.lock();
                try {
                    if (closed) {
                        return;
                    }
                    closed = true;
                    cancelIdleTimer();
                    EventEnvelope error = new EventEnvelope()
                            .setHeader(EventStreamWriter.X_EVENT_STREAM, EventStreamWriter.EXCEPTION)
                            .setStatus(status)
                            .setBody(Map.of(TYPE, ERROR, "status", status, MESSAGE, message));
                    if (!headSent) {
                        headSent = true;
                        error.setHeader(CONTENT_TYPE, TEXT_EVENT_STREAM);
                    }
                    if (envelopeRelay) {
                        deliverToCallback(error);
                    } else {
                        sendSegment(error);
                    }
                } finally {
                    lock.unlock();
                }
            }

            private void cancelIdleTimer() {
                if (timer != -1) {
                    Platform.getInstance().getVertx().cancelTimer(timer);
                    timer = -1;
                }
            }

            private void sendSegment(EventEnvelope segment) {
                segment.setTo(input.getReplyTo()).setFrom(ASYNC_HTTP_REQUEST)
                        .setCorrelationId(input.getCorrelationId())
                        .setTrace(input.getTraceId(), input.getTracePath());
                EventEmitter.getInstance().send(segment);
            }
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
            if (log.isDebugEnabled()) {
                logHttpResponse(response, b);
            }
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

        private void logHttpResponse(EventEnvelope response, byte[] b) {
            var sb = new StringBuilder();
            sb.append("\n<<< ").append(response.getStatus()).append('\n');
            response.getHeaders().forEach((k, v) ->
                    sb.append("    ").append(k).append(": ").append(v).append('\n'));
            if (b != null && b.length > 0) {
                sb.append(BODY_TAG).append(Utility.getInstance().getUTF(b)).append('\n');
            }
            log.debug("{}", sb);
        }
    }
}
