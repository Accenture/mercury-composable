package com.accenture.demo.tasks

import org.platformlambda.core.annotations.PreLoad
import org.platformlambda.core.models.TypedLambdaFunction
import org.platformlambda.core.util.CryptoApi
import org.platformlambda.core.util.MultiLevelMap
import org.platformlambda.core.util.Utility

@Suppress("UNCHECKED_CAST")
@PreLoad(route = "v1.decrypt.fields", instances = 10)
class DecryptFields : TypedLambdaFunction<Map<String, Any>, Map<String, Any>> {

    override fun handleEvent(
        headers: Map<String, String>,
        input: Map<String, Any>,
        instance: Int
    ): Map<String, Any> {
        require(input.containsKey(PROTECTED_FIELDS)) { MISSING + PROTECTED_FIELDS }
        require(input.containsKey(KEY)) { MISSING + KEY }
        val keyBytes: Any? = input[KEY]
        require(keyBytes is ByteArray) { KEY + " - Expect bytes, Actual: " + keyBytes?.javaClass }
        if (input.containsKey(DATASET)) {
            val key = keyBytes
            val dataset = input[DATASET] as Map<String, Any>
            val multiLevels = MultiLevelMap(dataset)
            val fields = util.split(input[PROTECTED_FIELDS] as String?, ", ")
            for (f in fields) {
                if (multiLevels.exists(f)) {
                    val cipherText = util.base64ToBytes(multiLevels.getElement(f).toString())
                    multiLevels.setElement(f, decryptField(cipherText, key))
                }
            }
            return multiLevels.map
        } else {
            throw IllegalArgumentException(MISSING + DATASET)
        }
    }

    private fun decryptField(cipherText: ByteArray, key: ByteArray?): String? {
        val b = crypto.aesDecrypt(cipherText, key)
        return util.getUTF(b)
    }

    companion object {
        private val util: Utility = Utility.getInstance()
        private val crypto = CryptoApi()

        private const val KEY = "key"
        private const val PROTECTED_FIELDS = "protected_fields"
        private const val DATASET = "dataset"
        private const val MISSING = "Missing "
    }
}
