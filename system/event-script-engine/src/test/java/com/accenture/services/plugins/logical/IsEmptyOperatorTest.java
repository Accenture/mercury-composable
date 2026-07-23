package com.accenture.services.plugins.logical;

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

    @ParameterizedTest
    @MethodSource("invalidInputs")
    void shouldThrowExceptionForInvalidInput(Object[] input, String expectedMessage) {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> operator.calculate(input));

        assertEquals(expectedMessage, exception.getMessage());
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
}
