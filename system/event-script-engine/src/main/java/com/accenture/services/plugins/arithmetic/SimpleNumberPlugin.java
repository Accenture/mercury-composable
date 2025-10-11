package com.accenture.services.plugins.arithmetic;

import org.platformlambda.core.models.PluginFunction;

public abstract class SimpleNumberPlugin implements PluginFunction {

    protected Long promoteNumber(Object o){
        return switch (o){
            case Short s -> s.longValue();
            case Integer i -> i.longValue();
            case Long l -> l;
            case String s -> Long.valueOf(s);
            default -> throw new IllegalArgumentException("Cannot add the object: " + o);
        };
    }

}
