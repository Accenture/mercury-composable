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

package org.platformlambda.discovery;

import com.accenture.adapters.FlowExecutor;
import org.platformlambda.core.annotations.MainApplication;
import org.platformlambda.core.models.EntryPoint;
import org.platformlambda.core.system.AutoStart;
import org.platformlambda.core.system.Platform;
import org.platformlambda.core.system.PostOffice;
import org.platformlambda.core.util.AppConfigReader;
import org.platformlambda.core.util.Utility;
import org.platformlambda.discovery.services.ContractCatalog;
import org.platformlambda.discovery.services.SkillSnapshot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Mercury AI contract provider - a standalone composable app.
 * <p>
 * Default mode is a read-only REST discovery server (rest.server.port, default 8999) whose
 * endpoints are wired rest.yaml -> Event Script flow -> function. With
 * {@code --export <directory>} it performs one offline skill export through the same
 * export-skill flow and exits.
 */
@MainApplication
public class AiContractProvider implements EntryPoint {
    private static final Logger log = LoggerFactory.getLogger(AiContractProvider.class);

    private static final String EXPORT_FLAG = "--export";
    private static final String EXPORT_FLOW = "export-skill";
    private static final String EXPORT_CALLBACK = "skill.export.callback";
    private static final long EXPORT_TIMEOUT_SECONDS = 30;

    public static void main(String[] args) {
        AutoStart.main(args);
    }

    @Override
    public void start(String[] args) throws Exception {
        var snapshot = SkillSnapshot.getInstance();
        assertConsistentAssembly(snapshot.getMercuryVersion(), snapshot.getEventScriptVersion());
        var contracts = ContractCatalog.getInstance().getContracts();
        log.info("Mercury {} operational contract ready - {} contracts, {} snapshot files",
                snapshot.getMercuryVersion(), contracts.size(), snapshot.getFiles().size());
        var exportDirectory = exportDirectory(args);
        if (exportDirectory != null) {
            System.exit(exportSkill(exportDirectory));
        } else {
            var port = AppConfigReader.getInstance().getProperty("rest.server.port", "8999");
            log.info("AI discovery endpoints ready - start with GET http://127.0.0.1:{}/api/discovery", port);
        }
    }

    /** The framework modules must come from the same Mercury release - refuse a mixed assembly. */
    static void assertConsistentAssembly(String platformCore, String eventScript) {
        if (!platformCore.equals(eventScript)) {
            throw new IllegalStateException("Mixed Mercury assembly - platform-core "
                    + platformCore + " but event-script-engine " + eventScript);
        }
    }

    private static String exportDirectory(String[] args) {
        for (int i = 0; i < args.length; i++) {
            if (EXPORT_FLAG.equals(args[i])) {
                if (i + 1 >= args.length || args[i + 1].isBlank()) {
                    throw new IllegalArgumentException("Usage: --export <existing-directory>");
                }
                return args[i + 1];
            }
        }
        return null;
    }

    /** One-shot CLI export through the export-skill flow; returns the process exit code. */
    private int exportSkill(String directory) {
        var platform = Platform.getInstance();
        var result = new CompletableFuture<Map<String, Object>>();
        platform.registerPrivate(EXPORT_CALLBACK, (headers, input, instance) -> {
            result.complete(input instanceof Map<?, ?> map ? asStringKeyedMap(map) : Map.of());
            return true;
        }, 1);
        try {
            var util = Utility.getInstance();
            var cid = util.getUuid();
            var po = new PostOffice("cli.export", cid, "EXPORT /skill");
            var dataset = new HashMap<String, Object>();
            dataset.put("headers", Map.of());
            dataset.put("body", Map.of("directory", directory));
            FlowExecutor.getInstance().launch(po, EXPORT_FLOW, dataset, EXPORT_CALLBACK, cid);
            var output = result.get(EXPORT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (output.get("skill_directory") instanceof String skillDirectory) {
                log.info("Mercury platform skill exported to {} ({} files, snapshot {})",
                        skillDirectory, output.get("files"), output.get("snapshot_sha256"));
                return 0;
            }
            log.error("Skill export failed - {}", output.get("message") == null
                    ? output : output.get("message"));
            return 1;
        } catch (Exception e) {
            log.error("Skill export failed - {}", e.getMessage());
            return 1;
        } finally {
            platform.release(EXPORT_CALLBACK);
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> asStringKeyedMap(Map<?, ?> map) {
        return (Map<String, Object>) map;
    }
}
