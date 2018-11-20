package dk.sdu.cloud.zenodo

import dk.sdu.cloud.client.AuthenticatedCloud
import dk.sdu.cloud.service.CommonServer
import dk.sdu.cloud.service.HttpServerProvider
import dk.sdu.cloud.service.KafkaServices
import dk.sdu.cloud.service.Micro
import dk.sdu.cloud.service.buildStreams
import dk.sdu.cloud.service.configureControllers
import dk.sdu.cloud.service.db.HibernateSessionFactory
import dk.sdu.cloud.service.forStream
import dk.sdu.cloud.service.installDefaultFeatures
import dk.sdu.cloud.service.startServices
import dk.sdu.cloud.service.tokenValidation
import dk.sdu.cloud.zenodo.api.ZenodoCommandStreams
import dk.sdu.cloud.zenodo.http.ZenodoController
import dk.sdu.cloud.zenodo.processors.PublishProcessor
import dk.sdu.cloud.zenodo.services.ZenodoOAuth
import dk.sdu.cloud.zenodo.services.ZenodoRPCService
import dk.sdu.cloud.zenodo.services.hibernate.PublicationHibernateDAO
import dk.sdu.cloud.zenodo.services.hibernate.ZenodoOAuthHibernateStateStore
import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.response.respond
import io.ktor.response.respondRedirect
import io.ktor.routing.get
import io.ktor.routing.route
import io.ktor.routing.routing
import io.ktor.server.engine.ApplicationEngine
import org.apache.kafka.streams.KafkaStreams
import org.slf4j.Logger

class Server(
    private val db: HibernateSessionFactory,
    private val cloud: AuthenticatedCloud,
    override val kafka: KafkaServices,
    private val config: Configuration,
    private val ktor: HttpServerProvider,
    private val micro: Micro
) : CommonServer {
    override val log: Logger = logger()

    override lateinit var httpServer: ApplicationEngine
    override lateinit var kStreams: KafkaStreams

    override fun start() {
        log.info("Configuring core services")
        val zenodoOauth = ZenodoOAuth(
            db = db,
            clientSecret = config.zenodo.clientSecret,
            clientId = config.zenodo.clientId,

            callback = "https://cloud.sdu.dk/zenodo/oauth",

            stateStore = ZenodoOAuthHibernateStateStore(),

            useSandbox = config.zenodo.useSandbox
        )

        val zenodo = ZenodoRPCService(config.zenodo.useSandbox, zenodoOauth)
        val publicationService = PublicationHibernateDAO(config.zenodo.useSandbox)
        log.info("Core services configured")

        kStreams = buildStreams { kBuilder ->
            PublishProcessor(db, zenodo, publicationService, cloud.parent, micro.tokenValidation).also {
                it.init(
                    kBuilder
                )
            }
        }

        httpServer = ktor {
            installDefaultFeatures(micro)

            routing {
                route("zenodo") {
                    get("oauth") {
                        val state =
                            call.request.queryParameters["state"] ?: return@get call.respond(HttpStatusCode.BadRequest)
                        val code =
                            call.request.queryParameters["code"] ?: return@get call.respond(HttpStatusCode.BadRequest)

                        val redirectTo = zenodoOauth.requestTokenWithCode(code, state) ?: "/"
                        call.respondRedirect(redirectTo)
                    }
                }

                configureControllers(
                    ZenodoController(
                        db,
                        publicationService,
                        zenodo,
                        kafka.producer.forStream(ZenodoCommandStreams.publishCommands)
                    )
                )
            }
        }

        startServices()
    }
}
