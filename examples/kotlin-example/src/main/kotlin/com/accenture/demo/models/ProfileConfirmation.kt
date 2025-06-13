package com.accenture.demo.models

data class ProfileConfirmation(
    var type: String? = null,
    var profile: Map<String, Any>? = null,
    var secure: List<String>? = null
)
