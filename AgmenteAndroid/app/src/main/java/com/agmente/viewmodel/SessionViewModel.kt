package com.agmente.viewmodel

import com.agmente.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import java.util.UUID

class SessionViewModel(
    val sessionId: String,
    initialCwd: String? = null
) {
    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages

    private val _chatEntries = MutableStateFlow<List<ChatEntry>>(emptyList())
    val chatEntries: StateFlow<List<ChatEntry>> = _chatEntries

    private val _isStreaming = MutableStateFlow(false)
    val isStreaming: StateFlow<Boolean> = _isStreaming

    private val _currentMode = MutableStateFlow<String?>(null)
    val currentMode: StateFlow<String?> = _currentMode

    var cwd: String? = initialCwd
        private set

    private var streamingMessageId: String? = null
    private var streamingContent = StringBuilder()

    fun addUserMessage(text: String) {
        val message = ChatMessage(
            role = ChatMessageRole.USER,
            content = text
        )
        _messages.update { it + message }
        rebuildEntries()
    }

    fun startStreaming() {
        _isStreaming.value = true
        streamingMessageId = UUID.randomUUID().toString()
        streamingContent = StringBuilder()

        val message = ChatMessage(
            id = streamingMessageId!!,
            role = ChatMessageRole.ASSISTANT,
            content = "",
            isStreaming = true
        )
        _messages.update { it + message }
        rebuildEntries()
    }

    fun appendStreamingContent(text: String) {
        streamingContent.append(text)
        val id = streamingMessageId ?: return

        _messages.update { messages ->
            messages.map {
                if (it.id == id) it.copy(content = streamingContent.toString(), isStreaming = true)
                else it
            }
        }
        rebuildEntries()
    }

    fun finishStreaming(finalContent: String? = null) {
        val id = streamingMessageId ?: return
        _isStreaming.value = false

        val content = finalContent ?: streamingContent.toString()
        _messages.update { messages ->
            messages.map {
                if (it.id == id) it.copy(content = content, isStreaming = false)
                else it
            }
        }
        streamingMessageId = null
        rebuildEntries()
    }

    fun addAssistantMessage(content: String, segments: List<MessageSegment> = emptyList()) {
        val message = ChatMessage(
            role = ChatMessageRole.ASSISTANT,
            content = content,
            segments = segments
        )
        _messages.update { it + message }
        rebuildEntries()
    }

    fun addSystemMessage(text: String) {
        val message = ChatMessage(role = ChatMessageRole.SYSTEM, content = text)
        _messages.update { it + message }
        rebuildEntries()
    }

    fun addErrorMessage(text: String) {
        val message = ChatMessage(role = ChatMessageRole.ERROR, content = text)
        _messages.update { it + message }
        rebuildEntries()
    }

    fun addToolCall(
        toolName: String,
        toolCallId: String?,
        input: String?,
        output: String?,
        status: String?
    ) {
        val segment = MessageSegment.ToolCall(toolName, toolCallId, input, output, status)
        addAssistantMessage("", listOf(segment))
    }

    fun updateToolCall(toolCallId: String, output: String?, status: String?) {
        _messages.update { messages ->
            messages.map { msg ->
                if (msg.role == ChatMessageRole.ASSISTANT) {
                    val updatedSegments = msg.segments.map { seg ->
                        if (seg is MessageSegment.ToolCall && seg.toolCallId == toolCallId) {
                            seg.copy(
                                output = output ?: seg.output,
                                status = status ?: seg.status
                            )
                        } else seg
                    }
                    msg.copy(segments = updatedSegments)
                } else msg
            }
        }
        rebuildEntries()
    }

    fun setMode(modeId: String) {
        _currentMode.value = modeId
    }

    fun setCwd(newCwd: String) {
        cwd = newCwd
    }

    fun clearMessages() {
        _messages.value = emptyList()
        _chatEntries.value = emptyList()
        _isStreaming.value = false
        streamingMessageId = null
    }

    fun setMessages(messages: List<ChatMessage>) {
        _messages.value = messages
        rebuildEntries()
    }

    private fun rebuildEntries() {
        val entries = ChatEntryMapper.map(_messages.value).toMutableList()
        if (_isStreaming.value) {
            entries.add(ChatEntry.StreamingIndicator())
        }
        _chatEntries.value = entries
    }
}
