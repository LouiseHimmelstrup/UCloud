package dk.sdu.cloud.auth.api

import dk.sdu.cloud.AccessRight
import dk.sdu.cloud.CommonErrorMessage
import dk.sdu.cloud.Role
import dk.sdu.cloud.Roles
import dk.sdu.cloud.SecurityScope
import dk.sdu.cloud.client.RESTDescriptions
import dk.sdu.cloud.client.bindEntireRequestFromBody
import io.ktor.http.HttpMethod

data class OneTimeAccessToken(val accessToken: String, val jti: String)
data class RequestOneTimeToken(val audience: String)
data class ClaimOneTimeToken(val jti: String)

data class TokenExtensionRequest(
    /**
     * A valid JWT for the security principal extension is requested
     */
    val validJWT: String,

    /**
     * A list of [SecurityScope]s that this request requires.
     *
     * It is not possible to ask for all.
     */
    val requestedScopes: List<String>,

    /**
     * How many ms the new token should be valid for.
     *
     * It is not possible to extend this deadline. Currently the maximum deadline is configured to be 24 hours.
     */
    val expiresIn: Long
)

typealias TokenExtensionResponse = AccessToken

data class TokenExtensionAudit(
    val requestedBy: String,
    val username: String?,
    val role: Role?,
    val requestedScopes: List<String>,
    val expiresIn: Long
)

object AuthDescriptions : RESTDescriptions("auth") {
    private const val baseContext = "/auth"

    val refresh = callDescription<Unit, AccessToken, Unit> {
        method = HttpMethod.Post
        name = "refresh"

        auth {
            roles = Roles.PUBLIC
            access = AccessRight.READ_WRITE
        }

        path {
            using(baseContext)
            +"refresh"
        }
    }

    val webRefresh = callDescription<Unit, AccessTokenAndCsrf, CommonErrorMessage> {
        method = HttpMethod.Post
        name = "refreshWeb"

        auth {
            roles = Roles.PUBLIC
            access = AccessRight.READ_WRITE
        }

        path {
            using(baseContext)
            +"refresh"
            +"web"
        }
    }

    val logout = callDescription<Unit, Unit, Unit> {
        method = HttpMethod.Post
        name = "logout"

        auth {
            roles = Roles.PUBLIC
            access = AccessRight.READ_WRITE
        }

        path {
            using(baseContext)
            +"logout"
        }
    }

    val webLogout = callDescription<Unit, Unit, CommonErrorMessage> {
        method = HttpMethod.Post
        name = "logoutWeb"

        auth {
            roles = Roles.PUBLIC
            access = AccessRight.READ_WRITE
        }

        path {
            using(baseContext)
            +"logout"
            +"web"
        }
    }

    val claim = callDescription<ClaimOneTimeToken, Unit, Unit> {
        method = HttpMethod.Post
        name = "claim"

        auth {
            roles = Roles.PRIVILEDGED
            access = AccessRight.READ_WRITE
        }

        path {
            using(baseContext)
            +"claim"
            +boundTo(ClaimOneTimeToken::jti)
        }
    }

    val requestOneTimeTokenWithAudience = callDescription<RequestOneTimeToken, OneTimeAccessToken, Unit> {
        method = HttpMethod.Post
        name = "requestOneTimeTokenWithAudience"

        auth {
            roles = Roles.PUBLIC
            access = AccessRight.READ_WRITE
        }

        path {
            using(baseContext)
            +"request"
        }

        params {
            +boundTo(RequestOneTimeToken::audience)
        }
    }

    val tokenExtension = callDescriptionWithAudit<TokenExtensionRequest, TokenExtensionResponse,
            CommonErrorMessage, TokenExtensionAudit> {
        method = HttpMethod.Post
        name = "tokenExtension"

        auth {
            roles = Roles.PRIVILEDGED
            access = AccessRight.READ_WRITE
        }

        path {
            using(baseContext)
            +"extend"
        }

        body { bindEntireRequestFromBody() }
    }
}
