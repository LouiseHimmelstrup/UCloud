package dk.sdu.cloud.app.license.api

import dk.sdu.cloud.AccessRight
import dk.sdu.cloud.CommonErrorMessage
import dk.sdu.cloud.calls.CallDescriptionContainer
import dk.sdu.cloud.calls.auth
import dk.sdu.cloud.calls.bindEntireRequestFromBody
import dk.sdu.cloud.calls.call
import dk.sdu.cloud.calls.http
import io.ktor.http.HttpMethod

data class ExampleRequest(val message: String)
data class ExampleResponse(val echo: String)

object AppLicenseDescriptions : CallDescriptionContainer("app.license") {
    val baseContext = "/api/app/license"

    val example = call<ExampleRequest, ExampleResponse, CommonErrorMessage>("example") {
        auth {
            access = AccessRight.READ
        }

        http {
            method = HttpMethod.Post

            path {
                using(baseContext)
                +"example"
            }

            body { bindEntireRequestFromBody() }
        }
    }
}
