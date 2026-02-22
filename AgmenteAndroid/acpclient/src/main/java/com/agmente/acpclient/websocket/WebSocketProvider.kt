package com.agmente.acpclient.websocket

sealed class WebSocketEvent {
    data object Connected : WebSocketEvent()
    data class Text(val text: String) : WebSocketEvent()
    data class Binary(val data: ByteArray) : WebSocketEvent() {
        override fun equals(other: Any?) =
            other is Binary && data.contentEquals(other.data)
        override fun hashCode() = data.contentHashCode()
    }
    data class Closed(val code: Int, val reason: String?) : WebSocketEvent()
    data class Error(val error: Throwable) : WebSocketEvent()
}

interface WebSocketConnection {
    suspend fun connect(url: String, headers: Map<String, String>)
    suspend fun send(text: String)
    suspend fun close(code: Int = 1000, reason: String? = null)
    fun onEvent(listener: (WebSocketEvent) -> Unit)
}

interface WebSocketProvider {
    fun createConnection(): WebSocketConnection
}
