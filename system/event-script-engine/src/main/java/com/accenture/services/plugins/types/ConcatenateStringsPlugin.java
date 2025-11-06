package com.accenture.services.plugins.types;

import com.accenture.utils.TypeConversionUtils;
import org.platformlambda.core.annotations.SimplePlugin;
import org.platformlambda.core.models.PluginFunction;

import java.util.Arrays;
import java.util.stream.Collectors;

@SimplePlugin
public class ConcatenateStringsPlugin implements PluginFunction {

    @Override
    public String getName() {
        return "concat";
    }

    @Override
    public Object calculate(Object... input) {
        return Arrays.stream(input)
                .map(TypeConversionUtils::getTextValue)
                .collect(Collectors.joining(""));
    }
}
