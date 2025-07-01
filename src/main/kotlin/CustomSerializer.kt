import kotlinx.serialization.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.JsonElement


object AnySerializer : KSerializer<Any> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("Any", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: Any) {
        val json = Json { encodeDefaults = true }
        when (value) {
            is String -> encoder.encodeString(value)
            is Number -> encoder.encodeString(value.toString())
            is Boolean -> encoder.encodeBoolean(value)
            is Map<*, *> -> encoder.encodeString(json.encodeToString(value))
            is List<*> -> encoder.encodeString(json.encodeToString(value))
            else -> encoder.encodeString(json.encodeToString(value))
        }
    }

    override fun deserialize(decoder: Decoder): Any {
        return decoder.decodeString()
    }
}