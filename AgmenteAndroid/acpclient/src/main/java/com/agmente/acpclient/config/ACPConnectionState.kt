package com.agmente.acpclient.config

sealed class ACPConnectionState {
    data object Disconnected : ACPConnectionState()
    data object Connecting : ACPConnectionState()
    data object Connected : ACPConnectionState()
    data class Failed(val error: Throwable) : ACPConnectionState()
}
