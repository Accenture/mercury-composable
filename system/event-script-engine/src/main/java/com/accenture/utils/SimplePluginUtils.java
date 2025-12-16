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

package com.accenture.utils;

import java.util.Arrays;
import java.util.stream.Stream;

public class SimplePluginUtils {

    public static void divideByZeroCheck(Object... input) {
        boolean anyZero = Arrays.stream(input)
                .map(SimplePluginUtils::promoteNumber)
                .anyMatch(l -> l == 0L);
        if (anyZero) {
            throw new IllegalStateException("Dividing the input: " + Arrays.toString(input) + " would cause Division By Zero");
        }
    }

    public static Stream<Long> promoteInput(Object... input) {
        return Arrays.stream(input).map(SimplePluginUtils::promoteNumber);
    }

    public static Long promoteNumber(Object o) {
        return switch (o) {
            case Short s -> s.longValue();
            case Integer i -> i.longValue();
            case Long l -> l;
            case String s -> Long.valueOf(s);
            default -> throw new IllegalArgumentException("Cannot add the object: " + o);
        };
    }
}
