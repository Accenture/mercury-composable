// Scaffolded from Mercury Composable's starter templates (https://github.com/Accenture/mercury-composable, Apache-2.0)

package com.accenture.starter;

import org.platformlambda.core.annotations.MainApplication;
import org.platformlambda.core.models.EntryPoint;
import org.platformlambda.core.system.AutoStart;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@MainApplication
public class MainApp implements EntryPoint {
    private static final Logger log = LoggerFactory.getLogger(MainApp.class);

    public static void main(String[] args) {
        AutoStart.main(args);
    }

    @Override
    public void start(String[] args) {
        // one-time application setup goes here; the CompileGraph gate has already
        // validated and compiled the manifest-listed graph models at startup
        log.info("Started");
    }
}
