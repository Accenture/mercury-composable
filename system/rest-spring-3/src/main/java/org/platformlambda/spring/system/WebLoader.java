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

package org.platformlambda.spring.system;

import io.github.classgraph.ClassInfo;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.FilterRegistration;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletRegistration;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.annotation.WebInitParam;
import jakarta.servlet.annotation.WebListener;
import jakarta.servlet.annotation.WebServlet;
import org.platformlambda.core.util.Feature;
import org.platformlambda.core.util.SimpleClassScanner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.servlet.ServletContextInitializer;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class WebLoader implements ServletContextInitializer {
    private static final Logger log = LoggerFactory.getLogger(WebLoader.class);

    @Override
    public void onStartup(ServletContext context) {
        SimpleClassScanner scanner = SimpleClassScanner.getInstance();
        Set<String> packages = scanner.getPackages();
        var totalServlets = new AtomicInteger(0);
        var totalFilters = new AtomicInteger(0);
        var totalListeners = new AtomicInteger(0);
        for (String p : packages) {
            List<ClassInfo> servletEndpoints = scanner.getAnnotatedClasses(p, WebServlet.class);
            loadWebServlets(servletEndpoints, context, totalServlets);
            List<ClassInfo> webFilterEndpoints = scanner.getAnnotatedClasses(p, WebFilter.class);
            loadWebFilters(webFilterEndpoints, context, totalFilters);
            List<ClassInfo> webListenerEndpoints = scanner.getAnnotatedClasses(p, WebListener.class);
            loadWebListeners(webListenerEndpoints, context, totalListeners);
        }
        if (totalServlets.get() > 0) {
            log.info("Total {} WebServlet{} registered", totalServlets.get(), totalServlets.get() == 1 ? "" : "s");
        }
        if (totalFilters.get() > 0) {
            log.info("Total {} WebFilter{} registered", totalFilters.get(), totalFilters.get() == 1 ? "" : "s");
        }
        if (totalListeners.get() > 0) {
            log.info("Total {} WebListener{} registered", totalListeners.get(), totalListeners.get() == 1 ? "" : "s");
        }
    }

    private void loadWebServlets(List<ClassInfo> servletEndpoints, ServletContext context, AtomicInteger total) {
        for (ClassInfo info : servletEndpoints) {
            final Class<?> cls = getClass("WebServlet", info.getName());
            if (cls != null) {
                WebServlet servlet = cls.getAnnotation(WebServlet.class);
                if (Feature.isRequired(cls)) {
                    ServletRegistration.Dynamic dynamic = context.addServlet(cls.getSimpleName(), cls.getName());
                    String[] urls = servlet.value().length == 0? servlet.urlPatterns() : servlet.value();
                    if (urls.length > 0) {
                        updateDynamicServlet(cls, servlet, dynamic, urls, total);
                    } else {
                        log.error("WebServlet {} is missing value or urlPatterns", cls.getName());
                    }
                }
            }
        }
    }

    private void updateDynamicServlet(Class<?> cls, WebServlet servlet,
                                      ServletRegistration.Dynamic dynamic, String[] urls, AtomicInteger total) {
        dynamic.addMapping(urls);
        dynamic.setAsyncSupported(servlet.asyncSupported());
        WebInitParam[] params = servlet.initParams();
        for (WebInitParam kv: params) {
            dynamic.setInitParameter(kv.name(), kv.value());
        }
        if (servlet.loadOnStartup() > 0) {
            dynamic.setLoadOnStartup(servlet.loadOnStartup());
        }
        int count = total.incrementAndGet();
        log.info("{} registered as WebServlet-{} {}{}{}", cls.getName(), count, Arrays.asList(urls),
                servlet.asyncSupported()? " with async support" : "",
                servlet.loadOnStartup() > 0? ", start up sequence "+servlet.loadOnStartup() : "");
    }

    private void loadWebFilters(List<ClassInfo> webFilterEndpoints, ServletContext context, AtomicInteger total) {
        for (ClassInfo info : webFilterEndpoints) {
            final Class<?> cls = getClass("WebFilter", info.getName());
            if (cls != null) {
                WebFilter filter = cls.getAnnotation(WebFilter.class);
                if (Feature.isRequired(cls)) {
                    FilterRegistration.Dynamic dynamic = context.addFilter(cls.getSimpleName(), cls.getName());
                    String[] urls = filter.value().length == 0? filter.urlPatterns() : filter.value();
                    if (urls.length > 0) {
                        List<DispatcherType> dispatcherTypes = Arrays.asList(filter.dispatcherTypes());
                        dynamic.addMappingForUrlPatterns(EnumSet.copyOf(dispatcherTypes), true, urls);
                        var count = total.incrementAndGet();
                        log.info("{} registered as WebFilter-{} {}", cls.getName(), count, Arrays.asList(urls));
                    } else {
                        log.error("WebFilter {} is missing value or urlPatterns", cls.getName());
                    }
                }
            }
        }
    }

    private void loadWebListeners(List<ClassInfo> webListenerEndpoints, ServletContext context, AtomicInteger total) {
        for (ClassInfo info : webListenerEndpoints) {
            final Class<?> cls = getClass("WebListener", info.getName());
            if (cls != null) {
                if (Feature.isRequired(cls)) {
                    context.addListener(cls.getName());
                    var count = total.incrementAndGet();
                    log.info("{} registered as WebListener-{}", cls.getName(), count);
                }
            }
        }
    }

    private Class<?> getClass(String type, String clazz) {
        try {
            return Class.forName(clazz);
        } catch (ClassNotFoundException e) {
            log.error("Unable to deploy {} {} - {}", type, clazz, e.getMessage());
            return null;
        }
    }
}
