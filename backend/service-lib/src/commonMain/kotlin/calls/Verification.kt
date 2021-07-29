package dk.sdu.cloud.calls

import io.ktor.http.*
import kotlin.reflect.KProperty

fun checkMinimumValue(property: KProperty<*>, value: Int, minimumValue: Int) {
    if (value < minimumValue) {
        throw RPCException("${property.name} must be at least $minimumValue", HttpStatusCode.BadRequest)
    }
}

fun checkMinimumValue(property: KProperty<*>, value: Long, minimumValue: Long) {
    if (value < minimumValue) {
        throw RPCException("${property.name} must be at least $minimumValue", HttpStatusCode.BadRequest)
    }
}

fun checkMaximumValue(property: KProperty<*>, value: Long, maximumValue: Long) {
    if (value > maximumValue) {
        throw RPCException("${property.name} cannot exceed $maximumValue", HttpStatusCode.BadRequest)
    }
}

fun checkMaximumValue(property: KProperty<*>, value: Int, maximumValue: Int) {
    if (value > maximumValue) {
        throw RPCException("${property.name} cannot exceed $maximumValue", HttpStatusCode.BadRequest)
    }
}

fun checkNumberInRange(property: KProperty<*>, value: Int, range: IntRange) {
    if (value !in range) {
        throw RPCException("${property.name} has an invalid value. Must be in $range", HttpStatusCode.BadRequest)
    }
}
