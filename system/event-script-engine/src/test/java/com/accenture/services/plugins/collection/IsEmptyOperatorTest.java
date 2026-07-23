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

package com.accenture.services.plugins.collection;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class IsEmptyOperatorTest {

    private final IsEmptyOperator operator = new IsEmptyOperator();

    static Stream<Arguments> emptyValues() {
        return Stream.of(
                Arguments.of(List.of(), true),
                Arguments.of(List.of("foo"), false),
                Arguments.of(Map.of(), true),
                Arguments.of(Map.of("k", "v"), false),
                Arguments.of("", true),
                Arguments.of("hello", false),
                Arguments.of(new String[]{}, true),
                Arguments.of(new String[]{"hello"}, false),
                Arguments.of(new int[]{}, true),
                Arguments.of(new int[]{1, 2, 3}, false));
    }

    @ParameterizedTest
    @MethodSource("emptyValues")
    void shouldCheckIfValueIsEmpty(Object input, boolean expected) {
        assertEquals(expected, operator.calculate(input));
    }

    static Stream<Arguments> invalidInputs() {
        return Stream.of(
                Arguments.of(
                        new Object[]{},
                        "One input is required to check if value is empty"),
                Arguments.of(
                        new Object[]{"a", "b"},
                        "One input is required to check if value is empty"),
                Arguments.of(
                        new Object[]{null},
                        "Input cannot be null to check if value is empty"),
                Arguments.of(
                        new Object[]{123},
                        "Unsupported input type to check if value is empty: java.lang.Integer"));
    }

    @ParameterizedTest
    @MethodSource("invalidInputs")
    void shouldThrowExceptionForInvalidInput(Object[] input, String expectedMessage) {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> operator.calculate(input));

        assertEquals(expectedMessage, exception.getMessage());
    }
}
