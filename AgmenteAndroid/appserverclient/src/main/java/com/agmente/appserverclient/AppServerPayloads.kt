package com.agmente.appserverclient

import com.agmente.appserverclient.model.JSONValue

data class AppServerClientInfo(
    val name: String,
    val version: String
)

data class AppServerInitializePayload(
    val clientInfo: AppServerClientInfo,
    val capabilities: Map<String, JSONValue> = emptyMap()
) {
    fun params(): JSONValue = JSONValue.obj(
        buildMap {
            put("clientInfo", JSONValue.obj(mapOf(
                "name" to JSONValue.string(clientInfo.name),
                "version" to JSONValue.string(clientInfo.version)
            )))
            if (capabilities.isNotEmpty()) {
                put("capabilities", JSONValue.obj(capabilities))
            }
        }
    )
}

enum class AppServerApprovalPolicy(val value: String) {
    ON_REQUEST("on-request"),
    NEVER("never"),
    UNLESS_ALLOW_LISTED("unless-allow-listed");

    fun toJSONValue() = JSONValue.string(value)
}

enum class AppServerSandboxMode(val value: String) {
    WORKSPACE_WRITE("workspaceWrite"),
    DANGER_FULL_ACCESS("dangerFullAccess");

    fun toJSONValue() = JSONValue.string(value)
}

data class AppServerSandboxPolicy(
    val type: AppServerSandboxMode
) {
    fun toJSONValue() = JSONValue.obj(mapOf("type" to type.toJSONValue()))
}

enum class AppServerReasoningEffort(val value: String) {
    LOW("low"), MEDIUM("medium"), HIGH("high")
}

enum class AppServerReasoningSummary(val value: String) {
    CONCISE("concise"), DETAILED("detailed"), AUTO("auto")
}

data class AppServerThreadStartPayload(
    val cwd: String? = null,
    val model: String? = null,
    val persistExtendedHistory: Boolean? = null
) {
    fun params(): JSONValue = JSONValue.obj(
        buildMap {
            cwd?.let { put("cwd", JSONValue.string(it)) }
            model?.let { put("model", JSONValue.string(it)) }
            persistExtendedHistory?.let { put("persistExtendedHistory", JSONValue.bool(it)) }
        }
    )
}

data class AppServerThreadResumePayload(
    val threadId: String,
    val persistExtendedHistory: Boolean? = null
) {
    fun params(): JSONValue = JSONValue.obj(
        buildMap {
            put("threadId", JSONValue.string(threadId))
            persistExtendedHistory?.let { put("persistExtendedHistory", JSONValue.bool(it)) }
        }
    )
}

data class AppServerThreadListPayload(
    val limit: Int? = null,
    val cursor: String? = null
) {
    fun params(): JSONValue = JSONValue.obj(
        buildMap {
            limit?.let { put("limit", JSONValue.number(it.toDouble())) }
            cursor?.let { put("cursor", JSONValue.string(it)) }
        }
    )
}

data class AppServerThreadArchivePayload(
    val threadId: String
) {
    fun params(): JSONValue = JSONValue.obj(
        mapOf("threadId" to JSONValue.string(threadId))
    )
}

sealed class AppServerUserInput {
    data class Text(val text: String) : AppServerUserInput()
    data class Image(val url: String, val detail: String? = null) : AppServerUserInput()
    data class LocalImage(val path: String, val detail: String? = null) : AppServerUserInput()

    fun toJSONValue(): JSONValue = when (this) {
        is Text -> JSONValue.obj(mapOf(
            "type" to JSONValue.string("text"),
            "text" to JSONValue.string(text)
        ))
        is Image -> JSONValue.obj(buildMap {
            put("type", JSONValue.string("image_url"))
            put("image_url", JSONValue.obj(buildMap {
                put("url", JSONValue.string(url))
                detail?.let { put("detail", JSONValue.string(it)) }
            }))
        })
        is LocalImage -> JSONValue.obj(buildMap {
            put("type", JSONValue.string("local_image"))
            put("path", JSONValue.string(path))
            detail?.let { put("detail", JSONValue.string(it)) }
        })
    }
}

data class AppServerTurnStartPayload(
    val threadId: String,
    val input: List<AppServerUserInput>,
    val model: String? = null,
    val approvalPolicy: AppServerApprovalPolicy? = null,
    val sandboxPolicy: AppServerSandboxPolicy? = null
) {
    fun params(): JSONValue = JSONValue.obj(
        buildMap {
            put("threadId", JSONValue.string(threadId))
            put("input", JSONValue.array(input.map { it.toJSONValue() }))
            model?.let { put("model", JSONValue.string(it)) }
            approvalPolicy?.let { put("approvalPolicy", it.toJSONValue()) }
            sandboxPolicy?.let { put("sandboxPolicy", it.toJSONValue()) }
        }
    )
}

data class AppServerTurnInterruptPayload(
    val threadId: String,
    val turnId: String
) {
    fun params(): JSONValue = JSONValue.obj(
        mapOf(
            "threadId" to JSONValue.string(threadId),
            "turnId" to JSONValue.string(turnId)
        )
    )
}

data class AppServerModelListPayload(
    val dummy: Unit = Unit
) {
    fun params(): JSONValue = JSONValue.obj(emptyMap())
}

data class AppServerSkillsListPayload(
    val dummy: Unit = Unit
) {
    fun params(): JSONValue = JSONValue.obj(emptyMap())
}
