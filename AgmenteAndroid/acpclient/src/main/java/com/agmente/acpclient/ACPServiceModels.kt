package com.agmente.acpclient

import com.agmente.acpclient.model.JSONValue

data class ACPInitializationPayload(
    val protocolVersion: Int = 1,
    val clientName: String,
    val clientVersion: String,
    val clientTitle: String? = null,
    val clientCapabilities: Map<String, JSONValue> = emptyMap(),
    val capabilities: Map<String, JSONValue> = emptyMap(),
    val options: Map<String, JSONValue> = emptyMap()
) {
    fun params(): JSONValue {
        val clientInfo = mutableMapOf<String, JSONValue>(
            "name" to JSONValue.string(clientName),
            "version" to JSONValue.string(clientVersion)
        )
        clientTitle?.let { clientInfo["title"] = JSONValue.string(it) }

        val obj = mutableMapOf<String, JSONValue>(
            "protocolVersion" to JSONValue.number(protocolVersion.toDouble()),
            "clientInfo" to JSONValue.obj(clientInfo),
            "clientCapabilities" to JSONValue.obj(clientCapabilities)
        )
        if (capabilities.isNotEmpty()) {
            obj["capabilities"] = JSONValue.obj(capabilities)
        }
        if (options.isNotEmpty()) {
            obj["options"] = JSONValue.obj(options)
        }
        return JSONValue.obj(obj)
    }
}

data class ACPSessionCreatePayload(
    val workingDirectory: String,
    val mcpServers: List<JSONValue> = emptyList(),
    val agent: String? = null,
    val metadata: Map<String, JSONValue> = emptyMap()
) {
    fun params(): JSONValue {
        val obj = mutableMapOf<String, JSONValue>(
            "cwd" to JSONValue.string(workingDirectory),
            "mcpServers" to JSONValue.array(mcpServers)
        )
        agent?.let { obj["agent"] = JSONValue.string(it) }
        if (metadata.isNotEmpty()) {
            obj["metadata"] = JSONValue.obj(metadata)
        }
        return JSONValue.obj(obj)
    }
}

data class ACPSessionLoadPayload(
    val sessionId: String,
    val workingDirectory: String,
    val mcpServers: List<JSONValue> = emptyList()
) {
    fun params(): JSONValue = JSONValue.obj(
        mapOf(
            "sessionId" to JSONValue.string(sessionId),
            "cwd" to JSONValue.string(workingDirectory),
            "mcpServers" to JSONValue.array(mcpServers)
        )
    )
}

data class ACPSessionResumePayload(
    val sessionId: String,
    val workingDirectory: String,
    val mcpServers: List<JSONValue> = emptyList()
) {
    fun params(): JSONValue = JSONValue.obj(
        mapOf(
            "sessionId" to JSONValue.string(sessionId),
            "cwd" to JSONValue.string(workingDirectory),
            "mcpServers" to JSONValue.array(mcpServers)
        )
    )
}

data class ACPSessionPromptPayload(
    val sessionId: String,
    val prompt: List<JSONValue>,
    val attachments: Map<String, JSONValue> = emptyMap(),
    val stream: Boolean? = null
) {
    fun params(): JSONValue {
        val obj = mutableMapOf<String, JSONValue>(
            "sessionId" to JSONValue.string(sessionId),
            "prompt" to JSONValue.array(prompt)
        )
        if (attachments.isNotEmpty()) {
            obj["attachments"] = JSONValue.obj(attachments)
        }
        stream?.let { obj["stream"] = JSONValue.bool(it) }
        return JSONValue.obj(obj)
    }
}

data class ACPSessionCancelPayload(
    val sessionId: String
) {
    fun params(): JSONValue = JSONValue.obj(
        mapOf("sessionId" to JSONValue.string(sessionId))
    )
}

data class ACPSessionListPayload(
    val limit: Int? = null,
    val cursor: String? = null,
    val workingDirectory: String? = null
) {
    fun params(): JSONValue {
        val obj = mutableMapOf<String, JSONValue>()
        limit?.let { obj["limit"] = JSONValue.number(it.toDouble()) }
        cursor?.let { obj["cursor"] = JSONValue.string(it) }
        workingDirectory?.let { obj["cwd"] = JSONValue.string(it) }
        return JSONValue.obj(obj)
    }
}

data class ACPSessionSetModePayload(
    val sessionId: String,
    val modeId: String
) {
    fun params(): JSONValue = JSONValue.obj(
        mapOf(
            "sessionId" to JSONValue.string(sessionId),
            "modeId" to JSONValue.string(modeId)
        )
    )
}
