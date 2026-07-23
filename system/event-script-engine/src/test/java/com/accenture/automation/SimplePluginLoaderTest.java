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

import com.accenture.models.PluginFunction;
import com.accenture.services.plugins.arithmetic.*;
import com.accenture.services.plugins.generators.DateGenerator;
import com.accenture.services.plugins.generators.Now;
import com.accenture.services.plugins.generators.UUIDGenerator;
import com.accenture.services.plugins.logical.*;
import com.accenture.services.plugins.types.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Supplier;
import java.util.stream.Stream;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class SimplePluginLoaderTest {
    private static final Logger log = LoggerFactory.getLogger(SimplePluginLoaderTest.class);
    private static final int WARMUP_ITERATIONS = 200;
    private static final int MEASURE_ITERATIONS = 20;

    private void doWarmup(Supplier<Object> func) {
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            func.get();
        }
    }

    public static Stream<Arguments> plugins() {
        Random random = new Random();
        byte[] randomBytes = new byte[32];
        random.nextBytes(randomBytes);
        return Stream.of(
                Arguments.of(new AddNumbers(), new Object[]{2,2}),
                Arguments.of(new IncrementNumbers(), new Object[]{5}),
                Arguments.of(new DecrementNumbers(), new Object[]{5}),
                Arguments.of(new SubtractNumbers(), new Object[]{5,4}),
                Arguments.of(new MultiplyNumbers(), new Object[]{5,5}),
                Arguments.of(new DivideNumbers(), new Object[]{10,5}),
                Arguments.of(new ModulusNumbers(), new Object[]{10,5}),
                Arguments.of(new RoundNumbers(), new Object[]{2.345, 2}),
                Arguments.of(new DateGenerator(), new Object[]{}),
                Arguments.of(new UUIDGenerator(), new Object[]{}),
                Arguments.of(new EqualsOperator(), new Object[]{5,5}),
                Arguments.of(new NotEqualsOperator(), new Object[]{5,6}),
                Arguments.of(new GreaterThanOperator(), new Object[]{5,3}),
                Arguments.of(new LessThanOperator(), new Object[]{5,7}),
                Arguments.of(new IsNullOperator(), new Object[]{null}),
                Arguments.of(new IsNotNullOperator(), new Object[]{5}),
                Arguments.of(new LogicalConjunction(), new Object[]{true, true}),
                Arguments.of(new LogicalDisjunction(), new Object[]{false, true}),
                Arguments.of(new LogicalNegation(), new Object[]{false}),
                Arguments.of(new TernaryOperator(), new Object[]{true, 1, 2}),
                Arguments.of(new Base64Conversion(), new Object[]{Base64.getEncoder().encodeToString(randomBytes)}),
                Arguments.of(new BinaryConversion(), new Object[]{randomBytes}),
                Arguments.of(new BooleanConversion(), new Object[]{"true"}),
                Arguments.of(new DoubleConversion(), new Object[]{"1.8"}),
                Arguments.of(new FloatConversion(), new Object[]{"1.7"}),
                Arguments.of(new IntegerConversion(), new Object[]{"4"}),
                Arguments.of(new LongConversion(), new Object[]{"7"}),
                Arguments.of(new TextConversion(), new Object[]{5}),
                Arguments.of(new ConcatenateStringsPlugin(), new Object[]{"Hello", " World!"}),
                Arguments.of(new GetLengthConversion(), new Object[]{List.of("foo", "bar")}),
                Arguments.of(new GetLengthConversion(), new Object[]{"Pneumonoultramicroscopicsilicovolcanoconiosis"}),
                Arguments.of(new Now(), new Object[]{}),
                Arguments.of(new Now(), new Object[]{"ms"}),
                Arguments.of(new StartsWithOperator(), new Object[]{"Hello World", "hello"}),
                Arguments.of(new EndsWithOperator(), new Object[]{"Hello World", "WORLD"}),
                Arguments.of(new IncludesOperator(), new Object[]{"Hello World", "lo Wo"}),
                Arguments.of(new IncludesOperator(), new Object[]{List.of("foo", "bar"), "bar"}),
                Arguments.of(new DefaultValue(), new Object[]{null, "fallback"}),
                Arguments.of(new InputValidation(), new Object[]{"hello", "id; String"}),
                Arguments.of(new InputValidation(), new Object[]{25, "age; Integer; 1; 99"}),
                Arguments.of(new ListOfMap(), new Object[]{
                        Map.of("hello", Map.of("world", List.of(1, 2, 3), "test", List.of("a", "b", "c")))}),
                Arguments.of(new ParseDate(), new Object[]{"12/25/2025", "MM/dd/yyyy; ms"}),
                Arguments.of(new ParseDateTime(), new Object[]{"12/25/2025 10:30:00", "MM/dd/yyyy HH:mm:ss; ms"}),
                Arguments.of(new RemoveKey(), new Object[]{Map.of("a", 1, "b", 2), "a"}),
                Arguments.of(new SubstringPlugin(), new Object[]{"Hello World", 0, 5}),
                Arguments.of(new UniqueSet(), new Object[]{List.of("a", "b", "a", "c", "b")}),
                Arguments.of(new UpdateListOfMap(), new Object[]{
                        List.of(Map.of("world", 1), Map.of("world", 2)),
                        Map.of("more", List.of("X", "Y"))}),
                Arguments.of(new IsEmptyOperator(), new Object[]{List.of()}),
                Arguments.of(new IsEmptyOperator(), new Object[]{List.of("foo")}),
                Arguments.of(new IsEmptyOperator(), new Object[]{Map.of()}),
                Arguments.of(new IsEmptyOperator(), new Object[]{""}),
                Arguments.of(new IsEmptyOperator(), new Object[]{new String[]{}})
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("plugins")
    void shouldBeSubmillisecondWhenRunningPlugins(PluginFunction func, Object[] params) {
        doWarmup(() -> func.calculate(params));
        long[] samples = new long[MEASURE_ITERATIONS];
        for (int i = 0; i < MEASURE_ITERATIONS; i++) {
            long start = System.nanoTime();
            func.calculate(params);
            samples[i] = (System.nanoTime() - start) / 1000; // nano to micro
        }
        Arrays.sort(samples);
        long median = samples[MEASURE_ITERATIONS / 2];
        log.info("Delta for {}: {} microseconds (median of {})", func.getName(), median, MEASURE_ITERATIONS);
        // 1 ms --> 1000 microseconds. SimplePlugin should be sub-millisecond so, less than 1000 microseconds
        assertTrue(median < 1000, "Should be sub-millisecond response times");
    }
}
