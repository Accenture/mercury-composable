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

 package com.accenture;

import com.accenture.adapters.HttpToFlow;
import com.accenture.services.Resilience4Flow;
import com.accenture.services.SimpleExceptionHandler;
import com.accenture.setup.TestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.platformlambda.core.system.Platform;
import org.platformlambda.core.system.ServiceDef;
import org.platformlambda.core.util.AppConfigReader;

import java.util.Map;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class EnvInstanceOverrideTest extends TestBase {

    private AppConfigReader reader;
    private Map<String, Integer> routeToInstancesMap;

    @BeforeEach
    void setupTest(){
        Platform platform = Platform.getInstance();
        this.reader = AppConfigReader.getInstance();

        ConcurrentMap<String, ServiceDef> serviceDefinitions =  platform.getLocalRoutingTable();
        this.routeToInstancesMap = serviceDefinitions.entrySet().stream()
                .collect(Collectors.toMap(
                                Map.Entry::getKey,
                                e -> e.getValue().getConcurrency()
                        )
                );
    }


    @Test
    void shouldReplaceInstancesFromOverride(){
        assertEquals(
                Integer.parseInt(reader.getProperty(Resilience4Flow.ENV_INSTANCE_PROPERTY)),
                routeToInstancesMap.get(Resilience4Flow.ROUTE)
        );

        assertEquals(
                Integer.parseInt(reader.getProperty(SimpleExceptionHandler.ENV_INSTANCE_PROPERTY)),
                routeToInstancesMap.get(SimpleExceptionHandler.ROUTE)
        );
    }

    @Test
    void shouldNotReplaceInstanceIfNoOverride(){
        assertFalse(reader.exists(HttpToFlow.ENV_INSTANCE_PROPERTY));

        assertEquals(
                200,
                routeToInstancesMap.get(HttpToFlow.ROUTE)
        );

    }
}
