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

package org.platformlambda.adapter.flow.services;

import com.accenture.adapters.FlowExecutor;
import org.platformlambda.core.models.EventEnvelope;
import org.platformlambda.core.util.MultiLevelMap;
import org.platformlambda.core.util.Utility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

public class CsvFlowAdapter extends Thread {
    private static final Logger log = LoggerFactory.getLogger(CsvFlowAdapter.class);

    private static final String FILE_PROTOCOL = "file:/";
    private boolean running = true;
    private final String folder;
    private final String flowId;
    private final String archive;

    public CsvFlowAdapter(String folder, String archive, String flowId) {
        this.folder = folder.substring(FILE_PROTOCOL.length()-1);
        this.archive = archive.substring(FILE_PROTOCOL.length()-1);
        this.flowId = flowId;
        Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown));
    }

    @Override
    public void run() {
        Utility util = Utility.getInstance();
        File archiveDir = new File(archive);
        File dataDir = new File(folder);
        if (dataDir.exists() && dataDir.isDirectory() && archiveDir.exists() && archiveDir.isDirectory()) {
            log.info("Flow adapter for {} started", folder);
            while (running) {
                List<File> files = getCsvFiles(dataDir);
                for (File f: files) {
                    // This is a demo to illustrate the concept and it handles only simple case of CSV encoding.
                    // (The CSV is created by Excel's CSV export feature)
                    //
                    // In your production code, you should use a proper CSV parser.
                    String content = util.file2str(f);
                    List<String> lines = util.split(content, "\r\n");
                    List<String> headers = new ArrayList<>();
                    int count = 0;
                    int processed = 0;
                    int failed = 0;
                    boolean firstLine = true;
                    for (String line: lines) {
                        if (firstLine) {
                            firstLine = false;
                            // filter out Unicode BOM control character if any
                            String text = (int) line.charAt(0) > 127? line.substring(1) : line;
                            headers.addAll(getHeaders(text));
                        } else {
                            count++;
                            try {
                                EventEnvelope result = executeFlow(count, getRecord(headers, line), f, archiveDir);
                                if (result.hasError()) {
                                    failed++;
                                } else {
                                    processed++;
                                }
                            } catch (Exception e) {
                                log.error("Unable to process row {} of {} - {}", count, f, e.getMessage());
                            }
                        }
                    }
                    if (processed == count) {
                        saveToArchive(archiveDir, f, "done");
                        log.warn("Processed {} rows from {}", processed, f);
                    } else {
                        saveToArchive(archiveDir, f, "error");
                        log.warn("Processed {}, failed {} from {}", processed, failed, f);
                    }
                }
                /*
                 * scan every 5 seconds
                 *
                 * In this example, we are using a polling method.
                 * In production, you should set up file creation event listener.
                 */
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    // ok to ignore
                }
            }
            log.info("Stopped");
        } else {
            log.error("Flow Adapter not installed - please ensure {} and {} exist", dataDir, archiveDir);
        }
    }

    private List<String> getHeaders(String line) {
        Utility util = Utility.getInstance();
        List<String> labels = new ArrayList<>();
        List<String> segments = util.split(line, ",");
        for (String s: segments) {
            labels.add(trimQuote(s.trim()));
        }
        return labels;
    }

    private Map<String, String> getRecord(List<String> headers, String line) {
        Utility util = Utility.getInstance();
        Map<String, String> result = new HashMap<>();
        List<String> segments = util.split(line, ",");
        int len = segments.size();
        for (int i=0; i < headers.size(); i++) {
            String label = headers.get(i);
            if (i < len) {
                String value = trimQuote(segments.get(i).trim());
                result.put(label, value);
            }
        }
        return result;
    }

    private String trimQuote(String text) {
        var item = text.endsWith("\"")? text.substring(0, text.length()-1) : text;
        return item.startsWith("\"")? item.substring(1) : item;
    }

    private List<File> getCsvFiles(File dir) {
        List<File> result = new ArrayList<>();
        File[] files = dir.listFiles();
        if (files != null) {
            for (File f: files) {
                if (!f.isDirectory() && f.getName().endsWith(".csv")) {
                    result.add(f);
                }
            }
        }
        return result;
    }

    private EventEnvelope executeFlow(int rowNumber, Map<String, String> data, File f, File archiveDir)
            throws IOException, ExecutionException, InterruptedException {
        var util = Utility.getInstance();
        var originator = "csv.flow.adapter";
        // the traceId and correlationId should come from upstream
        // for this example, they are generated randomly
        var cid = util.getUuid();
        var traceId = util.getUuid();
        var tracePath = "CSV /"+f.getName();
        var dataset = new MultiLevelMap();
        // the dataset for a flow must contain a "body"
        // header and metadata are optional
        dataset.setElement("body", data);
        dataset.setElement("header.filename", f.getName());
        dataset.setElement("header.row", rowNumber);
        return FlowExecutor.getInstance()
                .request(originator, traceId, tracePath, flowId, dataset.getMap(), cid, 10000).get();
    }

    private void saveToArchive(File archiveDir, File f, String ext) {
        String filename = f.getName() + "." + ext;
        File target = new File(archiveDir, filename);
        try {
            if (target.exists()) {
                Files.delete(target.toPath());
            }
            Files.move(f.toPath(), target.toPath());
        } catch (IOException e) {
            log.error("Unable to archive {} - {}", target, e.getMessage());
        }
    }

    private void shutdown() {
        running = false;
    }
}
