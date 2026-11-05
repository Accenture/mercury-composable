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

import org.junit.jupiter.api.Test;
import org.platformlambda.core.models.AsyncHttpRequest;
import org.platformlambda.core.util.Utility;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The Playground page's panel name: {@code playground.panel.name} renames the Minigraph panel by filling the page's
 * empty panel-name tag, and without it the page is served as the bundle built it.
 */
class GetIndexHtmlTest {
    private static final String PAGE = "<head>\n<title>Minigraph Playground</title>\n" +
            GetIndexHtml.PANEL_NAME_TAG + "\n</head>";

    @Test
    void theBuiltPageCarriesTheEmptyPanelNameTagOnce() {
        var page = Utility.getInstance().stream2str(getClass().getResourceAsStream("/template/playground.html"));
        assertEquals(page.indexOf(GetIndexHtml.PANEL_NAME_TAG), page.lastIndexOf(GetIndexHtml.PANEL_NAME_TAG));
        assertTrue(page.contains(GetIndexHtml.PANEL_NAME_TAG), "rebuild the webapp bundle (npm run release)");
    }

    @Test
    void aNameFillsTheTagEscapedForTheAttribute() {
        var page = GetIndexHtml.withPanelName(PAGE, "  R&D <dev> \"blue\" 'team'  ");
        assertTrue(page.contains("<meta name=\"playground-panel-name\" " +
                "content=\"R&amp;D &lt;dev&gt; &quot;blue&quot; &#39;team&#39;\">"));
        assertFalse(page.contains(GetIndexHtml.PANEL_NAME_TAG));
        // the browser tab keeps its title
        assertTrue(page.contains("<title>Minigraph Playground</title>"));
    }

    @Test
    void noNameOrABlankOneLeavesThePageAsBuilt() {
        assertSame(PAGE, GetIndexHtml.withPanelName(PAGE, null));
        assertSame(PAGE, GetIndexHtml.withPanelName(PAGE, "   "));
    }

    @Test
    void aPageWithoutTheTagIsServedUnchanged() {
        var page = "<head><title>Minigraph Playground</title></head>";
        assertSame(page, GetIndexHtml.withPanelName(page, "My Awesome Dev Dashboard"));
    }

    @Test
    void thePropertyRenamesThePanelOfTheServedPage() {
        // app.env=dev in the test configuration, so the function serves the Playground page
        System.setProperty(GetIndexHtml.PANEL_NAME, "My Awesome Dev Dashboard");
        try {
            var renamed = (String) new GetIndexHtml().handleEvent(Map.of(), new AsyncHttpRequest(), 1).getBody();
            assertTrue(renamed.contains("<meta name=\"playground-panel-name\" content=\"My Awesome Dev Dashboard\">"));
        } finally {
            System.clearProperty(GetIndexHtml.PANEL_NAME);
        }
        var plain = (String) new GetIndexHtml().handleEvent(Map.of(), new AsyncHttpRequest(), 1).getBody();
        assertTrue(plain.contains(GetIndexHtml.PANEL_NAME_TAG));
    }
}
