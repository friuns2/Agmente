package com.agmente.appserverclient

import com.agmente.appserverclient.model.JSONValue
import com.agmente.appserverclient.model.JSONRPCMessage
import java.text.SimpleDateFormat
import java.util.*

data class AppServerInitializeResult(
    val serverName: String?,
    val serverVersion: String?,
    val capabilities: Map<String, JSONValue>?
)

data class AppServerModel(
    val id: String,
    val name: String?,
    val provider: String?,
    val reasoningEfforts: List<String>?
)

data class AppServerModelListResult(
    val models: List<AppServerModel>,
    val defaultModel: String?
)

data class AppServerSkillScope(
    val name: String
)

data class AppServerSkill(
    val name: String,
    val displayName: String?,
    val description: String?,
    val scopes: List<AppServerSkillScope>
)

data class AppServerThreadSummary(
    val id: String,
    val preview: String?,
    val modelProvider: String?,
    val createdAt: Date?,
    val cwd: String? = null
)

data class AppServerThreadListResult(
    val threads: List<AppServerThreadSummary>,
    val nextCursor: String?
)

enum class AppServerTurnStatus(val value: String) {
    IN_PROGRESS("in_progress"),
    COMPLETED("completed"),
    INTERRUPTED("interrupted"),
    FAILED("failed");

    companion object {
        fun fromString(str: String): AppServerTurnStatus =
            entries.find { it.value == str } ?: COMPLETED
    }
}

data class AppServerTurnSummary(
    val id: String,
    val status: AppServerTurnStatus
)

data class AppServerThreadItem(
    val type: String,
    val id: String?,
    val payload: JSONValue?
)

data class AppServerThreadTurn(
    val id: String,
    val status: AppServerTurnStatus,
    val items: List<AppServerThreadItem>
)

data class AppServerThreadResumeResult(
    val id: String,
    val preview: String?,
    val cwd: String?,
    val createdAt: Date?,
    val turns: List<AppServerThreadTurn>
)

data class AppServerPlanStep(
    val step: String,
    val status: String
)

data class AppServerPlanUpdate(
    val turnId: String?,
    val explanation: String?,
    val steps: List<AppServerPlanStep>
)

object AppServerResponseParser {
    private val dateFormats = listOf(
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        },
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
    )

    private fun parseDate(dateString: String?): Date? {
        if (dateString == null) return null
        for (format in dateFormats) {
            try { return format.parse(dateString) } catch (_: Exception) { }
        }
        return null
    }

    fun parseInitialize(response: JSONRPCMessage): AppServerInitializeResult? {
        val result = (response as? JSONRPCMessage.Response)?.value?.result?.objectValue ?: return null
        val serverInfo = result["serverInfo"]?.objectValue
        val userAgent = result["userAgent"]?.stringValue
        val serverName = serverInfo?.get("name")?.stringValue
            ?: userAgent?.split("/")?.firstOrNull()
        val serverVersion = serverInfo?.get("version")?.stringValue
            ?: userAgent?.split("/")?.getOrNull(1)?.split(" ")?.firstOrNull()
        return AppServerInitializeResult(
            serverName = serverName,
            serverVersion = serverVersion,
            capabilities = result["capabilities"]?.objectValue
        )
    }

    fun parseModelList(response: JSONRPCMessage): AppServerModelListResult? {
        val result = (response as? JSONRPCMessage.Response)?.value?.result?.objectValue ?: return null
        val modelsArray = result["models"]?.arrayValue
            ?: result["data"]?.arrayValue
            ?: return null

        val models = modelsArray.mapNotNull { value ->
            val obj = value.objectValue ?: return@mapNotNull null
            val id = obj["id"]?.stringValue ?: return@mapNotNull null
            AppServerModel(
                id = id,
                name = obj["name"]?.stringValue,
                provider = obj["provider"]?.stringValue,
                reasoningEfforts = obj["reasoning_efforts"]?.arrayValue?.mapNotNull { it.stringValue }
            )
        }

        val defaultModel = result["default"]?.stringValue
            ?: models.firstOrNull { m ->
                val obj = modelsArray.firstOrNull { it.objectValue?.get("id")?.stringValue == m.id }
                obj?.objectValue?.get("isDefault")?.let { v ->
                    when (v) {
                        is JSONValue.BoolValue -> v.value
                        else -> false
                    }
                } ?: false
            }?.id
        return AppServerModelListResult(
            models = models,
            defaultModel = defaultModel
        )
    }

    fun parseSkillsList(response: JSONRPCMessage): List<AppServerSkill> {
        val result = (response as? JSONRPCMessage.Response)?.value?.result?.objectValue ?: return emptyList()
        val skillsArray = result["skills"]?.arrayValue ?: return emptyList()

        return skillsArray.mapNotNull { value ->
            val obj = value.objectValue ?: return@mapNotNull null
            val name = obj["name"]?.stringValue ?: return@mapNotNull null
            val scopes = obj["scopes"]?.arrayValue?.mapNotNull { scopeVal ->
                scopeVal.objectValue?.get("name")?.stringValue?.let { AppServerSkillScope(it) }
            } ?: emptyList()
            AppServerSkill(
                name = name,
                displayName = obj["displayName"]?.stringValue,
                description = obj["description"]?.stringValue,
                scopes = scopes
            )
        }
    }

    fun parseThreadList(response: JSONRPCMessage): AppServerThreadListResult? {
        val result = (response as? JSONRPCMessage.Response)?.value?.result?.objectValue ?: return null
        val threadsArray = result["threads"]?.arrayValue ?: return null

        val threads = threadsArray.mapNotNull { value ->
            val obj = value.objectValue ?: return@mapNotNull null
            val id = obj["id"]?.stringValue ?: return@mapNotNull null
            AppServerThreadSummary(
                id = id,
                preview = obj["preview"]?.stringValue,
                modelProvider = obj["model_provider"]?.stringValue,
                createdAt = parseDate(obj["created_at"]?.stringValue),
                cwd = obj["cwd"]?.stringValue
            )
        }

        return AppServerThreadListResult(
            threads = threads,
            nextCursor = result["next_cursor"]?.stringValue
        )
    }

    fun parseThreadStart(response: JSONRPCMessage): AppServerThreadSummary? {
        val result = (response as? JSONRPCMessage.Response)?.value?.result?.objectValue ?: return null
        val threadObj = result["thread"]?.objectValue ?: result
        val id = threadObj["id"]?.stringValue ?: return null
        return AppServerThreadSummary(
            id = id,
            preview = threadObj["preview"]?.stringValue,
            modelProvider = threadObj["modelProvider"]?.stringValue
                ?: threadObj["model_provider"]?.stringValue
                ?: result["modelProvider"]?.stringValue,
            createdAt = parseDate(threadObj["created_at"]?.stringValue
                ?: threadObj["createdAt"]?.stringValue),
            cwd = threadObj["cwd"]?.stringValue ?: result["cwd"]?.stringValue
        )
    }

    fun parseTurnStart(response: JSONRPCMessage): AppServerTurnSummary? {
        val result = (response as? JSONRPCMessage.Response)?.value?.result?.objectValue ?: return null
        val id = result["id"]?.stringValue ?: return null
        val statusStr = result["status"]?.stringValue ?: "in_progress"
        return AppServerTurnSummary(
            id = id,
            status = AppServerTurnStatus.fromString(statusStr)
        )
    }

    fun parseThreadResume(response: JSONRPCMessage): AppServerThreadResumeResult? {
        val result = (response as? JSONRPCMessage.Response)?.value?.result?.objectValue ?: return null
        val id = result["id"]?.stringValue ?: return null

        val turnsArray = result["turns"]?.arrayValue ?: emptyList()
        val turns = turnsArray.mapNotNull { turnVal ->
            val turnObj = turnVal.objectValue ?: return@mapNotNull null
            val turnId = turnObj["id"]?.stringValue ?: return@mapNotNull null
            val statusStr = turnObj["status"]?.stringValue ?: "completed"
            val itemsArray = turnObj["items"]?.arrayValue ?: emptyList()

            val items = itemsArray.mapNotNull { itemVal ->
                val itemObj = itemVal.objectValue ?: return@mapNotNull null
                val type = itemObj["type"]?.stringValue ?: return@mapNotNull null
                AppServerThreadItem(
                    type = type,
                    id = itemObj["id"]?.stringValue,
                    payload = itemVal
                )
            }

            AppServerThreadTurn(
                id = turnId,
                status = AppServerTurnStatus.fromString(statusStr),
                items = items
            )
        }

        return AppServerThreadResumeResult(
            id = id,
            preview = result["preview"]?.stringValue,
            cwd = result["cwd"]?.stringValue,
            createdAt = parseDate(result["created_at"]?.stringValue),
            turns = turns
        )
    }

    fun parsePlanUpdate(params: JSONValue?): AppServerPlanUpdate? {
        val obj = params?.objectValue ?: return null
        val stepsArray = obj["steps"]?.arrayValue ?: emptyList()
        val steps = stepsArray.mapNotNull { stepVal ->
            val stepObj = stepVal.objectValue ?: return@mapNotNull null
            val step = stepObj["step"]?.stringValue ?: return@mapNotNull null
            val status = stepObj["status"]?.stringValue ?: "pending"
            AppServerPlanStep(step = step, status = status)
        }
        return AppServerPlanUpdate(
            turnId = obj["turnId"]?.stringValue,
            explanation = obj["explanation"]?.stringValue,
            steps = steps
        )
    }
}
