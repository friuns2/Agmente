package com.agmente.acpclient

import com.agmente.acpclient.config.ACPConnectionState
import com.agmente.acpclient.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.atomic.AtomicInteger

interface ACPServiceDelegate {
    fun onNotification(method: String, params: JSONValue?)
    fun onServerRequest(id: JSONRPCID, method: String, params: JSONValue?)
    fun onConnectionStateChanged(state: ACPConnectionState)
    fun onError(error: Throwable)
}

class ACPService(
    private val client: ACPClient
) : ACPClientDelegate {

    private var delegate: ACPServiceDelegate? = null
    private val requestIdSequence = AtomicInteger(1)
    private val pendingRequests = mutableMapOf<String, CompletableDeferred<JSONRPCMessage>>()
    private val pendingMutex = Mutex()

    fun setDelegate(delegate: ACPServiceDelegate) {
        this.delegate = delegate
        client.setDelegate(this)
    }

    private fun nextRequestId(): JSONRPCID =
        JSONRPCID.IntId(requestIdSequence.getAndIncrement())

    private fun requestIdKey(id: JSONRPCID): String = when (id) {
        is JSONRPCID.IntId -> id.value.toString()
        is JSONRPCID.StringId -> id.value
    }

    suspend fun sendRequest(method: String, params: JSONValue?): JSONRPCMessage {
        val id = nextRequestId()
        val request = JSONRPCRequest(id = id, method = method, params = params)
        val message = JSONRPCMessage.Request(request)

        val deferred = CompletableDeferred<JSONRPCMessage>()
        val key = requestIdKey(id)

        pendingMutex.withLock {
            pendingRequests[key] = deferred
        }

        try {
            client.send(message)
            return deferred.await()
        } catch (e: Exception) {
            pendingMutex.withLock {
                pendingRequests.remove(key)
            }
            throw e
        }
    }

    suspend fun sendNotification(method: String, params: JSONValue? = null) {
        val notification = JSONRPCNotification(method = method, params = params)
        client.send(JSONRPCMessage.Notification(notification))
    }

    suspend fun sendResponse(id: JSONRPCID, result: JSONValue?) {
        val response = JSONRPCResponse(id = id, result = result)
        client.send(JSONRPCMessage.Response(response))
    }

    suspend fun initialize(payload: ACPInitializationPayload): JSONRPCMessage =
        sendRequest(ACPMethods.INITIALIZE, payload.params())

    suspend fun createSession(payload: ACPSessionCreatePayload): JSONRPCMessage =
        sendRequest(ACPMethods.SESSION_NEW, payload.params())

    suspend fun loadSession(payload: ACPSessionLoadPayload): JSONRPCMessage =
        sendRequest(ACPMethods.SESSION_LOAD, payload.params())

    suspend fun resumeSession(payload: ACPSessionResumePayload): JSONRPCMessage =
        sendRequest(ACPMethods.SESSION_RESUME, payload.params())

    suspend fun sendPrompt(payload: ACPSessionPromptPayload): JSONRPCMessage =
        sendRequest(ACPMethods.SESSION_PROMPT, payload.params())

    suspend fun cancelSession(payload: ACPSessionCancelPayload): JSONRPCMessage =
        sendRequest(ACPMethods.SESSION_CANCEL, payload.params())

    suspend fun listSessions(payload: ACPSessionListPayload): JSONRPCMessage =
        sendRequest(ACPMethods.SESSION_LIST, payload.params())

    suspend fun setSessionMode(payload: ACPSessionSetModePayload): JSONRPCMessage =
        sendRequest(ACPMethods.SESSION_SET_MODE, payload.params())

    suspend fun callJSONRPC(method: String, params: JSONValue?): JSONRPCMessage =
        sendRequest(method, params)

    // ACPClientDelegate

    override fun onConnectionStateChanged(state: ACPConnectionState) {
        delegate?.onConnectionStateChanged(state)
        if (state is ACPConnectionState.Disconnected || state is ACPConnectionState.Failed) {
            failAllPending(
                if (state is ACPConnectionState.Failed) state.error
                else ACPClientError.Disconnected
            )
        }
    }

    override fun onMessageReceived(message: JSONRPCMessage) {
        when (message) {
            is JSONRPCMessage.Response -> {
                val key = requestIdKey(message.value.id)
                kotlinx.coroutines.runBlocking {
                    pendingMutex.withLock {
                        pendingRequests.remove(key)
                    }
                }?.complete(message)
            }
            is JSONRPCMessage.Error -> {
                val id = message.value.id
                if (id != null) {
                    val key = requestIdKey(id)
                    kotlinx.coroutines.runBlocking {
                        pendingMutex.withLock {
                            pendingRequests.remove(key)
                        }
                    }?.complete(message)
                }
            }
            is JSONRPCMessage.Notification -> {
                delegate?.onNotification(message.value.method, message.value.params)
            }
            is JSONRPCMessage.Request -> {
                delegate?.onServerRequest(message.value.id, message.value.method, message.value.params)
            }
        }
    }

    override fun onError(error: Throwable) {
        delegate?.onError(error)
    }

    private fun failAllPending(error: Throwable) {
        kotlinx.coroutines.runBlocking {
            pendingMutex.withLock {
                pendingRequests.values.forEach { it.completeExceptionally(error) }
                pendingRequests.clear()
            }
        }
    }
}
