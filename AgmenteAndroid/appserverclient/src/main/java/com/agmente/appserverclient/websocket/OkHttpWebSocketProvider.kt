package com.agmente.appserverclient.websocket

import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.*
import okio.ByteString
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class OkHttpWebSocketProvider(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .protocols(listOf(Protocol.HTTP_1_1))
        .pingInterval(30, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .build()
) : WebSocketProvider {
    override fun createConnection(): WebSocketConnection =
        OkHttpWebSocketConnection(client)
}

class OkHttpWebSocketConnection(
    private val client: OkHttpClient
) : WebSocketConnection {

    private var webSocket: WebSocket? = null
    private var eventListener: ((WebSocketEvent) -> Unit)? = null

    override fun onEvent(listener: (WebSocketEvent) -> Unit) {
        eventListener = listener
    }

    override suspend fun connect(url: String, headers: Map<String, String>) {
        suspendCancellableCoroutine { continuation ->
            val requestBuilder = Request.Builder().url(url)
            headers.forEach { (key, value) ->
                requestBuilder.addHeader(key, value)
            }
            val request = requestBuilder.build()

            val listener = object : WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: Response) {
                    this@OkHttpWebSocketConnection.webSocket = webSocket
                    eventListener?.invoke(WebSocketEvent.Connected)
                    if (continuation.isActive) {
                        continuation.resume(Unit)
                    }
                }

                override fun onMessage(webSocket: WebSocket, text: String) {
                    eventListener?.invoke(WebSocketEvent.Text(text))
                }

                override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                    eventListener?.invoke(WebSocketEvent.Binary(bytes.toByteArray()))
                }

                override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                    webSocket.close(code, reason)
                    eventListener?.invoke(WebSocketEvent.Closed(code, reason))
                }

                override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                    eventListener?.invoke(WebSocketEvent.Closed(code, reason))
                }

                override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                    eventListener?.invoke(WebSocketEvent.Error(t))
                    if (continuation.isActive) {
                        continuation.resumeWithException(t)
                    }
                }
            }

            webSocket = client.newWebSocket(request, listener)

            continuation.invokeOnCancellation {
                webSocket?.close(1000, "Cancelled")
            }
        }
    }

    override suspend fun send(text: String) {
        webSocket?.send(text) ?: throw IllegalStateException("WebSocket not connected")
    }

    override suspend fun close(code: Int, reason: String?) {
        webSocket?.close(code, reason)
        webSocket = null
    }
}
