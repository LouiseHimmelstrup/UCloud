package dk.sdu.cloud.app.store.api

import dk.sdu.cloud.AccessRight
import dk.sdu.cloud.CommonErrorMessage
import dk.sdu.cloud.Roles
import dk.sdu.cloud.calls.CallDescriptionContainer
import dk.sdu.cloud.calls.auth
import dk.sdu.cloud.calls.bindEntireRequestFromBody
import dk.sdu.cloud.calls.bindToSubProperty
import dk.sdu.cloud.calls.call
import dk.sdu.cloud.calls.http
import dk.sdu.cloud.calls.types.BinaryStream
import dk.sdu.cloud.service.Page
import dk.sdu.cloud.service.PaginationRequest
import dk.sdu.cloud.service.WithPaginationRequest
import io.ktor.http.HttpMethod

data class FindApplicationAndOptionalDependencies(
    val name: String,
    val version: String
)

data class FavoriteRequest(
    val name: String,
    val version: String
)

data class TagSearchRequest(
    val query: String,
    override val itemsPerPage: Int?,
    override val page: Int?
) : WithPaginationRequest

val TagSearchRequest.tags: List<String> get() = query.split(",")

data class AppSearchRequest(
    val query: String,
    override val itemsPerPage: Int?,
    override val page: Int?
) : WithPaginationRequest

data class CreateTagsRequest(
    val tags: List<String>,
    val applicationName: String
)

typealias DeleteTagsRequest = CreateTagsRequest

data class UploadApplicationLogoRequest(
    val name: String,
    val data: BinaryStream
)

data class AdvancedSearchRequest(
    val name: String?,
    val version: String?,
    val description: String?,
    val tags: List<String>?,
    override val itemsPerPage: Int?,
    override val page: Int?
) : WithPaginationRequest


data class ClearLogoRequest(val name: String)
typealias ClearLogoResponse = Unit

data class FetchLogoRequest(val name: String)
typealias FetchLogoResponse = BinaryStream

typealias UploadApplicationLogoResponse = Unit

data class FindLatestByToolRequest(
    val tool: String,
    override val itemsPerPage: Int?,
    override val page: Int?
) : WithPaginationRequest
typealias FindLatestByToolResponse = Page<Application>

object AppStore : CallDescriptionContainer("hpc.apps") {
    const val baseContext = "/api/hpc/apps/"

    val toggleFavorite = call<FavoriteRequest, Unit, CommonErrorMessage>("toggleFavorite") {
        auth {
            access = AccessRight.READ_WRITE
        }

        http {
            method = HttpMethod.Post

            path {
                using(baseContext)
                +"favorites"
                +boundTo(FavoriteRequest::name)
                +boundTo(FavoriteRequest::version)
            }
        }
    }

    val retrieveFavorites =
        call<PaginationRequest, Page<ApplicationSummaryWithFavorite>, CommonErrorMessage>("retrieveFavorites") {
            auth {
                access = AccessRight.READ
            }

            http {
                method = HttpMethod.Get

                path {
                    using(baseContext)
                    +"favorites"
                }

                params {
                    +boundTo(PaginationRequest::itemsPerPage)
                    +boundTo(PaginationRequest::page)
                }
            }
        }


    val searchTags = call<TagSearchRequest, Page<ApplicationSummaryWithFavorite>, CommonErrorMessage>("searchTags") {
        auth {
            roles = Roles.AUTHENTICATED
            access = AccessRight.READ
        }

        http {
            method = HttpMethod.Get

            path {
                using(baseContext)
                +"searchTags"
            }

            params {
                +boundTo(TagSearchRequest::query)
                +boundTo(TagSearchRequest::itemsPerPage)
                +boundTo(TagSearchRequest::page)
            }
        }
    }

    val searchApps = call<AppSearchRequest, Page<ApplicationSummaryWithFavorite>, CommonErrorMessage>("searchApps") {
        auth {
            roles = Roles.AUTHENTICATED
            access = AccessRight.READ
        }

        http {
            method = HttpMethod.Get

            path {
                using(baseContext)
                +"search"
            }

            params {
                +boundTo(AppSearchRequest::query)
                +boundTo(AppSearchRequest::itemsPerPage)
                +boundTo(AppSearchRequest::page)
            }
        }
    }

    val findByName =
        call<FindByNameAndPagination, Page<ApplicationSummaryWithFavorite>, CommonErrorMessage>("findByName") {
            auth {
                roles = Roles.AUTHENTICATED
                access = AccessRight.READ
            }

            http {
                path {
                    using(baseContext)
                    +boundTo(FindByNameAndPagination::name)
                }

                params {
                    +boundTo(FindByNameAndPagination::itemsPerPage)
                    +boundTo(FindByNameAndPagination::page)
                }
            }
        }

    val advancedSearch = call<AdvancedSearchRequest, Page<ApplicationWithFavoriteAndTags>,CommonErrorMessage>("advancedSearch") {
        auth {
            roles = Roles.AUTHENTICATED
            access = AccessRight.READ
        }

        http {
            method = HttpMethod.Post

            path {
                using(baseContext)
                +"advancedSearch"
            }

            body {
                bindEntireRequestFromBody()
            }
        }
    }

    val findByNameAndVersion = call<
            FindApplicationAndOptionalDependencies,
            ApplicationWithFavoriteAndTags,
            CommonErrorMessage>("findByNameAndVersion") {
        auth {
            roles = Roles.AUTHENTICATED
            access = AccessRight.READ
        }

        http {
            path {
                using(baseContext)
                +boundTo(FindApplicationAndOptionalDependencies::name)
                +boundTo(FindApplicationAndOptionalDependencies::version)
            }
        }
    }

    val findBySupportedFileExtension =
        call<FindBySupportedFileExtension, Page<ApplicationSummaryWithFavorite>, CommonErrorMessage>("findBySupportedFileExtension") {
            auth {
                roles = Roles.AUTHENTICATED
                access = AccessRight.READ
            }

            http {
                path {
                    using(baseContext)
                    + "bySupportedFileExtension"
                }

                params {
                    +boundTo(FindBySupportedFileExtension::fileExtension)
                    +boundTo(FindBySupportedFileExtension::itemsPerPage)
                    +boundTo(FindBySupportedFileExtension::page)
                }
            }
        }


    val findLatestByTool = call<FindLatestByToolRequest, FindLatestByToolResponse, CommonErrorMessage>(
        "findLatestByTool"
    ) {
        auth {
            roles = Roles.AUTHENTICATED
            access = AccessRight.READ
        }

        http {
            path {
                using(baseContext)
                +"byTool"
                +boundTo(FindLatestByToolRequest::tool)
            }

            params {
                +boundTo(FindLatestByToolRequest::itemsPerPage)
                +boundTo(FindLatestByToolRequest::page)
            }
        }
    }

    val listAll = call<PaginationRequest, Page<ApplicationSummaryWithFavorite>, CommonErrorMessage>("listAll") {
        auth {
            roles = Roles.AUTHENTICATED
            access = AccessRight.READ
        }

        http {
            path {
                using(baseContext)
            }

            params {
                +boundTo(PaginationRequest::itemsPerPage)
                +boundTo(PaginationRequest::page)
            }
        }
    }

    val create = call<Unit, Unit, CommonErrorMessage>("create") {
        auth {
            roles = Roles.PRIVILEDGED
            access = AccessRight.READ
        }

        http {
            method = HttpMethod.Put
            path { using(baseContext) }
            // body { //YAML Body TODO Implement support }
        }
    }

    val createTag = call<CreateTagsRequest, Unit, CommonErrorMessage>("createTag") {
        auth {
            roles = Roles.PRIVILEDGED
            access = AccessRight.READ_WRITE
        }

        http {
            method = HttpMethod.Post

            path {
                using(baseContext)
                +"createTag"
            }

            body { bindEntireRequestFromBody() }
        }
    }

    val removeTag = call<DeleteTagsRequest, Unit, CommonErrorMessage>("removeTag") {
        auth {
            roles = Roles.PRIVILEDGED
            access = AccessRight.READ_WRITE
        }

        http {
            method = HttpMethod.Post

            path {
                using(baseContext)
                +"deleteTag"
            }

            body { bindEntireRequestFromBody() }
        }
    }

    val uploadLogo =
        call<UploadApplicationLogoRequest, UploadApplicationLogoResponse, CommonErrorMessage>("uploadLogo") {
            auth {
                roles = Roles.PRIVILEDGED
                access = AccessRight.READ_WRITE
            }

            http {
                method = HttpMethod.Post

                path {
                    using(baseContext)
                    +"uploadLogo"
                }

                headers {
                    +boundTo("Upload-Name", UploadApplicationLogoRequest::name)
                }

                body {
                    bindToSubProperty(UploadApplicationLogoRequest::data)
                }
            }
        }

    val clearLogo =
        call<ClearLogoRequest, ClearLogoResponse, CommonErrorMessage>("clearLogo") {
            auth {
                roles = Roles.PRIVILEDGED
                access = AccessRight.READ_WRITE
            }

            http {
                method = HttpMethod.Delete

                path {
                    using(baseContext)
                    +"clearLogo"
                    +boundTo(ClearLogoRequest::name)
                }
            }
        }


    val fetchLogo = call<FetchLogoRequest, FetchLogoResponse, CommonErrorMessage>("fetchLogo") {
        auth {
            access = AccessRight.READ
            roles = Roles.PUBLIC
        }

        http {
            method = HttpMethod.Get

            path {
                using(baseContext)
                +"logo"
                +boundTo(FetchLogoRequest::name)
            }
        }
    }
}
