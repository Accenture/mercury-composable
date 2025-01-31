/*

    Copyright 2018-2025 Accenture Technology

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

import java.lang.annotation.*;

/**
 * This indicates that the annotated class will be executed before the MainApplication runs.
 * <p>
 * Note that the "BeforeApplication" class should also implement the EntryPoint interface.
 * <p>
 * Smaller sequence will be executed first. Normal startup sequence must be between 3 and 999.
 * <p>
 * (Sequence 0 is reserved by the EssentialServiceLoader and 2 reserved by Event Script.
 * If your startup code must run before this system module, you can use sequence 1.
 * Otherwise, use a number between 3 and 999)
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface BeforeApplication {

    int sequence() default 10;

}
