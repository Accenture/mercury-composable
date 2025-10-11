package com.accenture.services.plugins.types;

import com.accenture.utils.TypeConversionUtils;
import org.platformlambda.core.annotations.SimplePlugin;
import org.platformlambda.core.models.PluginFunction;

import java.util.Arrays;
import java.util.List;

@SimplePlugin
public class DoubleConversion implements PluginFunction {

    @Override
    public String getName() {
        return "double";
    }

    @Override
    public Object calculate(Object... input) {
        List<Double> output = Arrays.stream(input)
                .map(TypeConversionUtils::convertDouble)
                .toList();
        
        return (output.size() == 1) ? output.getFirst() : output;
    }


}
