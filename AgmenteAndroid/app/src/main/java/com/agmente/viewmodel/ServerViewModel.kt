package com.agmente.viewmodel

import com.agmente.acpclient.*
import com.agmente.acpclient.config.ACPClientConfiguration
import com.agmente.acpclient.config.ACPConnectionState
import com.agmente.acpclient.model.*
import com.agmente.data.SessionStorage
import com.agmente.data.StoredSessionInfo
import com.agmente.model.MessageSegment
import com.agmente.model.ServerConfiguration
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class ServerViewModel(
    private val config: ServerConfiguration,
    private val storage: SessionStorage,
    private val scope: CoroutineScope
) : ServerViewModelContract, ACPClientManagerDelegate {

    override val id: String = config.id

    private val clientManager = ACPClientManager()

    override val connectionState: StateFlow<ACPConnectionState> = clientManager.connectionState

    private val _isInitialized = MutableStateFlow(false)
    override val isInitialized: StateFlow<Boolean> = _isInitialized

    private val _sessionSummaries = MutableStateFlow<List<SessionSummary>>(emptyList())
    override val sessionSummaries: StateFlow<List<SessionSummary>> = _sessionSummaries

    private val _selectedSessionId = MutableStateFlow<String?>(null)
    override val selectedSessionId: StateFlow<String?> = _selectedSessionId

    private val _isStreaming = MutableStateFlow(false)
    override val isStreaming: StateFlow<Boolean> = _isStreaming

    private val _agentInfo = MutableStateFlow<AgentProfile?>(null)
    override val agentInfo: StateFlow<AgentProfile?> = _agentInfo

    private val sessionViewModels = mutableMapOf<String, SessionViewModel>()

    override val currentSessionViewModel: SessionViewModel?
        get() = _selectedSessionId.value?.let { sessionViewModels[it] }

    override val availableModes: List<AgentModeOption>
        get() = _agentInfo.value?.modes ?: emptyList()

    override val initializationSummary: String
        get() {
            val info = _agentInfo.value ?: return "Not initialized"
            return "${info.displayNameWithVersion} | ACP"
        }

    init {
        clientManager.setDelegate(this)
    }

    private fun buildACPConfig(): ACPClientConfiguration = ACPClientConfiguration(
        endpoint = config.endpointURLString,
        authTokenProvider = if (config.token.isNotEmpty()) {
            { config.token }
        } else null,
        additionalHeaders = buildMap {
            if (config.cfAccessClientId.isNotEmpty()) {
                put("CF-Access-Client-Id", config.cfAccessClientId)
                put("CF-Access-Client-Secret", config.cfAccessClientSecret)
            }
        }
    )

    private suspend fun doInitialize() {
        val payload = ACPInitializationPayload(
            clientName = "Agmente Android",
            clientVersion = "1.0.0",
            clientCapabilities = mapOf(
                "filesystem" to JSONValue.obj(mapOf(
                    "read" to JSONValue.bool(true),
                    "write" to JSONValue.bool(true)
                )),
                "terminal" to JSONValue.obj(mapOf(
                    "support" to JSONValue.bool(true)
                ))
            )
        )
        val response = clientManager.initialize(payload)
        val initResult = ACPInitializeParser.parse(response)
        if (initResult != null) {
            _agentInfo.value = initResult.agentProfile
            _isInitialized.value = true

            val svc = clientManager.getService()
            svc?.sendNotification("initialized")
        }
    }

    fun connect() {
        scope.launch {
            try {
                clientManager.connect(buildACPConfig())
            } catch (_: Exception) { }
        }
    }

    fun initialize() {
        scope.launch {
            try {
                doInitialize()
            } catch (_: Exception) { }
        }
    }

    fun connectAndInitialize() {
        scope.launch {
            try {
                clientManager.connect(buildACPConfig())
                doInitialize()
            } catch (_: Exception) { }
        }
    }

    fun disconnect() {
        scope.launch {
            clientManager.disconnect()
            _isInitialized.value = false
        }
    }

    override fun fetchSessionList(force: Boolean) {
        scope.launch {
            try {
                val svc = clientManager.getService() ?: return@launch
                val response = svc.listSessions(ACPSessionListPayload())
                val sessions = ACPSessionListParser.parse(response)
                _sessionSummaries.value = sessions
            } catch (_: Exception) {
                val cached = storage.fetchSessions(config.id)
                _sessionSummaries.value = cached.map { it.toSessionSummary() }
            }
        }
    }

    override fun sendNewSession(workingDirectory: String?) {
        scope.launch {
            try {
                val svc = clientManager.getService() ?: return@launch
                val cwd = workingDirectory ?: config.workingDirectory
                val payload = ACPSessionCreatePayload(workingDirectory = cwd)
                val response = svc.createSession(payload)
                val result = (response as? JSONRPCMessage.Response)?.value?.result?.objectValue
                val sessionId = result?.get("sessionId")?.stringValue ?: return@launch

                val summary = SessionSummary(id = sessionId, title = null, cwd = cwd)
                _sessionSummaries.value = listOf(summary) + _sessionSummaries.value

                val vm = SessionViewModel(sessionId, cwd)
                sessionViewModels[sessionId] = vm
                _selectedSessionId.value = sessionId

                storage.saveSession(
                    StoredSessionInfo(sessionId, null, cwd, null),
                    config.id
                )
            } catch (_: Exception) { }
        }
    }

    override fun openSession(id: String) {
        _selectedSessionId.value = id
        if (sessionViewModels[id] == null) {
            val summary = _sessionSummaries.value.find { it.id == id }
            sessionViewModels[id] = SessionViewModel(id, summary?.cwd)
        }
    }

    override fun deleteSession(sessionId: String) {
        scope.launch {
            _sessionSummaries.value = _sessionSummaries.value.filter { it.id != sessionId }
            sessionViewModels.remove(sessionId)
            if (_selectedSessionId.value == sessionId) {
                _selectedSessionId.value = null
            }
            storage.deleteSession(sessionId, config.id)
        }
    }

    override fun archiveSession(sessionId: String) {
        // ACP does not support archiving; delegate to delete
        deleteSession(sessionId)
    }

    override fun sendPrompt(promptText: String) {
        val sessionId = _selectedSessionId.value ?: return
        val sessionVm = sessionViewModels[sessionId] ?: return

        sessionVm.addUserMessage(promptText)
        sessionVm.startStreaming()
        _isStreaming.value = true

        scope.launch {
            try {
                val svc = clientManager.getService() ?: return@launch
                val prompt = listOf(
                    JSONValue.obj(mapOf(
                        "type" to JSONValue.string("text"),
                        "text" to JSONValue.string(promptText)
                    ))
                )
                val payload = ACPSessionPromptPayload(
                    sessionId = sessionId,
                    prompt = prompt
                )
                val response = svc.sendPrompt(payload)

                val result = (response as? JSONRPCMessage.Response)?.value?.result?.objectValue
                val text = result?.get("text")?.stringValue
                    ?: result?.get("content")?.stringValue ?: ""

                sessionVm.finishStreaming(text)
                _isStreaming.value = false

                storage.updateSession(sessionId, config.id, null)
            } catch (e: Exception) {
                sessionVm.finishStreaming()
                sessionVm.addErrorMessage("Error: ${e.localizedMessage}")
                _isStreaming.value = false
            }
        }
    }

    override fun cancelCurrentRequest() {
        val sessionId = _selectedSessionId.value ?: return
        scope.launch {
            try {
                val svc = clientManager.getService() ?: return@launch
                svc.cancelSession(ACPSessionCancelPayload(sessionId))
            } catch (_: Exception) { }
            _isStreaming.value = false
            sessionViewModels[sessionId]?.finishStreaming()
        }
    }

    // ACPClientManagerDelegate

    override fun onConnectionStateChanged(state: ACPConnectionState) {
        if (state is ACPConnectionState.Disconnected) {
            _isInitialized.value = false
            _isStreaming.value = false
        }
    }

    override fun onNotification(method: String, params: JSONValue?) {
        if (method == "session/update") {
            val events = ACPSessionUpdateParser.parse(params)
            val sessionVm = currentSessionViewModel ?: return
            for (event in events) {
                when (event) {
                    is ACPSessionUpdateEvent.AgentMessage ->
                        sessionVm.appendStreamingContent(event.text)
                    is ACPSessionUpdateEvent.AgentThought ->
                        sessionVm.addAssistantMessage("", listOf(MessageSegment.Thought(event.text)))
                    is ACPSessionUpdateEvent.ToolCall ->
                        sessionVm.addToolCall(event.toolName, event.toolCallId, event.input, event.output, event.status)
                    is ACPSessionUpdateEvent.ToolCallUpdate ->
                        sessionVm.updateToolCall(event.toolCallId, event.output, event.status)
                    is ACPSessionUpdateEvent.StopReason -> {
                        sessionVm.finishStreaming()
                        _isStreaming.value = false
                    }
                    is ACPSessionUpdateEvent.ModeChange ->
                        sessionVm.setMode(event.modeId)
                    else -> { }
                }
            }
        }
    }

    override fun onServerRequest(id: JSONRPCID, method: String, params: JSONValue?) {
        // Handle permission requests etc.
        scope.launch {
            try {
                clientManager.getService()?.sendResponse(id, JSONValue.obj(mapOf(
                    "approved" to JSONValue.bool(true)
                )))
            } catch (_: Exception) { }
        }
    }

    override fun onError(error: Throwable) {
        // Errors handled via connection state
    }
}
