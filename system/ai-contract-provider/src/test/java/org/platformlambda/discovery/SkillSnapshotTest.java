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
        expected.add("references/test-reports/event-over-http-interop.md");
        expected.add("references/test-reports/progressive-rendering-interop.md");
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

    static Path reactorRoot() {
        // on a module-level run, maven.multiModuleProjectDirectory is the module itself,
        // so only trust the property when it actually holds the docs tree
        var configured = System.getProperty("mercury.reactor.root");
        if (configured != null && Files.isDirectory(Path.of(configured).resolve("docs/guides"))) {
            return Path.of(configured);
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
