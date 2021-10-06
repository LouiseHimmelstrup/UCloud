package dk.sdu.cloud.app.store.api

import dk.sdu.cloud.*
import dk.sdu.cloud.calls.*
import io.ktor.http.HttpMethod
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.encodeToJsonElement

@Serializable
data class Project(
    val id: String,
    val title: String
)
typealias ProjectGroup = Project

@Serializable
data class AccessEntity(
    val user: String? = null,
    val project: String? = null,
    val group: String? = null,
) {
    init {
        require(!user.isNullOrBlank() || (!project.isNullOrBlank() && !group.isNullOrBlank())) { "No access entity defined" }
    }
}

@Serializable
data class DetailedAccessEntity(
    val user: String? = null,
    val project: Project? = null,
    val group: ProjectGroup? = null,
) {
    init {
        require(!user.isNullOrBlank() || (project != null && group != null)) { "No access entity defined" }
    }
}

@Serializable
data class EntityWithPermission(
    val entity: AccessEntity,
    val permission: ApplicationAccessRight
)

@Serializable
data class DetailedEntityWithPermission(
    val entity: DetailedAccessEntity,
    val permission: ApplicationAccessRight
)

@Serializable
data class FindApplicationAndOptionalDependencies(
    val appName: String,
    val appVersion: String
)

@Serializable
data class HasPermissionRequest(
    val appName: String,
    val appVersion: String,
    val permission: Set<ApplicationAccessRight>
)

@Serializable
data class UpdateAclRequest(
    val applicationName: String,
    val changes: List<ACLEntryRequest>
) {
    init {
        if (changes.isEmpty()) throw IllegalArgumentException("changes cannot be empty")
        if (changes.size > 1000) throw IllegalArgumentException("Too many new entries")
    }
}

@Serializable
data class IsPublicRequest(
    val applications: List<NameAndVersion>
)

@Serializable
data class IsPublicResponse(
    val public: Map<NameAndVersion, Boolean>
)


@Serializable
data class ListAclRequest(
    val appName: String
)

@Serializable
data class FavoriteRequest(
    val appName: String,
    val appVersion: String
)

@Serializable
data class ACLEntryRequest(
    val entity: AccessEntity,
    val rights: ApplicationAccessRight,
    val revoke: Boolean = false
)

@Serializable
data class SetPublicRequest(
    val appName: String,
    val appVersion: String,
    val public: Boolean
)

@Serializable
data class TagSearchRequest(
    val query: String,
    val excludeTools: String? = null,
    override val itemsPerPage: Int? = null,
    override val page: Int? = null,
) : WithPaginationRequest

val TagSearchRequest.tags: List<String> get() = query.split(",")

@Serializable
data class AppSearchRequest(
    val query: String,
    override val itemsPerPage: Int? = null,
    override val page: Int? = null,
) : WithPaginationRequest

@Serializable
data class CreateTagsRequest(
    val tags: List<String>,
    val applicationName: String
)

typealias DeleteTagsRequest = CreateTagsRequest

@Serializable
@UCloudApiOwnedBy(ToolStore::class)
data class UploadApplicationLogoRequest(
    val name: String,
)

@Serializable
data class AdvancedSearchRequest(
    val query: String? = null,
    val tags: List<String>? = null,
    val showAllVersions: Boolean,
    override val itemsPerPage: Int? = null,
    override val page: Int? = null,
) : WithPaginationRequest


@Serializable
@UCloudApiOwnedBy(ToolStore::class)
data class ClearLogoRequest(val name: String)
typealias ClearLogoResponse = Unit

@Serializable
@UCloudApiOwnedBy(ToolStore::class)
data class FetchLogoRequest(val name: String)
typealias FetchLogoResponse = Unit

typealias UploadApplicationLogoResponse = Unit

@Serializable
data class FindLatestByToolRequest(
    val tool: String,
    override val itemsPerPage: Int? = null,
    override val page: Int? = null,
) : WithPaginationRequest
typealias FindLatestByToolResponse = Page<Application>

@Serializable
data class DeleteAppRequest(val appName: String, val appVersion: String)
typealias DeleteAppResponse = Unit

@UCloudApiExampleValue
fun exampleApplication(
    name: String,
    version: String,
    image: String,
    invocation: List<InvocationParameter>,
    parameters: List<ApplicationParameter>,
    toolBackend: ToolBackend = ToolBackend.DOCKER,
    type: ApplicationType = ApplicationType.BATCH,
    title: String = name.replace("-", " ").replaceFirstChar { it.uppercase() },
    invocationBlock: (ApplicationInvocationDescription) -> ApplicationInvocationDescription = { it }
): Application {
    return Application(
        ApplicationMetadata(
            name,
            version,
            listOf("UCloud"),
            title,
            "An example application",
            public = true
        ),
        ApplicationInvocationDescription(
            ToolReference(
                name, version, Tool(
                    "_ucloud",
                    1633329776235,
                    1633329776235,
                    NormalizedToolDescription(
                        NameAndVersion(name, version),
                        defaultNumberOfNodes = 1,
                        defaultTimeAllocation = SimpleDuration(1, 0, 0),
                        requiredModules = emptyList(),
                        authors = listOf("UCloud"),
                        title = title,
                        description = "An example tool",
                        backend = toolBackend,
                        license = "None",
                        image = image
                    )
                )
            ),
            invocation,
            parameters,
            listOf("*"),
            type
        ).let(invocationBlock)
    )
}

@UCloudApiExampleValue
val exampleBatchApplication = exampleApplication(
    "acme-batch",
    "1.0.0",
    "acme/batch:1.0.0",
    listOf(
        WordInvocationParameter("acme-batch"),
        VariableInvocationParameter(
            listOf("debug"),
            prefixGlobal = "--debug "
        ),
        VariableInvocationParameter(
            listOf("value")
        )
    ),
    listOf(
        ApplicationParameter.Bool(
            "debug",
            description = "Should debug be enabled?"
        ),
        ApplicationParameter.Text(
            "value",
            description = "The value for the batch application"
        )
    )
)

@UCloudApiExperimental(ExperimentalLevel.ALPHA)
object AppStore : CallDescriptionContainer("hpc.apps") {
    const val baseContext = "/api/hpc/apps/"

    init {
        description = """
Applications specify the input parameters and invocation of a software package.

${ToolStore.sharedIntroduction}

In concrete terms, the ["invocation"]($TYPE_REF_LINK ApplicationInvocationDescription) of an $TYPE_REF Application
covers:

- [Mandatory and optional input parameters.]($TYPE_REF_LINK ApplicationParameter) For example: text and numeric values,
  command-line flags and input files.
- [The command-line invocation, using values from the input parameters.]($TYPE_REF_LINK InvocationParameter)
- [Resources attached to the compute environment.]($TYPE_REF_LINK ApplicationParameter) For example: files, 
  IP addresses and software licenses.
- [An application type]($TYPE_REF_LINK ApplicationType), defining how the user interacts with it. For example: Batch,
  web and remote desktop (VNC).

${ApiConventions.nonConformingApiWarning}
        """.trimIndent()
    }

    private const val batchApplicationUseCase = "batch"
    private const val virtualMachineUseCase = "virtualMachine"
    private const val webUseCase = "web"
    private const val vncUseCase = "vnc"
    private const val fileExtensionUseCase = "fileExtensions"
    private const val defaultValuesUseCase = "defaultValues"

    @OptIn(UCloudApiExampleValue::class)
    override fun documentation() {
        useCase(
            batchApplicationUseCase,
            "Simple batch application",
            flow = {
                val user = basicUser()

                comment("""
                    Applications contain quite a lot of information. The most important pieces of information are
                    summarized below:
                    
                    - This Job will run a `BATCH` application
                      - See `invocation.applicationType`
                      
                    - The application should launch the `acme/batch:1.0.0` container
                      - `invocation.tool.tool.description.backend`
                      - `invocation.tool.tool.description.image`
                      
                    - The command-line invocation will look like this: `acme-batch --debug "Hello, World!"`. 
                      - The invocation is created from `invocation.invocation`
                      - With parameters defined in `invocation.parameters`
                """.trimIndent())

                success(
                    findByNameAndVersion,
                    FindApplicationAndOptionalDependencies(
                        exampleBatchApplication.metadata.name,
                        exampleBatchApplication.metadata.version
                    ),
                    ApplicationWithFavoriteAndTags(
                        exampleBatchApplication.metadata,
                        exampleBatchApplication.invocation,
                        false,
                        emptyList()
                    ),
                    user
                )
            }
        )

        useCase(
            virtualMachineUseCase,
            "Simple virtual machine",
            flow = {
                val user = basicUser()

                comment("""
                    This example shows an Application encoding a virtual machine. It will use the
                    "acme-operating-system" as its base image, as defined in the Tool. 
                """.trimIndent())

                val application = exampleApplication(
                    "acme-os",
                    "1.0.0",
                    "acme-operating-system",
                    emptyList(),
                    emptyList(),
                    ToolBackend.VIRTUAL_MACHINE
                )

                success(
                    findByNameAndVersion,
                    FindApplicationAndOptionalDependencies(application.metadata.name, application.metadata.version),
                    ApplicationWithFavoriteAndTags(application.metadata, application.invocation, false, emptyList()),
                    user
                )
            }
        )

        useCase(
            webUseCase,
            "Simple web application",
            flow = {
                val user = basicUser()

                comment("""
                    This example shows an Application with a graphical web interface. The web server, hosting the 
                    interface, runs on port 8080 as defined in the `invocation.web` section.
                """.trimIndent())

                val application = exampleApplication(
                    "acme-web",
                    "1.0.0",
                    "acme/web:1.0.0",
                    listOf(
                        WordInvocationParameter("web-server")
                    ),
                    emptyList(),
                    type = ApplicationType.WEB
                ) { invocation ->
                    invocation.copy(web = WebDescription(8080))
                }

                success(
                    findByNameAndVersion,
                    FindApplicationAndOptionalDependencies(application.metadata.name, application.metadata.version),
                    ApplicationWithFavoriteAndTags(application.metadata, application.invocation, false, emptyList()),
                    user
                )
            }
        )

        useCase(
            vncUseCase,
            "Simple remote desktop application (VNC)",
            flow = {
                val user = basicUser()

                comment("""
                    This example shows an Application with a graphical web interface. The VNC server, hosting the 
                    interface, runs on port 5900 as defined in the `invocation.vnc` section.
                """.trimIndent())

                val application = exampleApplication(
                    "acme-remote-desktop",
                    "1.0.0",
                    "acme/remote-desktop:1.0.0",
                    listOf(
                        WordInvocationParameter("vnc-server")
                    ),
                    emptyList(),
                    type = ApplicationType.VNC
                ) { invocation ->
                    invocation.copy(vnc = VncDescription())
                }

                success(
                    findByNameAndVersion,
                    FindApplicationAndOptionalDependencies(application.metadata.name, application.metadata.version),
                    ApplicationWithFavoriteAndTags(application.metadata, application.invocation, false, emptyList()),
                    user
                )
            }
        )

        useCase(
            fileExtensionUseCase,
            "Registering a file handler",
            flow = {
                val user = basicUser()

                comment("""
                    This example shows an Application with a graphical web interface. The web server, hosting the 
                    interface, runs on port 8080 as defined in the `invocation.web` section.
                """.trimIndent())

                comment("""
                    The Application also registers a file handler of all files with the `*.c` extension. This is used as
                    a hint for the frontend that files with this extension can be opened with this Application. When
                    opened like this, the file's parent folder will be mounted as a resource.
                """.trimIndent())

                val application = exampleApplication(
                    "acme-web",
                    "1.0.0",
                    "acme/web:1.0.0",
                    listOf(
                        WordInvocationParameter("web-server")
                    ),
                    emptyList(),
                    type = ApplicationType.WEB
                ) { invocation ->
                    invocation.copy(
                        web = WebDescription(8080),
                        fileExtensions = listOf(".c")
                    )
                }

                success(
                    findByNameAndVersion,
                    FindApplicationAndOptionalDependencies(application.metadata.name, application.metadata.version),
                    ApplicationWithFavoriteAndTags(application.metadata, application.invocation, false, emptyList()),
                    user
                )
            }
        )

        useCase(
            defaultValuesUseCase,
            "An Application with default values",
            flow = {
                val user = basicUser()

                comment("""
                    This example shows an Application which has a single input parameter. The parameter contains a 
                    textual value. If the user does not provide a specific value, it will default to 'hello'. UCloud 
                    passes this value as the first argument on the command-line.
                """.trimIndent())

                val application = exampleApplication(
                    "acme-web",
                    "1.0.0",
                    "acme/web:1.0.0",
                    listOf(
                        WordInvocationParameter("web-server"),
                        VariableInvocationParameter(listOf("variable"))
                    ),
                    listOf(
                        ApplicationParameter.Text(
                            "variable",
                            optional = true,
                            defaultValue = defaultMapper.encodeToJsonElement<AppParameterValue>(
                                AppParameterValue.Text("hello")
                            ),
                            "My Variable",
                            description = "A variable passed to the Application (default = 'hello')"
                        )
                    ),
                    type = ApplicationType.WEB
                ) { invocation -> invocation.copy(web = WebDescription(8080)) }

                success(
                    findByNameAndVersion,
                    FindApplicationAndOptionalDependencies(application.metadata.name, application.metadata.version),
                    ApplicationWithFavoriteAndTags(application.metadata, application.invocation, false, emptyList()),
                    user
                )
            }
        )
    }

    val toggleFavorite = call<FavoriteRequest, Unit, CommonErrorMessage>("toggleFavorite") {
        auth {
            access = AccessRight.READ_WRITE
        }

        http {
            method = HttpMethod.Post

            path {
                using(baseContext)
                +"favorites"
            }

            body { bindEntireRequestFromBody() }
        }

        documentation {
            summary = "Toggles the favorite status of an Application for the current user"
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

            documentation {
                summary = "Retrieves the list of favorite Applications for the curent user"
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
                +boundTo(TagSearchRequest::excludeTools)
                +boundTo(TagSearchRequest::itemsPerPage)
                +boundTo(TagSearchRequest::page)
            }
        }

        documentation {
            summary = "Browses the Application catalogue by tag"
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

        documentation {
            summary = "Searches in the Application catalogue using a free-text query"
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
                    +"byName"
                }

                params {
                    +boundTo(FindByNameAndPagination::appName)
                    +boundTo(FindByNameAndPagination::itemsPerPage)
                    +boundTo(FindByNameAndPagination::page)
                }
            }

            documentation {
                summary = "Finds Applications given an exact name"
            }
        }

    val isPublic =
        call<IsPublicRequest, IsPublicResponse, CommonErrorMessage>("isPublic") {
            auth {
                roles = Roles.PRIVILEGED
                access = AccessRight.READ
            }

            http {
                method = HttpMethod.Post

                path {
                    using(baseContext)
                    +"isPublic"
                }

                body {
                    bindEntireRequestFromBody()

                }
            }

            documentation {
                summary = "Checks if an Application is publicly accessible"
            }
        }

    val setPublic = call<SetPublicRequest, Unit, CommonErrorMessage>("setPublic")  {
        auth {
            roles = Roles.PRIVILEGED
            access = AccessRight.READ_WRITE
        }

        http {
            method = HttpMethod.Post

            path {
                using(baseContext)
                +"setPublic"
            }

            body {
                bindEntireRequestFromBody()
            }
        }

        documentation {
            summary = "Changes the 'publicly accessible' status of an Application"
        }
    }

    val advancedSearch = call<AdvancedSearchRequest, Page<ApplicationSummaryWithFavorite>,CommonErrorMessage>("advancedSearch") {
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

        documentation {
            summary = "Searches in the Application catalogue using more advanced parameters"
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
                +"byNameAndVersion"
            }

            params {
                +boundTo(FindApplicationAndOptionalDependencies::appName)
                +boundTo(FindApplicationAndOptionalDependencies::appVersion)
            }
        }

        documentation {
            summary = "Retrieves an Application by name and version"
        }
    }

    val hasPermission = call<
            HasPermissionRequest,
            Boolean,
            CommonErrorMessage>("hasPermission") {
        auth {
            roles = Roles.AUTHENTICATED
            access = AccessRight.READ
        }

        http {
            method = HttpMethod.Get

            path {
                using(baseContext)
                +"permission"
            }

            params {
                +boundTo(HasPermissionRequest::appName)
                +boundTo(HasPermissionRequest::appVersion)
                +boundTo(HasPermissionRequest::permission)
            }
        }

        documentation {
            summary = "Check if an entity has permission to use a specific Application"
        }
    }

    val listAcl = call<
            ListAclRequest,
            List<DetailedEntityWithPermission>,
            CommonErrorMessage>("listAcl") {
        auth {
            roles = Roles.PRIVILEGED
            access = AccessRight.READ
        }

        http {
            path {
                using(baseContext)
                +"list-acl"
            }

            params {
                +boundTo(ListAclRequest::appName)
            }
        }

        documentation {
            summary = "Retrieves the permission information associated with an Application"
        }
    }

    val updateAcl = call<UpdateAclRequest, Unit, CommonErrorMessage>("updateAcl") {
        auth {
            roles = Roles.PRIVILEGED
            access = AccessRight.READ_WRITE
        }

        http {
            method = HttpMethod.Post

            path {
                using(baseContext)
                +"updateAcl"
            }

            body { bindEntireRequestFromBody() }
        }

        documentation {
            summary = "Updates the permissions associated with an Application"
        }
    }

    val findBySupportedFileExtension =
        call<FindBySupportedFileExtension, PageV2<ApplicationWithExtension>, CommonErrorMessage>("findBySupportedFileExtension") {
            auth {
                access = AccessRight.READ
            }

            http {
                method = HttpMethod.Post

                path {
                    using(baseContext)
                    + "bySupportedFileExtension"
                }

                body {
                    bindEntireRequestFromBody()
                }
            }

            documentation {
                summary = "Finds a page of Application which can open a specific UFile"
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
            }

            params {
                +boundTo(FindLatestByToolRequest::tool)
                +boundTo(FindLatestByToolRequest::itemsPerPage)
                +boundTo(FindLatestByToolRequest::page)
            }
        }

        documentation {
            summary = "Retrieves the latest version of an Application using a specific tool"
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

        documentation {
            summary = "Lists all Applications"
            description = "Results are not ordered in any specific fashion"
        }
    }

    val create = call<Unit, Unit, CommonErrorMessage>("create") {
        auth {
            roles = Roles.PRIVILEGED
            access = AccessRight.READ
        }

        http {
            method = HttpMethod.Put
            path { using(baseContext) }
            body { bindEntireRequestFromBody() }
        }

        documentation {
            summary = "Creates a new Application and inserts it into the catalogue"
        }
    }

    val delete = call<DeleteAppRequest, DeleteAppResponse, CommonErrorMessage>("delete") {
        auth {
            roles = Roles.ADMIN
            access = AccessRight.READ_WRITE
        }

        http {
            method = HttpMethod.Delete

            path {
                using(baseContext)
            }

            body { bindEntireRequestFromBody() }
        }

        documentation {
            summary = "Removes an Application from the catalogue"
        }
    }

    val createTag = call<CreateTagsRequest, Unit, CommonErrorMessage>("createTag") {
        auth {
            roles = Roles.PRIVILEGED
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

        documentation {
            summary = "Attaches a set of tags to an Application"
        }
    }

    val removeTag = call<DeleteTagsRequest, Unit, CommonErrorMessage>("removeTag") {
        auth {
            roles = Roles.PRIVILEGED
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

        documentation {
            summary = "Removes a set of tags from an Application"
        }
    }

    val uploadLogo =
        call<UploadApplicationLogoRequest, UploadApplicationLogoResponse, CommonErrorMessage>("uploadLogo") {
            auth {
                roles = Roles.PRIVILEGED
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

                /*
                body {
                    bindToSubProperty(UploadApplicationLogoRequest::data)
                }
                 */
            }

            documentation {
                summary = "Uploads a logo and associates it with an Application"
            }
        }

    val clearLogo =
        call<ClearLogoRequest, ClearLogoResponse, CommonErrorMessage>("clearLogo") {
            auth {
                roles = Roles.PRIVILEGED
                access = AccessRight.READ_WRITE
            }

            http {
                method = HttpMethod.Delete

                path {
                    using(baseContext)
                    +"clearLogo"
                }

                body { bindEntireRequestFromBody() }
            }

            documentation {
                summary = "Removes a logo associated with an Application"
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
            }

            params {
                +boundTo(FetchLogoRequest::name)
            }
        }

        documentation {
            summary = "Retrieves a logo associated with an Application"
        }
    }
}
