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

package com.accenture.automation;

import com.accenture.models.PluginFunction;
import com.accenture.services.plugins.arithmetic.*;
import com.accenture.services.plugins.generators.DateGenerator;
import com.accenture.services.plugins.generators.UUIDGenerator;
import com.accenture.services.plugins.logical.*;
import com.accenture.services.plugins.types.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.util.Base64;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class SimplePluginLoaderTest {
    private static final Logger log = LoggerFactory.getLogger(SimplePluginLoaderTest.class);

    private void doWarmup(Supplier<Object> func){
        for(int i=0; i < 2; i++){
            func.get();
        }
    }

    public static Stream<Arguments> plugins(){
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
                Arguments.of(new DateGenerator(), new Object[]{}),
                Arguments.of(new UUIDGenerator(), new Object[]{}),
                Arguments.of(new EqualsOperator(), new Object[]{5,5}),
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
                Arguments.of(new GetLengthConversion(), new Object[]{"Pneumonoultramicroscopicsilicovolcanoconiosis"})

        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("plugins")
    public void shouldBeSubmillisecondWhenRunningPlugins(PluginFunction func, Object[] params) {
        doWarmup(() -> func.calculate(params));

        long start = System.nanoTime();

        func.calculate(params);

        long delta = (System.nanoTime() - start) / 1000; // nano to micro

        log.info("Delta: {}", delta);

        // 1 ms --> 1000 microseconds. SimplePlugin should be sub-millisecond so, less than 1000 microseconds
        assertTrue(delta < 1000, "Should be sub-millisecond response times");
    }
}
