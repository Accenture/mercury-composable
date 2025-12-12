package com.accenture.utils;

import java.util.Arrays;
import java.util.stream.Stream;

public class SimplePluginUtils {

    public static void divideByZeroCheck(Object... input){
        boolean anyZero = Arrays.stream(input)
                .map(SimplePluginUtils::promoteNumber)
                .anyMatch(l -> l == 0L);

        if(anyZero){
            throw new IllegalStateException("Dividing the input: " + Arrays.toString(input) + " would cause Division By Zero");
        }
    }

    public static Stream<Long> promoteInput(Object... input){
        return Arrays.stream(input).map(SimplePluginUtils::promoteNumber);
    }

    public static Long promoteNumber(Object o){
        return switch (o){
            case Short s -> s.longValue();
            case Integer i -> i.longValue();
            case Long l -> l;
            case String s -> Long.valueOf(s);
            default -> throw new IllegalArgumentException("Cannot add the object: " + o);
        };
    }

}
