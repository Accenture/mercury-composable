package com.accenture.demo.tasks

import com.accenture.demo.models.ProfileConfirmation
import org.platformlambda.core.annotations.PreLoad
import org.platformlambda.core.models.TypedLambdaFunction
import org.platformlambda.core.util.MultiLevelMap
import org.platformlambda.core.util.Utility

@PreLoad(route = "v1.create.profile", instances = 10)
class CreateProfile : TypedLambdaFunction<Map<String, Any>, ProfileConfirmation> {
    override fun handleEvent(headers: Map<String, String>, input: Map<String, Any>, instance: Int): ProfileConfirmation {
        if (!input.containsKey("id")) {
            throw IllegalArgumentException("Missing id")
        }

        val requiredFields = headers[REQUIRED_FIELDS]
            ?: throw IllegalArgumentException("Missing required_fields")

        val protectedFields = headers[PROTECTED_FIELDS]
            ?: throw IllegalArgumentException("Missing protected_fields")

        val data = MultiLevelMap(input)

        val fields = util.split(requiredFields, ", ")
        for (f in fields) {
            if (!data.exists(f)) {
                throw IllegalArgumentException("Missing $f")
            }
        }

        val pFields = util.split(protectedFields, ", ")
        for (f in pFields) {
            if (data.exists(f)) {
                data.setElement(f, "***")
            }
        }

        return ProfileConfirmation().apply {
            profile = data.map
            type = "CREATE"
            secure = pFields
        }
    }

    companion object {
        private val util = Utility.getInstance()
        private const val REQUIRED_FIELDS = "required_fields"
        private const val PROTECTED_FIELDS = "protected_fields"
    }
}
