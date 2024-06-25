package com.accenture.setup;

import org.junit.BeforeClass;
import org.platformlambda.core.system.AppStarter;
import org.platformlambda.core.util.AppConfigReader;

import java.util.concurrent.atomic.AtomicInteger;

public class TestBase {

    private static final AtomicInteger startCounter = new AtomicInteger(0);

    protected static String HOST;

    @BeforeClass
    public static void setup() {
        if (startCounter.incrementAndGet() == 1) {
            AppConfigReader config = AppConfigReader.getInstance();
            HOST = "http://127.0.0.1:" + config.getProperty("server.port", "8100");
            AppStarter.main(new String[0]);
        }
    }

}
