package com.accenture.minigraph.rest;

import org.platformlambda.core.annotations.PreLoad;
import org.platformlambda.core.models.AsyncHttpRequest;
import org.platformlambda.core.models.TypedLambdaFunction;
import org.platformlambda.core.serializers.SimpleMapper;
import org.platformlambda.core.util.AppConfigReader;
import org.platformlambda.core.util.Utility;

import java.io.File;
import java.util.Map;

@PreLoad(route = "show.graph.model")
public class DescribeGraph implements TypedLambdaFunction<AsyncHttpRequest, Object> {
    private final File tempDir;

    public DescribeGraph() {
        var config = AppConfigReader.getInstance();
        var location = config.getProperty("location.graph.temp", "/tmp/graph");
        this.tempDir = new File(location);
    }

    @Override
    public Object handleEvent(Map<String, String> headers, AsyncHttpRequest input, int instance) {
        var filename = input.getPathParameter("graph_id");
        if (filename == null) {
            throw new IllegalArgumentException("Missing path parameter 'graph_id'");
        }
        var file = new File(tempDir, filename + ".json");
        if (file.exists()) {
            var text = Utility.getInstance().file2str(file);
            return SimpleMapper.getInstance().getMapper().readValue(text, Map.class);
        } else {
            throw new IllegalArgumentException(String.format("Draft graph '%s' does not exist", filename));
        }
    }
}
