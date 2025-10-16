package com.accenture.services.plugins.arithmetic;

import org.platformlambda.core.annotations.SimplePlugin;

import java.util.Arrays;

@SimplePlugin
public class ModulusNumbers extends SimpleNumberPlugin {

    @Override
    public String getName() {
        return "mod";
    }

    @Override
    public Object calculate(Object... input) {
        if(input.length != 2){
            throw new IllegalArgumentException("Modulus expects only two values but got " + Arrays.toString(input));
        }

        divideByZeroCheck(input[1]);

        return promoteInput(input)
                .reduce((l1, l2) -> l1 % l2)
                .orElseThrow(() -> new IllegalStateException("Could not get modulus for the input: " + Arrays.toString(input)));
    }

}
