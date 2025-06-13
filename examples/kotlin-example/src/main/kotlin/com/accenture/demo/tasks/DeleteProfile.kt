package com.accenture.demo.tasks

import org.platformlambda.core.annotations.PreLoad
import org.platformlambda.core.exception.AppException
import org.platformlambda.core.models.TypedLambdaFunction
import org.platformlambda.core.util.Utility
import java.io.File
import java.nio.file.Files


@PreLoad(route = "v1.delete.profile", instances = 10)
class DeleteProfile : TypedLambdaFunction<Map<String, Any>, Map<String, Any>> {

    override fun handleEvent(
        headers: Map<String, String>,
        input: Map<String, Any>,
        instance: Int
    ): Map<String, Any> {
        if (!headers.containsKey(PROFILE_ID)) {
            throw AppException(400, "Missing profile_id")
        }
        val profileId = headers[PROFILE_ID]
        val f = File(TEMP_DATA_STORE, profileId + JSON_EXT)
        if (!f.exists()) {
            throw AppException(404, "Profile $profileId not found")
        }
        Files.delete(f.toPath())
        val util = Utility.getInstance()
        val result: MutableMap<String, Any> = HashMap()
        if (util.isDigits(profileId)) util.str2int(profileId) else profileId?.let { result.put(ID, it) }
        result.put(DELETED, true)
        return result
    }

    companion object {
        private const val PROFILE_ID = "profile_id"
        private const val ID = "id"
        private const val TEMP_DATA_STORE = "/tmp/store"
        private const val JSON_EXT = ".json"
        private const val DELETED = "deleted"
    }
}
