package com.accenture.automation;

import io.github.classgraph.ClassInfo;
import org.platformlambda.core.annotations.BeforeApplication;
import org.platformlambda.core.annotations.SimplePlugin;
import org.platformlambda.core.models.EntryPoint;
import org.platformlambda.core.models.PluginFunction;
import org.platformlambda.core.system.EventEmitter;
import org.platformlambda.core.system.Platform;
import org.platformlambda.core.util.SimpleClassScanner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Set;

/**
 * Loads Simple Plugins for use in Event-Script, should be loaded before CompileFlows
 */
@BeforeApplication(sequence = 3)
public class SimplePluginLoader implements EntryPoint {
    private static final Logger log = LoggerFactory.getLogger(SimplePluginLoader.class);

    @Override
    public void start(String[] args) {
        preloadSimplePlugins();
    }

    protected void preloadSimplePlugins() {
        log.info("Preloading Plugins started");

        SimpleClassScanner scanner = SimpleClassScanner.getInstance();
        Set<String> packages = scanner.getPackages(true);
        for (String p : packages) {
            scanPackageForPlugins(scanner, p);
        }
        log.info("Preloading Plugins completed");
    }




    protected void scanPackageForPlugins(SimpleClassScanner scanner, String pkg){
        Platform platform = Platform.getInstance();
        List<ClassInfo> services = scanner.getAnnotatedClasses(pkg, SimplePlugin.class);

        for(ClassInfo info: services){
            String serviceName = info.getName();
            log.info("Loading Plugin {}", serviceName);

            try {
                Class<?> cls = Class.forName(serviceName);
                Object o = cls.getDeclaredConstructor().newInstance();

                if(o instanceof PluginFunction macro){
                    platform.registerSimpleMacro(macro.getName(), macro);
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
    }

}

