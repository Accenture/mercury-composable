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

package com.accenture.minigraph.start;

import com.accenture.minigraph.annotations.FetchFeature;
import com.accenture.minigraph.common.FeatureDef;
import com.accenture.minigraph.common.FeatureRunner;
import io.github.classgraph.ClassInfo;
import org.platformlambda.core.annotations.MainApplication;
import org.platformlambda.core.models.EntryPoint;
import org.platformlambda.core.system.AutoStart;
import org.platformlambda.core.util.SimpleClassScanner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@MainApplication(sequence = 8)
public class PlaygroundLoader implements EntryPoint {
    private static final Logger log = LoggerFactory.getLogger(PlaygroundLoader.class);
    private static final ConcurrentMap<String, FeatureDef> features = new ConcurrentHashMap<>();

    public static void main(String[] args) {
        AutoStart.main(args);
    }

    @Override
    public void start(String[] args) {
        SimpleClassScanner scanner = SimpleClassScanner.getInstance();
        Set<String> packages = scanner.getPackages();
        for (String p : packages) {
            prepareFeatures(p);
        }
        log.info("Started");
    }

    public static FeatureDef getFeature(String feature) {
        return features.get(feature);
    }

    private void prepareFeatures(String eachPackage) {
        SimpleClassScanner scanner = SimpleClassScanner.getInstance();
        List<ClassInfo> services = scanner.getAnnotatedClasses(eachPackage, FetchFeature.class);
        for (ClassInfo info : services) {
            try {
                Class<?> cls = Class.forName(info.getName());
                FetchFeature feature = cls.getAnnotation(FetchFeature.class);
                Class<?> featureClass = Class.forName(info.getName());
                Object o = featureClass.getDeclaredConstructor().newInstance();
                if (o instanceof FeatureRunner runner) {
                    features.put(feature.value(), new FeatureDef(runner));
                    log.info("Class {} loaded as API fetcher feature '{}'", o.getClass().getName(), feature.value());
                } else {
                    log.error("Did you forget to implement FetchFeature interface for {}?", o.getClass().getName());
                }
            } catch (ClassNotFoundException e) {
                log.error("Class {} not found", info.getName());
            } catch (Exception e) {
                log.error("FetchFeature {} cannot be instantiated - {}", info.getName(), e.getMessage());
            }
        }
    }
}
