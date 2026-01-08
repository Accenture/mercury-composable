package com.accenture.setup;

import org.junit.jupiter.api.BeforeAll;
import org.platformlambda.core.system.AppStarter;
import org.platformlambda.core.util.AppConfigReader;

public class TestBase {

    protected static String HOST;

    @BeforeAll
    public static void setup() {
        AppConfigReader config = AppConfigReader.getInstance();
        HOST = "http://127.0.0.1:" + config.getProperty("server.port", "8100");
        AppStarter.main(new String[0]);
    }

}
