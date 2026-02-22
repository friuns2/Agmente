package com.agmente.data

import com.agmente.data.db.*
import com.agmente.model.ServerConfiguration
import kotlinx.serialization.json.Json
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import java.util.*

class SessionStorage(private val database: AgmenteDatabase) {

    private val serverDao = database.serverDao()
    private val sessionDao = database.sessionDao()
    private val messageDao = database.messageDao()

    private val json = Json { ignoreUnknownKeys = true }

    suspend fun fetchServers(): List<ServerConfiguration> {
        return serverDao.getAll().map { entity ->
            val usedDirs = try {
                json.decodeFromString(ListSerializer(String.serializer()), entity.usedWorkingDirectories)
            } catch (_: Exception) { emptyList() }

            ServerConfiguration(
                id = entity.id,
                name = entity.name,
                scheme = entity.scheme,
                host = entity.host,
                token = entity.token,
                cfAccessClientId = entity.cfAccessClientId,
                cfAccessClientSecret = entity.cfAccessClientSecret,
                workingDirectory = entity.workingDirectory,
                serverType = ServerType.fromValue(entity.serverType),
                usedWorkingDirectories = usedDirs
            )
        }
    }

    suspend fun saveServer(server: ServerConfiguration) {
        val usedDirsJson = json.encodeToString(
            ListSerializer(String.serializer()),
            server.usedWorkingDirectories
        )
        serverDao.insert(
            ServerEntity(
                id = server.id,
                name = server.name,
                scheme = server.scheme,
                host = server.host,
                token = server.token,
                cfAccessClientId = server.cfAccessClientId,
                cfAccessClientSecret = server.cfAccessClientSecret,
                workingDirectory = server.workingDirectory,
                serverType = server.serverType.value,
                usedWorkingDirectories = usedDirsJson
            )
        )
    }

    suspend fun deleteServer(id: String) {
        serverDao.deleteById(id)
    }

    suspend fun fetchUsedWorkingDirectories(serverId: String): List<String> {
        val entity = serverDao.getById(serverId) ?: return emptyList()
        return try {
            json.decodeFromString(ListSerializer(String.serializer()), entity.usedWorkingDirectories)
        } catch (_: Exception) { emptyList() }
    }

    suspend fun addUsedWorkingDirectory(directory: String, serverId: String): Boolean {
        val entity = serverDao.getById(serverId) ?: return false
        val dirs = try {
            json.decodeFromString(ListSerializer(String.serializer()), entity.usedWorkingDirectories).toMutableList()
        } catch (_: Exception) { mutableListOf() }

        if (!dirs.contains(directory)) {
            dirs.add(directory)
            val updated = entity.copy(
                usedWorkingDirectories = json.encodeToString(ListSerializer(String.serializer()), dirs)
            )
            serverDao.update(updated)
            return true
        }
        return false
    }

    suspend fun fetchSessions(serverId: String): List<StoredSessionInfo> {
        return sessionDao.getByServerId(serverId).map { entity ->
            StoredSessionInfo(
                sessionId = entity.sessionId,
                title = entity.title,
                cwd = entity.cwd,
                updatedAt = entity.updatedAt?.let { Date(it) }
            )
        }
    }

    suspend fun saveSession(session: StoredSessionInfo, serverId: String) {
        val existing = sessionDao.getByIds(session.sessionId, serverId)
        val entity = SessionEntity(
            sessionId = session.sessionId,
            serverId = serverId,
            title = session.title ?: existing?.title,
            cwd = session.cwd ?: existing?.cwd,
            updatedAt = session.updatedAt?.time ?: existing?.updatedAt
        )
        sessionDao.insert(entity)
    }

    suspend fun updateSession(sessionId: String, serverId: String, title: String?, touchUpdatedAt: Boolean = true) {
        val existing = sessionDao.getByIds(sessionId, serverId) ?: return
        val updated = existing.copy(
            title = title ?: existing.title,
            updatedAt = if (touchUpdatedAt) System.currentTimeMillis() else existing.updatedAt
        )
        sessionDao.update(updated)
    }

    suspend fun deleteSession(sessionId: String, serverId: String) {
        sessionDao.delete(sessionId, serverId)
    }

    suspend fun deleteAllSessions(serverId: String) {
        sessionDao.deleteByServerId(serverId)
    }

    suspend fun pruneSessions(serverId: String, keepIds: Set<String>): Int {
        val all = sessionDao.getByServerId(serverId)
        val toRemove = all.filter { it.sessionId !in keepIds }
        if (toRemove.isNotEmpty()) {
            sessionDao.pruneExcept(serverId, keepIds.toList())
        }
        return toRemove.size
    }

    suspend fun saveMessages(messages: List<StoredMessageInfo>, sessionId: String) {
        messageDao.deleteBySessionId(sessionId)
        messageDao.insertAll(messages.mapIndexed { index, msg ->
            MessageEntity(
                messageId = msg.messageId,
                sessionId = sessionId,
                role = msg.role,
                content = msg.content,
                createdAt = msg.createdAt.time,
                orderIndex = index,
                segmentsData = msg.segmentsData
            )
        })
    }

    suspend fun fetchMessages(sessionId: String): List<StoredMessageInfo> {
        return messageDao.getBySessionId(sessionId).map { entity ->
            StoredMessageInfo(
                messageId = entity.messageId,
                role = entity.role,
                content = entity.content,
                createdAt = Date(entity.createdAt),
                segmentsData = entity.segmentsData
            )
        }
    }

    suspend fun deleteMessages(sessionId: String) {
        messageDao.deleteBySessionId(sessionId)
    }
}

data class StoredSessionInfo(
    val sessionId: String,
    val title: String?,
    val cwd: String?,
    val updatedAt: Date?
) {
    fun toSessionSummary() = com.agmente.acpclient.model.SessionSummary(
        id = sessionId,
        title = title,
        cwd = cwd,
        updatedAt = updatedAt
    )
}

data class StoredMessageInfo(
    val messageId: String,
    val role: String,
    val content: String,
    val createdAt: Date,
    val segmentsData: String? = null
)
