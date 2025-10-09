package com.accenture.services.plugins;

import org.platformlambda.core.annotations.SimplePlugin;

import java.util.Arrays;

@SimplePlugin
public class DivideNumbers extends SimpleNumberPlugin {

    @Override
    public String getName() {
        return "div";
    }

    @Override
    public Object calculate(Object... input) {
        return Arrays.stream(input)
                .map(this::promoteNumber)
                .reduce((l1, l2) -> l1 / l2)
                .orElseThrow(() -> new IllegalStateException("Could not divide the input: " + Arrays.toString(input)));
    }

}
