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

package org.platformlambda.core.mock

import org.platformlambda.core.annotations.PreLoad
import org.platformlambda.core.models.KotlinLambdaFunction
import org.platformlambda.core.models.PoJo

@PreLoad(route = "input.list.of.pojo.kotlin", inputPojoClass = PoJo::class)
class InputAsListOfPoJoKotlin : KotlinLambdaFunction<List<PoJo?>, Any> {

    override suspend fun handleEvent(headers: Map<String, String>, input: List<PoJo?>, instance: Int): Any {
        val names: MutableList<String> = ArrayList()
        // prove that the list of pojo is correctly deserialized
        for (o in input) {
            if (o != null) {
                names.add(o.name)
            } else {
                names.add("null")
            }
        }
        val result: MutableMap<String, Any> = HashMap()
        result["names"] = names
        return result
    }
}
