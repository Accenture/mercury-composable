package com.accenture.services.plugins.types;

import com.accenture.utils.TypeConversionUtils;
import org.platformlambda.core.annotations.SimplePlugin;
import org.platformlambda.core.models.PluginFunction;

import java.util.Arrays;
import java.util.List;

@SimplePlugin
public class IntegerConversion implements PluginFunction {

    @Override
    public String getName() {
        return "int";
    }

    @Override
    public Object calculate(Object... input) {
        List<Integer> output = Arrays.stream(input)
                .map(TypeConversionUtils::convertInteger)
                .toList();

        return (output.size() == 1) ? output.getFirst() : output;
    }


}
