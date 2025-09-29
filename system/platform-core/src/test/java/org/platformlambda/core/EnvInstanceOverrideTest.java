package org.platformlambda.core;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.platformlambda.common.TestBase;
import org.platformlambda.core.services.NoOpFunction;
import org.platformlambda.core.system.Platform;
import org.platformlambda.core.system.ServiceDef;
import org.platformlambda.core.util.AppConfigReader;

import java.util.Map;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
                Integer.parseInt(reader.getProperty(NoOpFunction.ENV_INSTANCE_PROPERTY)),
                routeToInstancesMap.get(NoOpFunction.ROUTE)
        );
    }

}
