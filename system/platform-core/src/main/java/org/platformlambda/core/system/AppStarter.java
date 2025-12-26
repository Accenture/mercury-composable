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

package org.platformlambda.core.system;

import io.github.classgraph.ClassInfo;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.net.KeyCertOptions;
import io.vertx.core.net.PemKeyCertOptions;
import org.apache.logging.log4j.core.config.Configurator;
import org.platformlambda.automation.config.RoutingEntry;
import org.platformlambda.automation.http.AsyncHttpClient;
import org.platformlambda.automation.http.HttpRequestHandler;
import org.platformlambda.automation.models.AsyncContextHolder;
import org.platformlambda.automation.services.HttpRouter;
import org.platformlambda.automation.services.AsyncHttpResponse;
import org.platformlambda.automation.util.SimpleHttpUtility;
import org.platformlambda.core.annotations.BeforeApplication;
import org.platformlambda.core.annotations.MainApplication;
import org.platformlambda.core.annotations.PreLoad;
import org.platformlambda.core.annotations.WebSocketService;
import org.platformlambda.core.models.*;
import org.platformlambda.core.util.*;
import org.platformlambda.core.websocket.server.WsHandshakeHandler;
import org.platformlambda.core.websocket.server.WsRequestHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.text.NumberFormat;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class AppStarter {
    private static final Logger log = LoggerFactory.getLogger(AppStarter.class);
    private static final ConcurrentMap<String, LambdaFunction> wsLambdas = new ConcurrentHashMap<>();
    private static final AtomicInteger compileCycle = new AtomicInteger(0);
    private static final long HOUSEKEEPING_INTERVAL = 10 * 1000L;    // 10 seconds
    private static final String SKIP_OPTIONAL = "Skipping optional {}";
    private static final String CLASS_NOT_FOUND = "Class {} not found";
    private static final String JAVA_VERSION = "java.version";
    private static final String STATIC_CONTENT = "static-content";
    private static final String BEFORE_APP_PHASE = " during BeforeApplication phase";
    private static final String PRELOAD_PHASE = " during PreLoad phase";
    private static final String SERVER_STARTUP = " during HTTP server startup";
    private static final String MODULES_AUTOSTART = "modules.autostart";
    private static final String EVENT_SCRIPT_MANAGER = "event.script.manager";
    private static final String PRELOAD_PREFIX = "preload[";
    private static final String REST_PREFIX = "rest[";
    private static final String FLOW_PROTOCOL = "flow://";
    private static final String FLOW_ID = "flow_id";
    private static final String BODY_TYPE = "body.type";
    private static final String HEADER_TYPE = "header.type";
    private static final String TYPE = "type";
    private static final String START = "start";
    private static final String TEXT = "text";
    private static final String JSON = "json";
    private static final String COMPACT = "compact";
    private static final String FILEPATH = "file:";
    private static final String CLASSPATH = "classpath:";
    private static final String COMPACT_LOG4J = "log4j2-compact.xml";
    private static final String JSON_LOG4J = "log4j2-json.xml";
    private static final String IS_FALSE = "false";
    private static final String DEFAULT_INSTANCES = "-1";
    private static final int MAX_SEQ = 999;
    private static boolean loaded = false;
    private static boolean mainAppLoaded = false;
    private static boolean springBoot = false;
    private static String[] args = new String[0];
    private final long startTime = System.currentTimeMillis();
    private static AppStarter myInstance;

    public static void main(String[] args) {
        if (!loaded) {
            loaded = true;
            AppConfigReader config = AppConfigReader.getInstance();
            String logFormat = config.getProperty("log.format", TEXT);
            if (JSON.equalsIgnoreCase(logFormat)) {
                reConfigLogger(true);
            } else if (COMPACT.equalsIgnoreCase(logFormat)) {
                reConfigLogger(false);
            }
            // print application version
            log.info("Application version {}", Utility.getInstance().getVersion());
            /*
             * Print out basic JVM and memory information before starting app.
             * This helps to check if JVM is configured correctly.
             */
            Runtime runtime = Runtime.getRuntime();
            NumberFormat number = NumberFormat.getInstance();
            var javaVersion = System.getProperty(JAVA_VERSION);
            log.info("Java version {}", javaVersion);
            String maxMemory = number.format(runtime.maxMemory());
            String allocatedMemory = number.format(runtime.totalMemory());
            String freeMemory = number.format(runtime.freeMemory());
            log.info("Max memory: {}", maxMemory);
            log.info("Allocated memory: {}", allocatedMemory);
            log.info("Free memory: {}", freeMemory);
            AppStarter.args = args;
            myInstance = new AppStarter();
            // Run "BeforeApplication" modules
            myInstance.doApps(args, false);
            // preload services
            preload();
            // Setup REST automation and websocket server if needed
            myInstance.startHttpServerIfAny();
            // Run "MainApplication" modules
            if (!springBoot) {
                mainAppLoaded = true;
                log.info("Loading user application");
                myInstance.doApps(args, true);
            }
        }
    }

    private static void reConfigLogger(boolean json) {
        String xmlFile = json? JSON_LOG4J : COMPACT_LOG4J;
        try (InputStream res = Utility.class.getResourceAsStream("/"+ xmlFile)) {
            if (res != null) {
                String classPath = CLASSPATH + xmlFile;
                Configurator.reconfigure(URI.create(classPath));
                log.info("Logger reconfigured in {} mode", json? JSON : COMPACT);
            } else {
                log.error("Unable to reconfigure logger because {} does not exist", xmlFile);
            }
        } catch (IOException e) {
            log.error("Unable to reconfigure logger - {}", e.getMessage());
        }
    }

    public static void runAsSpringBootApp() {
        springBoot = true;
    }

    public static void runMainApp() {
        if (myInstance != null && !mainAppLoaded) {
            mainAppLoaded = true;
            myInstance.doApps(args, true);
        }
    }

    private void doApps(String[] args, boolean main) {
        // find and execute optional preparation modules
        Utility util = Utility.getInstance();
        SimpleClassScanner scanner = SimpleClassScanner.getInstance();
        Set<String> packages = scanner.getPackages(true);
        Map<String, Class<?>> steps = new HashMap<>();
        var counter = new AtomicInteger(0);
        for (String p : packages) {
            prepareApp(counter, steps, p, main);
        }
        executeOrderly(steps, args, main);
        // run additional "startup" composable functions asynchronously
        if (main) {
            AppConfigReader config = AppConfigReader.getInstance();
            Object o = config.get(MODULES_AUTOSTART);
            List<String> modules = new ArrayList<>();
            if (o instanceof String text) {
                modules.addAll(util.split(text, ", "));
            } else if (o instanceof List<?> items) {
                for (int i = 0; i < items.size(); i++) {
                    modules.add(config.getProperty(MODULES_AUTOSTART + "[" + i + "]"));
                }
            }
            PostOffice po = new PostOffice(MODULES_AUTOSTART, util.getUuid(), "START /modules");
            for (String svc : modules) {
                executeAutoStart(po, svc);
            }
        }
    }

    private void prepareApp(AtomicInteger counter, Map<String, Class<?>> steps, String folder, boolean isMain) {
        Utility util = Utility.getInstance();
        SimpleClassScanner scanner = SimpleClassScanner.getInstance();
        List<ClassInfo> services = scanner.getAnnotatedClasses(folder,
                                            isMain? MainApplication.class : BeforeApplication.class);
        for (ClassInfo info : services) {
            try {
                Class<?> cls = Class.forName(info.getName());
                if (Feature.isRequired(cls)) {
                    int seq = Math.max(0, getSequence(cls, isMain));
                    String key = util.zeroFill(seq, MAX_SEQ) + "." + util.zeroFill(counter.incrementAndGet(), MAX_SEQ);
                    steps.put(key, cls);
                } else {
                    log.info(SKIP_OPTIONAL + BEFORE_APP_PHASE, cls);
                }
            } catch (ClassNotFoundException e) {
                log.error(CLASS_NOT_FOUND + BEFORE_APP_PHASE, info.getName());
            }
        }
    }

    private void executeAutoStart(PostOffice po, String svc) {
        Utility util = Utility.getInstance();
        try {
            log.info("Running module: {}", svc);
            if (svc.startsWith(FLOW_PROTOCOL) && svc.length() > FLOW_PROTOCOL.length()) {
                String flowId = svc.substring(FLOW_PROTOCOL.length());
                var dataset = new MultiLevelMap();
                dataset.setElement(BODY_TYPE, START);
                dataset.setElement(HEADER_TYPE, START);
                EventEnvelope flowService = new EventEnvelope();
                flowService.setTo(EVENT_SCRIPT_MANAGER).setHeader(FLOW_ID, flowId);
                flowService.setCorrelationId(util.getUuid()).setBody(dataset.getMap());
                po.send(flowService);
            } else {
                po.send(svc, new Kv(TYPE, START));
            }
        } catch (IllegalArgumentException e) {
            log.error("Unable to start module '{}' - {}", svc, e.getMessage());
        }
    }

    private int getSequence(Class<?> cls, boolean isMain) {
        if (isMain) {
            MainApplication mainApp = cls.getAnnotation(MainApplication.class);
            return Math.min(MAX_SEQ, mainApp.sequence());
        } else {
            BeforeApplication beforeApp = cls.getAnnotation(BeforeApplication.class);
            return Math.min(MAX_SEQ, beforeApp.sequence());
        }
    }

    private void executeOrderly(Map<String, Class<?>> steps, String[] args, boolean isMain) {
        List<String> list = new ArrayList<>(steps.keySet());
        if (list.size() > 1) {
            Collections.sort(list);
        }
        int n = 0;
        int error = 0;
        for (String seq : list) {
            Class<?> cls = steps.get(seq);
            try {
                Object o = cls.getDeclaredConstructor().newInstance();
                if (o instanceof EntryPoint app) {
                    log.info("Starting {}", app.getClass().getName());
                    app.start(args);
                    n++;
                } else {
                    error++;
                    log.error("Unable to start {} because it is not an instance of {}",
                            cls.getName(), EntryPoint.class.getName());
                }
            } catch (Exception e) {
                error++;
                log.error("Unable to start - {}", cls.getName(), e);
            }
        }
        if (isMain && error == 0 && n == 0) {
            log.error("Missing MainApplication\n\n{}\n{}\n\n",
                    "Did you forget to annotate your main module with @MainApplication that implements EntryPoint?",
                    "and ensure the package parent is defined in 'web.component.scan' of application.properties.");
        }
    }

    private static Map<String, PreLoadInfo> getPreloadOverride() {
        Utility util = Utility.getInstance();
        Map<String, PreLoadInfo> result = new HashMap<>();
        AppConfigReader config = AppConfigReader.getInstance();
        String path = config.getProperty("yaml.preload.override");
        if (path != null) {
            List<String> paths = util.split(path, ", ");
            for (String p: paths) {
                try {
                    Map<String, PreLoadInfo> tasks = parsePreloadOverride(new ConfigReader(p));
                    if (result.isEmpty()) {
                        result.putAll(tasks);
                    } else {
                        overrideTasks(result, tasks);
                    }
                } catch (IllegalArgumentException e) {
                    log.error("Unable to load PreLoad entries from {} - {}", p, e.getMessage());
                }
            }
        }
        return result;
    }

    private static void overrideTasks(Map<String, PreLoadInfo> result, Map<String, PreLoadInfo> tasks) {
        for (var entry : tasks.entrySet()) {
            String route = entry.getKey();
            PreLoadInfo info = entry.getValue();
            if (result.containsKey(route)) {
                PreLoadInfo prior = result.get(route);
                prior.routes.addAll(info.routes);
                if (prior.instances == -1) {
                    prior.instances = info.instances;
                }
            } else {
                result.put(route, info);
            }
        }
    }

    private static Map<String, PreLoadInfo> parsePreloadOverride(ConfigReader config) {
        Map<String, PreLoadInfo> result = new HashMap<>();
        Object o = config.get("preload");
        if (o instanceof List<?> items) {
            for (int i=0; i < items.size(); i++) {
                if (config.get(PRELOAD_PREFIX+i+"]") instanceof Map) {
                    getOverrideEntry(config, i, result);
                } else {
                    throw new IllegalArgumentException(PRELOAD_PREFIX+i+"] is not a map of original and routes");
                }
            }
        } else {
            throw new IllegalArgumentException("preload must be a list of key-values for original and routes");
        }
        return result;
    }

    private static void getOverrideEntry(ConfigReader config, int i, Map<String, PreLoadInfo> result) {
        var util = Utility.getInstance();
        String original = config.getProperty(PRELOAD_PREFIX+i+"].original");
        Object routeList = config.get(PRELOAD_PREFIX+i+"].routes");
        int instances = util.str2int(config.getProperty(PRELOAD_PREFIX+i+"].instances", DEFAULT_INSTANCES));
        if (original == null || original.isEmpty()) {
            throw new IllegalArgumentException(PRELOAD_PREFIX+i+"] does not contain 'original'");
        }
        if (routeList instanceof List<?> items) {
            Set<String> routes = new HashSet<>();
            for (int j=0; j < items.size(); j++) {
                routes.add(config.getProperty(PRELOAD_PREFIX+i+"].routes["+j+"]"));
            }
            if ("true".equals(config.getProperty(PRELOAD_PREFIX+i+"].keep-original"))) {
                routes.add(original);
            }
            result.put(original, new PreLoadInfo(instances, routes));
        } else {
            throw new IllegalArgumentException(PRELOAD_PREFIX+i+"].routes must be a list");
        }
    }

    private static void preload() {
        EventEmitter po = EventEmitter.getInstance();
        log.info("Preloading started - {}", po.getId());
        Utility util = Utility.getInstance();
        Map<String, PreLoadInfo> preloadOverride = getPreloadOverride();
        SimpleClassScanner scanner = SimpleClassScanner.getInstance();
        Set<String> packages = scanner.getPackages(true);
        for (String p : packages) {
            List<ClassInfo> services = scanner.getAnnotatedClasses(p, PreLoad.class);
            for (ClassInfo info : services) {
                String serviceName = info.getName();
                log.info("Loading service {}", serviceName);
                try {
                    Class<?> cls = Class.forName(serviceName);
                    if (Feature.isRequired(cls)) {
                        PreLoad svc = cls.getAnnotation(PreLoad.class);
                        var md = new PreLoadMetadata();
                        md.routes = util.split(svc.route(), "[, ]");
                        if (md.routes.isEmpty()) {
                            log.error("Unable to preload {} - missing service route(s)", serviceName);
                        } else {
                            processPreload(serviceName, svc, cls, preloadOverride, md);
                        }
                    } else {
                        log.info(SKIP_OPTIONAL + PRELOAD_PHASE, cls);
                    }
                } catch (ClassNotFoundException | InvocationTargetException | InstantiationException |
                         IllegalAccessException | NoSuchMethodException | IllegalArgumentException e) {
                    log.error("Unable to preload {} - {}", serviceName, e.getMessage());
                }
            }
        }
        log.info("Preloading completed");
    }

    private static void processPreload(String serviceName, PreLoad svc, Class<?> cls,
                                       Map<String, PreLoadInfo> preloadOverride, PreLoadMetadata md)
            throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        md.instances = getInstancesFromEnv(svc.envInstances(), svc.instances());
        PreLoadInfo preLoadInfo = getMatchedPreload(preloadOverride, md.routes);
        if (preLoadInfo != null) {
            overridePreloadInfo(svc, preLoadInfo, md);
        }
        Object o = cls.getDeclaredConstructor().newInstance();
        Class<?> pojoClass = null;
        CustomSerializer mapper = null;
        if (svc.inputPojoClass() != Void.class) {
            pojoClass = svc.inputPojoClass();
        }
        if (svc.customSerializer() != Void.class) {
            mapper = getCustomSerializer(svc, md.routes);
        }
        if (o instanceof TypedLambdaFunction<?, ?> f) {
            registerFunctionWithRoutes(svc, md.routes, f, md.instances, mapper, pojoClass);
        } else {
            log.error("Unable to preload {} - {} must implement {}", serviceName, o.getClass(),
                    TypedLambdaFunction.class.getSimpleName());
        }
    }

    private static void overridePreloadInfo(PreLoad svc, PreLoadInfo preLoadInfo, PreLoadMetadata md) {
        md.routes = new ArrayList<>(preLoadInfo.routes);
        Collections.sort(md.routes);
        int updatedInstances = preLoadInfo.instances;
        if (updatedInstances > 0) {
            log.info("Preload [{}] as {}, instances {} to {}",
                    svc.route(), md.routes, md.instances, updatedInstances);
            md.instances = updatedInstances;
        } else {
            log.info("Preload [{}] as {}", svc.route(), md.routes);
        }
    }

    private static void registerFunctionWithRoutes(PreLoad svc, List<String> routes, TypedLambdaFunction<?, ?> f,
                                                   int instances, CustomSerializer mapper, Class<?> pojoClass) {
        var platform = Platform.getInstance();
        for (String r : routes) {
            if (svc.isPrivate()) {
                platform.registerPrivate(r, f, instances);
            } else {
                platform.register(r, f, instances);
            }
            updateServiceDef(r, mapper, pojoClass, svc);
        }
    }

    private static CustomSerializer getCustomSerializer(PreLoad svc, List<String> routes) {
        try {
            Object mapperObj = svc.customSerializer().getDeclaredConstructor().newInstance();
            if (mapperObj instanceof CustomSerializer serializer) {
                return serializer;
            } else {
                throw new IllegalArgumentException("Invalid implementation of CustomSerializer");
            }
        } catch (Exception ce) {
            log.error("Skipping custom serializer {} for {} - {}: {}",
                    svc.customSerializer(), routes,
                    ce.getClass().getSimpleName(), ce.getMessage());
            return null;
        }
    }

    private static PreLoadInfo getMatchedPreload(Map<String, PreLoadInfo> preloadOverride, List<String> routes) {
        for (String r: routes) {
            if (preloadOverride.containsKey(r)) {
                return preloadOverride.get(r);
            }
        }
        return null;
    }

    private static void updateServiceDef(String route, CustomSerializer mapper, Class<?> pojoClass, PreLoad service) {
        Platform platform = Platform.getInstance();
        if (mapper != null) {
            platform.setCustomSerializer(route, mapper);
        }
        if (pojoClass != null) {
            platform.setPoJoClass(route, pojoClass);
        }
        platform.setSerializationStrategy(route, service.inputStrategy(), service.outputStrategy());
    }

    private static int getInstancesFromEnv(String envInstances, int instances) {
        if (envInstances == null || envInstances.isEmpty()) {
            return Math.max(1, instances);
        } else {
            Utility util = Utility.getInstance();
            AppConfigReader config = AppConfigReader.getInstance();
            String env = config.getProperty(envInstances);
            return Math.max(1, util.isDigits(env)? util.str2int(env) : instances);
        }
    }

    private void prepareWebsocketServices(List<ClassInfo> services) {
        for (ClassInfo info : services) {
            try {
                Class<?> cls = Class.forName(info.getName());
                if (Feature.isRequired(cls)) {
                    WebSocketService annotation = cls.getAnnotation(WebSocketService.class);
                    if (!annotation.value().isEmpty()) {
                        if (!Utility.getInstance().validServiceName(annotation.value())) {
                            log.error("Unable to load {} ({}) because the path is not a valid service name",
                                    cls.getName(), annotation.value());
                        }
                        loadWebsocketServices(cls, annotation.namespace(), annotation.value());
                    }
                } else {
                    log.info(SKIP_OPTIONAL + SERVER_STARTUP, cls);
                }
            } catch (ClassNotFoundException e) {
                log.error(CLASS_NOT_FOUND + SERVER_STARTUP, info.getName());
            }
        }
    }

    private void startHttpServerIfAny() {
        // find and execute optional preparation modules
        final SimpleClassScanner scanner = SimpleClassScanner.getInstance();
        final Set<String> packages = scanner.getPackages(true);
        for (String p : packages) {
            List<ClassInfo> services = scanner.getAnnotatedClasses(p, WebSocketService.class);
            prepareWebsocketServices(services);
        }
        // start HTTP/websocket server
        final AppConfigReader config = AppConfigReader.getInstance();
        final boolean rest = "true".equals(config.getProperty("rest.automation", IS_FALSE));
        if (rest || !wsLambdas.isEmpty()) {
            final Utility util = Utility.getInstance();
            final int port = util.str2int(config.getProperty("websocket.server.port",
                                    config.getProperty("rest.server.port",
                                    config.getProperty("server.port", "8085"))));
            final boolean sslEnabled = "true".equals(config.getProperty("rest.server.ssl-enabled", IS_FALSE));
            if (port > 0) {
                // create a dedicated vertx event loop instance for the HTTP server
                final Vertx vertx = Vertx.vertx();
                final String sslCertPath = config.getProperty("rest.server.ssl.cert");
                final String sslKeyPath = config.getProperty("rest.server.ssl.key");
                final HttpServerOptions options = getHttpServerOptions(sslEnabled, sslCertPath, sslKeyPath);
                final HttpServer server = vertx.createHttpServer(options);
                // Compile endpoints to be used by the REST automation system
                renderRestEndpoints();
                // Start HTTP request and response handlers
                HttpRouter gateway = new HttpRouter();
                server.requestHandler(new HttpRequestHandler(gateway));
                setWebsocketHandler(server);
                startServer(server, port, gateway.getContexts());
            }
        }
    }

    private void setWebsocketHandler(HttpServer server) {
        if (!wsLambdas.isEmpty()) {
            List<String> wsPaths = new ArrayList<>(wsLambdas.keySet());
            if (wsPaths.size() > 1) {
                Collections.reverse(wsPaths);
            }
            server.webSocketHandshakeHandler(new WsHandshakeHandler(wsPaths))
                    .webSocketHandler(new WsRequestHandler(wsLambdas, wsPaths));
        }
    }

    private void startServer(HttpServer server, int port, ConcurrentMap<String, AsyncContextHolder> contexts) {
        var platform = Platform.getInstance();
        var startupMonitor = "loader." + platform.getOrigin();
        LambdaFunction f = (headers, input, instance) -> {
            platform.release(startupMonitor);
            String diff = NumberFormat.getInstance().format(System.currentTimeMillis() - startTime);
            warmUpVirtualThreadSystem();
            log.info("Modules loaded in {} ms", diff);
            return null;
        };
        platform.registerPrivate(startupMonitor, f, 1);
        server.listen(port).onSuccess(service -> {
            EventEmitter.getInstance().send(startupMonitor, "ready");
            platform.registerPrivate(AsyncHttpClient.ASYNC_HTTP_RESPONSE, new AsyncHttpResponse(contexts), 500);
            // start timeout handler
            Housekeeper housekeeper = new Housekeeper(contexts);
            platform.getVertx().setPeriodic(HOUSEKEEPING_INTERVAL,
                    t -> housekeeper.removeExpiredConnections());
            log.info("AsyncHttpContext housekeeper started");
            log.info("Reactive HTTP server running on port-{}", service.actualPort());
            if (!wsLambdas.isEmpty()) {
                log.info("Websocket server running on port-{}", service.actualPort());
            }
        }).onFailure(ex -> {
            log.error("Unable to start - {}", ex.getMessage());
            System.exit(-1);
        });
    }

    private void warmUpVirtualThreadSystem() {
        // send a few events to the "no.op" composable function where each execution consumes a new virtual thread
        var po = EventEmitter.getInstance();
        for (int i=0; i < 16; i++) {
            po.send(new EventEnvelope().setTo("no.op").setBody(Map.of("type", "warm-up", "seq", i)));
        }
        log.info("Virtual thread optimization completed");
    }

    /**
     * This function renders REST endpoints
     */
    public static void renderRestEndpoints() {
        if (myInstance != null) {
            int cycle = compileCycle.incrementAndGet();
            if (cycle == 1) {
                log.info("Compiling REST endpoints");
            } else {
                log.info("Recompiling REST endpoints - Pass {}", cycle);
            }
            ConfigReader restConfig = myInstance.getRestConfig();
            RoutingEntry restRouting = RoutingEntry.getInstance();
            restRouting.load(restConfig);
        }
    }

    private void loadWebsocketServices(Class<?> cls, String namespace, String value) {
        Utility util = Utility.getInstance();
        List<String> parts = util.split(namespace + "/" + value, "/");
        StringBuilder sb = new StringBuilder();
        for (String p: parts) {
            sb.append('/');
            sb.append(p);
        }
        String path = sb.toString();
        String wsEndpoint = path + "/{handle}";
        try {
            Object o = cls.getDeclaredConstructor().newInstance();
            if (o instanceof LambdaFunction f) {
                wsLambdas.put(path, f);
                log.info("{} loaded as WEBSOCKET SERVER endpoint {}", cls.getName(), wsEndpoint);
            } else {
                log.error("Unable to load {} ({}) because it is not an instance of {}",
                        cls.getName(), wsEndpoint, LambdaFunction.class.getName());
            }

        } catch (InstantiationException | IllegalAccessException | NoSuchMethodException |
                 InvocationTargetException e) {
            log.error("Unable to load {} ({}) - {}", cls.getName(), wsEndpoint, e.getMessage());
        }
    }

    private ConfigReader getRestConfig() {
        Utility util = Utility.getInstance();
        AppConfigReader reader = AppConfigReader.getInstance();
        boolean rest = "true".equals(reader.getProperty("rest.automation", IS_FALSE));
        List<String> paths = util.split(reader.getProperty("yaml.rest.automation",
                "classpath:/rest.yaml"), ", ");
        Map<String, Boolean> uniqueKeys = new HashMap<>();
        Map<String, Map<String, Object>> allRestEntries = new HashMap<>();
        Map<String, Object> staticContentFilter = new HashMap<>();
        Map<String, Map<String, Object>> allCorsEntries = new HashMap<>();
        Map<String, Map<String, Object>> allHeaderEntries = new HashMap<>();
        for (String p: paths) {
            final ConfigReader config;
            try {
                config = new ConfigReader(p);
                log.info("Loading config from {}", p);
            } catch (IllegalArgumentException e) {
                if (rest) {
                    log.warn("Unable to load REST entries from {} - {}", p, e.getMessage());
                }
                continue;
            }
            // load configuration for static content filters, cors and headers
            if (config.get(STATIC_CONTENT) instanceof Map) {
                staticContentFilter.put(STATIC_CONTENT, config.get(STATIC_CONTENT));
            }
            Map<String, Map<String, Object>> cors = getUniqueEntries(config, p, true);
            Map<String, Map<String, Object>> headers = getUniqueEntries(config, p, false);
            checkDuplicates(cors, headers, p, uniqueKeys);
            allCorsEntries.putAll(cors);
            allHeaderEntries.putAll(headers);
            if (isDuplicatedRest(config, p, uniqueKeys)) {
                return new ConfigReader();
            }
            allRestEntries.putAll(getUniqueRestEntries(config, p));
        }
        // merge configuration files
        List<String> rList = new ArrayList<>(allRestEntries.keySet());
        Collections.sort(rList);
        List<String> cList = new ArrayList<>(allCorsEntries.keySet());
        Collections.sort(cList);
        List<String> hList = new ArrayList<>(allHeaderEntries.keySet());
        Collections.sort(hList);
        MultiLevelMap mm = new MultiLevelMap();
        for (int i=0; i < rList.size(); i++) {
            mm.setElement(REST_PREFIX+i+"]", allRestEntries.get(rList.get(i)));
        }
        for (int i=0; i < cList.size(); i++) {
            mm.setElement("cors["+i+"]", allCorsEntries.get(cList.get(i)));
        }
        for (int i=0; i < hList.size(); i++) {
            mm.setElement("headers["+i+"]", allHeaderEntries.get(hList.get(i)));
        }
        if (staticContentFilter.containsKey(STATIC_CONTENT)) {
            mm.setElement(STATIC_CONTENT, staticContentFilter.get(STATIC_CONTENT));
        }
        return new ConfigReader().load(mm.getMap());
    }

    private void checkDuplicates(Map<String, Map<String, Object>> cors, Map<String, Map<String, Object>> headers,
                                 String p, Map<String, Boolean> uniqueKeys) {
        for (String k: cors.keySet()) {
            if (uniqueKeys.containsKey(k)) {
                log.warn("Duplicated 'cors' in {} will override a prior one '{}'", p, k);
            } else {
                uniqueKeys.put(k, true);
            }
        }
        for (String k: headers.keySet()) {
            if (uniqueKeys.containsKey(k)) {
                log.warn("Duplicated 'headers' in {} will override a prior one '{}'", p, k);
            } else {
                uniqueKeys.put(k, true);
            }
        }
    }

    private boolean isDuplicatedRest(ConfigReader config, String p, Map<String, Boolean> uniqueKeys) {
        var util = Utility.getInstance();
        // load REST entries
        Map<String, Map<String, Object>> compositeEndpoints = getUniqueRestEntries(config, p);
        for (String composite: compositeEndpoints.keySet()) {
            int sep = composite.lastIndexOf('[');
            String ep = composite.substring(0, sep).trim();
            List<String> methods = util.split(composite.substring(sep), "[, ]");
            for (String m: methods) {
                String ref = m + " " + ep;
                if (uniqueKeys.containsKey(ref)) {
                    log.error("REST endpoint rendering aborted due to duplicated entry '{}' in {}", ref, p);
                    return true;
                } else {
                    uniqueKeys.put(ref, true);
                }
            }
        }
        return false;
    }

    @SuppressWarnings({"unchecked"})
    private Map<String, Map<String, Object>> getUniqueRestEntries(ConfigReader config, String path) {
        Map<String, Map<String, Object>> result = new HashMap<>();
        MultiLevelMap map = new MultiLevelMap(config.getMap());
        Object entries = map.getElement("rest");
        if (entries instanceof List<?> items) {
            for (int i=0; i < items.size(); i++) {
                Object m = map.getElement(REST_PREFIX+i+"]");
                if (m instanceof Map) {
                    Object uri = map.getElement(REST_PREFIX+i+"].url");
                    Object methods = map.getElement(REST_PREFIX+i+"].methods");
                    if (uri instanceof String && methods instanceof List) {
                        result.put(String.valueOf(uri)+' '+methods, (Map<String, Object>) m);
                    } else {
                        log.error("REST entry-{} in {} is invalid", path, i+1);
                    }
                }
            }
        }
        return result;
    }

    @SuppressWarnings({"unchecked"})
    private Map<String, Map<String, Object>> getUniqueEntries(ConfigReader config, String path, boolean cors) {
        String type = cors? "cors" : "headers";
        Map<String, Map<String, Object>> result = new HashMap<>();
        MultiLevelMap map = new MultiLevelMap(config.getMap());
        Object entries = map.getElement(type);
        if (entries instanceof List<?> items) {
            for (int i=0; i < items.size(); i++) {
                Object m = map.getElement(type+"["+i+"]");
                if (m instanceof Map) {
                    Object id = map.getElement(type+"["+i+"].id");
                    if (id instanceof String corId) {
                        result.put(corId, (Map<String, Object>) m);
                    } else {
                        var typeUpper = type.toUpperCase();
                        log.error("{} entry-{} in {} is invalid", typeUpper, path, i+1);
                    }
                }
            }
        }
        return result;
    }

    private static class PreLoadInfo {
        int instances;
        Set<String> routes;

        PreLoadInfo(int instances, Set<String> routes) {
            this.instances = instances;
            this.routes = routes;
        }
    }

    private record Housekeeper(ConcurrentMap<String, AsyncContextHolder> contexts) {

        private void removeExpiredConnections() {
            // check async context timeout
            if (!contexts.isEmpty()) {
                List<String> list = new ArrayList<>(contexts.keySet());
                long now = System.currentTimeMillis();
                for (String id : list) {
                    AsyncContextHolder holder = contexts.get(id);
                    long t1 = holder.lastAccess;
                    if (now - t1 > holder.timeout) {
                        log.warn("Async HTTP Context {} timeout for {} ms", id, now - t1);
                        SimpleHttpUtility httpUtil = SimpleHttpUtility.getInstance();
                        httpUtil.sendError(id, holder.request, 408,
                                "Timeout for " + (holder.timeout / 1000) + " seconds");
                    }
                }
            }
        }
    }

    public static HttpServerOptions getHttpServerOptions(boolean sslEnabled, String sslCertPath, String sslKeyPath) {
        if (!sslEnabled) {
            return new HttpServerOptions().setTcpKeepAlive(true);
        }
        HttpServerOptions httpServerOptions = new HttpServerOptions().setTcpKeepAlive(true).setSsl(true);
        if (sslCertPath.startsWith(CLASSPATH) && sslKeyPath.startsWith(CLASSPATH)) {
            httpServerOptions.setKeyCertOptions(buildKeyCertOptionsFromResource(sslCertPath, sslKeyPath));
        } else if (sslCertPath.startsWith(FILEPATH) && sslKeyPath.startsWith(FILEPATH)) {
            httpServerOptions.setKeyCertOptions(buildKeyCertOptionsFromFile(sslCertPath, sslKeyPath));
        } else {
            throw new IllegalArgumentException("SSL cert and key files must prefix with either classpath: or file:");
        }
        return httpServerOptions;
    }

    private static KeyCertOptions buildKeyCertOptionsFromResource(String sslCertPath, String sslKeyPath) {
        String sslCertResourcePath = sslCertPath.substring(CLASSPATH.length());
        String sslKeyResourcePath = sslKeyPath.substring(CLASSPATH.length());
        try(InputStream sslCertIn = AppStarter.class.getResourceAsStream(sslCertResourcePath);
            InputStream sslKeyIn = AppStarter.class.getResourceAsStream(sslKeyResourcePath)) {
            if (sslCertIn == null) {
                throw new IllegalArgumentException("Unable to load SSL certificate from resource %s".formatted(sslCertPath));
            }
            if (sslKeyIn == null) {
                throw new IllegalArgumentException("Unable to load SSL private key from resource %s".formatted(sslKeyPath));
            }
            Buffer sslCertBuff = Buffer.buffer(sslCertIn.readAllBytes());
            Buffer sslKeyBuff = Buffer.buffer(sslKeyIn.readAllBytes());
            return new PemKeyCertOptions().addCertValue(sslCertBuff).addKeyValue(sslKeyBuff);
        } catch (IOException e) {
            throw new IllegalArgumentException("SSL option initiation failed - %s".formatted(e.getMessage()));
        }
    }

    private static KeyCertOptions buildKeyCertOptionsFromFile(String sslCertPath, String sslKeyPath) {
        String sslCertFilePath = sslCertPath.substring(FILEPATH.length());
        String sslKeyFilePath = sslKeyPath.substring(FILEPATH.length());
        return new PemKeyCertOptions().addCertPath(sslCertFilePath).addKeyPath(sslKeyFilePath);
    }

    private static class PreLoadMetadata {
        List<String> routes;
        int instances;
    }
}
