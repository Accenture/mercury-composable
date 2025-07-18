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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StringConversionTest {

    private static final Utility util = Utility.getInstance();

    private static final String INPUT = "hello world 012345678901234567890123456789012345678901234567890123456789";

    @Test
    void base64Test() {
        String base64 = util.bytesToBase64(INPUT.getBytes(), true, false);
        // verify that it is a pretty-print output
        assertTrue(base64.contains("\r\n"));
        byte[] b = util.base64ToBytes(base64);
        assertEquals(INPUT, new String(b));
    }

    @Test
    void hexTest() {
        String hexString = util.bytes2hex(INPUT.getBytes());
        byte[] b = util.hex2bytes(hexString);
        assertEquals(INPUT, new String(b));
    }

    @Test
    void normalizeUtcTimestamp() {
        Utility util = Utility.getInstance();
        String expected = "2020-07-09T01:02:03.123Z";
        String timestamp = "2020-07-09T01:02:03.12345678Z";
        assertEquals(expected, util.date2str(util.str2date(timestamp)));
        expected = "2020-07-09T01:02:03.120Z";
        timestamp = "2020-07-09T01:02:03.12Z";
        assertEquals(expected, util.date2str(util.str2date(timestamp)));
        expected = "2020-07-09T01:02:03.100Z";
        timestamp = "2020-07-09T01:02:03.1Z";
        assertEquals(expected, util.date2str(util.str2date(timestamp)));
        expected = "2020-07-09T01:02:03Z";
        timestamp = "2020-07-09T01:02:03.000Z";
        assertEquals(expected, util.date2str(util.str2date(timestamp)));
    }
}
