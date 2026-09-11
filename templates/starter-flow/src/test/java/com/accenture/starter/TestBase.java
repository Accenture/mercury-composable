// Scaffolded from Mercury Composable's starter templates (https://github.com/Accenture/mercury-composable, Apache-2.0)

package com.accenture.starter;

import org.junit.jupiter.api.BeforeAll;
import org.platformlambda.core.system.AutoStart;
import org.platformlambda.core.util.AppConfigReader;

import java.util.concurrent.atomic.AtomicInteger;

public class TestBase {
    private static final AtomicInteger startCounter = new AtomicInteger(0);
    protected static String host;

    @BeforeAll
    static void setup() {
        // start the application once for the whole test run
        if (startCounter.incrementAndGet() == 1) {
            AppConfigReader config = AppConfigReader.getInstance();
            host = "http://127.0.0.1:" + config.getProperty("rest.server.port", "8302");
            AutoStart.main(new String[0]);
        }
    }
}
