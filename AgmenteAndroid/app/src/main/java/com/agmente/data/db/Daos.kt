package com.agmente.data.db

import androidx.room.*

@Dao
interface ServerDao {
    @Query("SELECT * FROM servers ORDER BY name ASC")
    suspend fun getAll(): List<ServerEntity>

    @Query("SELECT * FROM servers WHERE id = :id")
    suspend fun getById(id: String): ServerEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(server: ServerEntity)

    @Delete
    suspend fun delete(server: ServerEntity)

    @Query("DELETE FROM servers WHERE id = :id")
    suspend fun deleteById(id: String)

    @Update
    suspend fun update(server: ServerEntity)
}

@Dao
interface SessionDao {
    @Query("SELECT * FROM sessions WHERE serverId = :serverId ORDER BY updatedAt DESC")
    suspend fun getByServerId(serverId: String): List<SessionEntity>

    @Query("SELECT * FROM sessions WHERE sessionId = :sessionId AND serverId = :serverId")
    suspend fun getByIds(sessionId: String, serverId: String): SessionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(session: SessionEntity)

    @Update
    suspend fun update(session: SessionEntity)

    @Query("DELETE FROM sessions WHERE sessionId = :sessionId AND serverId = :serverId")
    suspend fun delete(sessionId: String, serverId: String)

    @Query("DELETE FROM sessions WHERE serverId = :serverId")
    suspend fun deleteByServerId(serverId: String)

    @Query("DELETE FROM sessions WHERE serverId = :serverId AND sessionId NOT IN (:keepIds)")
    suspend fun pruneExcept(serverId: String, keepIds: List<String>)
}

@Dao
interface MessageDao {
    @Query("SELECT * FROM messages WHERE sessionId = :sessionId ORDER BY orderIndex ASC")
    suspend fun getBySessionId(sessionId: String): List<MessageEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(messages: List<MessageEntity>)

    @Query("DELETE FROM messages WHERE sessionId = :sessionId")
    suspend fun deleteBySessionId(sessionId: String)
}
