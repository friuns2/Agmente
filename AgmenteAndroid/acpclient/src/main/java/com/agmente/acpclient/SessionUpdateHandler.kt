package com.agmente.acpclient

import com.agmente.acpclient.model.*

sealed class ACPSessionUpdateEvent {
    data class AgentThought(val text: String) : ACPSessionUpdateEvent()
    data class UserMessage(val text: String) : ACPSessionUpdateEvent()
    data class AgentMessage(val text: String, val isStreaming: Boolean = false) : ACPSessionUpdateEvent()
    data class ToolCall(
        val toolName: String,
        val toolCallId: String?,
        val input: String?,
        val output: String?,
        val status: String?
    ) : ACPSessionUpdateEvent()
    data class ToolCallUpdate(
        val toolCallId: String,
        val output: String?,
        val status: String?
    ) : ACPSessionUpdateEvent()
    data class ModeChange(val modeId: String, val modeName: String?) : ACPSessionUpdateEvent()
    data class AvailableCommandsUpdate(val commands: List<SessionCommand>) : ACPSessionUpdateEvent()
    data class StopReason(val reason: String) : ACPSessionUpdateEvent()
}

object ACPSessionUpdateParser {
    fun parse(params: JSONValue?): List<ACPSessionUpdateEvent> {
        val obj = params?.objectValue ?: return emptyList()
        val events = mutableListOf<ACPSessionUpdateEvent>()

        val updates = obj["updates"]?.arrayValue ?: listOf(params)
        for (update in updates) {
            val updateObj = update.objectValue ?: continue
            parseUpdate(updateObj, events)
        }

        return events
    }

    private fun parseUpdate(update: Map<String, JSONValue>, events: MutableList<ACPSessionUpdateEvent>) {
        val kind = update["kind"]?.stringValue ?: update["type"]?.stringValue

        when (kind) {
            "thought" -> {
                val text = update["text"]?.stringValue ?: update["content"]?.stringValue ?: return
                events.add(ACPSessionUpdateEvent.AgentThought(text))
            }
            "text", "message", "agent_message" -> {
                val text = update["text"]?.stringValue ?: update["content"]?.stringValue ?: return
                val isStreaming = update["streaming"]?.boolValue ?: false
                events.add(ACPSessionUpdateEvent.AgentMessage(text, isStreaming))
            }
            "user_message" -> {
                val text = update["text"]?.stringValue ?: update["content"]?.stringValue ?: return
                events.add(ACPSessionUpdateEvent.UserMessage(text))
            }
            "tool_call", "toolCall" -> {
                val toolName = update["toolName"]?.stringValue ?: update["name"]?.stringValue ?: "unknown"
                val toolCallId = update["toolCallId"]?.stringValue ?: update["id"]?.stringValue
                val input = update["input"]?.stringValue
                val output = update["output"]?.stringValue
                val status = update["status"]?.stringValue
                events.add(ACPSessionUpdateEvent.ToolCall(toolName, toolCallId, input, output, status))
            }
            "tool_call_update", "toolCallUpdate" -> {
                val toolCallId = update["toolCallId"]?.stringValue ?: update["id"]?.stringValue ?: return
                val output = update["output"]?.stringValue
                val status = update["status"]?.stringValue
                events.add(ACPSessionUpdateEvent.ToolCallUpdate(toolCallId, output, status))
            }
            "mode_change", "modeChange" -> {
                val modeId = update["modeId"]?.stringValue ?: return
                val modeName = update["modeName"]?.stringValue
                events.add(ACPSessionUpdateEvent.ModeChange(modeId, modeName))
            }
            "available_commands", "availableCommands" -> {
                val commands = SessionCommand.parse(update)
                events.add(ACPSessionUpdateEvent.AvailableCommandsUpdate(commands))
            }
            "stop", "stop_reason" -> {
                val reason = update["reason"]?.stringValue ?: "unknown"
                events.add(ACPSessionUpdateEvent.StopReason(reason))
            }
        }
    }
}
