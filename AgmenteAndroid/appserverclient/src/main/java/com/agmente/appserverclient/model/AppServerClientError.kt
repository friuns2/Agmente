package com.agmente.appserverclient.model

sealed class AppServerClientError : Exception() {
    data object Disconnected : AppServerClientError()
    data object EncodingFailed : AppServerClientError()
    data object DecodingFailed : AppServerClientError()
}
