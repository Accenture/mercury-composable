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

package org.platformlambda.csv.flow;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.platformlambda.core.models.TypedLambdaFunction;
import org.platformlambda.core.system.AutoStart;
import org.platformlambda.core.system.Platform;
import org.platformlambda.core.util.Utility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CsvFlowTest {
    private static final Logger log = LoggerFactory.getLogger(CsvFlowTest.class);

    private static final String SAMPLE_DATA_CSV = "sample-data.csv";

    @BeforeAll
    static void setup() {
        AutoStart.main(new String[0]);
    }

    @Test
    void fileStagingAreaTest() throws IOException {
        final BlockingQueue<Map<String, Object>> bench = new ArrayBlockingQueue<>(10);
        TypedLambdaFunction<Map<String, Object>, Boolean> dataReceiver =
                (headers, input, instance) -> {
            bench.add(input);
            return true;
        };
        Platform.getInstance().registerPrivate("test.data.receiver", dataReceiver, 1);
        Utility util = Utility.getInstance();
        // put sample CSV data file into staging area
        try (InputStream in = this.getClass().getResourceAsStream("/data/"+SAMPLE_DATA_CSV)) {
            String text = util.stream2str(in);
            File staging = new File("/tmp/staging");
            File archive = new File("/tmp/archive");
            if (!staging.exists()) {
                if (staging.mkdirs()) {
                    log.info("Staging area {} created", staging);
                }
            }
            if (!archive.exists()) {
                if (archive.mkdirs()) {
                    log.info("Archive area {} created", archive);
                }
            }
            util.cleanupDir(archive, true);
            util.str2file(new File(staging, SAMPLE_DATA_CSV), text);
            for (int i=0; i < 3; i++) {
                Map<String, Object> row = bench.poll(10, TimeUnit.SECONDS);
                Assertions.assertNotNull(row);
                assertEquals(3, row.size());
                assertTrue(row.containsKey("address"));
                assertTrue(row.containsKey("telephone"));
                assertTrue(row.containsKey("name"));
                log.info("Flow completed - got {}", row);
            }
            assertTrue(waitForCsvCompletion());
            util.cleanupDir(archive, true);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private boolean waitForCsvCompletion() throws InterruptedException {
        File archive = new File("/tmp/archive");
        File f = new File(archive, SAMPLE_DATA_CSV+".done");
        for (int i=0; i < 10; i++) {
            if (f.exists()) {
                return true;
            }
            Thread.sleep(1000);
        }
        return false;
    }
}
