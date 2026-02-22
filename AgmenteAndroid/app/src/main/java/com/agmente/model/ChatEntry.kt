package com.agmente.model

import java.util.UUID

sealed class ChatEntry {
    abstract val id: String

    data class UserText(
        override val id: String = UUID.randomUUID().toString(),
        val text: String
    ) : ChatEntry()

    data class AssistantMarkdown(
        override val id: String = UUID.randomUUID().toString(),
        val markdown: String,
        val isStreaming: Boolean = false
    ) : ChatEntry()

    data class AssistantThought(
        override val id: String = UUID.randomUUID().toString(),
        val text: String
    ) : ChatEntry()

    data class AssistantPlan(
        override val id: String = UUID.randomUUID().toString(),
        val explanation: String?,
        val steps: List<PlanStepInfo>
    ) : ChatEntry()

    data class ToolCallEntry(
        override val id: String = UUID.randomUUID().toString(),
        val toolName: String,
        val toolCallId: String?,
        val input: String?,
        val output: String?,
        val status: String?
    ) : ChatEntry()

    data class FileChangesEntry(
        override val id: String = UUID.randomUUID().toString(),
        val diff: String
    ) : ChatEntry()

    data class SystemMessage(
        override val id: String = UUID.randomUUID().toString(),
        val text: String
    ) : ChatEntry()

    data class ErrorMessage(
        override val id: String = UUID.randomUUID().toString(),
        val text: String
    ) : ChatEntry()

    data class StreamingIndicator(
        override val id: String = "streaming-indicator"
    ) : ChatEntry()
}

object ChatEntryMapper {
    fun map(messages: List<ChatMessage>): List<ChatEntry> {
        val entries = mutableListOf<ChatEntry>()

        for (message in messages) {
            when (message.role) {
                ChatMessageRole.USER -> {
                    entries.add(ChatEntry.UserText(id = message.id, text = message.content))
                }
                ChatMessageRole.ASSISTANT -> {
                    if (message.segments.isEmpty()) {
                        entries.add(
                            ChatEntry.AssistantMarkdown(
                                id = message.id,
                                markdown = message.content,
                                isStreaming = message.isStreaming
                            )
                        )
                    } else {
                        for ((idx, segment) in message.segments.withIndex()) {
                            val segId = "${message.id}-seg-$idx"
                            when (segment) {
                                is MessageSegment.Text -> entries.add(
                                    ChatEntry.AssistantMarkdown(
                                        id = segId,
                                        markdown = segment.text,
                                        isStreaming = message.isStreaming && idx == message.segments.lastIndex
                                    )
                                )
                                is MessageSegment.Thought -> entries.add(
                                    ChatEntry.AssistantThought(id = segId, text = segment.text)
                                )
                                is MessageSegment.ToolCall -> entries.add(
                                    ChatEntry.ToolCallEntry(
                                        id = segId,
                                        toolName = segment.toolName,
                                        toolCallId = segment.toolCallId,
                                        input = segment.input,
                                        output = segment.output,
                                        status = segment.status
                                    )
                                )
                                is MessageSegment.Plan -> entries.add(
                                    ChatEntry.AssistantPlan(
                                        id = segId,
                                        explanation = segment.explanation,
                                        steps = segment.steps
                                    )
                                )
                                is MessageSegment.FileChanges -> entries.add(
                                    ChatEntry.FileChangesEntry(id = segId, diff = segment.diff)
                                )
                            }
                        }
                    }
                }
                ChatMessageRole.SYSTEM -> {
                    entries.add(ChatEntry.SystemMessage(id = message.id, text = message.content))
                }
                ChatMessageRole.ERROR -> {
                    entries.add(ChatEntry.ErrorMessage(id = message.id, text = message.content))
                }
            }
        }

        return entries
    }
}
