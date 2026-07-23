package com.accenture.services.plugins.collection;


import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GetLastOperatorTest {

    private final GetLastOperator operator = new GetLastOperator();

    static Stream<Arguments> validInputs() {
        return Stream.of(
                Arguments.of(List.of("a", "b", "c"), "c"),
                Arguments.of(List.of(1, 2, 3), 3),
                Arguments.of(List.of("single"), "single"));
    }

    @ParameterizedTest
    @MethodSource("validInputs")
    void shouldReturnLastItemFromList(Object input, Object expected) {
        assertEquals(expected, operator.calculate(input));
    }

    static Stream<Arguments> invalidInputs() {
        return Stream.of(
                Arguments.of(
                        new Object[]{},
                        "One input is required to get last item from list"
                ),
                Arguments.of(
                        new Object[]{"a", "b"},
                        "One input is required to get last item from list"
                ),
                Arguments.of(
                        new Object[]{null},
                        "Input cannot be null to get last item from list"
                ),
                Arguments.of(
                        new Object[]{List.of()},
                        "Input cannot be empty to get last item from list"
                ),
                Arguments.of(
                        new Object[]{42},
                        "Input must be a list to get last item"
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
