package com.accenture.services.plugins.arithmetic;

import org.platformlambda.core.annotations.SimplePlugin;

import java.util.Arrays;

@SimplePlugin
public class DecrementNumbers extends SimpleNumberPlugin {

    @Override
    public String getName() {
        return "decrement";
    }

    @Override
    public Object calculate(Object... input) {
        Integer[] arr = new Integer[10];
        arr[0] = 1;

        return promoteInput(input)
                .map(l -> l - 1)
                .toList();
    }
}
