package com.agmente.appserverclient

import com.agmente.appserverclient.config.AppServerConnectionState
import com.agmente.appserverclient.model.*
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.atomic.AtomicInteger

interface AppServerServiceDelegate {
    fun onEvent(event: AppServerEvent)
    fun onConnectionStateChanged(state: AppServerConnectionState)
    fun onError(error: Throwable)
}

class AppServerService(
    private val client: AppServerClient
) : AppServerClientDelegate {

    private var delegate: AppServerServiceDelegate? = null
    private val requestIdSequence = AtomicInteger(1)
    private val pendingRequests = mutableMapOf<String, CompletableDeferred<JSONRPCMessage>>()
    private val pendingMutex = Mutex()

    fun setDelegate(delegate: AppServerServiceDelegate) {
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

    suspend fun initialize(payload: AppServerInitializePayload): JSONRPCMessage =
        sendRequest(AppServerMethods.INITIALIZE, payload.params())

    suspend fun threadStart(payload: AppServerThreadStartPayload): JSONRPCMessage =
        sendRequest(AppServerMethods.THREAD_START, payload.params())

    suspend fun threadResume(payload: AppServerThreadResumePayload): JSONRPCMessage =
        sendRequest(AppServerMethods.THREAD_RESUME, payload.params())

    suspend fun threadList(payload: AppServerThreadListPayload): JSONRPCMessage =
        sendRequest(AppServerMethods.THREAD_LIST, payload.params())

    suspend fun threadArchive(payload: AppServerThreadArchivePayload): JSONRPCMessage =
        sendRequest(AppServerMethods.THREAD_ARCHIVE, payload.params())

    suspend fun turnStart(payload: AppServerTurnStartPayload): JSONRPCMessage =
        sendRequest(AppServerMethods.TURN_START, payload.params())

    suspend fun turnInterrupt(payload: AppServerTurnInterruptPayload): JSONRPCMessage =
        sendRequest(AppServerMethods.TURN_INTERRUPT, payload.params())

    suspend fun modelList(): JSONRPCMessage =
        sendRequest(AppServerMethods.MODEL_LIST, JSONValue.obj(emptyMap()))

    suspend fun skillsList(): JSONRPCMessage =
        sendRequest(AppServerMethods.SKILLS_LIST, JSONValue.obj(emptyMap()))

    // AppServerClientDelegate

    override fun onConnectionStateChanged(state: AppServerConnectionState) {
        delegate?.onConnectionStateChanged(state)
        if (state is AppServerConnectionState.Disconnected || state is AppServerConnectionState.Failed) {
            failAllPending(
                if (state is AppServerConnectionState.Failed) state.error
                else AppServerClientError.Disconnected
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
                val event = AppServerEventParser.parseNotification(
                    message.value.method, message.value.params
                )
                delegate?.onEvent(event)
            }
            is JSONRPCMessage.Request -> {
                val event = AppServerEventParser.parseRequest(
                    message.value.id, message.value.method, message.value.params
                )
                delegate?.onEvent(event)
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
