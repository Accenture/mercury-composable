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

package org.platformlambda.core.models;

import io.netty.handler.codec.http.cookie.ServerCookieEncoder;
import org.platformlambda.core.serializers.PayloadMapper;
import org.platformlambda.core.serializers.SimpleMapper;
import org.platformlambda.core.util.Utility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;

public class AsyncHttpRequest {
    private static final Logger log = LoggerFactory.getLogger(AsyncHttpRequest.class);
    private static final String HTTP_HEADERS = "headers";
    private static final String HTTP_METHOD = "method";
    private static final String IP_ADDRESS = "ip";
    private static final String HTTP_SESSION = "session";
    private static final String PARAMETERS = "parameters";
    private static final String HTTP_PROTOCOL = "http://";
    private static final String HTTPS_PROTOCOL = "https://";
    private static final String HTTP_SECURE = "https";
    private static final String QUERY = "query";
    private static final String PATH = "path";
    private static final String HTTP_COOKIES = "cookies";
    private static final String URL_LABEL = "url";
    private static final String HTTP_BODY = "body";
    private static final String FILE_UPLOAD = "upload";
    private static final String FILE_NAME = "filename";
    private static final String CONTENT_LENGTH = "size";
    private static final String TRUST_ALL_CERT = "trust_all_cert";
    private static final String TARGET_HOST = "host";
    private static final String FILE = "file";
    private static final String X_STREAM_ID = "x-stream-id";
    private static final String X_TTL = "x-ttl";
    private String method;
    private String queryString;
    private String url;
    private String ip;
    private String upload;
    private Map<String, String> headers = new HashMap<>();
    private Map<String, Object> queryParams = new HashMap<>();
    private Map<String, String> pathParams = new HashMap<>();
    private Map<String, String> cookies = new HashMap<>();
    private Map<String, String> session = new HashMap<>();
    private Object body;
    private String fileName;
    private String targetHost;
    private boolean trustAllCert = false;
    private boolean https = false;
    private int contentLength = -1;

    public AsyncHttpRequest() { }

    public AsyncHttpRequest(Object input) {
        fromMap(input);
    }

    public String getMethod() {
        return method;
    }

    public AsyncHttpRequest setMethod(String method) {
        if (method != null) {
            this.method = method.toUpperCase();
        }
        return this;
    }

    public String getUrl() {
        return url == null? "/" : url;
    }

    public AsyncHttpRequest setUrl(String url) {
        if (url != null) {
            this.url = url;
        }
        return this;
    }

    public String getRemoteIp() {
        return ip;
    }

    public AsyncHttpRequest setRemoteIp(String ip) {
        if (ip != null) {
            this.ip = ip;
        }
        return this;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public String getHeader(String key) {
        return caseInsensitiveGet(headers, key);
    }

    public AsyncHttpRequest removeHeader(String key) {
        caseInsensitiveDelete(headers, key);
        return this;
    }

    public AsyncHttpRequest setHeader(String key, String value) {
        // filter out CR and LF
        var v = value == null? "" : value.replace("\r", "").replace("\n", " ");
        headers.put(key, v);
        return this;
    }

    /**
     * Restore body to PoJo if class type information is available
     *
     * @return original body
     */
    public Object getBody() {
        return body;
    }

    /**
     * Convert body to a specific class
     * <p>
     * This would result in casting exception if using incompatible target class
     *
     * @param toValueType target class
     * @param <T> class type
     * @return target class
     */
    public <T> T getBody(Class<T> toValueType) {
        return SimpleMapper.getInstance().getMapper().readValue(body, toValueType);
    }

    /**
     * Convert body to a parameterizedClass
     *
     * @param toValueType target class
     * @param parameterClass parameter class(es)
     * @param <T> class type
     * @return target class with parameter class(es)
     */
    public <T> T getBody(Class<T> toValueType, Class<?>... parameterClass) {
        if (parameterClass.length == 0) {
            throw new IllegalArgumentException("Missing parameter class");
        }
        return SimpleMapper.getInstance().getMapper().restoreGeneric(body, toValueType, parameterClass);
    }

    /**
     * Set request body
     *
     * @param body will be converted to map if it is a PoJo
     * @return this
     */
    public AsyncHttpRequest setBody(Object body) {
        if (body == null || body instanceof Map || body instanceof List ||
                PayloadMapper.getInstance().isPrimitive(body)) {
            this.body = body;
        } else if (body instanceof Date) {
            this.body = Utility.getInstance().date2str((Date) body);
        } else {
            this.body = SimpleMapper.getInstance().getMapper().readValue(body, Map.class);
        }
        return this;
    }

    public String getStreamRoute() {
        return getHeader(X_STREAM_ID);
    }

    public AsyncHttpRequest setStreamRoute(String streamRoute) {
        if (streamRoute != null) {
            setHeader(X_STREAM_ID, streamRoute);
        }
        return this;
    }

    public boolean isStream() {
        return getStreamRoute() != null;
    }

    public String getFileName() {
        return fileName;
    }

    public AsyncHttpRequest setFileName(String fileName) {
        if (fileName != null) {
            this.fileName = fileName;
        }
        return this;
    }

    public boolean isFile() {
        return fileName != null;
    }

    public int getTimeoutSeconds() {
        String timeout = getHeader(X_TTL);
        if (timeout == null) {
            return -1;
        }
        return Math.max(1, Utility.getInstance().str2int(timeout)) / 1000;
    }

    public AsyncHttpRequest setTimeoutSeconds(int timeoutSeconds) {
        setHeader(X_TTL, String.valueOf(Math.max(1, timeoutSeconds) * 1000));
        return this;
    }

    public int getContentLength() {
        return Math.max(0, contentLength);
    }

    public AsyncHttpRequest setContentLength(int contentLength) {
        this.contentLength = Math.max(0, contentLength);
        return this;
    }

    public Map<String, String> getSessionInfo() {
        return session;
    }

    public String getSessionInfo(String key) {
        return caseInsensitiveGet(session, key);
    }

    public AsyncHttpRequest setSessionInfo(String key, String value) {
        // filter out CR and LF
        var v = value == null? "" : value.replace("\r", "").replace("\n", " ");
        session.put(key, v);
        return this;
    }

    public AsyncHttpRequest removeSessionInfo(String key) {
        caseInsensitiveDelete(session, key);
        return this;
    }

    public Map<String, String> getCookies() {
        return cookies;
    }

    public String getCookie(String key) {
        return caseInsensitiveGet(cookies, key);
    }

    public AsyncHttpRequest setCookie(String key, String value) {
        // avoid CR/LF attack
        try {
            ServerCookieEncoder.STRICT.encode(key, value);
            cookies.put(key, value);
        } catch(Exception e) {
            // removing trailing LF if any
            log.warn("Invalid cookie ignored ({}) - {}", key, e.getMessage().trim());
        }
        return this;
    }

    public AsyncHttpRequest removeCookie(String key) {
        caseInsensitiveDelete(cookies, key);
        return this;
    }

    public Map<String, String> getPathParameters() {
        return pathParams;
    }

    public String getPathParameter(String key) {
        return caseInsensitiveGet(pathParams, key);
    }

    public AsyncHttpRequest setPathParameter(String key, String value) {
        // filter out CR and LF
        var v = value == null? "" : value.replace("\r", "").replace("\n", " ");
        pathParams.put(key, v);
        return this;
    }

    public AsyncHttpRequest removePathParameter(String key) {
        caseInsensitiveDelete(pathParams, key);
        return this;
    }

    public String getQueryString() {
        return queryString;
    }

    public AsyncHttpRequest setQueryString(String queryString) {
        if (queryString != null) {
            final var value = queryString.trim();
            this.queryString = value.isEmpty()? null : value;
        } else {
            this.queryString = null;
        }
        return this;
    }

    public boolean isSecure() {
        return https;
    }

    public AsyncHttpRequest setSecure(boolean https) {
        this.https = https;
        return this;
    }

    public String getUploadTag() {
        return upload == null? FILE : upload;
    }

    public AsyncHttpRequest setUploadTag(String tag) {
        if (tag != null) {
            final var value = tag.trim();
            this.upload = value.isEmpty()? null : value;
        } else {
            this.upload = null;
        }
        return this;
    }

    public Map<String, Object> getQueryParameters() {
        return queryParams;
    }

    public String getTargetHost() {
        return targetHost;
    }

    public AsyncHttpRequest setTargetHost(String host) {
        if (host != null && (host.startsWith(HTTP_PROTOCOL) || host.startsWith(HTTPS_PROTOCOL))) {
            try {
                final var u = new URI(host);
                if (!u.getPath().isEmpty()) {
                    throw new IllegalArgumentException("Invalid host - Must not contain path");
                }
                if (u.getQuery() != null) {
                    throw new IllegalArgumentException("Invalid host - Must not contain query");
                }
            } catch (URISyntaxException e) {
                throw new IllegalArgumentException("Invalid host - "+e.getMessage());
            }
            this.targetHost = host;
            return this;
        } else {
            throw new IllegalArgumentException("Invalid host - must starts with "+HTTP_PROTOCOL+" or "+HTTPS_PROTOCOL);
        }
    }

    public boolean isTrustAllCert() {
        return trustAllCert;
    }

    public AsyncHttpRequest setTrustAllCert(boolean trustAllCert) {
        this.trustAllCert = trustAllCert;
        return this;
    }

    /**
     * Use this when you know it is a single value item.
     * @param key of the parameter
     * @return value of the parameter
     */
    @SuppressWarnings("unchecked")
    public String getQueryParameter(String key) {
        final var k = findActualQueryKey(key);
        if (k != null) {
            final var value = queryParams.get(k);
            if (value instanceof String strValue) {
                return strValue;
            } else if (value instanceof List) {
                final var params = (List<String>) value;
                if (!params.isEmpty()) {
                    return params.getFirst();
                }
            }
        }
        return null;
    }

    /**
     * Use this when you know it is a multi-value item.
     * @param key of the parameter
     * @return values of the parameter
     */
    @SuppressWarnings("unchecked")
    public List<String> getQueryParameters(String key) {
        final var k = findActualQueryKey(key);
        if (k != null) {
            final var values = queryParams.get(k);
            if (values instanceof String strValue) {
                return Collections.singletonList(strValue);
            } else if (values instanceof List) {
                return (List<String>) values;
            }
        }
        return Collections.emptyList();
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public AsyncHttpRequest setQueryParameter(String key, Object value) {
        if (key != null) {
            switch (value) {
                case String ignoredString -> this.queryParams.put(key, value);
                case List valueList -> {
                    final var params = new ArrayList<String>();
                    final var list = (List<Object>) valueList;
                    for (Object o : list) {
                        if (o != null) {
                            params.add(o instanceof String text? text : String.valueOf(o));
                        }
                    }
                    this.queryParams.put(key, params);
                }
                case null -> this.queryParams.put(key, "");
                default -> this.queryParams.put(key, String.valueOf(value));
            }
        }
        return this;
    }

    public AsyncHttpRequest removeQueryParameter(String key) {
        final var k = findActualQueryKey(key);
        if (k != null) {
            this.queryParams.remove(k);
        }
        return this;
    }

    /**
     * The set methods and toMap method are used for manually construct an HTTP request object
     * that are typically used for Unit Test or for a service to emulate a REST browser.
     * <p>
     * In normal case, the AsyncHttpRequest map is generated by the rest-automation application.
     *
     * @return async http request object as a map
     */
    public Map<String, Object> toMap() {
        Map<String, Object> result = new HashMap<>();
        if (!headers.isEmpty()) {
            result.put(HTTP_HEADERS, headers);
        }
        if (!cookies.isEmpty()) {
            result.put(HTTP_COOKIES, cookies);
        }
        if (!session.isEmpty()) {
            result.put(HTTP_SESSION, session);
        }
        if (method != null) {
            result.put(HTTP_METHOD, method);
        }
        if (ip != null) {
            result.put(IP_ADDRESS, ip);
        }
        if (url != null) {
            result.put(URL_LABEL, url);
        }
        if (fileName != null) {
            result.put(FILE_NAME, fileName);
        }
        if (contentLength != -1) {
            result.put(CONTENT_LENGTH, contentLength);
        }
        if (body != null) {
            result.put(HTTP_BODY, body);
        }
        if (queryString != null) {
            result.put(QUERY, queryString);
        }
        if (upload != null) {
            result.put(FILE_UPLOAD, upload);
        }
        if (!pathParams.isEmpty() || !queryParams.isEmpty()) {
            Map<String, Object> parameters = new HashMap<>();
            result.put(PARAMETERS, parameters);
            if (!pathParams.isEmpty()) {
                parameters.put(PATH, pathParams);
            }
            if (!queryParams.isEmpty()) {
                parameters.put(QUERY, queryParams);
            }
        }
        result.put(HTTP_SECURE, https);
        /*
         * Optional HTTP host name in the "relay" field
         *
         * This is used by the rest-automation "async.http.request" service
         * when forwarding HTTP request to a target HTTP endpoint.
         */
        if (targetHost != null) {
            result.put(TARGET_HOST, targetHost);
            result.put(TRUST_ALL_CERT, trustAllCert);
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private void fromMap(Object input) {
        if (input instanceof AsyncHttpRequest source) {
            this.headers = source.headers;
            this.cookies = source.cookies;
            this.session = source.session;
            this.method = source.method;
            this.ip = source.ip;
            this.url = source.url;
            this.fileName = source.fileName;
            this.contentLength = source.contentLength;
            this.body = source.body;
            this.queryString = source.queryString;
            this.https = source.https;
            this.targetHost = source.targetHost;
            this.trustAllCert = source.trustAllCert;
            this.upload = source.upload;
            this.pathParams = source.pathParams;
            this.queryParams = source.queryParams;
        }
        if (input instanceof Map) {
            final var map = (Map<String, Object>) input;
            if (map.containsKey(HTTP_HEADERS)) {
                headers = (Map<String, String>) map.get(HTTP_HEADERS);
            }
            if (map.containsKey(HTTP_COOKIES)) {
                cookies = (Map<String, String>) map.get(HTTP_COOKIES);
            }
            if (map.containsKey(HTTP_SESSION)) {
                session = (Map<String, String>) map.get(HTTP_SESSION);
            }
            if (map.containsKey(HTTP_METHOD)) {
                method = (String) map.get(HTTP_METHOD);
            }
            if (map.containsKey(IP_ADDRESS)) {
                ip = (String) map.get(IP_ADDRESS);
            }
            if (map.containsKey(URL_LABEL)) {
                url = (String) map.get(URL_LABEL);
            }
            if (map.containsKey(FILE_NAME)) {
                fileName = (String) map.get(FILE_NAME);
            }
            if (map.containsKey(CONTENT_LENGTH)) {
                contentLength = (int) map.get(CONTENT_LENGTH);
            }
            if (map.containsKey(HTTP_BODY)) {
                body = map.get(HTTP_BODY);
            }
            if (map.containsKey(QUERY)) {
                queryString = (String) map.get(QUERY);
            }
            if (map.containsKey(HTTP_SECURE)) {
                https = (boolean) map.get(HTTP_SECURE);
            }
            if (map.containsKey(TARGET_HOST)) {
                targetHost = (String) map.get(TARGET_HOST);
            }
            if (map.containsKey(TRUST_ALL_CERT)) {
                trustAllCert = (boolean) map.get(TRUST_ALL_CERT);
            }
            if (map.containsKey(FILE_UPLOAD)) {
                upload = (String) map.get(FILE_UPLOAD);
            }
            if (map.containsKey(PARAMETERS)) {
                Map<String, Object> parameters = (Map<String, Object>) map.get(PARAMETERS);
                if (parameters.containsKey(PATH)) {
                    pathParams = (Map<String, String>) parameters.get(PATH);
                }
                if (parameters.containsKey(QUERY)) {
                    queryParams = (Map<String, Object>) parameters.get(QUERY);
                }
            }
        }
    }

    private String findActualQueryKey(String key) {
        if (key != null) {
            for (var entry : queryParams.entrySet()) {
                if (entry.getKey().equalsIgnoreCase(key)) {
                    return entry.getKey();
                }
            }
        }
        return null;
    }

    private String caseInsensitiveGet(Map<String, String> map, String key) {
        if (key != null) {
            for (var entry : map.entrySet()) {
                if (entry.getKey().equalsIgnoreCase(key)) {
                    return entry.getValue();
                }
            }
        }
        return null;
    }

    private void caseInsensitiveDelete(Map<String, String> map, String key) {
        if (key != null) {
            String actualKey = null;
            for (var entry : map.entrySet()) {
                if (entry.getKey().equalsIgnoreCase(key)) {
                    actualKey = entry.getKey();
                    break;
                }
            }
            if (actualKey != null) {
                map.remove(actualKey);
            }
        }
    }
}
