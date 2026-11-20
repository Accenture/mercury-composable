package org.platformlambda.discovery.services;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The snapshot fails closed on a packaged page that links a file the skill does not package. The failure must name
 * the page, the target and the remedy - it is what a contributor sees when a new report or guide is linked before
 * it is packaged, and (through getInstance) what every test touching the class reports.
 */
class SkillSnapshotLinkTest {

    @Test
    void aBrokenLinkNamesThePageTheTargetAndTheRemedy() {
        var files = Map.of("references/guides/a-guide.md",
                "See [the report](../test-reports/not-packaged.md).".getBytes(StandardCharsets.UTF_8));
        var error = assertThrows(IllegalArgumentException.class, () -> SkillSnapshot.validateLinks(files));
        assertTrue(error.getMessage().contains(
                "Broken relative link in references/guides/a-guide.md -> ../test-reports/not-packaged.md"),
                error.getMessage());
        assertTrue(error.getMessage().contains("skill/files.list"), "the message says how to fix it: " + error.getMessage());
    }

    @Test
    void aLinkInsideTheSnapshotAnchorsAndExternalUrisPass() {
        var files = Map.of(
                "references/guides/a-guide.md",
                ("[a report](../test-reports/packaged.md#section) [here](#top) [site](https://example.test/x) "
                        + "[mail](mailto:someone@example.test)").getBytes(StandardCharsets.UTF_8),
                "references/test-reports/packaged.md", "# packaged".getBytes(StandardCharsets.UTF_8));
        assertDoesNotThrow(() -> SkillSnapshot.validateLinks(files));
    }
}
