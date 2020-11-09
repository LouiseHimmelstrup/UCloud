package dk.sdu.cloud.calls

import io.ktor.http.*
import org.intellij.lang.annotations.Language

/**
 * The [UCloudApiDoc] annotation is used to annotate request/response types and their properties
 *
 * To document a call please use [UCloudCallDoc] via [CallDescription.documentation].
 */
@Target(AnnotationTarget.FIELD, AnnotationTarget.CLASS)
annotation class UCloudApiDoc(@Language("markdown") val documentation: String)

interface UCloudCallDoc<R : Any, S : Any, E : Any> {
    val summary: String?
    val description: String?
    val examples: ArrayList<Example<R, S, E>>
    val errors: ArrayList<Error<E>>

    interface Example<R : Any, S : Any, E : Any>{
        val name: String
        val summary: String?
        val description: String?
        val statusCode: HttpStatusCode
        val request: R?
        val response: S?
        val error: E?
    }

    interface Error<E> {
        val statusCode: HttpStatusCode
        val description: String?
    }

    companion object {
        internal val key = AttributeKey<UCloudCallDoc<*, *, *>>("call-doc")
    }
}

class UCloudCallDocBuilder<R : Any, S : Any, E : Any> : UCloudCallDoc<R, S, E> {
    override var summary: String? = null
    @Language("markdown")
    override var description: String? = null
    override val examples = ArrayList<UCloudCallDoc.Example<R, S, E>>()
    override val errors = ArrayList<UCloudCallDoc.Error<E>>()

    class ExampleBuilder<R : Any, S : Any, E : Any>(override var name: String) : UCloudCallDoc.Example<R, S, E> {
        override var summary: String? = null
        @Language("markdown")
        override var description: String? = null
        override var statusCode: HttpStatusCode = HttpStatusCode.OK
        override var request: R? = null
        override var response: S? = null
        override var error: E? = null
    }

    class ErrorBuilder<E> : UCloudCallDoc.Error<E> {
        override var statusCode: HttpStatusCode = HttpStatusCode.BadRequest
        @Language("markdown")
        override var description: String? = null
    }
}

fun <R : Any, S : Any, E : Any> CallDescription<R, S, E>.documentation(
    handler: UCloudCallDocBuilder<R, S, E>.() -> Unit
) {
    attributes[UCloudCallDoc.key] = UCloudCallDocBuilder<R, S, E>().also { it.handler() }
}

val <R : Any, S : Any, E : Any> CallDescription<R, S, E>.docOrNull: UCloudCallDoc<R, S, E>?
    @Suppress("UNCHECKED_CAST")
    get() = attributes.getOrNull(UCloudCallDoc.key) as UCloudCallDoc<R, S, E>?

fun <R : Any, S : Any, E : Any> UCloudCallDocBuilder<R, S, E>.example(
    name: String,
    handler: UCloudCallDocBuilder.ExampleBuilder<R, S, E>.() -> Unit
) {
    examples.add(UCloudCallDocBuilder.ExampleBuilder<R, S, E>(name).also(handler))
}

fun <E : Any> UCloudCallDocBuilder<*, *, E>.error(handler: UCloudCallDocBuilder.ErrorBuilder<E>.() -> Unit) {
    errors.add(UCloudCallDocBuilder.ErrorBuilder<E>().also(handler))
}

