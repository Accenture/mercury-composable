package org.platformlambda.helpers.registry;

import org.platformlambda.core.annotations.MainApplication;
import org.platformlambda.core.models.EntryPoint;
import org.platformlambda.core.system.AutoStart;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Standalone Schema Registry dev server. Runs as a {@link MainApplication} on the platform-core runtime.
 * Provides a lightweight mock of the Confluent Schema Registry REST API for local development and testing,
 * satisfying both Avro and JSON Schema clients without any heavy external dependencies.
 */
@MainApplication
public class MainApp implements EntryPoint {
    private static final Logger log = LoggerFactory.getLogger(MainApp.class);

    public static void main(String[] args) {
        AutoStart.main(args);
    }

    @Override
    public void start(String[] args) {
        log.info("Minimalist Schema Registry Mock started. Ready to accept schemas.");
    }
}
