package com.agmente.appserverclient

import com.agmente.appserverclient.config.AppServerClientConfiguration
import com.agmente.appserverclient.config.AppServerConnectionState
import com.agmente.appserverclient.model.*
import com.agmente.appserverclient.websocket.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

interface AppServerClientDelegate {
    fun onConnectionStateChanged(state: AppServerConnectionState)
    fun onMessageReceived(message: JSONRPCMessage)
    fun onError(error: Throwable)
}

class AppServerClient(
    private val webSocketProvider: WebSocketProvider = OkHttpWebSocketProvider()
) {
    private var connection: WebSocketConnection? = null
    private var delegate: AppServerClientDelegate? = null
    private var config: AppServerClientConfiguration? = null

    private val _connectionState = MutableStateFlow<AppServerConnectionState>(AppServerConnectionState.Disconnected)
    val connectionState: StateFlow<AppServerConnectionState> = _connectionState

    fun setDelegate(delegate: AppServerClientDelegate) {
        this.delegate = delegate
    }

    suspend fun connect(config: AppServerClientConfiguration) {
        this.config = config
        _connectionState.value = AppServerConnectionState.Connecting
        delegate?.onConnectionStateChanged(AppServerConnectionState.Connecting)

        try {
            val conn = webSocketProvider.createConnection()
            connection = conn

            conn.onEvent { event ->
                when (event) {
                    is WebSocketEvent.Connected -> {
                        _connectionState.value = AppServerConnectionState.Connected
                        delegate?.onConnectionStateChanged(AppServerConnectionState.Connected)
                    }
                    is WebSocketEvent.Text -> {
                        val lines = event.text.split("\n").filter { it.isNotBlank() }
                        for (line in lines) {
                            try {
                                val message = parseJSONRPCMessage(line)
                                delegate?.onMessageReceived(message)
                            } catch (e: Exception) {
                                delegate?.onError(e)
                            }
                        }
                    }
                    is WebSocketEvent.Closed -> {
                        _connectionState.value = AppServerConnectionState.Disconnected
                        delegate?.onConnectionStateChanged(AppServerConnectionState.Disconnected)
                    }
                    is WebSocketEvent.Error -> {
                        _connectionState.value = AppServerConnectionState.Failed(event.error)
                        delegate?.onConnectionStateChanged(AppServerConnectionState.Failed(event.error))
                        delegate?.onError(event.error)
                    }
                    is WebSocketEvent.Binary -> { /* ignored */ }
                }
            }

            val headers = mutableMapOf<String, String>()
            headers.putAll(config.additionalHeaders)
            config.authTokenProvider?.let { provider ->
                val token = provider()
                if (token.isNotEmpty()) {
                    headers["Authorization"] = "Bearer $token"
                }
            }

            conn.connect(config.endpoint, headers)
        } catch (e: Exception) {
            _connectionState.value = AppServerConnectionState.Failed(e)
            delegate?.onConnectionStateChanged(AppServerConnectionState.Failed(e))
            throw e
        }
    }

    suspend fun send(message: JSONRPCMessage) {
        val conn = connection ?: throw AppServerClientError.Disconnected
        var text = encodeJSONRPCMessage(message)
        if (config?.appendNewline == true) {
            text += "\n"
        }
        conn.send(text)
    }

    suspend fun disconnect() {
        connection?.close()
        connection = null
        _connectionState.value = AppServerConnectionState.Disconnected
        delegate?.onConnectionStateChanged(AppServerConnectionState.Disconnected)
    }
}
