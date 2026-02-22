package com.agmente.acpclient.model

import java.util.Date

data class SessionSummary(
    val id: String,
    val title: String?,
    val cwd: String? = null,
    val updatedAt: Date? = null
)
