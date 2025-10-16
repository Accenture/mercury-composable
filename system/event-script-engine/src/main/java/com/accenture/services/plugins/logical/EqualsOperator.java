package com.accenture.services.plugins.logical;

import com.accenture.utils.TypeConversionUtils;
import org.platformlambda.core.annotations.SimplePlugin;
import org.platformlambda.core.models.PluginFunction;

import java.util.Arrays;
import java.util.Objects;

@SimplePlugin
public class EqualsOperator implements PluginFunction {

    @Override
    public String getName() {
        return "eq";
    }

    @Override
    public Object calculate(Object... input) {
        if(input.length == 0){
            return false;
        }

        return Arrays.stream(input)
                .reduce(Objects::equals)
                .orElse(false);
    }
}
