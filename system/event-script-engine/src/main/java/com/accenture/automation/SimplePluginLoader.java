package com.accenture.automation;

import com.accenture.Test;
import io.github.classgraph.ClassInfo;
import org.objectweb.asm.*;
import org.platformlambda.core.annotations.BeforeApplication;
import org.platformlambda.core.annotations.SimplePlugin;
import org.platformlambda.core.models.EntryPoint;
import org.platformlambda.core.models.PluginFunction;
import org.platformlambda.core.system.EventEmitter;
import org.platformlambda.core.system.Platform;
import org.platformlambda.core.util.SimpleClassScanner;

import org.platformlambda.core.util.Utility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Loads Simple Plugins for use in Event-Script, should be loaded before CompileFlows
 */
@BeforeApplication(sequence = 3)
public class SimplePluginLoader implements EntryPoint {
    private static final Logger log = LoggerFactory.getLogger(SimplePluginLoader.class);

    private static final Set<String> PRIMITIVE_TYPES = Stream.of(Integer.TYPE, Void.TYPE, Boolean.TYPE, Byte.TYPE,
                                                             Character.TYPE, Short.TYPE, Double.TYPE, Float.TYPE,
                                                            Long.TYPE)
                                                        .map(Type::getType)
                                                        .map(Type::getClassName)
                                                        .collect(Collectors.toSet());
    private static final Set<String> ALLOWED_PACKAGES = Set.of("java.lang", "java.util", "java.math", "com.accenture", SimplePlugin.class.getName(), PluginFunction.class.getName());

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


    protected Set<String> getUsedTypes(ClassInfo clazz){
        InputStream in = SimplePluginLoader.class.getClassLoader().getResourceAsStream(clazz.getResource().getPath());
        byte[] classBytes = Utility.getInstance().stream2bytes(in);

        Set<String> usedTypes = new HashSet<>();
        ClassReader cr = new ClassReader(classBytes);
        cr.accept(new ClassVisitor(Opcodes.ASM9) {

            @Override
            public void visit(int version, int access, String name, String signature, String superName, String[] interfaces){
                if (superName != null && !superName.equals("java/lang/Object")) { // Keep checking the parent for types
                    usedTypes.add(superName.replace('/', '.'));
                }

                if (interfaces != null) {
                    for (String i : interfaces) {
                        usedTypes.add(i.replace('/', '.'));
                    }
                }

                super.visit(version, access, name, signature, superName, interfaces);
            }

            @Override
            public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
                usedTypes.add(Type.getType(descriptor).getClassName());
                return super.visitField(access, name, descriptor, signature, value);
            }
            @Override
            public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
                Type methodType = Type.getMethodType(descriptor);
                usedTypes.add(methodType.getReturnType().getClassName());
                for (Type argType : methodType.getArgumentTypes()) {
                    usedTypes.add(argType.getClassName());
                }
                return super.visitMethod(access, name, descriptor, signature, exceptions);
            }
        }, 0);
        return usedTypes;
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
            log.warn("Found blacklisted classes when registering plugin: {}", blacklisted);
        }

        return blacklisted.isEmpty();
    }



    protected void scanPackageForPlugins(SimpleClassScanner scanner, String pkg){
        Platform platform = Platform.getInstance();
        List<ClassInfo> services = scanner.getAnnotatedClasses(pkg, SimplePlugin.class);

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

