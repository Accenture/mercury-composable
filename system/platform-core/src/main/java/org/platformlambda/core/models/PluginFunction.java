package org.platformlambda.core.models;


public interface PluginFunction {

    /**
     * Default interface method for the name of the Plugin
     * @return The name of runtime class in valid camelCase format
     */
    default String getName(){
        String name =  this.getClass().getSimpleName();
        return Character.toLowerCase(name.charAt(0)) + name.substring(1);
    }

    Object calculate(Object... input);
}
