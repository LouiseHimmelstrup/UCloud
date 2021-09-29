package dk.sdu.cloud.calls

import org.intellij.lang.annotations.Language
import kotlin.jvm.internal.CallableReference
import kotlin.reflect.KClass
import kotlin.reflect.KProperty

actual typealias Language = Language

actual fun CallDescriptionContainer.docCallRef(
    call: KProperty<CallDescription<*, *, *>>,
    qualified: Boolean?,
): String {
    val namespace = if (call is CallableReference) {
        runCatching {
            ((call.owner as KClass<*>).objectInstance as CallDescriptionContainer).namespace
        }.getOrDefault(this.namespace)
    } else {
        this.namespace
    }

    val isQualified = qualified ?: (this.namespace != namespace)
    return "$CALL_REF ${namespace}.${call.name})"
}
