package com.accenture.services.plugins.types;

import com.accenture.utils.TypeConversionUtils;
import org.platformlambda.core.annotations.SimplePlugin;
import org.platformlambda.core.models.PluginFunction;

import java.util.Arrays;

@SimplePlugin
public class BooleanConversion implements PluginFunction {

    @Override
    public String getName() {
        return "boolean";
    }

    @Override
    public Object calculate(Object... input) {
        var output = Arrays.stream(input)
                .map(TypeConversionUtils::convertBoolean)
                .toList();

        return (output.size() == 1) ? output.getFirst() : output;
    }
}
