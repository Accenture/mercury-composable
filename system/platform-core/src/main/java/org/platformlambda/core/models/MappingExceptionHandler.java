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

package org.platformlambda.core.models;

import org.platformlambda.core.exception.AppException;

/**
 * You can add this interface to your TypedLambdaFunction to catch object casting exception.
 * <p>
 *     The system will do best effort to map PoJo if you define a PoJo or a Map
 *     as input in a TypedLambdaFunction.
 * <p>
 *     This is designed to catch casting of Java primitive to a Map or PoJo since
 *     it is not possible to do meaningful mapping.
 * <p>
 *     You can implement this interface to handle this type of Casting exception.
 * <p>
 *     IMPORTANT:
 *     If you are using Event Script, the engine will guarantee that Java primitive
 *     is not mapped into the input of a Composable function in an event flow.
 *     Therefore, there is no need to use this interface to catch casting
 *     exception when using Event Script.
 */
public interface MappingExceptionHandler {

    void onError(String incomingRoute, AppException error, EventEnvelope event, int instance);
}
