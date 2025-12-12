package com.accenture.services.plugins.generators;

import com.accenture.utils.TypeConversionUtils;
import com.accenture.models.SimplePlugin;
import com.accenture.models.PluginFunction;

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
