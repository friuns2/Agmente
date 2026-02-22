package com.agmente.acpclient.config

data class ACPClientConfiguration(
    val endpoint: String,
    val authTokenProvider: (suspend () -> String)? = null,
    val additionalHeaders: Map<String, String> = emptyMap(),
    val pingIntervalMs: Long? = null,
    val appendNewline: Boolean = true
)
