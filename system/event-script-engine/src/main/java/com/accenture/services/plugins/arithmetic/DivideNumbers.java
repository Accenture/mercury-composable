package com.accenture.services.plugins.arithmetic;

import org.platformlambda.core.annotations.SimplePlugin;

import java.util.Arrays;
import java.util.stream.Stream;

@SimplePlugin
public class DivideNumbers extends SimpleNumberPlugin {

    @Override
    public String getName() {
        return "div";
    }

    @Override
    public Object calculate(Object... input) {
        divideByZeroCheck(input);

        return promoteInput(input)
                .reduce((l1, l2) -> l1 / l2)
                .orElseThrow(() -> new IllegalStateException("Could not divide the input: " + Arrays.toString(input)));
    }

}
