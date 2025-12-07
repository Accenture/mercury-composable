package com.accenture.services.plugins.generators;

import com.accenture.utils.TypeConversionUtils;
import org.platformlambda.core.annotations.SimplePlugin;
import org.platformlambda.core.models.PluginFunction;

@SimplePlugin
public class UUIDGenerator implements PluginFunction {

    @Override
    public String getName() {
        return "uuid";
    }

    @Override
    public Object calculate(Object... input) {
        return TypeConversionUtils.getUUID();
    }
}
