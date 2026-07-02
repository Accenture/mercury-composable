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

package com.accenture.automation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SimpleTypeMatchingConverterTest {
    private final SimpleTypeMatchingConverter converter = SimpleTypeMatchingConverter.getInstance();

    @Test
    void convertsSimpleTypesOnLhs() {
        assertEquals("f:text(model.someKey) -> output.body.x",
                converter.convert("model.someKey:text -> output.body.x"));
        assertEquals("f:binary(model.someKey) -> output.body.x",
                converter.convert("model.someKey:binary -> output.body.x"));
        assertEquals("f:int(model.someKey) -> output.body.x",
                converter.convert("model.someKey:int -> output.body.x"));
        assertEquals("f:uuid(model.someKey) -> output.body.x",
                converter.convert("model.someKey:uuid -> output.body.x"));
        assertEquals("f:b64(model.someKey) -> output.body.x",
                converter.convert("model.someKey:b64 -> output.body.x"));
        assertEquals("f:length(model.someKey) -> output.body.x",
                converter.convert("model.someKey:length -> output.body.x"));
    }

    @Test
    void convertsSimpleTypesOnRhs() {
        assertEquals("f:int(input.path_parameter.userid) -> model.userid",
                converter.convert("input.path_parameter.userid -> model.userid:int"));
    }

    @Test
    void convertsNegate() {
        assertEquals("f:not(model.bool) -> negate_value",
                converter.convert("model.bool:! -> negate_value"));
    }

    @Test
    void convertsAndOr() {
        assertEquals("f:and(model.positive, model.negative) -> output.body.and",
                converter.convert("model.positive:and(model.negative) -> output.body.and"));
        assertEquals("f:or(model.positive, model.negative) -> output.body.or",
                converter.convert("model.positive:or(model.negative) -> output.body.or"));
    }

    @Test
    void convertsSubstring() {
        assertEquals("f:substring(model.text, int(0), int(5)) -> substring",
                converter.convert("model.text:substring(0, 5) -> substring"));
        assertEquals("f:substring(model.text, int(6)) -> substring2",
                converter.convert("model.text:substring(6) -> substring2"));
    }

    @Test
    void convertsConcat() {
        assertEquals("f:concat(model.a, model.b, text(,), model.c) -> concat_string",
                converter.convert("model.a:concat(model.b, text(,), model.c) -> concat_string"));
    }

    @Test
    void convertsBooleanValueMatch() {
        assertEquals("f:eq(text(hello), text(hello)) -> model.positive",
                converter.convert("text(hello) -> model.positive:boolean(hello=true)"));
        assertEquals("f:ne(text(hello), text(hello)) -> model.negative",
                converter.convert("text(hello) -> model.negative:boolean(hello=false)"));
        assertEquals("f:eq(model.n:text, text(3)) -> model.running",
                converter.convert("model.n:boolean(3=true) -> model.running"));
        assertEquals("f:ne(model.n:text, text(3)) -> model.running",
                converter.convert("model.n:boolean(3=false) -> model.running"));
        // tolerate optional whitespace around the parenthesis, matching legacy behavior
        assertEquals("f:isNull(model.none) -> none_is_true",
                converter.convert("model.none:boolean (null = true) -> none_is_true"));
        assertEquals("f:notNull(model.none) -> none_is_false",
                converter.convert("model.none:boolean (null=false) -> none_is_false"));
    }

    @Test
    void leavesUnrecognizedOrMalformedSyntaxUnconverted() {
        String unknown = "model.text:unknown -> no-change";
        assertEquals(unknown, converter.convert(unknown));
        String unbalanced = "model.text:substring(0, 10 -> keep-as-text";
        assertEquals(unbalanced, converter.convert(unbalanced));
    }

    @Test
    void leavesPlainMappingUnconverted() {
        String plain = "model.pojo -> data";
        assertEquals(plain, converter.convert(plain));
    }
}
