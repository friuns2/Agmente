package com.agmente.data.db

import androidx.room.*

enum class ServerType(val value: String) {
    ACP("acp"),
    CODEX_APP_SERVER("codexAppServer");

    companion object {
        fun fromValue(value: String): ServerType =
            entries.find { it.value == value } ?: ACP
    }
}

@Entity(tableName = "servers")
data class ServerEntity(
    @PrimaryKey val id: String,
    val name: String,
    val scheme: String,
    val host: String,
    val token: String = "",
    val cfAccessClientId: String = "",
    val cfAccessClientSecret: String = "",
    val workingDirectory: String = "",
    val serverType: String = ServerType.ACP.value,
    val usedWorkingDirectories: String = "[]"
)
