package com.agmente.acpclient.websocket

import kotlinx.coroutines.suspendCancellableCoroutine
import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
import java.net.URI
import java.nio.ByteBuffer
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class JavaWebSocketProvider : WebSocketProvider {
    override fun createConnection(): WebSocketConnection =
        JavaWebSocketConnection()
}

class JavaWebSocketConnection : WebSocketConnection {

    private var client: WebSocketClient? = null
    private var eventListener: ((WebSocketEvent) -> Unit)? = null

    override fun onEvent(listener: (WebSocketEvent) -> Unit) {
        eventListener = listener
    }

    override suspend fun connect(url: String, headers: Map<String, String>) {
        suspendCancellableCoroutine { continuation ->
            val uri = URI(url)
            val wsClient = object : WebSocketClient(uri, headers) {
                override fun onOpen(handshakedata: ServerHandshake?) {
                    eventListener?.invoke(WebSocketEvent.Connected)
                    if (continuation.isActive) {
                        continuation.resume(Unit)
                    }
                }

                override fun onMessage(message: String?) {
                    if (message != null) {
                        eventListener?.invoke(WebSocketEvent.Text(message))
                    }
                }

                override fun onMessage(bytes: ByteBuffer?) {
                    if (bytes != null) {
                        val arr = ByteArray(bytes.remaining())
                        bytes.get(arr)
                        eventListener?.invoke(WebSocketEvent.Binary(arr))
                    }
                }

                override fun onClose(code: Int, reason: String?, remote: Boolean) {
                    eventListener?.invoke(WebSocketEvent.Closed(code, reason ?: ""))
                }

                override fun onError(ex: Exception?) {
                    val error = ex ?: Exception("Unknown WebSocket error")
                    eventListener?.invoke(WebSocketEvent.Error(error))
                    if (continuation.isActive) {
                        continuation.resumeWithException(error)
                    }
                }
            }

            wsClient.connectionLostTimeout = 30
            client = wsClient
            wsClient.connect()

            continuation.invokeOnCancellation {
                wsClient.close(1000, "Cancelled")
            }
        }
    }

    override suspend fun send(text: String) {
        client?.send(text) ?: throw IllegalStateException("WebSocket not connected")
    }

    override suspend fun close(code: Int, reason: String?) {
        client?.close(code, reason)
        client = null
    }
}
