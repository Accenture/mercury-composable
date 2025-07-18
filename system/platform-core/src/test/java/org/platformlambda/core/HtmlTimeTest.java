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

package org.platformlambda.core;

import org.junit.jupiter.api.Test;
import org.platformlambda.core.util.Utility;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HtmlTimeTest {

    @Test
    void conversionTest() {
        Utility util = Utility.getInstance();
        Date now = new Date();
        String timestampWithoutMilli = util.date2str(now, true);
        Date normalizedTime = util.str2date(timestampWithoutMilli);
        String timestamp = util.getHtmlDate(normalizedTime);
        Date converted = util.getHtmlDate(timestamp);
        assertEquals(normalizedTime, converted);
    }
}
