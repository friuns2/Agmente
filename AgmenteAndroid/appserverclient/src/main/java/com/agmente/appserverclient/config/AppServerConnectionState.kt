package com.agmente.appserverclient.config

sealed class AppServerConnectionState {
    data object Disconnected : AppServerConnectionState()
    data object Connecting : AppServerConnectionState()
    data object Connected : AppServerConnectionState()
    data class Failed(val error: Throwable) : AppServerConnectionState()
}
