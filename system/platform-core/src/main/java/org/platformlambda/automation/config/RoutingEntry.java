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

package org.platformlambda.automation.config;

import org.platformlambda.automation.http.AsyncHttpClient;
import org.platformlambda.automation.models.*;
import org.platformlambda.core.util.ConfigReader;
import org.platformlambda.core.util.MultiLevelMap;
import org.platformlambda.core.util.Utility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;

public class RoutingEntry {
    private static final Logger log = LoggerFactory.getLogger(RoutingEntry.class);
    private static final String SERVICE_URL = "service URL ";
    private static final String HTTP = "http://";
    private static final String HTTPS = "https://";
    private static final String REST = "rest";
    private static final String CORS = "cors";
    private static final String AUTHENTICATION = "authentication";
    private static final String UPLOAD = "upload";
    private static final String TRACING = "tracing";
    private static final String SERVICE = "service";
    private static final String FLOW = "flow";
    private static final String METHODS = "methods";
    private static final String URL_LABEL = "url";
    private static final String ID = "id";
    private static final String OPTIONS_METHOD = "OPTIONS";
    private static final String OPTIONS = "options";
    private static final String HEADERS = "headers";
    private static final String DEFAULT_VALUE = "default";
    private static final String TRUST_ALL_CERT = "trust_all_cert";
    private static final String URL_REWRITE = "url_rewrite";
    private static final String TIMEOUT = "timeout";
    private static final String REQUEST = "request";
    private static final String RESPONSE = "response";
    private static final String ADD = "add";
    private static final String DROP = "drop";
    private static final String KEEP = "keep";
    private static final String AUTHENTICATION_SERVICE_NAME = "authentication service name ";
    private static final String SKIP_INVALID_ENTRY = "Skipping invalid REST entry";
    private static final String ACCESS_CONTROL_PREFIX = "Access-Control-";
    private static final String[] VALID_METHODS = {"GET", "PUT", "POST", "DELETE", "HEAD", "PATCH", OPTIONS_METHOD};
    private static final List<String> METHOD_LIST = Arrays.asList(VALID_METHODS);
    private static final int FIVE_MINUTES = 5 * 60;
    private static final Map<String, RouteInfo> routes = new HashMap<>();
    private static final Map<String, Boolean> exactRoutes = new HashMap<>();
    // id -> maps for options and headers
    private static final Map<String, CorsInfo> corsConfig = new HashMap<>();
    // id -> add, drop, keep
    private static final Map<String, HeaderInfo> requestHeaderInfo = new HashMap<>();
    private static final Map<String, HeaderInfo> responseHeaderInfo = new HashMap<>();
    private static final List<String> urlPaths = new ArrayList<>();
    private SimpleHttpFilter requestFilter;
    private List<String> noCachePages;
    private static final RoutingEntry instance = new RoutingEntry();

    private RoutingEntry() {
        // singleton
    }

    public static RoutingEntry getInstance() {
        return instance;
    }

    public SimpleHttpFilter getRequestFilter() {
        return requestFilter;
    }

    public List<String> getNoCachePages() {
        return noCachePages;
    }

    public AssignedRoute getRouteInfo(String method, String url) {
        Utility util = Utility.getInstance();
        StringBuilder sb = new StringBuilder();
        List<String> urlParts = util.split(url, "/");
        for (String p: urlParts) {
            sb.append('/');
            sb.append(p);
        }
        // do case-insensitive matching for exact URL
        String normalizedUrl = sb.toString().toLowerCase();
        String key = method+":"+normalizedUrl;
        if (exactRoutes.containsKey(normalizedUrl)) {
            return new AssignedRoute(routes.get(key));
        } else {
            return getSimilarRoute(method, urlParts);
        }
    }

    private AssignedRoute getSimilarRoute(String method, List<String> urlParts) {
        AssignedRoute similar = null;
        for (String u: urlPaths) {
            AssignedRoute info = getMatchedRoute(urlParts, method, u);
            if (info != null) {
                if (similar == null) {
                    similar = info;
                }
                // Both URL path and method are correct
                if (routes.containsKey(method + ":" + u)) {
                    return info;
                }
            }
        }
        // similar path found but method does not match - reject with HTTP-405 'Method Not Allowed'
        return similar;
    }

    public HeaderInfo getRequestHeaderInfo(String id) {
        return requestHeaderInfo.get(id);
    }

    public HeaderInfo getResponseHeaderInfo(String id) {
        return responseHeaderInfo.get(id);
    }

    public CorsInfo getCorsInfo(String id) {
        return corsConfig.get(id);
    }

    private AssignedRoute getMatchedRoute(List<String> urlParts, String method, String configured) {
        // "configured" is a lower case URL in the routing entry
        String key = method+":"+configured;
        AssignedRoute result = new AssignedRoute(routes.get(key));
        Utility util = Utility.getInstance();
        List<String> segments = util.split(configured, "/");
        if (matchRoute(urlParts, segments, configured.endsWith("*"))) {
            addArguments(result, urlParts, segments);
            return result;
        }
        return null;
    }

    private void addArguments(AssignedRoute info, List<String> urlParts, List<String> configured) {
        for (int i=0; i < configured.size(); i++) {
            String configuredItem = configured.get(i);
            if (configuredItem.startsWith("{") && configuredItem.endsWith("}")) {
                info.setArgument(configuredItem.substring(1, configuredItem.length()-1), urlParts.get(i));
            }
        }
    }

    private boolean matchRoute(List<String> urlParts, List<String> segments, boolean wildcard) {
        // segment is lowercase parts of the configured URL
        if (wildcard) {
            if (segments.size() > urlParts.size()) {
                return false;
            }
        } else {
            if (segments.size() != urlParts.size()) {
                return false;
            }
        }
        for (int i=0; i < segments.size(); i++) {
            if (notMatched(segments.get(i), urlParts, i)) {
                return false;
            }
        }
        return true;
    }

    private boolean notMatched(String item, List<String> urlParts, int i) {
        if (item.startsWith("{") && item.endsWith("}")) {
            return false;
        }
        if ("*".equals(item)) {
            return false;
        }
        // case-insensitive comparison using lowercase
        String inputItem = urlParts.get(i).toLowerCase();
        if (item.endsWith("*")) {
            String prefix = item.substring(0, item.length()-1);
            if (inputItem.startsWith(prefix)) {
                return false;
            }
        }
        return !inputItem.equals(item);
    }

    private List<String> getNoCacheConfig(ConfigReader config) {
        Object noCache = config.get("static-content.no-cache-pages");
        if (noCache != null) {
            if (noCache instanceof List<?> items) {
                List<String> noCacheList = new ArrayList<>();
                for (int i = 0; i < items.size(); i++) {
                    noCacheList.add(config.getProperty("static-content.no-cache-pages[" + i + "]"));
                }
                if (invalidFilterParameters(noCacheList)) {
                    log.error("static-content.no-cache-pages ignored - invalid syntax {}", noCache);
                } else {
                    log.info("static-content.no-cache-pages loaded: {}", noCacheList);
                    return noCacheList;
                }
            } else {
                log.error("static-content.no-cache-pages ignored - please check syntax");
            }
        }
        return Collections.emptyList();
    }

    private SimpleHttpFilter getStaticContentFilter(ConfigReader config) {
        if (config.exists("static-content.filter")) {
            Object path = config.get("static-content.filter.path");
            String service = config.getProperty("static-content.filter.service");
            if (path instanceof List<?> pList && service != null && !service.isEmpty()) {
                if (!Utility.getInstance().validServiceName(service)) {
                    log.error("Static content filter ignored: '{} -> {}' - invalid service name", path, service);
                    return null;
                }
                List<String> pathList = new ArrayList<>();
                for (int i = 0; i < pList.size(); i++) {
                    pathList.add(config.getProperty("static-content.filter.path[" + i + "]"));
                }
                List<String> exclusionList = new ArrayList<>();
                if (invalidStaticContentFilter(config, exclusionList, pathList)) {
                    return null;
                }
                log.info("static-content.filter loaded: {} -> {}, exclusion {}", pathList, service, exclusionList);
                return new SimpleHttpFilter(pathList, exclusionList, service);
            } else {
                log.error("static-content.filter ignored - please check syntax");
            }
        }
        return null;
    }

    private boolean invalidStaticContentFilter(ConfigReader config, List<String> exclusionList, List<String> pathList) {
        Object exclusion = config.get("static-content.filter.exclusion");
        if (exclusion instanceof List<?> items) {
            for (int i = 0; i < items.size(); i++) {
                exclusionList.add(config.getProperty("static-content.filter.exclusion[" + i + "]"));
            }
        }
        if (pathList.isEmpty()) {
            log.error("static-content.filter.path is empty");
            return true;
        }
        if (invalidFilterParameters(pathList)) {
            log.error("static-content.filter.path ignored - invalid syntax {}", pathList);
            return true;
        }
        if (invalidFilterParameters(exclusionList)) {
            log.error("static-content.filter.exclusion ignored - invalid syntax {}", exclusion);
            return true;
        }
        return false;
    }

    private boolean invalidFilterParameters(List<String> items) {
        Utility util = Utility.getInstance();
        for (String item: items) {
            // allow either startsWith or endsWith
            if (item.startsWith("*") && item.endsWith("*")) {
                return true;
            }
            // accept only "*" as prefix or suffix
            if (item.contains("**")) {
                return true;
            }
            List<String> parts = util.split(item, "*");
            if (parts.size() != 1) {
                return true;
            }
        }
        return false;
    }

    public void load(ConfigReader config) {
        if (!config.exists(REST)) {
            log.warn("No user defined REST endpoints found");
        }
        this.requestFilter = getStaticContentFilter(config);
        this.noCachePages = getNoCacheConfig(config);
        if (config.exists(HEADERS)) {
            loadHeaderSection(config);
        }
        if (config.exists(CORS)) {
            loadCorsSection(config);
        }
        addDefaultEndpoints(config);
        sortEndpoints(config);
        loadRestSection(config);
    }

    private void loadHeaderSection(ConfigReader config) {
        Object headerList = config.get(HEADERS);
        boolean valid = false;
        if (headerList instanceof List<?> items && isListOfMap(items)) {
            loadHeaderTransform(config, items.size());
            valid = true;
        }

        if (!valid) {
            log.error("'headers' section must be a list of request and response entries");
        }
    }

    private void loadCorsSection(ConfigReader config) {
        Object corsList = config.get(CORS);
        boolean valid = false;
        if (corsList instanceof List<?> items && isListOfMap(items)) {
            loadCors(config, items.size());
            valid = true;
        }
        if (!valid) {
            log.error("'cors' section must be a list of Access-Control entries (id, options and headers)");
        }
    }

    private void loadRestSection(ConfigReader config) {
        Object rest = config.get(REST);
        boolean valid = false;
        if (rest instanceof List<?> items && isListOfMap(items)) {
            loadRest(config);
            valid = true;
        }
        if (!valid) {
            log.error("'rest' section must be a list of endpoint entries (url, service, methods, timeout...)");
        }
        List<String> exact = new ArrayList<>();
        for (String r: exactRoutes.keySet()) {
            if (!exact.contains(r)) {
                exact.add(r);
            }
        }
        printRestEntryStats(exact);
    }

    private void printRestEntryStats(List<String> exact) {
        Collections.sort(exact);
        if (!exact.isEmpty()) {
            var message = new HashMap<String, Object>();
            message.put("match", "exact");
            message.put("total", exact.size());
            message.put("path", exact);
            log.info("{}", message);
        }
        // sort URLs for easy parsing
        if (!routes.isEmpty()) {
            for (String r: routes.keySet()) {
                int colon = r.indexOf(':');
                if (colon > 0) {
                    String urlOnly = r.substring(colon+1);
                    if (!exactRoutes.containsKey(urlOnly) && !urlPaths.contains(urlOnly)) {
                        urlPaths.add(urlOnly);
                    }
                }
            }
        }
        Collections.sort(urlPaths);
        if (!urlPaths.isEmpty()) {
            var message = new HashMap<String, Object>();
            message.put("match", "parameters");
            message.put("total", urlPaths.size());
            message.put("path", urlPaths);
            log.info("{}", message);
        }
    }

    private boolean isListOfMap(List<?> items) {
        for (Object o: items) {
            if (!(o instanceof Map)) {
                return false;
            }
        }
        return true;
    }

    private void loadRest(ConfigReader config) {
        if (config.get(REST) instanceof List<?> items) {
            for (int i = 0; i < items.size(); i++) {
                Object services = config.get(REST + "[" + i + "]." + SERVICE);
                Object methods = config.get(REST + "[" + i + "]." + METHODS);
                String url = config.getProperty(REST + "[" + i + "]." + URL_LABEL);
                if (url != null && methods instanceof List &&
                        (services instanceof List || services instanceof String)) {
                    loadRestEntry(config, i, !url.contains("{") && !url.contains("}") && !url.contains("*"));
                } else {
                    log.error("{} - {}", SKIP_INVALID_ENTRY, config.get(REST + "[" + i + "]"));
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void addDefaultEndpoints(ConfigReader config) {
        ConfigReader defaultRest = new ConfigReader();
        defaultRest.load("/default-rest.yaml");
        Object defaultRestEntries = defaultRest.get(REST);
        List<Object> defaultRestList = (List<Object>) defaultRestEntries;
        int defaultTotal = defaultRestList.size();
        Map<String, Integer> essentials = new HashMap<>();
        List<String> configured = new ArrayList<>();
        for (int i=0; i < defaultTotal; i++) {
            String methods = defaultRest.getProperty(REST+"["+i+"]."+METHODS);
            String url = defaultRest.getProperty(REST+"["+i+"]."+URL_LABEL);
            essentials.put(url+" "+methods, i);
        }
        Object restEntries = config.get(REST, new ArrayList<>());
        List<Object> restList = (List<Object>) restEntries;
        int total = restList.size();
        for (int i=0; i < total; i++) {
            String methods = config.getProperty(REST+"["+i+"]."+METHODS);
            String url = config.getProperty(REST+"["+i+"]."+URL_LABEL);
            if (url != null && methods != null) {
                configured.add(url+" "+methods);
            }
        }
        // find out if there are missing default entries in the configured list
        List<String> missing = new ArrayList<>();
        for (String entry: essentials.keySet()) {
            if (!configured.contains(entry)) {
                missing.add(entry);
            }
        }
        if (!missing.isEmpty()) {
            MultiLevelMap map = new MultiLevelMap(config.getMap());
            for (String entry : missing) {
                int idx = essentials.get(entry);
                map.setElement(REST + "[" + total + "]", defaultRest.get(REST + "[" + idx + "]"));
                total++;
            }
            config.reload(map.getMap());
        }
    }

    @SuppressWarnings("unchecked")
    private void sortEndpoints(ConfigReader config) {
        Map<String, Object> endpoints = new HashMap<>();
        Object restEntries = config.get(REST);
        List<Object> restList = (List<Object>) restEntries;
        int total = restList.size();
        for (int i=0; i < total; i++) {
            Object entry = config.get(REST+"["+i+"]");
            String methods = config.getProperty(REST+"["+i+"]."+METHODS);
            String url = config.getProperty(REST+"["+i+"]."+URL_LABEL);
            if (url != null && methods != null) {
                endpoints.put(url+" "+methods, entry);
            }
        }
        int n = 0;
        MultiLevelMap map = new MultiLevelMap(config.getMap());
        List<String> keys = new ArrayList<>(endpoints.keySet());
        Collections.sort(keys);
        for (String k : keys) {
            map.setElement(REST + "[" + n + "]", endpoints.get(k));
            n++;
        }
        config.reload(map.getMap());
    }

    @SuppressWarnings("unchecked")
    private void loadRestEntry(ConfigReader config, int idx, boolean exact) {
        RouteInfo info = new RouteInfo();
        Object services = config.get(REST+"["+idx+"]."+SERVICE);
        List<String> methods = (List<String>) config.get(REST+"["+idx+"]."+METHODS);
        String url = config.getProperty(REST+"["+idx+"]."+URL_LABEL).toLowerCase();
        String flowId = config.getProperty(REST+"["+idx+"]."+FLOW);
        if (flowId != null && !flowId.isEmpty()) {
            info.flowId = flowId;
        }
        try {
            info.services = validateServiceList(services);
        } catch (IllegalArgumentException e) {
            log.error("Skipping entry {} - {}", config.get(REST+"["+idx+"]"), e.getMessage());
            return;
        }
        info.primary = info.services.getFirst();
        String upload = config.getProperty(REST+"["+idx+"]."+UPLOAD);
        if (upload != null) {
            info.upload = "true".equalsIgnoreCase(upload);
        }
        try {
            validateAuthService(config, info, idx);
            validateCorsAndHeaders(config, info, idx);
        } catch (IllegalArgumentException e) {
            log.error("Skip invalid entry - {}", e.getMessage());
            return;
        }
        String tracing = config.getProperty(REST+"["+idx+"]."+TRACING);
        if ("true".equalsIgnoreCase(tracing)) {
            info.tracing = true;
        }
        // drop query string when parsing URL
        if (url.contains("?")) {
            url = url.substring(0, url.indexOf('?'));
        }
        info.timeoutSeconds = getDurationInSeconds(config.getProperty(REST+"["+idx+"]."+TIMEOUT));
        if (info.primary.startsWith(HTTP) || info.primary.startsWith(HTTPS)) {
            try {
                validateHttpTarget(config, info, idx);
            } catch (IllegalArgumentException e) {
                log.error("Skip invalid entry with HTTP target - {}", e.getMessage());
                return;
            }
        } else {
            String trustAll = config.getProperty(REST+"["+idx+"]."+TRUST_ALL_CERT);
            if (trustAll != null) {
                log.warn("{} parameter for {} is not relevant for regular service", TRUST_ALL_CERT, info.primary);
            }
        }
        // remove OPTIONS method
        methods.remove(OPTIONS_METHOD);
        validateMethods(url, methods, config, info, idx, exact);
    }

    private void validateMethods(String url, List<String> methods, ConfigReader config, RouteInfo info, int i, boolean exact) {
        if (validMethods(methods)) {
            List<String> allMethods = new ArrayList<>(new HashSet<>(methods));
            Collections.sort(allMethods);
            info.methods = allMethods;
            if (exact) {
                exactRoutes.put(url, true);
            }
            String nUrl = getUrl(url, exact);
            if (nUrl == null) {
                log.error("Skip entry with invalid URL {}", config.get(REST+"["+i+"]"));
            } else {
                info.url = nUrl;
                // ensure OPTIONS method is supported
                allMethods.add(OPTIONS_METHOD);
                printRestEntry(allMethods, nUrl, info);
            }
        } else {
            log.error("Skip entry with invalid method {}", config.get(REST+"["+i+"]"));
        }
    }

    private void validateCorsAndHeaders(ConfigReader config, RouteInfo info, int i) {
        String corsId = config.getProperty(REST+"["+i+"]."+CORS);
        if (corsId != null) {
            if (corsConfig.containsKey(corsId)) {
                info.corsId = corsId;
            } else {
                throw new IllegalArgumentException("cors ID "+corsId+" is not found, "+config.get(REST+"["+i+"]"));
            }
        }
        String headerId = config.getProperty(REST+"["+i+"]."+HEADERS);
        if (invalidHeaderId(headerId, info)) {
            throw new IllegalArgumentException("headers ID "+headerId+" is not found, "+config.get(REST+"["+i+"]"));
        }
    }

    private boolean invalidHeaderId(String headerId, RouteInfo info) {
        if (headerId != null) {
            boolean foundTransform = false;
            if (requestHeaderInfo.containsKey(headerId)) {
                info.requestTransformId = headerId;
                foundTransform = true;
            }
            if (responseHeaderInfo.containsKey(headerId)) {
                info.responseTransformId = headerId;
                foundTransform = true;
            }
            return !foundTransform;
        }
        return false;
    }

    private void printRestEntry(List<String> allMethods, String nUrl, RouteInfo info) {
        for (String m: allMethods) {
            String key = m+":"+nUrl;
            routes.put(key, info);
            String flowHint = info.flowId == null? "" : ", flow=" + info.flowId;
            if (!OPTIONS_METHOD.equals(m)) {
                if (info.defaultAuthService != null) {
                    log.info("{} {} -> {} -> {}, timeout={}s, tracing={}{}",
                            m, nUrl, info.defaultAuthService, info.services,
                            info.timeoutSeconds, info.tracing, flowHint);
                } else {
                    log.info("{} {} -> {}, timeout={}s, tracing={}{}",
                            m, nUrl, info.services, info.timeoutSeconds, info.tracing, flowHint);
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void validateHttpTarget(ConfigReader config, RouteInfo info, int i) {
        Object rewrite = config.get(REST+"["+i+"]."+URL_REWRITE);
        // URL rewrite
        if (rewrite instanceof List) {
            List<String> urlRewrite = (List<String>) rewrite;
            if (urlRewrite.size() == 2) {
                info.urlRewrite = urlRewrite;
            } else {
                throw new IllegalArgumentException(URL_REWRITE + " - " + urlRewrite +
                                                    ". It should contain a list of 2 prefixes");
            }
        } else {
            var clsName = rewrite == null? "null" : rewrite.getClass().getSimpleName();
            throw new IllegalArgumentException(URL_REWRITE + " - " + rewrite +
                                                ", expected: List<String>, actual: " + clsName);
        }
        try {
            URI u = new URI(info.primary);
            if (!u.getPath().isEmpty()) {
                throw new IllegalArgumentException(SERVICE_URL + info.primary + " - Must not contain path");
            }
            if (u.getQuery() != null) {
                throw new IllegalArgumentException(SERVICE_URL + info.primary + " - Must not contain query");
            }
            String trustAll = config.getProperty(REST+"["+i+"]."+TRUST_ALL_CERT);
            if (info.primary.startsWith(HTTPS) && "true".equalsIgnoreCase(trustAll)) {
                info.trustAllCert = true;
                log.warn("Be careful - {}=true for {}", TRUST_ALL_CERT, info.primary);
            }
            if (info.primary.startsWith(HTTP) && trustAll != null) {
                log.warn("{}=true for {} is not relevant - Do you meant https?", TRUST_ALL_CERT, info.primary);
            }
            // set primary to ASYNC_HTTP_REQUEST
            info.host = info.primary;
            info.primary = AsyncHttpClient.ASYNC_HTTP_REQUEST;
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException(SERVICE_URL + info.primary + " - " + e.getMessage());
        }
    }

    private void validateAuthService(ConfigReader config, RouteInfo info, int i) {
        Utility util = Utility.getInstance();
        Object authConfig = config.get(REST+"["+i+"]."+ AUTHENTICATION);
        // authentication: "v1.api.auth"
        if (authConfig instanceof String) {
            String auth = authConfig.toString().trim();
            if (util.validServiceName(auth)) {
                info.defaultAuthService = auth;
            } else {
                throw new IllegalArgumentException(AUTHENTICATION_SERVICE_NAME + config.get(REST+"["+i+"]"));
            }
        }
        /*
            authentication:
            - "x-app-name: demo : v1.demo.auth"
            - "authorization: v1.basic.auth"
            - "default: v1.api.auth"
         */
        if (authConfig instanceof List<?> items) {
            for (Object o : items) {
                String authEntry = String.valueOf(o);
                List<String> parts = util.split(authEntry, ": ");
                if (parts.size() == 2) {
                    validate2partAuthList(config, info, parts, i);
                } else if (parts.size() == 3) {
                    validate3partAuthList(config, info, parts, i);
                } else {
                    throw new IllegalArgumentException(AUTHENTICATION_SERVICE_NAME + config.get(REST+"["+i+"]"));
                }
            }
            if (info.defaultAuthService == null) {
                throw new IllegalArgumentException("missing default authentication: " + config.get(REST+"["+i+"]"));
            }
        }
    }

    private void validate2partAuthList(ConfigReader config, RouteInfo info, List<String> parts, int i) {
        Utility util = Utility.getInstance();
        String authHeader = parts.get(0);
        String authService = parts.get(1);
        if (util.validServiceName(authService)) {
            if (DEFAULT_VALUE.equals(authHeader)) {
                info.defaultAuthService = authService;
            } else {
                info.setAuthService(authHeader, "*", authService);
            }
        } else {
            throw new IllegalArgumentException(config.getProperty(REST+"["+i+"]"));
        }
    }

    private void validate3partAuthList(ConfigReader config, RouteInfo info, List<String> parts, int i) {
        Utility util = Utility.getInstance();
        String authHeader = parts.get(0);
        String authValue = parts.get(1);
        String authService = parts.get(2);
        if (util.validServiceName(authService)) {
            info.setAuthService(authHeader, authValue, authService);
        } else {
            throw new IllegalArgumentException(config.getProperty(REST+"["+i+"]"));
        }
    }

    @SuppressWarnings("unchecked")
    private List<String> validateServiceList(Object svcList) {
        List<String> list = svcList instanceof String svc? Collections.singletonList(svc) : (List<String>) svcList;
        List<String> result = new ArrayList<>();
        for (String item: list) {
            String service = item.trim().toLowerCase();
            if (!service.isEmpty() && !result.contains(service)) {
                result.add(service);
            }
        }
        if (result.isEmpty()) {
            throw new IllegalArgumentException("Missing service");
        }
        String firstItem = result.getFirst();
        if (firstItem.startsWith(HTTP) || firstItem.startsWith(HTTPS)) {
            if (result.size() > 1) {
                throw new IllegalArgumentException("HTTP relay supports a single URL only");
            }
            return result;
        }
        validateMultipleServices(result);
        return result;
    }

    private void validateMultipleServices(List<String> result) {
        Utility util = Utility.getInstance();
        for (String item: result) {
            if (item.startsWith(HTTP) || item.startsWith(HTTPS)) {
                throw new IllegalArgumentException("Cannot mix HTTP and service target");
            }
            if (!util.validServiceName(item) || !item.contains(".")) {
                throw new IllegalArgumentException("Invalid service name");
            }
        }
    }

    private boolean validMethods(List<String> methods) {
        if (methods.isEmpty()) {
            return false;
        }
        for (String m: methods) {
            if (!METHOD_LIST.contains(m)) {
                return false;
            }
        }
        return true;
    }

    private String getUrl(String url, boolean exact) {
        StringBuilder sb = new StringBuilder();
        List<String> parts = Utility.getInstance().split(url.toLowerCase(), "/");
        for (String p: parts) {
            String s = p.trim();
            sb.append('/');
            sb.append(s);
            if (!exact && invalidWildcardUrl(s)) {
                return null;
            }
        }
        return sb.toString();
    }

    private boolean invalidWildcardUrl(String s) {
        if (s.contains("{") || s.contains("}")) {
            if (s.contains("*")) {
                log.error("wildcard url segment must not mix arguments with *, actual: {}", s);
                return true;
            }
            if (!validArgument(s)) {
                log.error("argument url segment must be enclosed by curly brackets, actual: {}", s);
                return true;
            }
        }
        if (s.contains("*") && !validWildcard(s)) {
            log.error("wildcard url segment must ends with *, actual: {}", s);
            return true;
        }
        return false;
    }

    private boolean validArgument(String arg) {
        if (arg.startsWith("{") && arg.endsWith("}")) {
            String v = arg.substring(1, arg.length()-1);
            if (v.isEmpty()) {
                return false;
            } else {
                return !v.contains("{") && !v.contains("}");
            }
        } else {
            return false;
        }
    }

    private boolean validWildcard(String wildcard) {
        if ("*".equals(wildcard)) {
            return true;
        }
        if (!wildcard.endsWith("*")) {
            return false;
        }
        List<String> stars = Utility.getInstance().split(wildcard, "*");
        return stars.size() == 1;
    }

    private void loadCors(ConfigReader config, int total) {
        for (int i=0; i < total; i++) {
            String id = config.getProperty(CORS+"["+i+"]."+ID);
            Object options = config.get(CORS+"["+i+"]."+OPTIONS);
            Object headers = config.get(CORS+"["+i+"]."+HEADERS);
            if (id != null && options instanceof List<?> optionList && headers instanceof List<?> headerList) {
                if (validCorsList(optionList) && validCorsList(headerList)) {
                    loadCorsEntry(config, id, optionList, headerList, i);
                } else {
                    log.error("Skipping invalid cors entry id={}, options={}, headers={}", id, options, headers);
                }
            } else {
                log.error("Skipping invalid cors definition {}", config.get(CORS+"["+i+"]"));
            }
        }
    }

    private void loadCorsEntry(ConfigReader config, String id, List<?> optList, List<?> headerList, int i) {
        CorsInfo info = new CorsInfo();
        for (int j = 0; j < optList.size(); j++) {
            info.addOption(config.getProperty(CORS + "[" + i + "]." + OPTIONS + "[" + j + "]"));
        }
        for (int j = 0; j < headerList.size(); j++) {
            info.addHeader(config.getProperty(CORS + "[" + i + "]." + HEADERS + "[" + j + "]"));
        }
        corsConfig.put(id, info);
        var origin = info.getOrigin(false);
        log.info("Loaded {} cors headers ({})", id, origin);
    }

    private boolean validCorsList(List<?> list) {
        for (Object o: list) {
            if (o instanceof String text) {
                if (!validCorsElement(text)) {
                    return false;
                }
            } else {
                log.error("cors header must be a list of strings, actual: {}", list);
                return false;
            }
        }
        return true;
    }

    private boolean validCorsElement(String element) {
        if (!element.startsWith(ACCESS_CONTROL_PREFIX)) {
            log.error("cors header must start with {}, actual: {}", ACCESS_CONTROL_PREFIX, element);
            return false;
        }
        int colon = element.indexOf(':');
        if (colon == -1) {
            log.error("cors header must contain key-value separated by a colon, actual: {}", element);
            return false;
        }
        String value = element.substring(colon+1).trim();
        if (value.isEmpty()) {
            var v = element.substring(0, colon);
            log.error("Missing value in cors header {}", v);
            return false;
        }
        return true;
    }

    private void loadHeaderTransform(ConfigReader config, int total) {
        for (int i=0; i < total; i++) {
            String id = config.getProperty(HEADERS+"["+i+"]."+ID);
            if (id != null) {
                loadHeaderEntry(config, i, true);
                loadHeaderEntry(config, i, false);
            } else {
                log.error("Skipping invalid header definition - Missing {}[{}]", HEADERS, i);
            }
        }
    }

    private void loadHeaderEntry(ConfigReader config, int idx, boolean isRequest) {
        String id = config.getProperty(HEADERS+"["+idx+"]."+ID);
        String type = isRequest? REQUEST : RESPONSE;
        Object go = config.get(HEADERS+"["+idx+"]."+type);
        if (go instanceof Map) {
            int addCount = 0;
            int dropCount = 0;
            int keepCount = 0;
            HeaderInfo info = new HeaderInfo();
            Object addList = config.get(HEADERS+"["+idx+"]."+type+"."+ADD);
            if (addList instanceof List<?> items) {
                addCount += addHeaders(config, idx, items, type, info, id);
            }
            Object dropList = config.get(HEADERS+"["+idx+"]."+type+"."+DROP);
            if (dropList instanceof List<?> items) {
                dropCount += dropHeaders(config, idx, items, type, info);
            }
            Object keepList = config.get(HEADERS+"["+idx+"]."+type+"."+KEEP);
            if (keepList instanceof List<?> items) {
                keepCount += keepHeaders(config, idx, items, type, info);
            }
            if (isRequest) {
                requestHeaderInfo.put(id, info);
            } else {
                responseHeaderInfo.put(id, info);
            }
            log.info("Loaded {}, {} headers, add={}, drop={}, keep={}", id, type, addCount, dropCount, keepCount);
        }
    }

    private int addHeaders(ConfigReader config, int i, List<?> items, String type, HeaderInfo info, String id) {
        int count = 0;
        for (int j=0; j < items.size(); j++) {
            boolean valid = false;
            String kv = config.getProperty(HEADERS+"["+i+"]."+type+"."+ADD+"["+j+"]", "null");
            int colon = kv.indexOf(':');
            if (colon > 0) {
                String k = kv.substring(0, colon).trim().toLowerCase();
                String v = kv.substring(colon+1).trim();
                if (!k.isEmpty() && !v.isEmpty()) {
                    info.addHeader(k, v);
                    count++;
                    valid = true;
                }
            }
            if (!valid) {
                log.warn("Skipping invalid header {} {}[{}].{}.{}", id, HEADERS, i, type, ADD);
            }
        }
        return count;
    }

    private int dropHeaders(ConfigReader config, int i, List<?> items, String type, HeaderInfo info) {
        int count = 0;
        for (int j=0; j < items.size(); j++) {
            String key = config.getProperty(HEADERS+"["+i+"]."+type+"."+DROP+"["+j+"]");
            if (key != null) {
                info.drop(key);
                count++;
            }
        }
        return count;
    }

    private int keepHeaders(ConfigReader config, int i, List<?> items, String type, HeaderInfo info) {
        int count = 0;
        for (int j=0; j < items.size(); j++) {
            String key = config.getProperty(HEADERS+"["+i+"]."+type+"."+KEEP+"["+j+"]");
            if (key != null) {
                info.keep(key);
                count++;
            }
        }
        return count;
    }

    public int getDurationInSeconds(String duration) {
        if (duration == null) {
            // default 30 seconds
            return 30;
        } else {
            int result = Utility.getInstance().getDurationInSeconds(duration);
            // 5 seconds > result > 5 minutes
            return Math.clamp(result, 5, FIVE_MINUTES);
        }
    }
}
