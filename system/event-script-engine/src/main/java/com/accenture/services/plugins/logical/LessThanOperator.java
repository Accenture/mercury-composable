package com.accenture.services.plugins.logical;

import com.accenture.services.plugins.arithmetic.SimpleNumberPlugin;
import org.platformlambda.core.annotations.SimplePlugin;

@SimplePlugin
public class LessThanOperator extends SimpleNumberPlugin {

    @Override
    public String getName() {
        return "lt";
    }

    @Override
    public Object calculate(Object... input) {
        if(input.length == 0){
            throw new IllegalArgumentException("Input is required to compare using 'Greater Than'");
        }

        Long first = promoteNumber(input[0]);

        return promoteInput(input)
                .skip(1)
                .allMatch(next -> first < next); // If Stream is empty, allMatch will still return true
    }
}
