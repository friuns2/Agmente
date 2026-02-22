package com.agmente.viewmodel

import com.agmente.acpclient.config.ACPConnectionState
import com.agmente.acpclient.model.AgentModeOption
import com.agmente.acpclient.model.AgentProfile
import com.agmente.acpclient.model.SessionSummary
import kotlinx.coroutines.flow.StateFlow

interface ServerViewModelContract {
    val id: String
    val connectionState: StateFlow<ACPConnectionState>
    val isInitialized: StateFlow<Boolean>
    val sessionSummaries: StateFlow<List<SessionSummary>>
    val selectedSessionId: StateFlow<String?>
    val currentSessionViewModel: SessionViewModel?
    val isStreaming: StateFlow<Boolean>
    val agentInfo: StateFlow<AgentProfile?>
    val availableModes: List<AgentModeOption>
    val initializationSummary: String

    fun fetchSessionList(force: Boolean = false)
    fun sendNewSession(workingDirectory: String? = null)
    fun openSession(id: String)
    fun deleteSession(sessionId: String)
    fun archiveSession(sessionId: String)
    fun sendPrompt(promptText: String)
    fun cancelCurrentRequest()
}
