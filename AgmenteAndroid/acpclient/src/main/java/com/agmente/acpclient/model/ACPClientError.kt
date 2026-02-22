package com.agmente.acpclient.model

sealed class ACPClientError : Exception() {
    data object Disconnected : ACPClientError()
    data object EncodingFailed : ACPClientError()
    data object DecodingFailed : ACPClientError()
    data object NotImplemented : ACPClientError()
}
