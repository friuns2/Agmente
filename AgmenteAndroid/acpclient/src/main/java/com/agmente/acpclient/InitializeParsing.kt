package com.agmente.acpclient

import com.agmente.acpclient.model.*

data class ACPInitializeResult(
    val protocolVersion: Int?,
    val agentProfile: AgentProfile?,
    val isCodex: Boolean
)

object ACPInitializeParser {
    fun parse(response: JSONRPCMessage): ACPInitializeResult? {
        val result = when (response) {
            is JSONRPCMessage.Response -> response.value.result?.objectValue
            else -> null
        } ?: return null

        val protocolVersion = result["protocolVersion"]?.numberValue?.toInt()
        val agentProfile = AgentProfile.parse(result)

        val serverInfo = result["serverInfo"]?.objectValue
        val userAgent = serverInfo?.get("name")?.stringValue ?: ""
        val isCodex = userAgent.lowercase().startsWith("codex")

        return ACPInitializeResult(
            protocolVersion = protocolVersion,
            agentProfile = agentProfile,
            isCodex = isCodex
        )
    }
}
