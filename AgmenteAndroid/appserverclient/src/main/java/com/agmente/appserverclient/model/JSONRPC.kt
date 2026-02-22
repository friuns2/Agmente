package com.agmente.appserverclient.model

import kotlinx.serialization.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*
import kotlinx.serialization.json.*

@Serializable(with = JSONRPCIDSerializer::class)
sealed class JSONRPCID {
    data class StringId(val value: String) : JSONRPCID()
    data class IntId(val value: Int) : JSONRPCID()
}

@OptIn(InternalSerializationApi::class)
object JSONRPCIDSerializer : KSerializer<JSONRPCID> {
    override val descriptor: SerialDescriptor =
        buildSerialDescriptor("JSONRPCID", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: JSONRPCID) {
        val jsonEncoder = encoder as JsonEncoder
        when (value) {
            is JSONRPCID.StringId -> jsonEncoder.encodeJsonElement(JsonPrimitive(value.value))
            is JSONRPCID.IntId -> jsonEncoder.encodeJsonElement(JsonPrimitive(value.value))
        }
    }

    override fun deserialize(decoder: Decoder): JSONRPCID {
        val jsonDecoder = decoder as JsonDecoder
        val element = jsonDecoder.decodeJsonElement()
        return when {
            element is JsonPrimitive && element.isString -> JSONRPCID.StringId(element.content)
            element is JsonPrimitive -> JSONRPCID.IntId(element.content.toInt())
            else -> throw SerializationException("Invalid JSONRPCID: $element")
        }
    }
}

@Serializable
data class JSONRPCRequest(
    val jsonrpc: String = "2.0",
    val id: JSONRPCID,
    val method: String,
    val params: JSONValue? = null
)

@Serializable
data class JSONRPCNotification(
    val jsonrpc: String = "2.0",
    val method: String,
    val params: JSONValue? = null
)

@Serializable
data class JSONRPCResponse(
    val jsonrpc: String = "2.0",
    val id: JSONRPCID,
    val result: JSONValue? = null
)

@Serializable
data class JSONRPCErrorDetail(
    val code: Int,
    val message: String,
    val data: JSONValue? = null
)

@Serializable
data class JSONRPCErrorResponse(
    val jsonrpc: String = "2.0",
    val id: JSONRPCID? = null,
    val error: JSONRPCErrorDetail
)

@Serializable(with = JSONRPCMessageSerializer::class)
sealed class JSONRPCMessage {
    data class Request(val value: JSONRPCRequest) : JSONRPCMessage()
    data class Notification(val value: JSONRPCNotification) : JSONRPCMessage()
    data class Response(val value: JSONRPCResponse) : JSONRPCMessage()
    data class Error(val value: JSONRPCErrorResponse) : JSONRPCMessage()
}

@OptIn(InternalSerializationApi::class)
object JSONRPCMessageSerializer : KSerializer<JSONRPCMessage> {
    override val descriptor: SerialDescriptor =
        buildSerialDescriptor("JSONRPCMessage", StructureKind.OBJECT)

    override fun serialize(encoder: Encoder, value: JSONRPCMessage) {
        val jsonEncoder = encoder as JsonEncoder
        val element = when (value) {
            is JSONRPCMessage.Request -> Json.encodeToJsonElement(value.value)
            is JSONRPCMessage.Notification -> Json.encodeToJsonElement(value.value)
            is JSONRPCMessage.Response -> Json.encodeToJsonElement(value.value)
            is JSONRPCMessage.Error -> Json.encodeToJsonElement(value.value)
        }
        jsonEncoder.encodeJsonElement(element)
    }

    override fun deserialize(decoder: Decoder): JSONRPCMessage {
        val jsonDecoder = decoder as JsonDecoder
        val element = jsonDecoder.decodeJsonElement()
        val obj = element.jsonObject

        val method = obj["method"]?.jsonPrimitive?.contentOrNull
        val id = obj["id"]
        val error = obj["error"]

        return when {
            method != null && id != null && id !is JsonNull ->
                JSONRPCMessage.Request(Json.decodeFromJsonElement(element))
            method != null ->
                JSONRPCMessage.Notification(Json.decodeFromJsonElement(element))
            error != null && error !is JsonNull ->
                JSONRPCMessage.Error(Json.decodeFromJsonElement(element))
            id != null && id !is JsonNull ->
                JSONRPCMessage.Response(Json.decodeFromJsonElement(element))
            else -> throw SerializationException("Unknown JSON-RPC message shape: $element")
        }
    }
}

private val jsonCodec = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
    isLenient = true
}

fun parseJSONRPCMessage(text: String): JSONRPCMessage =
    jsonCodec.decodeFromString(JSONRPCMessageSerializer, text)

fun encodeJSONRPCMessage(message: JSONRPCMessage): String =
    jsonCodec.encodeToString(JSONRPCMessageSerializer, message)
