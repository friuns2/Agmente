package com.agmente.acpclient

import com.agmente.acpclient.model.*

sealed class ACPResponseAction {
    data class SessionActivated(val sessionId: String, val cwd: String?) : ACPResponseAction()
    data class Initialized(val result: ACPInitializeResult) : ACPResponseAction()
    data class SessionListReceived(val sessions: List<SessionSummary>) : ACPResponseAction()
    data class StopReason(val reason: String) : ACPResponseAction()
    data class RpcError(val code: Int, val message: String) : ACPResponseAction()
}

object ACPResponseDispatcher {
    fun dispatch(method: String, response: JSONRPCMessage): List<ACPResponseAction> {
        return when (response) {
            is JSONRPCMessage.Response -> dispatchSuccess(method, response)
            is JSONRPCMessage.Error -> dispatchError(response)
            else -> emptyList()
        }
    }

    private fun dispatchSuccess(method: String, response: JSONRPCMessage.Response): List<ACPResponseAction> {
        val actions = mutableListOf<ACPResponseAction>()
        val result = response.value.result?.objectValue

        when (method) {
            ACPMethods.INITIALIZE -> {
                val initResult = ACPInitializeParser.parse(response)
                if (initResult != null) {
                    actions.add(ACPResponseAction.Initialized(initResult))
                }
            }
            ACPMethods.SESSION_NEW, ACPMethods.SESSION_LOAD -> {
                val sessionId = result?.get("sessionId")?.stringValue
                val cwd = result?.get("cwd")?.stringValue
                if (sessionId != null) {
                    actions.add(ACPResponseAction.SessionActivated(sessionId, cwd))
                }
            }
            ACPMethods.SESSION_LIST -> {
                val sessions = ACPSessionListParser.parse(response)
                actions.add(ACPResponseAction.SessionListReceived(sessions))
            }
            ACPMethods.SESSION_PROMPT -> {
                val stopReason = result?.get("stopReason")?.stringValue
                if (stopReason != null) {
                    actions.add(ACPResponseAction.StopReason(stopReason))
                }
            }
        }

        return actions
    }

    private fun dispatchError(response: JSONRPCMessage.Error): List<ACPResponseAction> {
        return listOf(
            ACPResponseAction.RpcError(
                code = response.value.error.code,
                message = response.value.error.message
            )
        )
    }
}
