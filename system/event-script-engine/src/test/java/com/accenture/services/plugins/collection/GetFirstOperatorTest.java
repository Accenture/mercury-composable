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
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GetFirstOperatorTest {

    private final GetFirstOperator operator = new GetFirstOperator();

    static Stream<Arguments> validInputs() {
        return Stream.of(
                Arguments.of(List.of("a", "b", "c"), "a"),
                Arguments.of(List.of(1, 2, 3), 1),
                Arguments.of(List.of("single"), "single"));
    }

    @ParameterizedTest
    @MethodSource("validInputs")
    void shouldReturnFirstItemFromList(Object input, Object expected) {
        assertEquals(expected, operator.calculate(input));
    }

    static Stream<Arguments> invalidInputs() {
        return Stream.of(
                Arguments.of(
                        new Object[]{},
                        "One input is required to get first item from list"
                ),
                Arguments.of(
                        new Object[]{"a", "b"},
                        "One input is required to get first item from list"
                ),
                Arguments.of(
                        new Object[]{null},
                        "Input cannot be null to get first item from list"
                ),
                Arguments.of(
                        new Object[]{List.of()},
                        "Input cannot be empty to get first item from list"
                ),
                Arguments.of(
                        new Object[]{42},
                        "Input must be a list to get first item"
                ));
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
