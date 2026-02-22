package com.agmente.acpclient

import com.agmente.acpclient.model.*
import java.text.SimpleDateFormat
import java.util.*

object ACPSessionListParser {
    private val dateFormats = listOf(
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        },
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
    )

    fun parse(response: JSONRPCMessage): List<SessionSummary> {
        val result = when (response) {
            is JSONRPCMessage.Response -> response.value.result
            else -> null
        } ?: return emptyList()

        val sessionsArray = when (result) {
            is JSONValue.ArrayValue -> result.value
            is JSONValue.ObjectValue -> result.value["sessions"]?.arrayValue
            else -> null
        } ?: return emptyList()

        return sessionsArray.mapNotNull { value ->
            val obj = value.objectValue ?: return@mapNotNull null
            val id = obj["id"]?.stringValue ?: obj["sessionId"]?.stringValue ?: return@mapNotNull null
            val title = obj["title"]?.stringValue ?: obj["summary"]?.stringValue
            val cwd = obj["cwd"]?.stringValue
            val updatedAtStr = obj["updatedAt"]?.stringValue ?: obj["lastActivityAt"]?.stringValue
            val updatedAt = updatedAtStr?.let { parseDate(it) }

            SessionSummary(id = id, title = title, cwd = cwd, updatedAt = updatedAt)
        }
    }

    private fun parseDate(dateString: String): Date? {
        for (format in dateFormats) {
            try {
                return format.parse(dateString)
            } catch (_: Exception) { }
        }
        return null
    }
}
