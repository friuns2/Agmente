package com.agmente.data.db

import androidx.room.*

@Entity(
    tableName = "messages",
    foreignKeys = [ForeignKey(
        entity = SessionEntity::class,
        parentColumns = ["sessionId"],
        childColumns = ["sessionId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("sessionId")]
)
data class MessageEntity(
    @PrimaryKey val messageId: String,
    val sessionId: String,
    val role: String,
    val content: String,
    val createdAt: Long,
    val orderIndex: Int,
    val segmentsData: String? = null
)
