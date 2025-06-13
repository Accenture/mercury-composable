package com.accenture.demo.tasks

import com.accenture.demo.models.Profile
import org.platformlambda.core.annotations.PreLoad
import org.platformlambda.core.exception.AppException
import org.platformlambda.core.models.TypedLambdaFunction
import org.platformlambda.core.serializers.SimpleMapper
import org.platformlambda.core.util.Utility
import java.io.File

@PreLoad(route = "v1.get.profile", instances = 10)
class GetProfile : TypedLambdaFunction<MutableMap<String, Any>, Profile> {

    override fun handleEvent(
        headers: MutableMap<String, String>,
        input: MutableMap<String, Any>,
        instance: Int
    ): Profile {
        if (!headers.containsKey(PROFILE_ID)) {
            throw AppException(400, "Missing profile_id")
        }
        val profileId = headers.get(PROFILE_ID)
        val profileFile = File(TEMP_DATA_STORE, profileId + JSON_EXT)
        if (!profileFile.exists()) {
            throw AppException(404, "Profile $profileId not found")
        }
        val json = util.file2str(profileFile)
        return SimpleMapper.getInstance().getMapper().readValue(json, Profile::class.java)
    }

    companion object {
        private val util: Utility = Utility.getInstance()

        private const val PROFILE_ID = "profile_id"
        private const val TEMP_DATA_STORE = "/tmp/store"
        private const val JSON_EXT = ".json"
    }
}
