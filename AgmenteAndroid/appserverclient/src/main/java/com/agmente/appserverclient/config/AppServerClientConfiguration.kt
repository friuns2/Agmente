package com.agmente.appserverclient.config

data class AppServerClientConfiguration(
    val endpoint: String,
    val authTokenProvider: (suspend () -> String)? = null,
    val additionalHeaders: Map<String, String> = emptyMap(),
    val pingIntervalMs: Long? = null,
    val appendNewline: Boolean = true,
    val includeJSONRPCHeader: Boolean = false
)
