package com.agmente.model

import com.agmente.data.db.ServerType
import java.util.UUID

data class ServerConfiguration(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val scheme: String = "ws",
    val host: String = "",
    val token: String = "",
    val cfAccessClientId: String = "",
    val cfAccessClientSecret: String = "",
    val workingDirectory: String = "",
    val serverType: ServerType = ServerType.ACP,
    val usedWorkingDirectories: List<String> = emptyList()
) {
    val endpointURLString: String
        get() = "$scheme://$host"
}
