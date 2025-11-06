package com.accenture.services.plugins.arithmetic;

import com.accenture.services.plugins.logical.TernaryOperator;
import org.platformlambda.core.models.PluginFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.stream.Stream;


public abstract class SimpleNumberPlugin implements PluginFunction {
    private static final Logger log = LoggerFactory.getLogger(TernaryOperator.class);

    protected void divideByZeroCheck(Object... input){
        boolean anyZero = Arrays.stream(input)
                .map(this::promoteNumber)
                .anyMatch(l -> l == 0L);

        if(anyZero){
            throw new IllegalStateException("Dividing the input: " + Arrays.toString(input) + " would cause Division By Zero");
        }
    }

    protected Stream<Long> promoteInput(Object... input){
        return Arrays.stream(input).map(this::promoteNumber);
    }

    protected Long promoteNumber(Object o){
        return switch (o){
            case Short s -> s.longValue();
            case Integer i -> i.longValue();
            case Long l -> l;
            case String s -> Long.valueOf(s);
            default -> throw new IllegalArgumentException("Cannot add the object: " + o);
        };
    }

}
