package com.agmente.acpclient

import com.agmente.acpclient.config.ACPClientConfiguration
import com.agmente.acpclient.config.ACPConnectionState
import com.agmente.acpclient.model.*
import com.agmente.acpclient.websocket.JavaWebSocketProvider
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.UUID

interface ACPClientManagerDelegate {
    fun onConnectionStateChanged(state: ACPConnectionState)
    fun onNotification(method: String, params: JSONValue?)
    fun onServerRequest(id: JSONRPCID, method: String, params: JSONValue?)
    fun onError(error: Throwable)
}

class ACPClientManager : ACPServiceDelegate {

    private var client: ACPClient? = null
    private var service: ACPService? = null
    private var delegate: ACPClientManagerDelegate? = null
    private var config: ACPClientConfiguration? = null
    val clientId: String = UUID.randomUUID().toString()

    private val _connectionState = MutableStateFlow<ACPConnectionState>(ACPConnectionState.Disconnected)
    val connectionState: StateFlow<ACPConnectionState> = _connectionState

    fun setDelegate(delegate: ACPClientManagerDelegate) {
        this.delegate = delegate
    }

    fun getService(): ACPService? = service

    suspend fun connect(config: ACPClientConfiguration) {
        this.config = config

        val acpClient = ACPClient(JavaWebSocketProvider())
        val acpService = ACPService(acpClient)
        acpService.setDelegate(this)

        val augmentedConfig = config.copy(
            additionalHeaders = config.additionalHeaders + mapOf(
                "X-Client-Id" to clientId
            )
        )

        client = acpClient
        service = acpService

        acpClient.connect(augmentedConfig)
    }

    suspend fun disconnect() {
        client?.disconnect()
        client = null
        service = null
    }

    suspend fun initialize(payload: ACPInitializationPayload): JSONRPCMessage {
        val svc = service ?: throw ACPClientError.Disconnected
        return svc.initialize(payload)
    }

    // ACPServiceDelegate

    override fun onNotification(method: String, params: JSONValue?) {
        delegate?.onNotification(method, params)
    }

    override fun onServerRequest(id: JSONRPCID, method: String, params: JSONValue?) {
        delegate?.onServerRequest(id, method, params)
    }

    override fun onConnectionStateChanged(state: ACPConnectionState) {
        _connectionState.value = state
        delegate?.onConnectionStateChanged(state)
    }

    override fun onError(error: Throwable) {
        delegate?.onError(error)
    }
}
