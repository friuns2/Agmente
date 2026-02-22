package com.agmente.appserverclient.model

import kotlinx.serialization.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*
import kotlinx.serialization.json.*

@Serializable(with = JSONValueSerializer::class)
sealed class JSONValue {
    data class StringValue(val value: String) : JSONValue()
    data class NumberValue(val value: Double) : JSONValue()
    data class BoolValue(val value: Boolean) : JSONValue()
    data class ObjectValue(val value: Map<String, JSONValue>) : JSONValue()
    data class ArrayValue(val value: List<JSONValue>) : JSONValue()
    data object Null : JSONValue()

    val stringValue: String? get() = (this as? StringValue)?.value
    val numberValue: Double? get() = (this as? NumberValue)?.value
    val boolValue: Boolean? get() = (this as? BoolValue)?.value
    val objectValue: Map<String, JSONValue>? get() = (this as? ObjectValue)?.value
    val arrayValue: List<JSONValue>? get() = (this as? ArrayValue)?.value
    val isNull: Boolean get() = this is Null

    companion object {
        fun string(value: String) = StringValue(value)
        fun number(value: Double) = NumberValue(value)
        fun bool(value: Boolean) = BoolValue(value)
        fun obj(value: Map<String, JSONValue>) = ObjectValue(value)
        fun array(value: List<JSONValue>) = ArrayValue(value)
        fun nil() = Null
    }
}

@OptIn(InternalSerializationApi::class)
object JSONValueSerializer : KSerializer<JSONValue> {
    override val descriptor: SerialDescriptor =
        buildSerialDescriptor("JSONValue", PolymorphicKind.SEALED)

    override fun serialize(encoder: Encoder, value: JSONValue) {
        val jsonEncoder = encoder as JsonEncoder
        val element = toJsonElement(value)
        jsonEncoder.encodeJsonElement(element)
    }

    override fun deserialize(decoder: Decoder): JSONValue {
        val jsonDecoder = decoder as JsonDecoder
        val element = jsonDecoder.decodeJsonElement()
        return fromJsonElement(element)
    }

    fun toJsonElement(value: JSONValue): JsonElement = when (value) {
        is JSONValue.StringValue -> JsonPrimitive(value.value)
        is JSONValue.NumberValue -> JsonPrimitive(value.value)
        is JSONValue.BoolValue -> JsonPrimitive(value.value)
        is JSONValue.ObjectValue -> JsonObject(value.value.mapValues { toJsonElement(it.value) })
        is JSONValue.ArrayValue -> JsonArray(value.value.map { toJsonElement(it) })
        is JSONValue.Null -> JsonNull
    }

    fun fromJsonElement(element: JsonElement): JSONValue = when (element) {
        is JsonNull -> JSONValue.Null
        is JsonPrimitive -> when {
            element.isString -> JSONValue.StringValue(element.content)
            element.content == "true" || element.content == "false" ->
                JSONValue.BoolValue(element.content.toBoolean())
            else -> JSONValue.NumberValue(element.content.toDouble())
        }
        is JsonObject -> JSONValue.ObjectValue(
            element.mapValues { fromJsonElement(it.value) }
        )
        is JsonArray -> JSONValue.ArrayValue(element.map { fromJsonElement(it) })
    }
}
