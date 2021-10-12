package dk.sdu.cloud

import dk.sdu.cloud.accounting.api.Accounting
import dk.sdu.cloud.accounting.api.Products
import dk.sdu.cloud.accounting.api.Visualization
import dk.sdu.cloud.accounting.api.Wallets
import dk.sdu.cloud.activity.api.Activity
import dk.sdu.cloud.alerting.api.Alerting
import dk.sdu.cloud.app.kubernetes.api.KubernetesCompute
import dk.sdu.cloud.app.kubernetes.api.KubernetesIngresses
import dk.sdu.cloud.app.kubernetes.api.KubernetesLicenseMaintenance
import dk.sdu.cloud.app.kubernetes.api.KubernetesLicenses
import dk.sdu.cloud.app.kubernetes.api.KubernetesNetworkIP
import dk.sdu.cloud.app.kubernetes.api.KubernetesNetworkIPMaintenance
import dk.sdu.cloud.app.kubernetes.api.Maintenance
import dk.sdu.cloud.app.orchestrator.api.IngressControl
import dk.sdu.cloud.app.orchestrator.api.IngressProvider
import dk.sdu.cloud.app.orchestrator.api.Ingresses
import dk.sdu.cloud.app.orchestrator.api.Jobs
import dk.sdu.cloud.app.orchestrator.api.JobsControl
import dk.sdu.cloud.app.orchestrator.api.JobsProvider
import dk.sdu.cloud.app.orchestrator.api.LicenseControl
import dk.sdu.cloud.app.orchestrator.api.LicenseProvider
import dk.sdu.cloud.app.orchestrator.api.Licenses
import dk.sdu.cloud.app.orchestrator.api.NetworkIPControl
import dk.sdu.cloud.app.orchestrator.api.NetworkIPProvider
import dk.sdu.cloud.app.orchestrator.api.NetworkIPs
import dk.sdu.cloud.app.orchestrator.api.Shells
import dk.sdu.cloud.app.store.api.AppStore
import dk.sdu.cloud.app.store.api.ToolStore
import dk.sdu.cloud.audit.ingestion.api.Auditing
import dk.sdu.cloud.auth.api.AuthDescriptions
import dk.sdu.cloud.auth.api.AuthProviders
import dk.sdu.cloud.auth.api.ServiceLicenseAgreement
import dk.sdu.cloud.auth.api.TwoFactorAuthDescriptions
import dk.sdu.cloud.auth.api.UserDescriptions
import dk.sdu.cloud.avatar.api.AvatarDescriptions
import dk.sdu.cloud.calls.ApiConventions
import dk.sdu.cloud.calls.CallDescriptionContainer
import dk.sdu.cloud.elastic.management.api.ElasticManagement
import dk.sdu.cloud.file.orchestrator.api.ChunkedUploadProtocol
import dk.sdu.cloud.file.orchestrator.api.FileCollections
import dk.sdu.cloud.file.orchestrator.api.FileCollectionsControl
import dk.sdu.cloud.file.orchestrator.api.FileCollectionsProvider
import dk.sdu.cloud.file.orchestrator.api.FileMetadata
import dk.sdu.cloud.file.orchestrator.api.FileMetadataTemplateNamespaces
import dk.sdu.cloud.file.orchestrator.api.Files
import dk.sdu.cloud.file.orchestrator.api.FilesControl
import dk.sdu.cloud.file.orchestrator.api.FilesProvider
import dk.sdu.cloud.file.orchestrator.api.Shares
import dk.sdu.cloud.file.orchestrator.api.SharesControl
import dk.sdu.cloud.file.orchestrator.api.SharesProvider
import dk.sdu.cloud.file.ucloud.api.UCloudFileCollections
import dk.sdu.cloud.file.ucloud.api.UCloudFiles
import dk.sdu.cloud.file.ucloud.api.UCloudShares
import dk.sdu.cloud.grant.api.Gifts
import dk.sdu.cloud.grant.api.Grants
import dk.sdu.cloud.mail.api.MailDescriptions
import dk.sdu.cloud.news.api.News
import dk.sdu.cloud.notification.api.NotificationDescriptions
import dk.sdu.cloud.password.reset.api.PasswordResetDescriptions
import dk.sdu.cloud.project.api.ProjectGroups
import dk.sdu.cloud.project.api.ProjectMembers
import dk.sdu.cloud.project.api.Projects
import dk.sdu.cloud.project.favorite.api.ProjectFavorites
import dk.sdu.cloud.provider.api.Providers
import dk.sdu.cloud.provider.api.Resources
import dk.sdu.cloud.redis.cleaner.api.RedisCleaner
import dk.sdu.cloud.slack.api.SlackDescriptions
import dk.sdu.cloud.support.api.SupportDescriptions
import dk.sdu.cloud.task.api.Tasks
import java.util.*

sealed class Chapter {
    abstract val id: String
    abstract val title: String
    abstract var path: List<Chapter.Node>

    data class Node(
        override val id: String,
        override val title: String,
        val children: List<Chapter>
    ) : Chapter() {
        override var path: List<Chapter.Node> = emptyList()
    }

    data class Feature(
        override val id: String,
        override val title: String,
        val container: CallDescriptionContainer
    ) : Chapter() {
        override var path: List<Chapter.Node> = emptyList()
    }
}

fun Chapter.addPaths(path: List<Chapter.Node> = emptyList()) {
    this.path = path
    if (this !is Chapter.Node) return
    val newPath = path + listOf(this)
    children.forEach { it.addPaths(newPath) }
}

fun Chapter.previous(): Chapter? {
    val parent = path.lastOrNull()
    return if (parent != null) {
        val indexOfSelf = parent.children.indexOf(this)
        if (indexOfSelf > 0) {
            parent.children[indexOfSelf - 1]
        } else {
            val previousSection = parent.previous()
            if (previousSection is Chapter.Node) {
                previousSection.children.lastOrNull() ?: previousSection
            } else {
                previousSection
            }
        }
    } else {
        null
    }
}

fun generateCode() {
    val structure = Chapter.Node(
        "developer-guide",
        "UCloud Developer Guide",
        listOf(
            Chapter.Node(
                "accounting-and-projects",
                "Accounting and Project Management",
                listOf(
                    Chapter.Node(
                        "projects",
                        "Projects",
                        listOf(
                            Chapter.Feature("projects", "Projects", Projects),
                            Chapter.Feature("members", "Members", ProjectMembers),
                            Chapter.Feature("groups", "Groups", ProjectGroups),
                            Chapter.Feature("favorites", "Favorites", ProjectFavorites)
                        )
                    ),
                    Chapter.Feature("providers", "Providers", Providers),
                    Chapter.Feature("products", "Products", Products),
                    Chapter.Node(
                        "accounting",
                        "Accounting",
                        listOf(
                            Chapter.Feature("wallets", "Wallets", Wallets),
                            Chapter.Feature("allocations", "Accounting Operations", Accounting),
                            Chapter.Feature("visualization", "Visualization of Usage", Visualization)
                        )
                    ),
                    Chapter.Node(
                        "grants",
                        "Grants",
                        listOf(
                            Chapter.Feature("grants", "Allocation Process", Grants),
                            Chapter.Feature("gifts", "Gifts", Gifts)
                        )
                    )
                )
            ),
            Chapter.Node(
                "orchestration",
                "Orchestration of Resources",
                listOf(
                    Chapter.Node(
                        "storage",
                        "Storage",
                        listOf(
                            Chapter.Feature("filecollections", "Drives (FileCollection)", FileCollections),
                            Chapter.Feature("files", "Files", Files),
                            Chapter.Feature("shares", "Shares", Shares),
                            Chapter.Node(
                                "metadata",
                                "Metadata",
                                listOf(
                                    Chapter.Feature("templates", "Templates", FileMetadataTemplateNamespaces),
                                    Chapter.Feature("documents", "Documents", FileMetadata)
                                )
                            ),
                            Chapter.Node(
                                "providers",
                                "Provider APIs",
                                listOf(
                                    Chapter.Node(
                                        "drives",
                                        "Drives (FileCollection)",
                                        listOf(
                                            Chapter.Feature(
                                                "ingoing",
                                                "Ingoing API",
                                                FileCollectionsProvider(PROVIDER_ID_PLACEHOLDER)
                                            ),
                                            Chapter.Feature(
                                                "outgoing",
                                                "Outgoing API",
                                                FileCollectionsControl
                                            )
                                        )
                                    ),
                                    Chapter.Node(
                                        "files",
                                        "Files",
                                        listOf(
                                            Chapter.Feature(
                                                "ingoing",
                                                "Ingoing API",
                                                FilesProvider(PROVIDER_ID_PLACEHOLDER)
                                            ),
                                            Chapter.Feature(
                                                "outgoing",
                                                "Outgoing API",
                                                FilesControl
                                            )
                                        )
                                    ),
                                    Chapter.Feature(
                                        "upload",
                                        "Upload Protocol",
                                        ChunkedUploadProtocol(PROVIDER_ID_PLACEHOLDER, "/placeholder")
                                    ),
                                    Chapter.Node(
                                        "shares",
                                        "Shares",
                                        listOf(
                                            Chapter.Feature(
                                                "ingoing",
                                                "Ingoing API",
                                                SharesProvider(PROVIDER_ID_PLACEHOLDER)
                                            ),
                                            Chapter.Feature(
                                                "outgoing",
                                                "Outgoing API",
                                                SharesControl
                                            )
                                        )
                                    )
                                )
                            )
                        )
                    ),
                    Chapter.Node(
                        "compute",
                        "Compute",
                        listOf(
                            Chapter.Node(
                                "appstore",
                                "Application Store",
                                listOf(
                                    Chapter.Feature("tools", "Tools", ToolStore),
                                    Chapter.Feature("apps", "Applications", AppStore)
                                )
                            ),
                            Chapter.Feature("jobs", "Jobs", Jobs),
                            Chapter.Feature("ips", "Public IPs (NetworkIP)", NetworkIPs),
                            Chapter.Feature("ingress", "Public Links (Ingress)", Ingresses),
                            Chapter.Feature("license", "Software Licenses", Licenses),
                            Chapter.Node(
                                "providers",
                                "Provider APIs",
                                listOf(
                                    Chapter.Node(
                                        "jobs",
                                        "Jobs",
                                        listOf(
                                            Chapter.Feature(
                                                "ingoing",
                                                "Ingoing API",
                                                JobsProvider(PROVIDER_ID_PLACEHOLDER)
                                            ),
                                            Chapter.Feature(
                                                "outgoing",
                                                "Outgoing API",
                                                JobsControl
                                            )
                                        )
                                    ),
                                    Chapter.Feature("shells", "Shells", Shells(PROVIDER_ID_PLACEHOLDER)),
                                    Chapter.Node(
                                        "ips",
                                        "Public IPs (NetworkIP)",
                                        listOf(
                                            Chapter.Feature(
                                                "ingoing",
                                                "Ingoing API",
                                                NetworkIPProvider(PROVIDER_ID_PLACEHOLDER)
                                            ),
                                            Chapter.Feature(
                                                "outgoing",
                                                "Outgoing API",
                                                NetworkIPControl
                                            )
                                        )
                                    ),
                                    Chapter.Node(
                                        "ingress",
                                        "Public Links (Ingress)",
                                        listOf(
                                            Chapter.Feature(
                                                "ingoing",
                                                "Ingoing API",
                                                IngressProvider(PROVIDER_ID_PLACEHOLDER)
                                            ),
                                            Chapter.Feature(
                                                "outgoing",
                                                "Outgoing API",
                                                IngressControl
                                            )
                                        )
                                    ),
                                    Chapter.Node(
                                        "licenses",
                                        "Software Licenses",
                                        listOf(
                                            Chapter.Feature(
                                                "ingoing",
                                                "Ingoing API",
                                                LicenseProvider(PROVIDER_ID_PLACEHOLDER)
                                            ),
                                            Chapter.Feature(
                                                "outgoing",
                                                "Outgoing API",
                                                LicenseControl
                                            )
                                        )
                                    ),
                                )
                            )
                        )
                    )
                )
            ),
            Chapter.Node(
                "core",
                "Core",
                listOf(
                    Chapter.Feature("types", "Core Types", CoreTypes),
                    Chapter.Feature("api-conventions", "API Conventions", ApiConventions),
                    Chapter.Feature("resources", "Resources", Resources),
                    Chapter.Node(
                        "users",
                        "Users",
                        listOf(
                            Chapter.Feature("creation", "User Creation", UserDescriptions),
                            Chapter.Node(
                                "authentication",
                                "Authentication",
                                listOf(
                                    Chapter.Feature("users", "User Authentication", AuthDescriptions),
                                    Chapter.Feature("providers", "Provider Authentication", AuthProviders),
                                    Chapter.Feature("password-reset", "Password Reset", PasswordResetDescriptions)
                                )
                            ),
                            Chapter.Feature("slas", "SLAs", ServiceLicenseAgreement),
                            Chapter.Feature("2fa", "2FA", TwoFactorAuthDescriptions),
                            Chapter.Feature("avatars", "Avatars", AvatarDescriptions),
                        )
                    ),
                    Chapter.Node(
                        "monitoring",
                        "Monitoring and Alerting",
                        listOf(
                            Chapter.Feature("auditing", "Auditing", Auditing),
                            Chapter.Feature("alerting", "Alerting", Alerting),
                            Chapter.Feature("activity", "Activity", Activity),
                            Chapter.Node(
                                "scripts",
                                "Scripts",
                                listOf(
                                    Chapter.Feature("redis", "Redis Cleanup", RedisCleaner),
                                    Chapter.Feature("elastic", "Elastic Cleanup", ElasticManagement)
                                )
                            )
                        )
                    ),
                    Chapter.Node(
                        "communication",
                        "Communication",
                        listOf(
                            Chapter.Feature("news", "News", News),
                            Chapter.Feature("notifications", "Notifications", NotificationDescriptions),
                            Chapter.Feature("tasks", "Tasks", Tasks),
                            Chapter.Feature("support", "Support", SupportDescriptions),
                            Chapter.Feature("slack", "Slack", SlackDescriptions),
                            Chapter.Feature("mail", "Mail", MailDescriptions),
                        )
                    )
                )
            ),
            Chapter.Node(
                "built-in-provider",
                "Built-in Provider",
                listOf(
                    Chapter.Node(
                        "storage",
                        "UCloud/Storage",
                        listOf(
                            Chapter.Feature("file-collections", "File Collections", UCloudFileCollections),
                            Chapter.Feature("files", "Files", UCloudFiles),
                            Chapter.Feature("shares", "Shares", UCloudShares)
                        )
                    ),
                    Chapter.Node(
                        "compute",
                        "UCloud/Compute",
                        listOf(
                            Chapter.Feature("jobs", "Jobs", KubernetesCompute),
                            Chapter.Feature("ingress", "Public Links (Ingress)", KubernetesIngresses),
                            Chapter.Node(
                                "ips",
                                "Public IPs (NetworkIP)",
                                listOf(
                                    Chapter.Feature("feature", "Feature", KubernetesNetworkIP),
                                    Chapter.Feature("maintenance", "Maintenance", KubernetesNetworkIPMaintenance)
                                )
                            ),
                            Chapter.Node(
                                "licenses",
                                "Software Licenses",
                                listOf(
                                    Chapter.Feature("feature", "Feature", KubernetesLicenses),
                                    Chapter.Feature("maintenance", "Maintenance", KubernetesLicenseMaintenance)
                                )
                            ),
                            Chapter.Feature("maintenance", "Maintenance", Maintenance)
                        )
                    )
                )
            )
        )
    )
    structure.addPaths()

    val stack = LinkedList<Chapter?>(listOf(structure))
    val types = LinkedHashMap<String, GeneratedType>()
    val callsByFeature = HashMap<Chapter.Feature, List<GeneratedRemoteProcedureCall>>()
    var firstPass = true

    while (true) {
        val chapter = stack.pollFirst()
        if (chapter == null) {
            if (firstPass) {
                stack.add(structure)
                firstPass = false

                var didFail = false
                for ((name, type) in types) {
                    if (type.owner == null && name.startsWith("dk.sdu.cloud.")) {
                        println("$name has no owner. You can fix this by attaching @UCloudApiOwnedBy(XXX::class)")
                        didFail = true
                    }
                }
                if (didFail) break
                continue
            } else {
                break
            }
        }

        val actualPreviousSection = chapter.previous()
        when (chapter) {
            is Chapter.Feature -> {
                if (firstPass) {
                    chapter.container.documentation()
                    callsByFeature[chapter] = generateCalls(chapter.container, types)
                } else {
                    val nextSection = stack.peek()
                    val calls = callsByFeature.getValue(chapter)
                    generateMarkdown(
                        actualPreviousSection,
                        nextSection,
                        chapter.path,
                        types,
                        calls,
                        chapter
                    )

                    generateTypeScriptCode(types, calls, chapter.title, chapter.container)
                }
            }

            is Chapter.Node -> {
                for (child in chapter.children.reversed()) {
                    stack.addFirst(child)
                }
                var nextSection = stack.peek()
                while (nextSection is Chapter.Node) {
                    // NOTE(Dan): don't set next section to null if not needed
                    val next = nextSection.children.firstOrNull() ?: break
                    nextSection = next
                }
                generateMarkdownChapterTableOfContents(actualPreviousSection, nextSection, chapter.path, chapter)
            }
        }
    }
}
