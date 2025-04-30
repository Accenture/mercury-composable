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

import io.netty.handler.codec.http.cookie.ServerCookieEncoder;
import io.vertx.core.MultiMap;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.Cookie;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import org.platformlambda.automation.config.RoutingEntry;
import org.platformlambda.automation.http.AsyncHttpClient;
import org.platformlambda.automation.models.*;
import org.platformlambda.automation.util.CustomContentTypeResolver;
import org.platformlambda.automation.util.MimeTypeResolver;
import org.platformlambda.automation.util.SimpleHttpUtility;
import org.platformlambda.core.annotations.EventInterceptor;
import org.platformlambda.core.annotations.ZeroTracing;
import org.platformlambda.core.exception.AppException;
import org.platformlambda.core.models.AsyncHttpRequest;
import org.platformlambda.core.models.EventEnvelope;
import org.platformlambda.core.models.LambdaFunction;
import org.platformlambda.core.serializers.SimpleMapper;
import org.platformlambda.core.serializers.SimpleXmlParser;
import org.platformlambda.core.system.*;
import org.platformlambda.core.util.AppConfigReader;
import org.platformlambda.core.util.CryptoApi;
import org.platformlambda.core.util.Utility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * This is reserved for system use.
 * DO NOT use this directly in your application code.
 */
public class HttpRouter {
    private static final Logger log = LoggerFactory.getLogger(HttpRouter.class);
    private static final CustomContentTypeResolver resolver = CustomContentTypeResolver.getInstance();
    private static final CryptoApi crypto = new CryptoApi();
    private static final SimpleXmlParser xmlReader = new SimpleXmlParser();
    private static final AtomicInteger initCounter = new AtomicInteger(0);
    private static final String HTTP_REQUEST = "http.request";
    private static final String AUTH_HANDLER = "http.auth.handler";
    private static final String CONTENT_TYPE = "Content-Type";
    private static final String CONTENT_LEN = "Content-Length";
    private static final String X_NO_STREAM = "x-small-payload-as-bytes";
    private static final String X_TTL = "x-ttl";
    private static final String APPLICATION_FORM_URLENCODED = "application/x-www-form-urlencoded";
    private static final String MULTIPART_FORM_DATA = "multipart/form-data";
    private static final String APPLICATION_JSON = "application/json";
    private static final String APPLICATION_XML = "application/xml";
    private static final String APPLICATION_OCTET_STREAM = "application/octet-stream";
    private static final String X_RAW_XML = "x-raw-xml";
    private static final String TEXT_HTML = "text/html";
    private static final String TEXT_PLAIN = "text/plain";
    private static final String PROTOCOL = "x-forwarded-proto";
    private static final String HTTPS = "https";
    private static final String COOKIE = "cookie";
    private static final String OPTIONS = "OPTIONS";
    private static final String GET = "GET";
    private static final String PUT = "PUT";
    private static final String POST = "POST";
    private static final String PATCH = "PATCH";
    private static final String INDEX_HTML = "index.html";
    private static final String HTML_EXT = ".html";
    private static final String CLASSPATH = "classpath:";
    private static final String FILEPATH = "file:";
    private static final String ETAG = "ETag";
    private static final String IF_NONE_MATCH = "If-None-Match";
    private static final int BUFFER_SIZE = 4 * 1024;
    private static final long FILTER_TIMEOUT = 10000;
    private static final byte[] NOTHING = new byte[0];
    private static final String SIGNATURE = "/" + Utility.getInstance().getUuid();
    private static final File SIGNATURE_FOLDER = new File(SIGNATURE);
    private static final Path SIGNATURE_FOLDER_PATH = SIGNATURE_FOLDER.toPath();
    // requestId -> context
    private static final ConcurrentMap<String, AsyncContextHolder> contexts = new ConcurrentHashMap<>();
    private static List<String> traceIdLabels;
    private static String staticFolder;
    private static String resourceFolder;

    public HttpRouter() {
        initialize();
    }

    public static void initialize() {
        if (initCounter.incrementAndGet() == 1) {
            Platform platform = Platform.getInstance();
            Utility util = Utility.getInstance();
            AppConfigReader config = AppConfigReader.getInstance();
            List<String> labels = util.split(config.getProperty("trace.http.header"), ", ");
            if (labels.isEmpty()) {
                labels.add("X-Trace-Id");
            }
            traceIdLabels = labels;
            log.info("Initialized with HTTP trace headers {}", traceIdLabels);
            String folder = config.getProperty("spring.web.resources.static-locations",
                    config.getProperty("static.html.folder", "classpath:/public"));
            if (folder.endsWith("/")) {
                folder = folder.substring(0, folder.length()-1);
            }
            if (folder.startsWith(CLASSPATH)) {
                resourceFolder = folder.substring(CLASSPATH.length());
            } else if (folder.startsWith(FILEPATH)) {
                staticFolder = folder.substring(FILEPATH.length());
            } else if (folder.startsWith("/")) {
                staticFolder = folder;
            } else {
                log.warn("Static content folder must start with {} or {}", CLASSPATH, FILEPATH);
            }
            // initialize mime-type and custom content-type resolvers
            MimeTypeResolver.getInstance().init();
            CustomContentTypeResolver.getInstance().init();
            // register authentication handler
            try {
                platform.registerPrivate(AUTH_HANDLER, new HttpAuth(), 200);
            } catch (IOException e) {
                log.error("Unable to load {} - {}", AUTH_HANDLER, e.getMessage());
            }
        }
    }

    public static String getDefaultTraceIdLabel() {
        return traceIdLabels.getFirst();
    }

    public ConcurrentMap<String, AsyncContextHolder> getContexts() {
        return contexts;
    }

    public static void closeContext(String requestId) {
        contexts.remove(requestId);
    }

    @SuppressWarnings("rawtypes")
    public void handleEvent(AssignedRoute route, String requestId, int status, String error) {
        AsyncContextHolder holder = contexts.get(requestId);
        if (holder != null) {
            Utility util = Utility.getInstance();
            HttpServerRequest request = holder.request;
            String uri = util.getDecodedUri(request.path());
            SimpleHttpUtility httpUtil = SimpleHttpUtility.getInstance();
            HttpServerResponse response = request.response();
            if (error != null) {
                if (GET.equals(request.method().name()) && status == 404) {
                    EtagFile file = getStaticFile(uri);
                    if (file != null) {
                        RoutingEntry restConfig = RoutingEntry.getInstance();
                        boolean noCache = matchedElement(restConfig.getNoCachePages(), uri);
                        SimpleHttpFilter filter = restConfig.getRequestFilter();
                        if (filter != null && needFilter(filter, uri)) {
                            EventEmitter po = EventEmitter.getInstance();
                            if (po.exists(filter.service)) {
                                try {
                                    var event = new EventEnvelope()
                                                    .setTo(filter.service).setBody(createHttpRequest(request, uri));
                                    po.asyncRequest(event, FILTER_TIMEOUT, false)
                                        .onSuccess(filtered -> {
                                            // this allows the filter to set HTTP response headers
                                            Map<String, String> headers = filtered.getHeaders();
                                            headers.forEach(response::putHeader);
                                            // this allows the filter to send redirect-url or throw exception
                                            if (filtered.getStatus() == 200) {
                                                sendStaticFile(requestId, file, noCache, request, response);
                                            } else {
                                                response.setStatusCode(filtered.getStatus());
                                                Object body = filtered.getRawBody();
                                                final byte[] content = switch (body) {
                                                    case null -> NOTHING;
                                                    case String s -> util.getUTF(s);
                                                    case byte[] bytes -> bytes;
                                                    case Map m ->
                                                            SimpleMapper.getInstance().getMapper().writeValueAsBytes(m);
                                                    default -> util.getUTF(String.valueOf(body));
                                                };
                                                if (content.length > 0) {
                                                    response.write(Buffer.buffer(content));
                                                }
                                                closeContext(requestId);
                                                response.end();
                                            }
                                        });
                                    return;
                                } catch (IOException e) {
                                    log.error("Unable to filter static content HTTP-GET {} - {}",
                                            filter.service, e.getMessage());
                                }
                            } else {
                                log.warn("Static content filter {} ignored because it does not exist", filter.service);
                            }
                        }
                        sendStaticFile(requestId, file, noCache, request, response);
                        return;
                    }
                }
                httpUtil.sendError(requestId, request, status, error);
            } else {
                try {
                    routeRequest(requestId, route, holder);
                } catch (AppException e) {
                    httpUtil.sendError(requestId, request, e.getStatus(), e.getMessage());
                }
            }
        }
    }

    private boolean needFilter(SimpleHttpFilter filter, String path) {
        boolean pathFound = matchedElement(filter.pathList, path);
        boolean exclusionFound = matchedElement(filter.exclusionList, path);
        return pathFound && !exclusionFound;
    }

    private boolean matchedElement(List<String> elements, String path) {
        for (String p: elements) {
            if (p.endsWith("*")) {
                if (path.startsWith(p.substring(0, p.length()-1))) {
                    return true;
                }
            } else if (p.startsWith("*")) {
                if (path.endsWith(p.substring(1))) {
                    return true;
                }
            } else {
                if (path.equals(p)) {
                    return true;
                }
            }
        }
        return false;
    }

    private AsyncHttpRequest createHttpRequest(HttpServerRequest request, String path) {
        AsyncHttpRequest req = new AsyncHttpRequest();
        String queryString = request.query();
        if (queryString != null) {
            req.setQueryString(queryString);
        }
        req.setUrl(path);
        req.setMethod(request.method().name());
        req.setSecure(HTTPS.equals(request.getHeader(PROTOCOL)));
        MultiMap params = request.params();
        for (String key: params.names()) {
            List<String> values = params.getAll(key);
            if (values.size() == 1) {
                req.setQueryParameter(key, values.getFirst());
            }
            if (values.size() > 1) {
                req.setQueryParameter(key, values);
            }
        }
        boolean hasCookies = false;
        MultiMap headerMap = request.headers();
        for (String key: headerMap.names()) {
            String value = headerMap.get(key);
            if (COOKIE.equalsIgnoreCase(key)) {
                hasCookies = true;
            } else {
                req.setHeader(key, value);
            }
        }
        // load cookies
        if (hasCookies) {
            Set<Cookie> cookies = request.cookies();
            for (Cookie c : cookies) {
                try {
                    ServerCookieEncoder.STRICT.encode(c.getName(), c.getValue());
                    req.setCookie(c.getName(), c.getValue());
                } catch (Exception e) {
                    log.warn("Invalid cookie {} ignored during filtering - {}", c.getName(), e.getMessage());
                }
            }
        }
        return req.setRemoteIp(request.remoteAddress().hostAddress());
    }

    private void sendStaticFile(String requestId, EtagFile file, boolean noCache,
                                HttpServerRequest request, HttpServerResponse response) {
        response.putHeader(CONTENT_TYPE, getFileContentType(file.name));
        if (noCache) {
            response.putHeader("Cache-Control", "no-cache, no-store");
            response.putHeader("Pragma", "no-cache");
            response.putHeader("Expires", "Thu, 01 Jan 1970 00:00:00 GMT");
            response.putHeader(CONTENT_LEN, String.valueOf(file.content.length));
            response.write(Buffer.buffer(file.content));
        } else {
            String ifNoneMatch = request.getHeader(IF_NONE_MATCH);
            if (file.sameTag(ifNoneMatch)) {
                response.setStatusCode(304);
                response.putHeader(CONTENT_LEN, "0");
            } else {
                response.putHeader(ETAG, file.eTag);
                response.putHeader(CONTENT_LEN, String.valueOf(file.content.length));
                response.write(Buffer.buffer(file.content));
            }
        }
        closeContext(requestId);
        response.end();
    }

    /**
     * This is a very primitive way to resolve content-type for proper loading of
     * HTML, CSS and Javascript contents by a browser.
     * <p>
     * It is not intended to be a comprehensive MIME type resolver.
     *
     * @param filename from the URI path
     * @return content type
     */
    private String getFileContentType(String filename) {
        if (filename.contains(".") && !filename.endsWith(".")) {
            String ext = filename.substring(filename.lastIndexOf('.')+1).toLowerCase();
            String contentType = MimeTypeResolver.getInstance().getMimeType(ext);
            if (contentType != null) {
                return contentType;
            }
        }
        return APPLICATION_OCTET_STREAM;
    }

    /**
     * Retrieve a static file from HTML resource folder
     * <p>
     * Since the incoming HTTP request is protected by the util.getDecodedUri(path) method,
     * the additional "path traversal" guarantees that it can catch such an attack even when
     * the gatekeeper logic misses it.
     *
     * @param uriPath relative to HTML folder
     * @return file in eTag format
     */
    private EtagFile getStaticFile(String uriPath) {
        // trim trailing white space because the normalize() function does not allow that in Windows OS
        List<String> parts = Utility.getInstance().split(uriPath.trim(), "/\\");
        StringBuilder sb = new StringBuilder();
        if (parts.isEmpty()) {
            sb.append('/');
        } else {
            for (String p : parts) {
                sb.append('/');
                sb.append(p);
            }
        }
        String normalized = sb.toString();
        final Path tp;
        try {
            tp = new File(SIGNATURE_FOLDER, normalized).toPath().normalize();
        } catch(InvalidPathException e) {
            return null;
        }
        if (!tp.startsWith(SIGNATURE_FOLDER_PATH)) {
            log.error("Reject path traversal attack - {}", uriPath);
            return null;
        }
        // convert Windows path to regular Unix path
        String relativePath = tp.toString().substring(SIGNATURE.length()).replace("\\", "/");
        // assume ".html" if filename does not have a file extension
        String filename = parts.isEmpty()? INDEX_HTML : parts.getLast();
        if (normalized.endsWith("/")) {
            relativePath += INDEX_HTML;
            filename = INDEX_HTML;
        } else if (!filename.contains(".")) {
            relativePath += HTML_EXT;
            filename += HTML_EXT;
        }
        EtagFile result = null;
        if (resourceFolder != null) {
            result = getResourceFile(relativePath);
        }
        if (staticFolder != null) {
            result = getLocalFile(relativePath);
        }
        if (result != null) {
            result.name = filename;
        }
        return result;
    }

    private EtagFile getResourceFile(String relativePath) {
        String resPath = resourceFolder + (relativePath.startsWith("/")? relativePath : "/" + relativePath);
        Utility util = Utility.getInstance();
        InputStream in = this.getClass().getResourceAsStream(resPath);
        if (in != null) {
            byte[] b = Utility.getInstance().stream2bytes(in);
            return new EtagFile(util.bytes2hex(crypto.getSHA256(b)), b);
        }
        return null;
    }

    private EtagFile getLocalFile(String relativePath) {
        Utility util = Utility.getInstance();
        File f = new File(staticFolder, relativePath);
        if (f.exists() && !f.isDirectory()) {
            byte[] b = Utility.getInstance().file2bytes(f);
            return new EtagFile(util.bytes2hex(crypto.getSHA256(b)), b);
        }
        return null;
    }

    private void routeRequest(String requestId, AssignedRoute route, AsyncContextHolder holder)
            throws AppException {
        Utility util = Utility.getInstance();
        HttpServerRequest request = holder.request;
        String uri = util.getDecodedUri(request.path());
        String method = request.method().name();
        holder.setUrl(uri).setMethod(method).setResHeaderId(route.info.responseTransformId);
        SimpleHttpUtility httpUtil = SimpleHttpUtility.getInstance();
        if (OPTIONS.equals(method)) {
            // insert CORS headers for OPTIONS
            if (route.info.corsId == null) {
                throw new AppException(405, "Method not allowed");
            } else {
                CorsInfo corsInfo = RoutingEntry.getInstance().getCorsInfo(route.info.corsId);
                if (corsInfo != null && !corsInfo.options.isEmpty()) {
                    HttpServerResponse response = request.response();
                    for (String ch : corsInfo.options.keySet()) {
                        String prettyHeader = getHeaderCase(ch);
                        if (prettyHeader != null) {
                            response.putHeader(prettyHeader, corsInfo.options.get(ch));
                        }
                    }
                    closeContext(requestId);
                    response.setStatusCode(204).end();
                } else {
                    throw new AppException(405, "Method not allowed");
                }
            }
            return;
        }
        HttpServerResponse response = request.response();
        // insert CORS headers for the HTTP response
        if (route.info.corsId != null) {
            CorsInfo corsInfo = RoutingEntry.getInstance().getCorsInfo(route.info.corsId);
            if (corsInfo != null && !corsInfo.headers.isEmpty()) {
                for (String ch : corsInfo.headers.keySet()) {
                    String prettyHeader = getHeaderCase(ch);
                    if (prettyHeader != null) {
                        response.putHeader(prettyHeader, corsInfo.headers.get(ch));
                    }
                }
            }
        }
        // check if target service is available
        String authService = null;
        EventEmitter po = EventEmitter.getInstance();
        if (!po.exists(route.info.primary)) {
            throw new AppException(503, "Service " + route.info.primary + " not reachable");
        }
        if (route.info.defaultAuthService != null) {
            List<String> authHeaders = route.info.getAuthHeaders();
            if (!authHeaders.isEmpty()) {
                for (String h: authHeaders) {
                    String v = request.getHeader(h);
                    if (v != null) {
                        String svc = route.info.getAuthService(h);
                        if (svc == null) {
                            svc = route.info.getAuthService(h, v);
                        }
                        if (svc != null) {
                            authService = svc;
                            break;
                        }
                    }
                }
            }
            if (authService == null) {
                authService = route.info.defaultAuthService;
            }
            if (!po.exists(authService)) {
                throw new AppException(503, "Service " + authService + " not reachable");
            }
        }
        AsyncHttpRequest req = new AsyncHttpRequest();
        String queryString = request.query();
        if (queryString != null) {
            req.setQueryString(queryString);
        }
        req.setUrl(httpUtil.normalizeUrl(uri, route.info.urlRewrite));
        if (route.info.host != null) {
            req.setTargetHost(route.info.host);
            req.setTrustAllCert(route.info.trustAllCert);
        }
        req.setMethod(method);
        req.setSecure(HTTPS.equals(request.getHeader(PROTOCOL)));
        req.setTimeoutSeconds(route.info.timeoutSeconds);
        if (!route.arguments.isEmpty()) {
            for (String p: route.arguments.keySet()) {
                req.setPathParameter(p, route.arguments.get(p));
            }
        }
        MultiMap params = request.params();
        for (String key: params.names()) {
            List<String> values = params.getAll(key);
            if (values.size() == 1) {
                req.setQueryParameter(key, values.getFirst());
            }
            if (values.size() > 1) {
                req.setQueryParameter(key, values);
            }
        }
        boolean hasCookies = false;
        Map<String, String> headers = new HashMap<>();
        MultiMap headerMap = request.headers();
        for (String key: headerMap.names()) {
            // Single-value HTTP header is assumed
            String value = headerMap.get(key);
            if (COOKIE.equalsIgnoreCase(key)) {
                hasCookies = true;
            } else {
                headers.put(key.toLowerCase(), value);
            }
        }
        // load cookies
        if (hasCookies) {
            Set<Cookie> cookies = request.cookies();
            for (Cookie c : cookies) {
                try {
                    ServerCookieEncoder.STRICT.encode(c.getName(), c.getValue());
                    req.setCookie(c.getName(), c.getValue());
                } catch (Exception e) {
                    log.warn("Invalid cookie {} ignored during routing - {}", c.getName(), e.getMessage());
                }
            }
        }
        RoutingEntry re = RoutingEntry.getInstance();
        if (route.info.requestTransformId != null) {
            headers = httpUtil.filterHeaders(re.getRequestHeaderInfo(route.info.requestTransformId), headers);
        }
        for (Map.Entry<String, String> entry: headers.entrySet()) {
            req.setHeader(entry.getKey(), entry.getValue());
        }
        if (route.info.flowId != null) {
            req.setHeader("x-flow-id", route.info.flowId);
        }
        req.setRemoteIp(request.remoteAddress().hostAddress());
        // Distributed tracing required?
        String traceId = null;
        String tracePath = null;
        // Set trace header if needed
        if (route.info.tracing) {
            List<String> traceHeader = getTraceId(request);
            traceId = traceHeader.get(1);
            tracePath = method + " " + uri;
            if (queryString != null) {
                tracePath += "?" + queryString;
            }
            response.putHeader(traceHeader.get(0), traceHeader.get(1));
        }
        final HttpRequestEvent requestEvent = new HttpRequestEvent(requestId, route.info.primary,
                                                    authService, traceId, tracePath,
                                                    route.info.services, route.info.timeoutSeconds * 1000L,
                                                    route.info.tracing);
        // load HTTP body
        if (POST.equals(method) || PUT.equals(method) || PATCH.equals(method)) {
            final AtomicBoolean inputComplete = new AtomicBoolean(false);
            final ByteArrayOutputStream requestBody = new ByteArrayOutputStream();
            String contentType = resolver.getContentType(request.getHeader(CONTENT_TYPE));
            if (contentType == null) {
                contentType = "?";
            }
            if (contentType.startsWith(MULTIPART_FORM_DATA) && POST.equals(method) && route.info.upload) {
                final StreamHolder stream = new StreamHolder(route.info.timeoutSeconds);
                request.uploadHandler(upload -> {
                    req.setFileName(upload.filename());
                    final AtomicInteger total = new AtomicInteger();
                    upload.handler(block -> {
                        int len = block.length();
                        if (len > 0) {
                            total.addAndGet(len);
                            pipeHttpInputToStream(stream.getPublisher(), block, len);
                        }
                    }).endHandler(end -> {
                        int size = total.get();
                        req.setContentLength(size);
                        if (size > 0) {
                            req.setStreamRoute(stream.getInputStreamId());
                            stream.close();
                        }
                        sendRequestToService(request, requestEvent.setHttpRequest(req));
                    });
                });
                request.resume();

            } else if (contentType.startsWith(APPLICATION_JSON)) {
                request.bodyHandler(block -> {
                    byte[] b = block.getBytes(0, block.length());
                    requestBody.write(b, 0, b.length);
                    if (inputComplete.get()) {
                        String text = util.getUTF(requestBody.toByteArray());
                        String trimmed = text.trim();
                        try {
                            if (trimmed.isEmpty()) {
                                req.setBody(new HashMap<>());
                            } else if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
                                req.setBody(SimpleMapper.getInstance().getMapper().readValue(text, Map.class));
                            } else if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
                                req.setBody(SimpleMapper.getInstance().getMapper().readValue(text, List.class));
                            } else {
                                req.setBody(text);
                            }
                        } catch(Exception e) {
                            req.setBody(text);
                        }
                        sendRequestToService(request, requestEvent.setHttpRequest(req));
                    }
                }).endHandler(done -> inputComplete.set(true));
            } else if (contentType.startsWith(APPLICATION_XML)) {
                boolean rawXml = "true".equals(request.getHeader(X_RAW_XML));
                request.bodyHandler(block -> {
                    byte[] b = block.getBytes(0, block.length());
                    requestBody.write(b, 0, b.length);
                    if (inputComplete.get()) {
                        String text = util.getUTF(requestBody.toByteArray());
                        if (rawXml) {
                            req.setBody(text);
                        } else {
                            try {
                                req.setBody(text.isEmpty()? new HashMap<>() : xmlReader.parse(text));
                            } catch (Exception e) {
                                req.setBody(text);
                            }
                        }
                        sendRequestToService(request, requestEvent.setHttpRequest(req));
                    }
                }).endHandler(done -> inputComplete.set(true));
            } else if (APPLICATION_FORM_URLENCODED.equals(contentType) ||
                    contentType.startsWith(TEXT_HTML) || contentType.startsWith(TEXT_PLAIN)) {
                final boolean urlEncodeParameters = APPLICATION_FORM_URLENCODED.equals(contentType);
                request.bodyHandler(block -> {
                    byte[] b = block.getBytes(0, block.length());
                    requestBody.write(b, 0, b.length);
                    if (inputComplete.get()) {
                        String text = util.getUTF(requestBody.toByteArray());
                        if (urlEncodeParameters) {
                            Map<String, String> kv = httpUtil.decodeQueryString(text);
                            for (Map.Entry<String, String> entry: kv.entrySet()) {
                                req.setQueryParameter(entry.getKey(), entry.getValue());
                            }
                        } else {
                            req.setBody(text);
                        }
                        sendRequestToService(request, requestEvent.setHttpRequest(req));
                    }
                }).endHandler(done -> inputComplete.set(true));
            } else {
                /*
                 * Input is not JSON, XML or TEXT.
                 *
                 * If content-length is not given, the payload size is variable.
                 * The "x-small-payload-as-bytes" header allows the system to
                 * render variable size payload as a fix length content.
                 */
                int contentLen = util.str2int(request.getHeader(CONTENT_LEN));
                boolean noStream = "true".equals(request.getHeader(X_NO_STREAM));
                if (noStream || contentLen > 0) {
                    request.bodyHandler(block -> {
                        byte[] b = block.getBytes(0, block.length());
                        requestBody.write(b, 0, b.length);
                        if (inputComplete.get()) {
                            req.setBody(requestBody.toByteArray());
                            sendRequestToService(request, requestEvent.setHttpRequest(req));
                        }
                    }).endHandler(done -> inputComplete.set(true));
                } else {
                    final AtomicInteger total = new AtomicInteger();
                    final StreamHolder stream = new StreamHolder(route.info.timeoutSeconds);
                    request.bodyHandler(block -> {
                        int len = block.length();
                        if (len > 0) {
                            total.addAndGet(len);
                            pipeHttpInputToStream(stream.getPublisher(), block, len);
                        }
                        if (inputComplete.get()) {
                            int size = total.get();
                            req.setContentLength(size);
                            if (size > 0) {
                                req.setStreamRoute(stream.getInputStreamId())
                                    .setHeader(X_TTL, String.valueOf(stream.getPublisher().getTimeToLive()));
                                stream.close();
                            }
                            sendRequestToService(request, requestEvent.setHttpRequest(req));
                        }
                    }).endHandler(end -> inputComplete.set(true));
                }
            }
        } else {
            sendRequestToService(request, requestEvent.setHttpRequest(req));
        }
    }

    public void sendRequestToService(HttpServerRequest request, HttpRequestEvent requestEvent) {
        SimpleHttpUtility httpUtil = SimpleHttpUtility.getInstance();
        EventEmitter po = EventEmitter.getInstance();
        if (requestEvent.authService != null) {
            try {
                po.send(AUTH_HANDLER, requestEvent.toMap());
            } catch (IOException e) {
                httpUtil.sendError(requestEvent.requestId, request,400, e.getMessage());
            }
        } else {
            EventEnvelope event = new EventEnvelope();
            event.setTo(requestEvent.primary).setFrom(HTTP_REQUEST)
                    .setCorrelationId(requestEvent.requestId).setBody(requestEvent.httpRequest)
                    .setReplyTo(AsyncHttpClient.ASYNC_HTTP_RESPONSE + "@" + Platform.getInstance().getOrigin());
            // enable distributed tracing if needed
            if (requestEvent.tracing) {
                event.setTrace(requestEvent.traceId, requestEvent.tracePath);
            }
            try {
                po.send(event);
                // copying to secondary services if any
                if (requestEvent.services.size() > 1) {
                    for (String secondary : requestEvent.services) {
                        if (!secondary.equals(requestEvent.primary)) {
                            EventEnvelope copy = new EventEnvelope().setTo(secondary).setFrom(HTTP_REQUEST)
                                                        .setBody(requestEvent.httpRequest);
                            if (requestEvent.tracing) {
                                copy.setTrace(requestEvent.traceId, requestEvent.tracePath);
                            }
                            sendToSecondaryTarget(copy);
                        }
                    }
                }
            } catch (IOException e) {
                httpUtil.sendError(requestEvent.requestId, request, 400, e.getMessage());
            }
        }
    }

    private void sendToSecondaryTarget(EventEnvelope event) {
        try {
            EventEmitter.getInstance().send(event);
        } catch (Exception e) {
            log.warn("Unable to copy event to {} - {}", event.getTo(), e.getMessage());
        }
    }

    private void pipeHttpInputToStream(EventPublisher publisher, Buffer block, int len) {
        if (block != null && len > 0) {
            try {
                byte[] data = block.getBytes(0, len);
                if (data.length > BUFFER_SIZE) {
                    int bytesRead = 0;
                    byte[] buffer = new byte[BUFFER_SIZE];
                    ByteArrayInputStream in = new ByteArrayInputStream(data);
                    while (bytesRead < data.length) {
                        int n = in.read(buffer);
                        bytesRead += n;
                        publisher.publish(buffer, 0, n);
                    }
                } else {
                    publisher.publish(data);
                }
            } catch (IOException e) {
                log.error("Unexpected error while reading HTTP input stream", e);
            }
        }
    }

    /**
     * Get X-Trace-Id from HTTP request headers if any.
     * Otherwise, generate a unique ID.
     *
     * @param request HTTP
     * @return traceLabel and traceId
     */
    private List<String> getTraceId(HttpServerRequest request) {
        List<String> result = new ArrayList<>();
        for (String label: traceIdLabels) {
            String id = request.getHeader(label);
            if (id != null) {
                result.add(label);
                result.add(id);
            }
        }
        if (result.isEmpty()) {
            result.add(getDefaultTraceIdLabel());
            result.add(Utility.getInstance().getUuid());
        }
        return result;
    }

    private String getHeaderCase(String header) {
        StringBuilder sb = new StringBuilder();
        List<String> parts = Utility.getInstance().split(header, "-");
        for (String p: parts) {
            sb.append(p.substring(0, 1).toUpperCase());
            if (p.length() > 1) {
                sb.append(p.substring(1).toLowerCase());
            }
            sb.append('-');
        }
        return sb.isEmpty() ? null : sb.substring(0, sb.length()-1);
    }
}

/**
 * This is reserved for system use.
 * DO NOT use this directly in your application code.
 */
@ZeroTracing
@EventInterceptor
class HttpAuth implements LambdaFunction {
    private static final Logger log = LoggerFactory.getLogger(HttpAuth.class);
    private static final String HTTP_REQUEST = "http.request";

    @Override
    public Object handleEvent(Map<String, String> headers, Object input, int instance) throws IOException {
        if (input instanceof EventEnvelope incomingEvent) {
            EventEmitter po = EventEmitter.getInstance();
            HttpRequestEvent evt = new HttpRequestEvent(incomingEvent.getBody());
            if (evt.authService != null && evt.requestId != null &&
                    evt.httpRequest != null && !evt.httpRequest.isEmpty()) {
                AsyncHttpRequest req = new AsyncHttpRequest(evt.httpRequest);
                EventEnvelope authRequest = new EventEnvelope();
                // the AsyncHttpRequest is sent as a map
                authRequest.setTo(evt.authService).setBody(evt.httpRequest);
                // distributed tracing required?
                if (evt.tracing) {
                    authRequest.setFrom(HTTP_REQUEST);
                    authRequest.setTrace(evt.traceId, evt.tracePath);
                }
                po.asyncRequest(authRequest, evt.timeout)
                        .onSuccess(response -> {
                            if (Boolean.TRUE.equals(response.getBody())) {
                                /*
                                 * Upon successful authentication,
                                 * the authentication service may save session information as headers
                                 * (auth headers are converted to lower case for case insensitivity)
                                 */
                                Map<String, String> authResHeaders = response.getHeaders();
                                for (Map.Entry<String, String> entry : authResHeaders.entrySet()) {
                                    req.setSessionInfo(entry.getKey(), entry.getValue());
                                }
                                // forward request to target service(s)
                                EventEnvelope event = new EventEnvelope();
                                event.setTo(evt.primary).setBody(req)
                                        .setCorrelationId(evt.requestId)
                                        .setReplyTo(AsyncHttpClient.ASYNC_HTTP_RESPONSE + "@" + Platform.getInstance().getOrigin());
                                // enable distributed tracing if needed
                                if (evt.tracing) {
                                    event.setFrom(evt.authService);
                                    event.setTrace(evt.traceId, evt.tracePath);
                                }
                                try {
                                    po.send(event);
                                    // copying to secondary services if any
                                    if (evt.services.size() > 1) {
                                        for (String secondary : evt.services) {
                                            if (!secondary.equals(evt.primary)) {
                                                EventEnvelope copy = new EventEnvelope()
                                                        .setTo(secondary).setBody(evt.httpRequest);
                                                if (evt.tracing) {
                                                    copy.setFrom(HTTP_REQUEST);
                                                    copy.setTrace(evt.traceId, evt.tracePath);
                                                }
                                                sendToSecondaryTarget(copy);
                                            }
                                        }
                                    }
                                } catch (IOException e) {
                                    sendError(evt, 400, e.getMessage());
                                }
                            } else {
                                sendError(evt, 401, "Unauthorized");
                            }
                        })
                        .onFailure(e -> sendError(evt, 408, e.getMessage()));
            }
        }
        return null;
    }

    private void sendError(HttpRequestEvent evt, int status, String message) {
        EventEmitter po = EventEmitter.getInstance();
        EventEnvelope event = new EventEnvelope();
        Map<String, Object> result = new HashMap<>();
        result.put("status", status);
        result.put("message", message);
        result.put("type", "error");
        event.setTo(AsyncHttpClient.ASYNC_HTTP_RESPONSE).setCorrelationId(evt.requestId).setStatus(status).setBody(result);
        // enable distributed tracing if needed
        if (evt.tracing) {
            event.setFrom(evt.authService);
            event.setTrace(evt.traceId, evt.tracePath);
        }
        try {
            po.send(event);
        } catch (IOException e) {
            log.error("Unable to send error to {} - {}", AsyncHttpClient.ASYNC_HTTP_RESPONSE, e.getMessage());
        }
    }

    private void sendToSecondaryTarget(EventEnvelope event) {
        try {
            EventEmitter.getInstance().send(event);
        } catch (Exception e) {
            log.warn("Unable to copy event to {} - {}", event.getTo(), e.getMessage());
        }
    }
}

