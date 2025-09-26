package com.accenture;

import com.accenture.adapters.HttpToFlow;
import com.accenture.service.EchoEndpoint;
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

public class EnvInstanceOverrideTest extends TestBase {

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
        assertFalse(reader.exists(EchoEndpoint.ENV_INSTANCE_PROPERTY));

        assertEquals(
                200,
                routeToInstancesMap.get(HttpToFlow.ROUTE)
        );

        assertEquals(
                10,
                routeToInstancesMap.get(EchoEndpoint.ROUTE)
        );
    }
}
