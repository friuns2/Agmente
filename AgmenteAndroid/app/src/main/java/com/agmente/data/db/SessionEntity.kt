package com.agmente.data.db

import androidx.room.*

@Entity(
    tableName = "sessions",
    foreignKeys = [ForeignKey(
        entity = ServerEntity::class,
        parentColumns = ["id"],
        childColumns = ["serverId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("serverId")]
)
data class SessionEntity(
    @PrimaryKey val sessionId: String,
    val serverId: String,
    val title: String? = null,
    val cwd: String? = null,
    val updatedAt: Long? = null
)
