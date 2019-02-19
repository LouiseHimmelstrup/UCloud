package dk.sdu.cloud.kafka

import dk.sdu.cloud.kafka.JsonSerde.jsonSerde
import org.apache.kafka.common.serialization.Serde
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.common.utils.Bytes
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.kstream.Consumed
import org.apache.kafka.streams.kstream.KStream
import java.nio.ByteBuffer

interface StreamDescription<K, V> {
    val name: String
    val keySerde: Serde<K>
    val valueSerde: Serde<V>

    val desiredPartitions: Int? get() = null
    val desiredReplicas: Short? get() = null

    fun stream(builder: StreamsBuilder): KStream<K, V> =
        builder.stream(name, Consumed.with(keySerde, valueSerde))
}

class SimpleStreamDescription<Key, Value>(
    override val name: String,
    override val keySerde: Serde<Key>,
    override val valueSerde: Serde<Value>,
    override val desiredPartitions: Int? = null,
    override val desiredReplicas: Short? = null
) : StreamDescription<Key, Value>

class MappedStreamDescription<K, V>(
    override val name: String,
    override val keySerde: Serde<K>,
    override val valueSerde: Serde<V>,
    override val desiredPartitions: Int? = null,
    override val desiredReplicas: Short? = null,
    val mapper: (V) -> K
) : StreamDescription<K, V>

@Deprecated(message = "Not used", replaceWith = ReplaceWith("String"))
typealias RawAuthToken = String

abstract class KafkaDescriptions {
    @PublishedApi
    internal val streams = ArrayList<StreamDescription<*, *>>()

    inline fun <reified K : Any, reified V : Any> stream(
        topicName: String,
        keySerde: Serde<K> = defaultSerdeOrJson(),
        valueSerde: Serde<V> = defaultSerdeOrJson(),
        desiredPartitions: Int? = null,
        desiredReplicas: Short? = null
    ): StreamDescription<K, V> {
        return SimpleStreamDescription(
            topicName,
            keySerde,
            valueSerde,
            desiredPartitions,
            desiredReplicas
        ).also { streams.add(it) }
    }

    inline fun <reified K : Any, reified V : Any> stream(
        topicName: String,
        keySerde: Serde<K> = defaultSerdeOrJson(),
        valueSerde: Serde<V> = defaultSerdeOrJson(),
        desiredPartitions: Int? = null,
        desiredReplicas: Short? = null,
        noinline keyMapper: (V) -> K
    ): MappedStreamDescription<K, V> {
        return MappedStreamDescription(
            topicName,
            keySerde,
            valueSerde,
            desiredPartitions,
            desiredReplicas,
            keyMapper
        ).also { streams.add(it) }
    }
}

@Suppress("UNCHECKED_CAST")
inline fun <reified Type : Any> defaultSerdeOrJson(): Serde<Type> = when (Type::class) {
    String::class -> Serdes.String() as Serde<Type>
    Double::class -> Serdes.Double() as Serde<Type>
    Int::class -> Serdes.Integer() as Serde<Type>
    Float::class -> Serdes.Float() as Serde<Type>
    Short::class -> Serdes.Short() as Serde<Type>
    ByteArray::class -> Serdes.ByteArray() as Serde<Type>
    ByteBuffer::class -> Serdes.ByteBuffer() as Serde<Type>
    Bytes::class -> Serdes.Bytes() as Serde<Type>

    else -> jsonSerde()
}