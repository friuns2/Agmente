package com.agmente.acpclient

import com.agmente.acpclient.config.ACPClientConfiguration
import com.agmente.acpclient.config.ACPConnectionState
import com.agmente.acpclient.model.*
import com.agmente.acpclient.websocket.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

interface ACPClientDelegate {
    fun onConnectionStateChanged(state: ACPConnectionState)
    fun onMessageReceived(message: JSONRPCMessage)
    fun onError(error: Throwable)
}

class ACPClient(
    private val webSocketProvider: WebSocketProvider = OkHttpWebSocketProvider()
) {
    private var connection: WebSocketConnection? = null
    private var delegate: ACPClientDelegate? = null
    private var config: ACPClientConfiguration? = null

    private val _connectionState = MutableStateFlow<ACPConnectionState>(ACPConnectionState.Disconnected)
    val connectionState: StateFlow<ACPConnectionState> = _connectionState

    fun setDelegate(delegate: ACPClientDelegate) {
        this.delegate = delegate
    }

    suspend fun connect(config: ACPClientConfiguration) {
        this.config = config
        _connectionState.value = ACPConnectionState.Connecting
        delegate?.onConnectionStateChanged(ACPConnectionState.Connecting)

        try {
            val conn = webSocketProvider.createConnection()
            connection = conn

            conn.onEvent { event ->
                when (event) {
                    is WebSocketEvent.Connected -> {
                        _connectionState.value = ACPConnectionState.Connected
                        delegate?.onConnectionStateChanged(ACPConnectionState.Connected)
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
                        _connectionState.value = ACPConnectionState.Disconnected
                        delegate?.onConnectionStateChanged(ACPConnectionState.Disconnected)
                    }
                    is WebSocketEvent.Error -> {
                        _connectionState.value = ACPConnectionState.Failed(event.error)
                        delegate?.onConnectionStateChanged(ACPConnectionState.Failed(event.error))
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
            _connectionState.value = ACPConnectionState.Failed(e)
            delegate?.onConnectionStateChanged(ACPConnectionState.Failed(e))
            throw e
        }
    }

    suspend fun send(message: JSONRPCMessage) {
        val conn = connection ?: throw ACPClientError.Disconnected
        var text = encodeJSONRPCMessage(message)
        if (config?.appendNewline == true) {
            text += "\n"
        }
        conn.send(text)
    }

    suspend fun disconnect() {
        connection?.close()
        connection = null
        _connectionState.value = ACPConnectionState.Disconnected
        delegate?.onConnectionStateChanged(ACPConnectionState.Disconnected)
    }
}
