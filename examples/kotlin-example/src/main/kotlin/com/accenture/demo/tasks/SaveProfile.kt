package com.accenture.demo.tasks

import org.platformlambda.core.annotations.PreLoad
import org.platformlambda.core.models.TypedLambdaFunction
import org.platformlambda.core.serializers.SimpleMapper
import org.platformlambda.core.util.MultiLevelMap
import org.platformlambda.core.util.Utility
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File

@PreLoad(route = "v1.save.profile", instances = 10)
class SaveProfile : TypedLambdaFunction<Map<String, Any>, Void> {

    override fun handleEvent(
        headers: Map<String, String>,
        input: Map<String, Any>,
        instance: Int
    ): Void? {
        require(input.containsKey("id")) { "Missing id in profile" }
        val requiredFields: String = headers[REQUIRED_FIELDS]!!
        requireNotNull(requiredFields) { "Missing required_fields" }
        val dataset = MultiLevelMap(input)
        val fields = util.split(requiredFields, ", ")
        for (f in fields) {
            require(dataset.exists(f)) { "Missing $f" }
        }
        // save only fields that are in the interface contract
        val filtered = MultiLevelMap()
        for (f in fields) {
            filtered.setElement(f, dataset.getElement(f))
        }
        val mapper = SimpleMapper.getInstance().getMapper()
        val json = mapper.writeValueAsString(filtered.getMap())
        val folder = File(TEMP_DATA_STORE)
        if (!folder.exists() && folder.mkdirs()) {
            log.info("Temporary key folder {} created", folder)
        }
        val id: String? = input.get("id").toString()
        val file = File(folder, id + JSON_EXT)
        util.str2file(file, json)
        log.info("Profile {} saved", id)
        // this task does not have any output
        return null
    }

    companion object {
        private val log: Logger = LoggerFactory.getLogger(SaveProfile::class.java)
        private val util: Utility = Utility.getInstance()
        private const val TEMP_DATA_STORE = "/tmp/store"
        private const val JSON_EXT = ".json"
        private const val REQUIRED_FIELDS = "required_fields"
    }
}
