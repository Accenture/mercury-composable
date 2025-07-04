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
    private static final String SKIP_INVALID_AUTH = "Skipping entry with invalid authentication service name {}";
    private static final String SKIP_INVALID_ENTRY = "Skipping invalid REST entry {}";
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
    private static SimpleHttpFilter requestFilter;
    private static List<String> noCachePages;
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
            // then compare each segment in the URL, also with case insensitivity
            AssignedRoute similar = null;
            for (String u: urlPaths) {
                AssignedRoute info = getMatchedRoute(urlParts, method, u);
                if (info != null) {
                    if (similar == null) {
                        similar = info;
                    }
                    // both URL path and method are correct
                    if (routes.containsKey(method + ":" + u)) {
                        return info;
                    }
                }
            }
            /*
             * Similar path found but method does not match.
             * This allows it to reject the request with "HTTP-405 Method Not Allowed".
             */
            return similar;
        }
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
            String configuredItem = segments.get(i);
            if (configuredItem.startsWith("{") && configuredItem.endsWith("}")) {
                continue;
            }
            if ("*".equals(configuredItem)) {
                continue;
            }
            // case-insensitive comparison using lowercase
            String inputItem = urlParts.get(i).toLowerCase();
            if (configuredItem.endsWith("*")) {
                String prefix = configuredItem.substring(0, configuredItem.length()-1);
                if (inputItem.startsWith(prefix)) {
                    continue;
                }
            }
            if (inputItem.equals(configuredItem)) {
                continue;
            }
            return false;
        }
        return true;
    }

    @SuppressWarnings("unchecked")
    private List<String> getNoCacheConfig(ConfigReader config) {
        Object noCache = config.get("static-content.no-cache-pages");
        if (noCache != null) {
            if (noCache instanceof List) {
                List<String> noCacheList = new ArrayList<>();
                List<Object> cList = (List<Object>) noCache;
                for (int i = 0; i < cList.size(); i++) {
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

    @SuppressWarnings(value="unchecked")
    private SimpleHttpFilter getFilter(ConfigReader config) {
        if (config.exists("static-content.filter")) {
            Object path = config.get("static-content.filter.path");
            Object exclusion = config.get("static-content.filter.exclusion");
            String service = config.getProperty("static-content.filter.service");
            if (path instanceof List && service != null && !service.isEmpty()) {
                if (!Utility.getInstance().validServiceName(service)) {
                    log.error("Static content filter ignored: '{} -> {}' - invalid service name", path, service);
                    return null;
                }
                List<String> pathList = new ArrayList<>();
                List<String> exclusionList = new ArrayList<>();
                List<Object> pList = (List<Object>) path;
                for (int i = 0; i < pList.size(); i++) {
                    pathList.add(config.getProperty("static-content.filter.path[" + i + "]"));
                }
                if (pathList.isEmpty()) {
                    log.error("static-content.filter.path is empty");
                    return null;
                }
                if (exclusion instanceof List) {
                    List<Object> eList = (List<Object>) exclusion;
                    for (int i = 0; i < eList.size(); i++) {
                        exclusionList.add(config.getProperty("static-content.filter.exclusion[" + i + "]"));
                    }
                }
                if (invalidFilterParameters(pathList)) {
                    log.error("static-content.filter.path ignored - invalid syntax {}", path);
                    return null;
                }
                if (invalidFilterParameters(exclusionList)) {
                    log.error("static-content.filter.exclusion ignored - invalid syntax {}", exclusion);
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

    @SuppressWarnings(value = "unchecked")
    public void load(ConfigReader config) {
        requestFilter = getFilter(config);
        noCachePages = getNoCacheConfig(config);
        if (config.exists(HEADERS)) {
            Object headerList = config.get(HEADERS);
            boolean valid = false;
            if (headerList instanceof List) {
                List<Object> list = (List<Object>) headerList;
                if (isListOfMap(list)) {
                    loadHeaderTransform(config, list.size());
                    valid = true;
                }
            }
            if (!valid) {
                log.error("'headers' section must be a list of request and response entries");
            }
        }
        if (config.exists(CORS)) {
            Object corsList = config.get(CORS);
            boolean valid = false;
            if (corsList instanceof List) {
                List<Object> list = (List<Object>) corsList;
                if (isListOfMap((List<Object>) corsList)) {
                    loadCors(config, list.size());
                    valid = true;
                }
            }
            if (!valid) {
                log.error("'cors' section must be a list of Access-Control entries (id, options and headers)");
            }
        }
        if (config.exists(REST)) {
            Object rest = config.get(REST);
            boolean valid = false;
            if (rest instanceof List) {
                if (isListOfMap((List<Object>) rest)) {
                    loadRest(config);
                    valid = true;
                }
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
            if (exact.size() > 1) {
                Collections.sort(exact);
            }
            if (!exact.isEmpty()) {
                var message = new HashMap<String, Object>();
                message.put("type", "url");
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
            if (urlPaths.size() > 1) {
                Collections.sort(urlPaths);
            }
            if (!urlPaths.isEmpty()) {
                var message = new HashMap<String, Object>();
                message.put("type", "url");
                message.put("match", "parameters");
                message.put("total", urlPaths.size());
                message.put("path", urlPaths);
                log.info("{}", message);
            }
        }
    }

    private boolean isListOfMap(List<Object> list) {
        for (Object o: list) {
            if (!(o instanceof Map)) {
                return false;
            }
        }
        return true;
    }

    @SuppressWarnings("unchecked")
    private void loadRest(ConfigReader config) {
        addDefaultEndpoints(config);
        sortEndpoints(config);
        Object restEntries = config.get(REST);
        int total = ((List<Object>) restEntries).size();
        for (int i=0; i < total; i++) {
            Object services = config.get(REST+"["+i+"]."+SERVICE);
            Object methods = config.get(REST+"["+i+"]."+METHODS);
            String url = config.getProperty(REST+"["+i+"]."+URL_LABEL);
            if (url != null && methods instanceof List &&
                    (services instanceof List || services instanceof String)) {
                loadRestEntry(config, i, !url.contains("{") && !url.contains("}") && !url.contains("*"));
            } else {
                log.error(SKIP_INVALID_ENTRY, config.get(REST+"["+i+"]"));
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
        Object restEntries = config.get(REST);
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
        Utility util = Utility.getInstance();
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
        Object authConfig = config.get(REST+"["+idx+"]."+ AUTHENTICATION);
        // authentication: "v1.api.auth"
        if (authConfig instanceof String) {
            String auth = authConfig.toString().trim();
            if (util.validServiceName(auth)) {
                info.defaultAuthService = auth;
            } else {
                log.error(SKIP_INVALID_AUTH, config.get(REST+"["+idx+"]"));
                return;
            }
        }
        /*
            authentication:
            - "x-app-name: demo : v1.demo.auth"
            - "authorization: v1.basic.auth"
            - "default: v1.api.auth"
         */
        if (authConfig instanceof List) {
            List<Object> authList = (List<Object>) authConfig;
            for (Object o : authList) {
                String authEntry = String.valueOf(o);
                List<String> parts = util.split(authEntry, ": ");
                if (parts.size() == 2) {
                    String authHeader = parts.get(0);
                    String authService = parts.get(1);
                    if (util.validServiceName(authService)) {
                        if (DEFAULT_VALUE.equals(authHeader)) {
                            info.defaultAuthService = authService;
                        } else {
                            info.setAuthService(authHeader, "*", authService);
                        }
                    } else {
                        log.error(SKIP_INVALID_AUTH, config.get(REST+"["+idx+"]"));
                        return;
                    }

                } else if (parts.size() == 3) {
                    String authHeader = parts.get(0);
                    String authValue = parts.get(1);
                    String authService = parts.get(2);
                    if (util.validServiceName(authService)) {
                        info.setAuthService(authHeader, authValue, authService);
                    } else {
                        log.error(SKIP_INVALID_AUTH, config.get(REST+"["+idx+"]"));
                        return;
                    }

                } else {
                    log.error(SKIP_INVALID_AUTH, config.get(REST+"["+idx+"]"));
                    return;
                }
            }
            if (info.defaultAuthService == null) {
                log.error("Skipping entry because it is missing default authentication service {}",
                            config.get(REST+"["+idx+"]"));
                return;
            }
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
        String corsId = config.getProperty(REST+"["+idx+"]."+CORS);
        if (corsId != null) {
            if (corsConfig.containsKey(corsId)) {
                info.corsId = corsId;
            } else {
                log.error("Skipping invalid entry because cors ID {} is not found, {}",
                            corsId, config.get(REST+"["+idx+"]"));
                return;
            }
        }
        String headerId = config.getProperty(REST+"["+idx+"]."+HEADERS);
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
            if (!foundTransform) {
                log.error("Skipping invalid entry because headers ID {} is not found, {}",
                        headerId, config.get(REST+"["+idx+"]"));
                return;
            }
        }
        if (info.primary.startsWith(HTTP) || info.primary.startsWith(HTTPS)) {
            Object rewrite = config.get(REST+"["+idx+"]."+URL_REWRITE);
            // URL rewrite
            if (rewrite instanceof List) {
                List<String> urlRewrite = (List<String>) rewrite;
                if (urlRewrite.size() == 2) {
                    info.urlRewrite = urlRewrite;
                } else {
                    log.error("Skipping entry with invalid {} - {}. It should contain a list of 2 prefixes",
                            URL_REWRITE, urlRewrite);
                    return;
                }
            } else {
                log.error("Skipping entry with invalid {} - {}, expected: List<String>, actual: {}",
                        URL_REWRITE, rewrite, rewrite.getClass().getSimpleName());
                return;
            }
            try {
                URI u = new URI(info.primary);
                if (!u.getPath().isEmpty()) {
                    log.error("Skipping entry with invalid service URL {} - Must not contain path", info.primary);
                    return;
                }
                if (u.getQuery() != null) {
                    log.error("Skipping entry with invalid service URL {} - Must not contain query", info.primary);
                    return;
                }
                String trustAll = config.getProperty(REST+"["+idx+"]."+TRUST_ALL_CERT);
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
                log.error("Skipping entry with invalid service URL {} - {}", info.primary, e.getMessage());
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
        if (validMethods(methods)) {
            List<String> allMethods = new ArrayList<>(new HashSet<>(methods));
            Collections.sort(allMethods);
            info.methods = allMethods;
            if (exact) {
                exactRoutes.put(url, true);
            }
            String nUrl = getUrl(url, exact);
            if (nUrl == null) {
                log.error("Skipping invalid entry {}", config.get(REST+"["+idx+"]"));
            } else {
                info.url = nUrl;
                // ensure OPTIONS method is supported
                allMethods.add(OPTIONS_METHOD);
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
        } else {
            log.error("Skipping entry with invalid method {}", config.get(REST+"["+idx+"]"));
        }
    }

    @SuppressWarnings("unchecked")
    private List<String> validateServiceList(Object svcList) {
        Utility util = Utility.getInstance();
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
        for (String item: result) {
            if (item.startsWith(HTTP) || item.startsWith(HTTPS)) {
                throw new IllegalArgumentException("Cannot mix HTTP and service target");
            }
            if (!util.validServiceName(item) || !item.contains(".")) {
                throw new IllegalArgumentException("Invalid service name");
            }
        }
        return result;
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
            if (!exact) {
                if (s.contains("{") || s.contains("}")) {
                    if (s.contains("*")) {
                        log.error("wildcard url segment must not mix arguments with *, actual: {}", s);
                        return null;
                    }
                    if (!validArgument(s)) {
                        log.error("argument url segment must be enclosed by curly brackets, actual: {}", s);
                        return null;
                    }
                }
                if (s.contains("*") && !validWildcard(s)) {
                    log.error("wildcard url segment must ends with *, actual: {}", s);
                    return null;
                }
            }
            sb.append(s);
        }
        return sb.toString();
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

    @SuppressWarnings("unchecked")
    private void loadCors(ConfigReader config, int total) {
        for (int i=0; i < total; i++) {
            String id = config.getProperty(CORS+"["+i+"]."+ID);
            Object options = config.get(CORS+"["+i+"]."+OPTIONS);
            Object headers = config.get(CORS+"["+i+"]."+HEADERS);
            if (id != null && options instanceof List && headers instanceof List) {
                List<Object> optionList = (List<Object>) options;
                List<Object> headerList = (List<Object>) headers;
                if (validCorsList(optionList) && validCorsList(headerList)) {
                    CorsInfo info = new CorsInfo();
                    for (int j = 0; j < optionList.size(); j++) {
                        info.addOption(config.getProperty(CORS + "[" + i + "]." + OPTIONS + "[" + j + "]"));
                    }
                    for (int j = 0; j < headerList.size(); j++) {
                        info.addHeader(config.getProperty(CORS + "[" + i + "]." + HEADERS + "[" + j + "]"));
                    }
                    corsConfig.put(id, info);
                    log.info("Loaded {} cors headers ({})", id, info.getOrigin(false));
                } else {
                    log.error("Skipping invalid cors entry id={}, options={}, headers={}", id, options, headers);
                }
            } else {
                log.error("Skipping invalid cors definition {}", config.get(CORS+"["+i+"]"));
            }
        }
    }

    private boolean validCorsList(List<Object> list) {
        for (Object o: list) {
            if (o instanceof String) {
                if (!validCorsElement((String) o)) {
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
            log.error("Missing value in cors header {}", element.substring(0, colon));
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

    @SuppressWarnings("unchecked")
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
            if (addList instanceof List) {
                List<Object> items = (List<Object>) addList;
                for (int j=0; j < items.size(); j++) {
                    boolean valid = false;
                    String kv = config.getProperty(HEADERS+"["+idx+"]."+type+"."+ADD+"["+j+"]", "null");
                    int colon = kv.indexOf(':');
                    if (colon > 0) {
                        String k = kv.substring(0, colon).trim().toLowerCase();
                        String v = kv.substring(colon+1).trim();
                        if (!k.isEmpty() && !v.isEmpty()) {
                            info.addHeader(k, v);
                            addCount++;
                            valid = true;
                        }
                    }
                    if (!valid) {
                        log.warn("Skipping invalid header {} {}[{}].{}.{}", id, HEADERS, idx, type, ADD);
                    }
                }
            }
            Object dropList = config.get(HEADERS+"["+idx+"]."+type+"."+DROP);
            if (dropList instanceof List) {
                List<Object> items = (List<Object>) dropList;
                for (int j=0; j < items.size(); j++) {
                    String key = config.getProperty(HEADERS+"["+idx+"]."+type+"."+DROP+"["+j+"]");
                    if (key != null) {
                        info.drop(key);
                        dropCount++;
                    }
                }
            }
            Object keepList = config.get(HEADERS+"["+idx+"]."+type+"."+KEEP);
            if (keepList instanceof List) {
                List<Object> items = (List<Object>) keepList;
                for (int j=0; j < items.size(); j++) {
                    String key = config.getProperty(HEADERS+"["+idx+"]."+type+"."+KEEP+"["+j+"]");
                    if (key != null) {
                        info.keep(key);
                        keepCount++;
                    }
                }
            }
            if (isRequest) {
                requestHeaderInfo.put(id, info);
            } else {
                responseHeaderInfo.put(id, info);
            }
            log.info("Loaded {}, {} headers, add={}, drop={}, keep={}", id, type, addCount, dropCount, keepCount);
        }
    }

    public int getDurationInSeconds(String duration) {
        if (duration == null) {
            // default 30 seconds
            return 30;
        } else {
            int result = Utility.getInstance().getDurationInSeconds(duration);
            // set maximum to 5 minutes and minimum to 5 seconds
            return Math.min(FIVE_MINUTES, Math.max(result, 5));
        }
    }

}
