package com.agmente.appserverclient

import com.agmente.appserverclient.model.*

object AppServerEventParser {

    fun parseNotification(method: String, params: JSONValue?): AppServerEvent {
        val obj = params?.objectValue

        return when (method) {
            "thread/started" -> {
                val threadId = obj?.get("id")?.stringValue ?: ""
                AppServerEvent.ThreadStarted(
                    AppServerThreadSummary(
                        id = threadId,
                        preview = obj?.get("preview")?.stringValue,
                        modelProvider = obj?.get("model_provider")?.stringValue,
                        createdAt = null
                    )
                )
            }
            "turn/started" -> {
                val threadId = obj?.get("threadId")?.stringValue ?: ""
                val turnId = obj?.get("id")?.stringValue ?: obj?.get("turnId")?.stringValue ?: ""
                val statusStr = obj?.get("status")?.stringValue ?: "in_progress"
                AppServerEvent.TurnStarted(
                    threadId = threadId,
                    turn = AppServerTurnSummary(
                        id = turnId,
                        status = AppServerTurnStatus.fromString(statusStr)
                    )
                )
            }
            "turn/completed" -> {
                val threadId = obj?.get("threadId")?.stringValue ?: ""
                val turnId = obj?.get("id")?.stringValue ?: obj?.get("turnId")?.stringValue ?: ""
                val statusStr = obj?.get("status")?.stringValue ?: "completed"
                AppServerEvent.TurnCompleted(
                    threadId = threadId,
                    turn = AppServerTurnSummary(
                        id = turnId,
                        status = AppServerTurnStatus.fromString(statusStr)
                    )
                )
            }
            "item/agentMessage/delta" -> {
                val threadId = obj?.get("threadId")?.stringValue ?: ""
                val turnId = obj?.get("turnId")?.stringValue ?: ""
                val delta = obj?.get("delta")?.stringValue ?: ""
                AppServerEvent.AgentMessageDelta(threadId, turnId, delta)
            }
            "item/started" -> {
                val threadId = obj?.get("threadId")?.stringValue ?: ""
                val turnId = obj?.get("turnId")?.stringValue ?: ""
                val type = obj?.get("type")?.stringValue ?: ""
                AppServerEvent.ItemStarted(
                    threadId, turnId,
                    AppServerThreadItem(type = type, id = obj?.get("id")?.stringValue, payload = params)
                )
            }
            "item/completed" -> {
                val threadId = obj?.get("threadId")?.stringValue ?: ""
                val turnId = obj?.get("turnId")?.stringValue ?: ""
                val type = obj?.get("type")?.stringValue ?: ""
                AppServerEvent.ItemCompleted(
                    threadId, turnId,
                    AppServerThreadItem(type = type, id = obj?.get("id")?.stringValue, payload = params)
                )
            }
            "turn/diff/updated" -> {
                val threadId = obj?.get("threadId")?.stringValue ?: ""
                val turnId = obj?.get("turnId")?.stringValue ?: ""
                AppServerEvent.DiffUpdated(threadId, turnId, params ?: JSONValue.Null)
            }
            "turn/plan/updated" -> {
                val planUpdate = AppServerResponseParser.parsePlanUpdate(params)
                    ?: AppServerPlanUpdate(null, null, emptyList())
                AppServerEvent.PlanUpdated(planUpdate)
            }
            "thread/tokenUsage/updated" -> {
                val threadId = obj?.get("threadId")?.stringValue ?: ""
                val turnId = obj?.get("turnId")?.stringValue
                AppServerEvent.TokenUsageUpdated(threadId, turnId, params ?: JSONValue.Null)
            }
            else -> AppServerEvent.GenericNotification(method, params)
        }
    }

    fun parseRequest(id: JSONRPCID, method: String, params: JSONValue?): AppServerEvent {
        return when (method) {
            "item/commandExecution/requestApproval",
            "item/fileChange/requestApproval" ->
                AppServerEvent.ApprovalRequested(method, id, params)
            else ->
                AppServerEvent.GenericRequest(id, method, params)
        }
    }
}
