package com.agmente.viewmodel

import com.agmente.acpclient.config.ACPConnectionState
import com.agmente.acpclient.model.AgentModeOption
import com.agmente.acpclient.model.AgentProfile
import com.agmente.acpclient.model.SessionSummary
import com.agmente.appserverclient.*
import com.agmente.appserverclient.config.AppServerClientConfiguration
import com.agmente.appserverclient.config.AppServerConnectionState
import com.agmente.appserverclient.model.*
import com.agmente.appserverclient.websocket.JavaWebSocketProvider
import com.agmente.data.SessionStorage
import com.agmente.data.StoredSessionInfo
import com.agmente.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map

class CodexServerViewModel(
    private val config: com.agmente.model.ServerConfiguration,
    private val storage: SessionStorage,
    private val scope: CoroutineScope
) : ServerViewModelContract, AppServerServiceDelegate {

    override val id: String = config.id

    private val client = AppServerClient(JavaWebSocketProvider())
    private val service = AppServerService(client)

    private val _connectionState = MutableStateFlow<ACPConnectionState>(ACPConnectionState.Disconnected)
    override val connectionState: StateFlow<ACPConnectionState> = _connectionState

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

    private val _models = MutableStateFlow<List<AppServerModel>>(emptyList())
    val models: StateFlow<List<AppServerModel>> = _models

    private val _selectedModel = MutableStateFlow<String?>(null)
    val selectedModel: StateFlow<String?> = _selectedModel

    private val _approvalPolicy = MutableStateFlow(AppServerApprovalPolicy.ON_REQUEST)
    val approvalPolicy: StateFlow<AppServerApprovalPolicy> = _approvalPolicy

    private val sessionViewModels = mutableMapOf<String, SessionViewModel>()
    private var activeTurnId: String? = null

    override val currentSessionViewModel: SessionViewModel?
        get() = _selectedSessionId.value?.let { sessionViewModels[it] }

    override val availableModes: List<AgentModeOption> get() = emptyList()

    override val initializationSummary: String
        get() {
            val info = _agentInfo.value
            return if (info != null) "${info.displayNameWithVersion} | Codex" else "Codex app-server"
        }

    init {
        service.setDelegate(this)
    }

    private suspend fun doConnect() {
        _connectionState.value = ACPConnectionState.Connecting
        val appConfig = AppServerClientConfiguration(
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
        client.connect(appConfig)
    }

    fun connect() {
        scope.launch {
            try {
                doConnect()
            } catch (e: Exception) {
                _connectionState.value = ACPConnectionState.Failed(e)
            }
        }
    }

    private suspend fun doInitialize() {
        val payload = AppServerInitializePayload(
            clientInfo = AppServerClientInfo("Agmente Android", "1.0.0")
        )
        val response = service.initialize(payload)
        val result = AppServerResponseParser.parseInitialize(response)
        if (result != null) {
            _agentInfo.value = AgentProfile(
                id = null,
                name = result.serverName ?: "Codex",
                version = result.serverVersion
            )
            _isInitialized.value = true
            service.sendNotification(AppServerMethods.INITIALIZED)
            fetchModels()
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
        android.util.Log.d("CodexServerVM", "connectAndInitialize to ${config.endpointURLString}")
        scope.launch {
            try {
                doConnect()
                android.util.Log.d("CodexServerVM", "connected, initializing...")
                doInitialize()
                android.util.Log.d("CodexServerVM", "initialized OK")
            } catch (e: Exception) {
                android.util.Log.e("CodexServerVM", "connectAndInitialize failed", e)
                _connectionState.value = ACPConnectionState.Failed(e)
            }
        }
    }

    fun disconnect() {
        scope.launch {
            client.disconnect()
            _isInitialized.value = false
            _connectionState.value = ACPConnectionState.Disconnected
        }
    }

    private fun fetchModels() {
        scope.launch {
            try {
                val response = service.modelList()
                val result = AppServerResponseParser.parseModelList(response)
                if (result != null) {
                    _models.value = result.models
                    _selectedModel.value = result.defaultModel ?: result.models.firstOrNull()?.id
                }
            } catch (_: Exception) { }
        }
    }

    fun setModel(modelId: String) {
        _selectedModel.value = modelId
    }

    fun setApprovalPolicy(policy: AppServerApprovalPolicy) {
        _approvalPolicy.value = policy
    }

    override fun fetchSessionList(force: Boolean) {
        scope.launch {
            try {
                val response = service.threadList(AppServerThreadListPayload())
                val result = AppServerResponseParser.parseThreadList(response) ?: return@launch
                _sessionSummaries.value = result.threads.map { thread ->
                    SessionSummary(
                        id = thread.id,
                        title = thread.preview,
                        cwd = thread.cwd,
                        updatedAt = thread.createdAt
                    )
                }
            } catch (_: Exception) { }
        }
    }

    override fun sendNewSession(workingDirectory: String?) {
        scope.launch {
            try {
                val cwd = workingDirectory ?: config.workingDirectory
                val payload = AppServerThreadStartPayload(
                    cwd = cwd.ifEmpty { null },
                    model = _selectedModel.value
                )
                val response = service.threadStart(payload)
                val thread = AppServerResponseParser.parseThreadStart(response)
                if (thread == null) {
                    android.util.Log.e("CodexServerVM", "thread/start parse failed: $response")
                    return@launch
                }

                val summary = SessionSummary(
                    id = thread.id,
                    title = thread.preview,
                    cwd = thread.cwd ?: cwd,
                    updatedAt = thread.createdAt
                )
                _sessionSummaries.value = listOf(summary) + _sessionSummaries.value

                val vm = SessionViewModel(thread.id, thread.cwd ?: cwd)
                sessionViewModels[thread.id] = vm
                _selectedSessionId.value = thread.id

                storage.saveSession(
                    StoredSessionInfo(thread.id, thread.preview, thread.cwd ?: cwd, thread.createdAt),
                    config.id
                )
            } catch (e: Exception) {
                android.util.Log.e("CodexServerVM", "sendNewSession failed", e)
            }
        }
    }

    override fun openSession(id: String) {
        _selectedSessionId.value = id
        if (sessionViewModels[id] == null) {
            val summary = _sessionSummaries.value.find { it.id == id }
            sessionViewModels[id] = SessionViewModel(id, summary?.cwd)
            resumeThread(id)
        }
    }

    private fun resumeThread(threadId: String) {
        scope.launch {
            try {
                val response = service.threadResume(
                    AppServerThreadResumePayload(
                        threadId = threadId,
                        persistExtendedHistory = true
                    )
                )
                val result = AppServerResponseParser.parseThreadResume(response) ?: return@launch
                val sessionVm = sessionViewModels[threadId] ?: return@launch

                val messages = mutableListOf<ChatMessage>()
                for (turn in result.turns) {
                    for (item in turn.items) {
                        val itemObj = item.payload?.objectValue
                        when (item.type) {
                            "user_message", "message" -> {
                                val text = itemObj?.get("text")?.stringValue
                                    ?: itemObj?.get("content")?.stringValue ?: ""
                                val role = itemObj?.get("role")?.stringValue
                                if (role == "user" || item.type == "user_message") {
                                    messages.add(ChatMessage(role = ChatMessageRole.USER, content = text))
                                } else {
                                    messages.add(ChatMessage(role = ChatMessageRole.ASSISTANT, content = text))
                                }
                            }
                            "assistant_message" -> {
                                val text = itemObj?.get("text")?.stringValue
                                    ?: itemObj?.get("content")?.stringValue ?: ""
                                messages.add(ChatMessage(role = ChatMessageRole.ASSISTANT, content = text))
                            }
                            "tool_call", "function_call" -> {
                                val toolName = itemObj?.get("name")?.stringValue ?: "tool"
                                messages.add(ChatMessage(
                                    role = ChatMessageRole.ASSISTANT,
                                    content = "",
                                    segments = listOf(MessageSegment.ToolCall(
                                        toolName = toolName,
                                        toolCallId = item.id,
                                        input = itemObj?.get("arguments")?.stringValue,
                                        output = itemObj?.get("output")?.stringValue,
                                        status = "completed"
                                    ))
                                ))
                            }
                        }
                    }
                }
                sessionVm.setMessages(messages)
            } catch (_: Exception) { }
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
        scope.launch {
            try {
                service.threadArchive(AppServerThreadArchivePayload(sessionId))
                _sessionSummaries.value = _sessionSummaries.value.filter { it.id != sessionId }
                sessionViewModels.remove(sessionId)
                if (_selectedSessionId.value == sessionId) {
                    _selectedSessionId.value = null
                }
            } catch (_: Exception) { }
        }
    }

    override fun sendPrompt(promptText: String) {
        val threadId = _selectedSessionId.value ?: return
        val sessionVm = sessionViewModels[threadId] ?: return

        sessionVm.addUserMessage(promptText)
        sessionVm.startStreaming()
        _isStreaming.value = true

        scope.launch {
            try {
                val sandboxPolicy = when (_approvalPolicy.value) {
                    AppServerApprovalPolicy.NEVER ->
                        AppServerSandboxPolicy(AppServerSandboxMode.DANGER_FULL_ACCESS)
                    else ->
                        AppServerSandboxPolicy(AppServerSandboxMode.WORKSPACE_WRITE)
                }

                val payload = AppServerTurnStartPayload(
                    threadId = threadId,
                    input = listOf(AppServerUserInput.Text(promptText)),
                    model = _selectedModel.value,
                    approvalPolicy = _approvalPolicy.value,
                    sandboxPolicy = sandboxPolicy
                )
                service.turnStart(payload)
            } catch (e: Exception) {
                sessionVm.finishStreaming()
                sessionVm.addErrorMessage("Error: ${e.localizedMessage}")
                _isStreaming.value = false
            }
        }
    }

    override fun cancelCurrentRequest() {
        val threadId = _selectedSessionId.value ?: return
        val turnId = activeTurnId ?: return
        scope.launch {
            try {
                service.turnInterrupt(AppServerTurnInterruptPayload(threadId, turnId))
            } catch (_: Exception) { }
            _isStreaming.value = false
            sessionViewModels[threadId]?.finishStreaming()
        }
    }

    // AppServerServiceDelegate

    override fun onEvent(event: AppServerEvent) {
        when (event) {
            is AppServerEvent.ThreadStarted -> { }
            is AppServerEvent.TurnStarted -> {
                activeTurnId = event.turn.id
            }
            is AppServerEvent.TurnCompleted -> {
                val sessionVm = sessionViewModels[event.threadId] ?: return
                sessionVm.finishStreaming()
                _isStreaming.value = false
                activeTurnId = null
            }
            is AppServerEvent.AgentMessageDelta -> {
                val sessionVm = sessionViewModels[event.threadId] ?: return
                sessionVm.appendStreamingContent(event.delta)
            }
            is AppServerEvent.ItemStarted -> {
                val sessionVm = sessionViewModels[event.threadId] ?: return
                val itemObj = event.item.payload?.objectValue
                if (event.item.type == "tool_call" || event.item.type == "function_call") {
                    val toolName = itemObj?.get("name")?.stringValue ?: "tool"
                    sessionVm.addToolCall(
                        toolName = toolName,
                        toolCallId = event.item.id,
                        input = itemObj?.get("arguments")?.stringValue,
                        output = null,
                        status = "running"
                    )
                }
            }
            is AppServerEvent.ItemCompleted -> {
                val sessionVm = sessionViewModels[event.threadId] ?: return
                if (event.item.type == "tool_call" || event.item.type == "function_call") {
                    val itemObj = event.item.payload?.objectValue
                    sessionVm.updateToolCall(
                        toolCallId = event.item.id ?: "",
                        output = itemObj?.get("output")?.stringValue,
                        status = "completed"
                    )
                }
            }
            is AppServerEvent.PlanUpdated -> {
                val sessionVm = currentSessionViewModel ?: return
                sessionVm.addAssistantMessage("", listOf(
                    MessageSegment.Plan(
                        explanation = event.update.explanation,
                        steps = event.update.steps.map { PlanStepInfo(it.step, it.status) }
                    )
                ))
            }
            is AppServerEvent.DiffUpdated -> {
                val sessionVm = sessionViewModels[event.threadId] ?: return
                val diffStr = event.diff.stringValue ?: event.diff.toString()
                sessionVm.addAssistantMessage("", listOf(MessageSegment.FileChanges(diffStr)))
            }
            is AppServerEvent.ApprovalRequested -> {
                scope.launch {
                    try {
                        service.sendResponse(event.requestId, JSONValue.obj(mapOf(
                            "approved" to JSONValue.bool(true)
                        )))
                    } catch (_: Exception) { }
                }
            }
            else -> { }
        }
    }

    override fun onConnectionStateChanged(state: AppServerConnectionState) {
        android.util.Log.d("CodexServerVM", "onConnectionStateChanged: $state -> mapping to ACPConnectionState")
        _connectionState.value = when (state) {
            is AppServerConnectionState.Connected -> ACPConnectionState.Connected
            is AppServerConnectionState.Connecting -> ACPConnectionState.Connecting
            is AppServerConnectionState.Disconnected -> ACPConnectionState.Disconnected
            is AppServerConnectionState.Failed -> ACPConnectionState.Failed(state.error)
        }
    }

    override fun onError(error: Throwable) {
        // Errors handled via connection state
    }
}
