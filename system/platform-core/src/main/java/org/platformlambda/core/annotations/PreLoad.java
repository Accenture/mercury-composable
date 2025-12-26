/*

    Copyright 2018-2026 Accenture Technology

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

        http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

 */

package org.platformlambda.core.annotations;

import org.platformlambda.core.system.ServiceDef.SerializationStrategy;

import java.lang.annotation.*;

/**
 * This indicates the class is a service to be preloaded.
 * (for a class to be preloaded, it must use a default constructor without arguments)
 * <p>
 * customSerializer is optional. It allows you to apply a pre-configured ObjectMapper suitable for
 * your user function. It should point to a fully qualified classpath to a class implementing
 * the CustomSerializer interface.
 * <p>
 * envInstances is optional. If present, it must be a parameter in application.properties (or application.yml).
 * The parameter may fetch value from an environment variable using "${ENV_VAR:default_value}" format.
 * If the parameter does not exist, or it does not resolve to a numeric value, the "instances" value in this
 * annotation will be used instead.
 * <p>
 * inputPojoClass is optional. When using TypeLambdaFunction, the input class has precedence over this parameter.
 * When input class is not provided, you may set the input class using this parameter. This inputPoJoClass
 * is also used when the TypedLambdaFunction's input is an Object and the input payload is a list of Maps.
 * It will convert the list of Maps back to a list of PoJo.
 * <p>
 * Note that Event Script's Input/Output Data Mapping does not support list of PoJo because it would be
 * more intuitive to map key-values to input arguments of a user function.
 * <p>
 * inputStrategy and outputStrategy are default, snake or camel.
 * This allows you to adjust serialization case strategy for a composable function that interfaces with
 * an external system or entity. When it is set to "default", the system will use the serialization case
 * strategy stated in "snake.case.serialization = true or false" of application.properties.
 * The system will apply snake case or camel case serialization strategy for input and output
 * when it is "snake" or "camel" accordingly.
 * <p>
 * inputStrategy and outputStrategy have no effect when customSerializer if configured.
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface PreLoad {
    String route();
    Class<?> customSerializer() default Void.class;
    Class<?> inputPojoClass() default Void.class;
    int instances() default 1;
    String envInstances() default "";
    boolean isPrivate() default true;
    SerializationStrategy inputStrategy() default SerializationStrategy.DEFAULT;
    SerializationStrategy outputStrategy() default SerializationStrategy.DEFAULT;
}
