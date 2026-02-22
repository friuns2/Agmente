package com.agmente.appserverclient.model

import com.agmente.appserverclient.*

sealed class AppServerEvent {
    data class ThreadStarted(val thread: AppServerThreadSummary) : AppServerEvent()
    data class TurnStarted(val threadId: String, val turn: AppServerTurnSummary) : AppServerEvent()
    data class TurnCompleted(val threadId: String, val turn: AppServerTurnSummary) : AppServerEvent()
    data class AgentMessageDelta(val threadId: String, val turnId: String, val delta: String) : AppServerEvent()
    data class ItemStarted(val threadId: String, val turnId: String, val item: AppServerThreadItem) : AppServerEvent()
    data class ItemCompleted(val threadId: String, val turnId: String, val item: AppServerThreadItem) : AppServerEvent()
    data class DiffUpdated(val threadId: String, val turnId: String, val diff: JSONValue) : AppServerEvent()
    data class PlanUpdated(val update: AppServerPlanUpdate) : AppServerEvent()
    data class TokenUsageUpdated(val threadId: String, val turnId: String?, val payload: JSONValue) : AppServerEvent()
    data class ApprovalRequested(val method: String, val requestId: JSONRPCID, val params: JSONValue?) : AppServerEvent()
    data class GenericNotification(val method: String, val params: JSONValue?) : AppServerEvent()
    data class GenericRequest(val id: JSONRPCID, val method: String, val params: JSONValue?) : AppServerEvent()
}
