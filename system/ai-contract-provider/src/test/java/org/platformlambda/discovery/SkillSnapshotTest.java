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

import org.junit.jupiter.api.Test;
import org.platformlambda.core.exception.AppException;
import org.platformlambda.discovery.services.SkillSnapshot;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.error.YAMLException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SkillSnapshotTest {

    /**
     * files.list is the checked inventory of the packaged guide closure. This test walks the
     * real docs tree, so ADDING a file under docs/guides fails here with a one-line fix:
     * add the path to skill/files.list (the pom packages docs/guides/** automatically).
     */
    @Test
    void inventoryEqualsTheDocumentationClosure() throws IOException {
        var docs = reactorRoot().resolve("docs");
        var expected = new TreeSet<String>();
        expected.add("SKILL.md");
        expected.add("security.json");
        expected.add("references/index.md");
        expected.add("references/arch-decisions/ADR.md");
        expected.add("references/arch-decisions/RFC.md");
        expected.add("references/test-reports/event-over-http-interop.md");
        expected.add("references/test-reports/llm-helper-certification.md");
        expected.add("references/test-reports/otel-dynatrace-certification.md");
        expected.add("references/test-reports/progressive-rendering-interop.md");
        expected.add("references/test-reports/streaming-return-route-cross-pod.md");
        expected.add("references/fixtures/rest-bindings.yaml");
        try (var paths = Files.walk(docs.resolve("guides"))) {
            paths.filter(Files::isRegularFile).forEach(path ->
                    expected.add("references/" + docs.relativize(path).toString().replace('\\', '/')));
        }
        assertEquals(new ArrayList<>(expected), SkillSnapshot.getInstance().inventory(),
                "skill/files.list must equal the docs/guides closure plus the fixed extras");
    }

    @Test
    void renderedSnapshotExpandsIncludesAndResolvesEveryLink() {
        // render() fails closed on an unexpanded include or a broken relative link,
        // so a successful load already proves both; assert the observable outcomes too
        var files = SkillSnapshot.getInstance().getFiles();
        assertTrue(files.containsKey(SkillSnapshot.INSTALLED_CONTRACTS));
        var developerGuide = new String(files.get("references/guides/ai-developer-guide.md"),
                StandardCharsets.UTF_8);
        assertFalse(developerGuide.contains("--8<--"), "mkdocs include must be expanded");
        assertTrue(developerGuide.contains("service: 'http.flow.adapter'"),
                "the corrected flow binding example must be embedded");
    }

    @Test
    void manifestHashesRecompute() {
        var snapshot = SkillSnapshot.getInstance();
        var manifest = snapshot.getManifest();
        assertEquals("mercury-platform-skill", manifest.get("type"));
        assertEquals(System.getProperty("mercury.version.under.test"),
                manifest.get("mercury_version"),
                "served mercury_version must match the reactor version under test");
        @SuppressWarnings("unchecked")
        var entries = (List<Map<String, Object>>) manifest.get("files");
        assertEquals(snapshot.getFiles().size(), entries.size());
        var rebuilt = new StringBuilder();
        for (Map<String, Object> entry : entries) {
            var path = (String) entry.get("path");
            var expected = SkillSnapshot.sha256(snapshot.getFiles().get(path));
            assertEquals(expected, entry.get("sha256"), path);
            rebuilt.append(path).append('\n').append(expected).append('\n');
        }
        assertEquals(SkillSnapshot.sha256(rebuilt.toString().getBytes(StandardCharsets.UTF_8)),
                manifest.get("snapshot_sha256"));
    }

    @Test
    void readFileServesOnlyExactInventoryMembers() {
        var snapshot = SkillSnapshot.getInstance();
        var skill = snapshot.readFile("SKILL.md");
        assertEquals("text/markdown", skill.get("type"));
        assertTrue(String.valueOf(skill.get("content")).contains("name: mercury-platform"));
        for (String attempt : new String[]{null, "", "../pom.xml",
                "references/../../secrets", "manifest.json", "no/such/file.md"}) {
            var e = assertThrows(AppException.class, () -> snapshot.readFile(attempt));
            assertEquals(404, e.getStatus());
        }
    }

    /**
     * Every documentation page's YAML front matter must parse. MkDocs renders a page whose front
     * matter fails to parse WITH the metadata as body text, and {@code mkdocs build --strict} says
     * nothing about it - the graph-contract page shipped so in 4.12.22, over one colon-space inside
     * an unquoted summary. The rule: a page that opens with {@code ---} closes the block with
     * {@code ---} or {@code ...}, the block is a YAML map with a non-blank {@code title}, and every
     * page under docs/guides (the pages the contract serves) carries such a block.
     */
    @Test
    void everyDocumentationPageFrontMatterParses() throws IOException {
        var docs = reactorRoot().resolve("docs");
        var pages = new ArrayList<Path>();
        try (var paths = Files.walk(docs)) {
            paths.filter(p -> p.toString().endsWith(".md")).sorted().forEach(pages::add);
        }
        var problems = new ArrayList<String>();
        var parsed = 0;
        for (var page : pages) {
            var rel = docs.relativize(page).toString().replace('\\', '/');
            var problem = frontMatterProblem(page, rel.startsWith("guides/"));
            if (problem == null) {
                parsed++;
            } else {
                problems.add(rel + ": " + problem);
            }
        }
        assertEquals(List.of(), problems, "front matter must parse as YAML (see the method javadoc)");
        assertTrue(parsed >= 50, "the walk must have seen the documentation tree, saw " + parsed);
    }

    /** null when the page is fine; otherwise one line naming what is wrong. */
    static String frontMatterProblem(Path page, boolean required) throws IOException {
        var lines = Files.readAllLines(page, StandardCharsets.UTF_8);
        if (lines.isEmpty() || !lines.getFirst().strip().equals("---")) {
            return required ? "a guide page needs YAML front matter" : null;
        }
        var end = -1;
        for (int i = 1; i < lines.size(); i++) {
            var line = lines.get(i).strip();
            if (line.equals("---") || line.equals("...")) {
                end = i;
                break;
            }
        }
        if (end < 0) {
            return "the front matter never closes";
        }
        Object meta;
        try {
            meta = new Yaml().load(String.join("\n", lines.subList(1, end)));
        } catch (YAMLException e) {
            return "invalid YAML - " + String.valueOf(e.getMessage()).lines().findFirst().orElse("");
        }
        if (!(meta instanceof Map<?, ?> map)) {
            return "the front matter is not a map";
        }
        if (!(map.get("title") instanceof String title) || title.isBlank()) {
            return "the front matter has no title";
        }
        return null;
    }

    static Path reactorRoot() {
        // on a module-level run, maven.multiModuleProjectDirectory is the module itself,
        // so only trust the property when it actually holds the docs tree
        var configured = System.getProperty("mercury.reactor.root");
        if (configured != null) {
            var root = Path.of(configured);
            if (Files.isDirectory(root.resolve("docs/guides"))) {
                return root;
            }
        }
        var current = Path.of(System.getProperty("user.dir")).toAbsolutePath().normalize();
        while (current != null && !Files.isDirectory(current.resolve("docs/guides"))) {
            current = current.getParent();
        }
        if (current == null) {
            throw new IllegalStateException("Mercury reactor root not found");
        }
        return current;
    }
}
