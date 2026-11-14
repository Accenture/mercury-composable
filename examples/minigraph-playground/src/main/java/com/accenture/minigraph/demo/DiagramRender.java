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

package com.accenture.minigraph.demo;

import org.platformlambda.core.annotations.PreLoad;
import org.platformlambda.core.exception.AppException;
import org.platformlambda.core.models.EventEnvelope;
import org.platformlambda.core.models.TypedLambdaFunction;
import org.platformlambda.core.system.PostOffice;
import org.platformlambda.core.util.AppConfigReader;
import org.platformlambda.core.util.Utility;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * The diagram-assistant graph's tool, {@code diagram.render}: a function of this application in front of the
 * MCP-bound route {@code diagram.render.mcp}, which the MCP binding of a language pack serves by calling a
 * Mermaid MCP server.
 * <p>
 * The server answers the verdict together with the rendered image, and an agent loop returns a tool's
 * whole reply to the model. This function keeps the payload out of band: it calls the binding, stores the
 * SVG under a reference id in the folder {@code diagram.store.folder} (default {@code /tmp/rendered_diagrams},
 * the file named {@code <reference>.svg}) beside an HTML page, {@code <reference>.html}, that inlines the SVG
 * so a browser renders it from the file system - the server's SVG starts with a {@code <style>} element and
 * uses {@code foreignObject} labels, which an {@code <img>} tag may refuse - and returns the verdict, the code
 * and the reference, never the image. The model reads a small result and reports the reference; a person
 * opens the page. An invalid diagram stores nothing and returns the server's error for the model to fix.
 */
@PreLoad(route = "diagram.render", instances = 10)
public class DiagramRender implements TypedLambdaFunction<Map<String, Object>, Map<String, Object>> {
    static final String MCP_ROUTE = "diagram.render.mcp";
    static final String FOLDER_KEY = "diagram.store.folder";
    static final String DEFAULT_FOLDER = "/tmp/rendered_diagrams";
    private static final long TIMEOUT_MS = 25000;
    private static final String DIAGRAM_CODE = "diagramCode";
    private static final String RAW_SVG = "rawSVG";

    @Override
    public Map<String, Object> handleEvent(Map<String, String> headers, Map<String, Object> input, int instance)
            throws Exception {
        if (!(input.get(DIAGRAM_CODE) instanceof String code) || code.isBlank()) {
            throw new AppException(400, DIAGRAM_CODE + " is required");
        }
        var po = new PostOffice(headers, instance);
        EventEnvelope reply = po.asyncRequest(new EventEnvelope().setTo(MCP_ROUTE).setBody(input), TIMEOUT_MS)
                .await(TIMEOUT_MS, TimeUnit.MILLISECONDS);
        if (reply.hasError()) {
            throw new AppException(reply.getStatus(), String.valueOf(reply.getError()));
        }
        if (!(reply.getBody() instanceof Map<?, ?> rendered)) {
            throw new AppException(502, MCP_ROUTE + " answered outside the tool contract");
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("valid", Boolean.TRUE.equals(rendered.get("valid")));
        Object renderedCode = rendered.get(DIAGRAM_CODE);
        result.put(DIAGRAM_CODE, renderedCode == null ? code : renderedCode);
        if (rendered.get("title") != null) {
            result.put("title", rendered.get("title"));
        }
        if (rendered.get("error") != null) {
            result.put("error", rendered.get("error"));
        }
        if (Boolean.TRUE.equals(rendered.get("valid")) && rendered.get(RAW_SVG) instanceof String svg && !svg.isBlank()) {
            String reference = Utility.getInstance().getUuid();
            File folder = new File(AppConfigReader.getInstance().getProperty(FOLDER_KEY, DEFAULT_FOLDER));
            if (!folder.exists() && !folder.mkdirs()) {
                throw new AppException(500, "Unable to create " + folder);
            }
            File file = new File(folder, reference + ".svg");
            if (!Utility.getInstance().str2file(file, svg)) {
                throw new AppException(500, "Unable to write " + file);
            }
            File page = new File(folder, reference + ".html");
            String title = rendered.get("title") instanceof String t && !t.isBlank() ? t : reference;
            if (!Utility.getInstance().str2file(page, page(title, reference, code, svg))) {
                throw new AppException(500, "Unable to write " + page);
            }
            result.put("reference", reference);
            result.put("file", file.getAbsolutePath());
            result.put("page", page.getAbsolutePath());
            result.put("bytes", file.length());
        }
        return result;
    }

    /**
     * The HTML page beside the SVG: the title, the reference, the Mermaid code and the inlined image.
     */
    static String page(String title, String reference, String code, String svg) {
        return "<!DOCTYPE html>\n<html lang=\"en\">\n<head>\n<meta charset=\"utf-8\">\n<title>" + escape(title) + "</title>\n"
                + "<style>\n"
                + "  body { font-family: -apple-system, Helvetica, Arial, sans-serif; margin: 2rem; color: #222; background: #fafafa; }\n"
                + "  h1 { font-size: 1.3rem; }\n"
                + "  .meta { color: #666; font-size: .85rem; font-family: Menlo, monospace; }\n"
                + "  .diagram { background: #fff; border: 1px solid #ddd; border-radius: 8px; padding: 1rem; max-width: 960px; overflow: auto; }\n"
                + "  .diagram svg { width: 100%; height: auto; }\n"
                + "  pre { background: #f3f3f3; border-radius: 6px; padding: .8rem 1rem; max-width: 960px; overflow: auto; font-size: .85rem; }\n"
                + "</style>\n</head>\n<body>\n"
                + "<h1>" + escape(title) + "</h1>\n"
                + "<p class=\"meta\">reference " + reference + " &middot; <a href=\"" + reference + ".svg\">" + reference + ".svg</a>"
                + " &middot; rendered by the diagram assistant through its MCP-bound tool</p>\n"
                + "<div class=\"diagram\">" + svg + "</div>\n"
                + "<h2>Mermaid code</h2>\n<pre>" + escape(code) + "</pre>\n"
                + "</body>\n</html>\n";
    }

    private static String escape(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
}
