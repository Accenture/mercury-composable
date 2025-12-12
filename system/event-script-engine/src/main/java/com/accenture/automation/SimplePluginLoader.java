package com.accenture.automation;

import com.accenture.models.SimplePlugin;
import com.accenture.utils.RecursiveClassTypeExaminer;
import com.accenture.utils.SimplePluginUtils;
import io.github.classgraph.ClassInfo;
import org.objectweb.asm.*;
import org.platformlambda.core.annotations.BeforeApplication;
import org.platformlambda.core.models.EntryPoint;
import com.accenture.models.PluginFunction;
import org.platformlambda.core.util.SimpleClassScanner;

import org.platformlambda.core.util.Utility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Loads Simple Plugins for use in Event-Script, should be loaded before CompileFlows
 */
@BeforeApplication(sequence = 3)
public class SimplePluginLoader implements EntryPoint {
    private static final Logger log = LoggerFactory.getLogger(SimplePluginLoader.class);

    private static final ConcurrentMap<String, PluginFunction> simplePluginRegistry = new ConcurrentHashMap<>();

    private static final Set<String> PRIMITIVE_TYPES = Stream.of(Integer.TYPE, Void.TYPE, Boolean.TYPE, Byte.TYPE,
                                                             Character.TYPE, Short.TYPE, Double.TYPE, Float.TYPE,
                                                            Long.TYPE)
                                                        .map(Type::getType)
                                                        .map(Type::getClassName)
                                                        .collect(Collectors.toSet());
    private static final Set<String> ALLOWED_PACKAGES = Set.of("java.lang", "java.util", "java.math", "java.time",
            SimplePlugin.class.getName(),
            PluginFunction.class.getName(),
            SimplePluginUtils.class.getName());

    /**
     * Internal API that returns loaded Plugins
     *
     * @return registry of all loaded plugins
     */
    public static ConcurrentMap<String, PluginFunction> getLoadedSimplePlugins() {
        return simplePluginRegistry;
    }

    public static PluginFunction getSimplePluginByName(String pluginName){
        return getLoadedSimplePlugins().get(pluginName);
    }

    public static boolean containsSimplePlugin(String pluginName){
        return simplePluginRegistry.containsKey(pluginName);
    }

    @Override
    public void start(String[] args) {
        preloadSimplePlugins();
    }

    protected void preloadSimplePlugins() {
        log.info("Preloading Plugins started");

        SimpleClassScanner scanner = SimpleClassScanner.getInstance();
        Set<String> packages = scanner.getPackages(true);
        for (String p : packages) {
            registerPlugins(scanner, p);
        }
        log.info("Preloading Plugins completed");
    }


    protected void analyzeClass(String className, Set<String> allTypes, Set<String> visitedClasses){
        // Skip if already visited
        if (visitedClasses.contains(className)) {
            return;
        }

        // Skip java.lang to prevent deep recursion
        if (className.startsWith("java.lang.")) {
            visitedClasses.add(className);
            return;
        }

        visitedClasses.add(className);

        try {
            ClassReader reader = new ClassReader(className);
            RecursiveClassTypeExaminer visitor = new RecursiveClassTypeExaminer();
            reader.accept(visitor, ClassReader.SKIP_CODE);

            // Add types from this class
            allTypes.addAll(visitor.getTypes());

            // Recursively analyze superclass
            if (visitor.getSuperClass() != null) {
                analyzeClass(visitor.getSuperClass(), allTypes, visitedClasses);
            }

            // Recursively analyze interfaces
            for (String iface : visitor.getInterfaces()) {
                analyzeClass(iface, allTypes, visitedClasses);
            }

        } catch (IOException e) {
            System.err.println("Warning: Could not load class: " + className);
        }
    }

    protected Set<String> getUsedTypes(ClassInfo clazz){
        Set<String> allTypes = new HashSet<>();
        Set<String> visitedClasses = new HashSet<>();

        analyzeClass(clazz.getName(), allTypes, visitedClasses);

        return allTypes;
    }

    /**
     * Determines whether we should register plugin. This method is designed to be future-proof.
     * Currently, the only restriction is based on the type of packages included.
     * @param clazz The clazz we are introspecting
     * @return true if we should register this plugin, false otherwise
     */
    protected boolean shouldRegisterPlugin(ClassInfo clazz){
        Set<String> types = getUsedTypes(clazz);
        var blacklisted = types.stream()
                .filter(s -> ALLOWED_PACKAGES.stream().noneMatch(s::startsWith))
                .filter(s -> PRIMITIVE_TYPES.stream().noneMatch(s::equalsIgnoreCase))
                .collect(Collectors.toSet());

        if(! blacklisted.isEmpty()){
            log.warn("Found blacklisted classes {} when registering plugin {}", blacklisted, clazz.getName());
        }

        return blacklisted.isEmpty();
    }


    /**
     * Register a SimplePlugin that implements the PluggableFunction interface
     *
     * @param plugin The class implementing the plugin, containing the name `f:<name>`
     * @throws IllegalArgumentException when name of plugin is not provided
     */
    protected void registerSimplePlugin(PluginFunction plugin) {
        if (plugin == null) {
            throw new IllegalArgumentException("Missing Plugin to assign");
        }

        var name = plugin.getName();

        if (simplePluginRegistry.containsKey(name)) {
            log.warn("Reloading SimplePlugin {}", name);
        }

        // save into local registry
        simplePluginRegistry.put(name, plugin);
    }


    protected void registerPlugins(SimpleClassScanner scanner, String pkg){
        var plugins = scanPackageForPlugins(scanner, pkg);
        plugins.forEach(this::registerSimplePlugin);
    }


    protected List<PluginFunction> scanPackageForPlugins(SimpleClassScanner scanner, String pkg){
        List<ClassInfo> services = scanner.getAnnotatedClasses(pkg, SimplePlugin.class);

        List<PluginFunction> pluginFunctions = new LinkedList<>();

        for(ClassInfo info: services){
            String serviceName = info.getName();
            log.info("Loading Plugin {}", serviceName);

            if(! shouldRegisterPlugin(info)){
                log.warn("Skipping SimplePlugin {} - utilizes blacklisted types", serviceName);
                continue;
            }

            try {
                Class<?> cls = Class.forName(serviceName);
                Object o = cls.getDeclaredConstructor().newInstance();

                if(o instanceof PluginFunction plugin){
                    pluginFunctions.add(plugin);
                }
                else {
                    log.error("Unable to preload SimplePlugin {} - {} must implement {}", serviceName, o.getClass(),
                            PluginFunction.class.getSimpleName());
                }
            } catch (ClassNotFoundException | InvocationTargetException | InstantiationException |
                     IllegalAccessException | NoSuchMethodException | IllegalArgumentException e) {
                log.error("Unable to load SimplePlugin {} - {}", serviceName, e.getMessage());
            }
        }

        return pluginFunctions;
    }

}

