package dk.sdu.cloud.auth.api

import dk.sdu.cloud.AccessRight
import dk.sdu.cloud.CommonErrorMessage
import dk.sdu.cloud.FindByStringId
import dk.sdu.cloud.Role
import dk.sdu.cloud.Roles
import dk.sdu.cloud.calls.CallDescriptionContainer
import dk.sdu.cloud.calls.audit
import dk.sdu.cloud.calls.auth
import dk.sdu.cloud.calls.bindEntireRequestFromBody
import dk.sdu.cloud.calls.call
import dk.sdu.cloud.calls.http
import io.ktor.http.HttpMethod

data class LookupUsersRequest(val users: List<String>)
data class UserLookup(val subject: String, val uid: Long, val role: Role)
data class LookupUsersResponse(val results: Map<String, UserLookup?>)

data class LookupEmailRequest(val userId: String)
data class LookupEmailResponse(val email: String)

data class LookupUserWithEmailRequest(val email: String)
data class LookupUserWithEmailResponse(val userId: String, val firstNames: String)

typealias CreateUserAudit = List<CreateSingleUserAudit>

data class CreateSingleUserAudit(val username: String, val role: Role?)

typealias CreateUserRequest = List<CreateSingleUserRequest>

data class CreateSingleUserRequest(val username: String, val password: String?, val role: Role?) {
    override fun toString() = "CreateUserRequest(username = $username, role = $role)"
}

typealias CreateUserResponse = List<CreateSingleUserResponse>
typealias CreateSingleUserResponse = AuthenticationTokens

class ChangePasswordAudit

data class ChangePasswordRequest(val currentPassword: String, val newPassword: String) {
    override fun toString() = "ChangePasswordRequest()"
}

data class ChangePasswordWithResetRequest(val userId: String, val newPassword: String)

data class LookupUIDRequest(val uids: List<Long>)
data class LookupUIDResponse(val users: Map<Long, UserLookup?>)

object UserDescriptions : CallDescriptionContainer("auth.users") {
    const val baseContext = "/auth/users"

    val createNewUser = call<CreateUserRequest, CreateUserResponse, CommonErrorMessage>("createNewUser") {
        audit<CreateUserAudit>()

        auth {
            roles = Roles.PRIVILEDGED
            access = AccessRight.READ_WRITE
        }

        http {
            method = HttpMethod.Post
            path {
                using(baseContext)
                +"register"
            }

            body { bindEntireRequestFromBody() }
        }
    }

    val changePassword = call<ChangePasswordRequest, Unit, CommonErrorMessage>("changePassword") {
        audit<ChangePasswordAudit>()

        auth {
            roles = Roles.END_USER
            access = AccessRight.READ_WRITE
        }

        http {
            method = HttpMethod.Post
            path {
                using(baseContext)
                +"password"
            }

            body { bindEntireRequestFromBody() }
        }
    }

    val changePasswordWithReset = call<ChangePasswordWithResetRequest, Unit, CommonErrorMessage>("changePasswordWithReset") {
        audit<ChangePasswordAudit>()

        auth {
            roles = setOf(Role.SERVICE)
            access = AccessRight.READ_WRITE
        }

      http {
            method = HttpMethod.Post
            path {
                using(baseContext)
                +"password"
                +"reset"
            }

            body { bindEntireRequestFromBody() }
        }
    }

    val lookupUsers = call<LookupUsersRequest, LookupUsersResponse, CommonErrorMessage>("lookupUsers") {
        auth {
            roles = Roles.PRIVILEDGED
            access = AccessRight.READ
        }

        http {
            method = HttpMethod.Post
            path {
                using(baseContext)
                +"lookup"
            }

            body { bindEntireRequestFromBody() }
        }
    }

    val lookupEmail = call<LookupEmailRequest, LookupEmailResponse, CommonErrorMessage>("lookupEmail") {
        auth {
            roles = setOf(Role.SERVICE)
            access = AccessRight.READ
        }

        http {
            method = HttpMethod.Post
            path {
                using(baseContext)
                +"lookup"
                +"email"
            }

            body { bindEntireRequestFromBody() }
        }
    }

    val lookupUserWithEmail = call<LookupUserWithEmailRequest, LookupUserWithEmailResponse, CommonErrorMessage>("lookupUserWithEmail") {
        auth {
            roles = setOf(Role.SERVICE)
            access = AccessRight.READ
        }

        http {
            method = HttpMethod.Post
            path {
                using(baseContext)
                +"lookup"
                +"with-email"
            }

            body { bindEntireRequestFromBody() }
        }
    }

    val lookupUID = call<LookupUIDRequest, LookupUIDResponse, CommonErrorMessage>("lookupUID") {
        auth {
            roles = Roles.PRIVILEDGED
            access = AccessRight.READ
        }

        http {
            method = HttpMethod.Post

            path {
                using(baseContext)
                +"lookup-uid"
            }

            body { bindEntireRequestFromBody() }
        }
    }

    val openUserIterator = call<Unit, FindByStringId, CommonErrorMessage>("openUserIterator") {
        auth {
            roles = Roles.PRIVILEDGED
            access = AccessRight.READ_WRITE
        }

        http {
            method = HttpMethod.Post
            path {
                using(baseContext)
                +"iterator"
                +"open"
            }
        }
    }

    /**
     * Fetches more principals from an iterator.
     *
     * Note: twoFactorAuthentication field is not calculated correctly at the moment.
     */
    val fetchNextIterator = call<FindByStringId, List<Principal>, CommonErrorMessage>("fetchNextIterator") {
        auth {
            roles = Roles.PRIVILEDGED
            access = AccessRight.READ_WRITE
        }

        http {
            method = HttpMethod.Post
            path {
                using(baseContext)
                +"iterator"
                +"next"
            }

            body { bindEntireRequestFromBody() }
        }
    }

    val closeIterator = call<FindByStringId, Unit, CommonErrorMessage>("closeIterator") {
        auth {
            roles = Roles.PRIVILEDGED
            access = AccessRight.READ_WRITE
        }

        http {
            method = HttpMethod.Post
            path {
                using(baseContext)
                +"iterator"
                +"close"
            }

            body { bindEntireRequestFromBody() }
        }
    }
}
