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

package com.accenture.services.plugins.types;

import com.accenture.util.TypeConversionUtils;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * A null operand renders as the text "null" ({@code String.valueOf} semantics —
 * the behavior of the pre-plugin built-ins and of the Rust engine's
 * {@code get_text_value}). The text/binary conversions are pattern switches,
 * which are null-hostile without an explicit case null: a flow feeding an
 * unset model variable into f:concat used to throw NPE instead.
 */
class TextValueNullHandlingTest {

    @Test
    void nullOperandConcatenatesAsText() {
        assertEquals("anullb", new ConcatenateStringsPlugin().calculate("a", null, "b"));
        assertEquals("nullnull", new ConcatenateStringsPlugin().calculate(null, null));
    }

    @Test
    void textConversionOfNullYieldsText() {
        assertEquals("null", TypeConversionUtils.getTextValue(null));
        assertEquals("null", new TextConversion().calculate((Object) null));
    }

    @Test
    void binaryConversionOfNullYieldsEmptyArray() {
        // for byte arrays a null operand relaxes to an empty array, not "null" text
        assertArrayEquals(new byte[0], TypeConversionUtils.getBinaryValue(null));
    }
}
