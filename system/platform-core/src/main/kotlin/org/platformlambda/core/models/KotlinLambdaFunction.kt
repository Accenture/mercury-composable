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

package org.platformlambda.core.models

/**
 * Composable function interface contract definition
 * <p>
 * Note that the input class should be a Map or a PoJo
 *
 * @param <I> input class
 * @param <O> output class
 */
interface KotlinLambdaFunction<I, O> {
    @Throws(Exception::class)
    suspend fun handleEvent(headers: Map<String, String>, input: I, instance: Int): O
}