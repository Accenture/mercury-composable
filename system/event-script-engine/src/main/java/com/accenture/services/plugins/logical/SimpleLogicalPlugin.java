package com.accenture.services.plugins.logical;

import com.accenture.utils.TypeConversionUtils;
import org.platformlambda.core.models.PluginFunction;

import java.util.Arrays;
import java.util.stream.Stream;

public abstract class SimpleLogicalPlugin implements PluginFunction {

    protected Stream<Boolean> promoteInput(Object... input){
        return Arrays.stream(input).map(TypeConversionUtils::convertBoolean);
    }

}
