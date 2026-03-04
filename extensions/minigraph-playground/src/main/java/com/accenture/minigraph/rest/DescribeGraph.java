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

package com.accenture.minigraph.rest;

import org.platformlambda.core.annotations.PreLoad;
import org.platformlambda.core.models.AsyncHttpRequest;
import org.platformlambda.core.models.TypedLambdaFunction;
import org.platformlambda.core.serializers.SimpleMapper;
import org.platformlambda.core.util.AppConfigReader;
import org.platformlambda.core.util.Utility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Map;

@PreLoad(route = "show.graph.model")
public class DescribeGraph implements TypedLambdaFunction<AsyncHttpRequest, Object> {
    private static final Logger log = LoggerFactory.getLogger(DescribeGraph.class);
    private static final String FILE_NAMESPACE = "file:";
    private static final String CLASSPATH_NAMESPACE = "classpath:";
    private final File tempDir;

    public DescribeGraph() {
        var config = AppConfigReader.getInstance();
        var location = config.getProperty("location.graph.temp", "/tmp/graph");
        if (location.startsWith(CLASSPATH_NAMESPACE)) {
            location = "/tmp/graph";
            log.error("location.graph.temp must use 'file:/' namespace. Revert to default /tmp/graph");
        }
        this.tempDir = new File(location.startsWith(FILE_NAMESPACE) ?
                                location.substring(FILE_NAMESPACE.length()) : location);
    }

    @Override
    public Object handleEvent(Map<String, String> headers, AsyncHttpRequest input, int instance) {
        var filename = input.getPathParameter("graph_id");
        if (filename == null) {
            throw new IllegalArgumentException("Missing path parameter 'graph_id'");
        }
        System.out.println(tempDir.getAbsolutePath());

        var file = new File(tempDir, filename + ".json");
        System.out.println("Loading graph from " + file.getAbsolutePath());
        if (file.exists()) {
            var text = Utility.getInstance().file2str(file);
            return SimpleMapper.getInstance().getMapper().readValue(text, Map.class);
        } else {
            throw new IllegalArgumentException(String.format("Draft graph '%s' does not exist", filename));
        }
    }
}
