package dk.sdu.cloud.calls

import kotlinx.serialization.KSerializer
import kotlinx.serialization.serializer

class AuditDescription<A : Any> internal constructor(
    val context: CallDescription<*, *, *>,
    val auditType: KSerializer<A>,
    val retentionPeriod: Long,
    val longRunningResponseTime: Boolean
) {
    companion object {
        val descriptionKey = AttributeKey<AuditDescription<Any>>("audit-description")
        val DEFAULT_RETENTION_PERIOD: Long = 1000L * 60 * 60 * 24 * 365
    }
}

inline fun <reified A : Any> CallDescription<*, *, *>.audit(
    noinline builder: AuditDescriptionBuilder<A>.() -> Unit = {}
) {
    @Suppress("UNCHECKED_CAST")
    attributes[AuditDescription.descriptionKey] =
        AuditDescriptionBuilder<A>(
            this,
            containerRef.fixedSerializer<A>()
        ).also(builder).build() as AuditDescription<Any>
}

@Suppress("UNCHECKED_CAST")
val CallDescription<*, *, *>.auditOrNull: AuditDescription<Any>?
    get() = attributes.getOrNull(AuditDescription.descriptionKey)

class AuditDescriptionBuilder<A : Any>(
    val context: CallDescription<*, *, *>,
    val auditType: KSerializer<A>
) {
    var retentionPeriod: Long = AuditDescription.DEFAULT_RETENTION_PERIOD
    var longRunningResponseTime: Boolean = false

    fun build(): AuditDescription<A> = AuditDescription(context, auditType, retentionPeriod, longRunningResponseTime)
}
