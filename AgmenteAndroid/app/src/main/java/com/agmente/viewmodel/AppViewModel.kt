package com.agmente.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.agmente.BuildConfig
import com.agmente.acpclient.config.ACPConnectionState
import com.agmente.acpclient.model.SessionSummary
import com.agmente.data.SessionStorage
import com.agmente.data.db.AgmenteDatabase
import com.agmente.data.db.ServerType
import com.agmente.model.ServerConfiguration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AppViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AgmenteDatabase.getDatabase(application)
    private val storage = SessionStorage(database)

    private val _servers = MutableStateFlow<List<ServerConfiguration>>(emptyList())
    val servers: StateFlow<List<ServerConfiguration>> = _servers

    private val _selectedServerId = MutableStateFlow<String?>(null)
    val selectedServerId: StateFlow<String?> = _selectedServerId

    private val _devMode = MutableStateFlow(false)
    val devMode: StateFlow<Boolean> = _devMode

    private val serverViewModels = mutableMapOf<String, ServerViewModelContract>()

    private val _selectedServerViewModel = MutableStateFlow<ServerViewModelContract?>(null)
    val selectedServerViewModel: StateFlow<ServerViewModelContract?> = _selectedServerViewModel

    init {
        loadServers()
    }

    private fun loadServers() {
        viewModelScope.launch {
            var servers = storage.fetchServers()
            if (servers.isEmpty() && BuildConfig.DEBUG) {
                val defaultServer = ServerConfiguration(
                    id = "debug-codex-local",
                    name = "Local Codex",
                    scheme = "ws",
                    host = "192.168.1.6:8788",
                    serverType = ServerType.CODEX_APP_SERVER
                )
                storage.saveServer(defaultServer)
                servers = listOf(defaultServer)
            }
            _servers.value = servers
            if (servers.isNotEmpty() && _selectedServerId.value == null) {
                selectServer(servers.first().id)
            }
        }
    }

    fun selectServer(id: String) {
        _selectedServerId.value = id
        if (serverViewModels[id] == null) {
            val config = _servers.value.find { it.id == id } ?: return
            val vm = when (config.serverType) {
                ServerType.CODEX_APP_SERVER ->
                    CodexServerViewModel(config, storage, viewModelScope)
                else ->
                    ServerViewModel(config, storage, viewModelScope)
            }
            serverViewModels[id] = vm
        }
        _selectedServerViewModel.value = serverViewModels[id]
    }

    fun addServer(config: ServerConfiguration) {
        viewModelScope.launch {
            storage.saveServer(config)
            _servers.value = _servers.value + config
            selectServer(config.id)
        }
    }

    fun updateServer(config: ServerConfiguration) {
        viewModelScope.launch {
            storage.saveServer(config)
            _servers.value = _servers.value.map {
                if (it.id == config.id) config else it
            }
            serverViewModels.remove(config.id)
            selectServer(config.id)
        }
    }

    fun deleteServer(id: String) {
        viewModelScope.launch {
            storage.deleteServer(id)
            _servers.value = _servers.value.filter { it.id != id }
            serverViewModels.remove(id)
            if (_selectedServerId.value == id) {
                val nextId = _servers.value.firstOrNull()?.id
                _selectedServerId.value = nextId
                _selectedServerViewModel.value = nextId?.let { serverViewModels[it] }
            }
        }
    }

    fun connectSelectedServer() {
        android.util.Log.d("AppViewModel", "connectSelectedServer: selectedId=${_selectedServerId.value}, vmKeys=${serverViewModels.keys}")
        val vm = _selectedServerViewModel.value
        if (vm == null) {
            android.util.Log.e("AppViewModel", "No selected server VM!")
            return
        }
        android.util.Log.d("AppViewModel", "Connecting via ${vm::class.simpleName}")
        when (vm) {
            is ServerViewModel -> vm.connectAndInitialize()
            is CodexServerViewModel -> vm.connectAndInitialize()
        }
    }

    fun disconnectSelectedServer() {
        val vm = _selectedServerViewModel.value ?: return
        when (vm) {
            is ServerViewModel -> vm.disconnect()
            is CodexServerViewModel -> vm.disconnect()
        }
    }

    fun toggleDevMode() {
        _devMode.value = !_devMode.value
    }
}
