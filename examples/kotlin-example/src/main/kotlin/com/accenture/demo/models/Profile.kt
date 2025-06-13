package com.accenture.demo.models

import org.platformlambda.core.serializers.SimpleMapper

data class Profile(
    var id: Int? = null,
    var name: String? = null,
    var address: String? = null,
    var telephone: String? = null
) {
    constructor(kv: Map<String, Any>) : this() {
        val profile = SimpleMapper.getInstance().mapper.readValue(kv, Profile::class.java)
        this.id = profile.id
        this.name = profile.name
        this.address = profile.address
        this.telephone = profile.telephone
    }
}

