package com.agmente.model

import java.util.UUID

enum class ChatMessageRole {
    USER, ASSISTANT, SYSTEM, ERROR
}

sealed class MessageSegment {
    data class Text(val text: String) : MessageSegment()
    data class Thought(val text: String) : MessageSegment()
    data class ToolCall(
        val toolName: String,
        val toolCallId: String?,
        val input: String?,
        val output: String?,
        val status: String?
    ) : MessageSegment()
    data class Plan(
        val explanation: String?,
        val steps: List<PlanStepInfo>
    ) : MessageSegment()
    data class FileChanges(
        val diff: String
    ) : MessageSegment()
}

data class PlanStepInfo(
    val step: String,
    val status: String
)

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val role: ChatMessageRole,
    val content: String,
    val segments: List<MessageSegment> = emptyList(),
    val isStreaming: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)
