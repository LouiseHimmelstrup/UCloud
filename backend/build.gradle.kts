import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import kotlin.system.exitProcess

repositories {
    mavenCentral()
}

buildscript {
    repositories {
        mavenCentral()
        maven { setUrl("https://plugins.gradle.org/m2/") }
    }

    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.9.10")
        classpath("org.jetbrains.kotlin:kotlin-serialization:1.9.10")
    }
}

// https://guides.gradle.org/creating-multi-project-builds/
// https://docs.gradle.org/current/userguide/multi_project_builds.html
subprojects {
    val groupBuilder = ArrayList<String>()
    run {
        var currentProject: Project? = project
        while (currentProject != null && currentProject != rootProject) {
            groupBuilder.add(currentProject.name)
            currentProject = currentProject.parent
        }
    }
    group = if (groupBuilder.isEmpty()) {
        "dk.sdu.cloud"
    } else {
        "dk.sdu.cloud." + groupBuilder.reversed().joinToString(".")
    }

    val isUtil = project.name == "util"
    val isApi = project.name == "api"
    val isService = project.name.endsWith("-service")

    repositories {
        mavenCentral()
    }

    if (isService) {
        apply(plugin = "org.jetbrains.kotlin.jvm")
        apply(plugin = "org.jetbrains.kotlin.plugin.serialization")
        apply(plugin = "application")

        extensions.configure<org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension>("kotlin") {
            jvmToolchain(20)

            sourceSets {
                val generated by creating {
                    dependencies {
                        implementation(project(":service-lib"))
                    }
                }

                val main by getting {
                    dependencies {
                        dependsOn(generated)

                        val myApiProject = project.childProjects["api"]
                        if (myApiProject != null) {
                            implementation(myApiProject)
                        }

                        val myUtilProject = project.childProjects["util"]
                        if (myUtilProject != null) {
                            implementation(myUtilProject)
                        }
                        implementation(project(":service-lib-server"))
                    }
                }

                val test by getting {
                    dependencies {
                        implementation(kotlin("test"))
                    }
                }
            }
        }

        val generateBuildConfig by tasks.creating {
            doFirst {
                if (project.version == "unspecified") {
                    throw IllegalStateException("Version not set for service: ${project.name} (${project.version})")
                }

                run {
                    val src = File(project.projectDir, "src/generated/kotlin")
                    src.mkdirs()
                    val simpleName = project.name.replace("-service", "")
                    val packageName = simpleName.replace("-", ".")
                    val className = simpleName.split("-").joinToString("") { it.capitalize() }

                    File(src, "Description.kt").writeText(
                        """
                        package dk.sdu.cloud.$packageName.api
                        
                        import dk.sdu.cloud.ServiceDescription
                        
                        object ${className}ServiceDescription : ServiceDescription {
                            override val name = "$simpleName"
                            override val version = "${project.version}"
                        }
                    """.trimIndent()
                    )
                }

                run {
                    val src = File(project.projectDir, "src/generated/resources")
                    src.mkdirs()

                    File(src, "name.txt").writeText(project.name)
                    File(src, "version.txt").writeText(project.version.toString())
                }
            }
        }

        tasks.withType<KotlinCompile>().configureEach {
            kotlinOptions.freeCompilerArgs += "-progressive"
            kotlinOptions.freeCompilerArgs += "-Xopt-in=kotlin.RequiresOptIn"

            dependsOn(generateBuildConfig)
        }

        tasks.withType<org.gradle.api.tasks.JavaExec>().configureEach {
            systemProperty("log4j2.configurationFactory", "dk.sdu.cloud.micro.Log4j2ConfigFactory")
            jvmArgs("--add-modules", "jdk.incubator.vector")
        }

        tasks.withType<Test>().configureEach {
            systemProperty("log4j2.configurationFactory", "dk.sdu.cloud.micro.Log4j2ConfigFactory")
            systemProperty("java.io.tmpdir", System.getProperty("java.io.tmpdir"))
            jvmArgs("--add-modules", "jdk.incubator.vector", "-Xmx32G")

            testLogging {
                events(*TestLogEvent.values())
                exceptionFormat = TestExceptionFormat.FULL
                outputs.upToDateWhen { false }
                showExceptions = true
                showCauses = true
                showStackTraces = true
                showStandardStreams = true

                debug {
                    events(*TestLogEvent.values())
                    exceptionFormat = TestExceptionFormat.FULL
                }
                info.events = debug.events
                info.exceptionFormat = debug.exceptionFormat

                addTestListener(object : TestListener {
                    override fun beforeSuite(suite: TestDescriptor?) {
                        // Empty
                    }

                    override fun afterSuite(suite: TestDescriptor, result: TestResult) {
                        val size = 80
                        if (suite.parent != null) return
                        print(
                            buildString {
                                appendLine()
                                repeat(size) { append('-') }
                                appendLine()
                                appendLine(result.resultType.toString())
                                repeat(size) { append('-') }
                                appendLine()

                                append(" TESTS:".padEnd(size - result.testCount.toString().length))
                                appendLine(result.testCount)
                                append("PASSED:".padEnd(size - result.successfulTestCount.toString().length))
                                appendLine(result.successfulTestCount)
                                append("FAILED:".padEnd(size - result.failedTestCount.toString().length))
                                appendLine(result.failedTestCount)
                            }
                        )
                    }

                    override fun beforeTest(testDescriptor: TestDescriptor?) {
                        // Empty
                    }

                    override fun afterTest(testDescriptor: TestDescriptor?, result: TestResult?) {
                        // Empty
                    }
                })
            }

            filter {
                includeTestsMatching("*Test")
            }
        }

        tasks.withType<Jar> {
            val name = if (groupBuilder.isEmpty()) {
                "ucloud"
            } else {
                "ucloud-" + groupBuilder.reversed().joinToString("-")
            }

            archiveBaseName.set(name)
        }
    }

    if (isApi) {
        apply(plugin = "org.jetbrains.kotlin.jvm")
        apply(plugin = "org.jetbrains.kotlin.plugin.serialization")
        apply(plugin = "maven-publish")

        val apiProject = project
        apiProject.parent?.afterEvaluate {
            apiProject.version = project.version
        }


        extensions.configure<org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension>("kotlin") {
            sourceSets {
                val main by getting {
                    dependencies {
                        implementation(project(":service-lib"))
                    }
                }

                val test by getting {
                    dependencies {
                    }
                }
            }

            jvmToolchain(20)
        }

        extensions.configure<PublishingExtension>("publishing") {
            repositories {
                maven {
                    mavenLocal()

                    maven {
                        name = "UCloudMaven"
                        url = uri("https://mvn.cloud.sdu.dk/releases")
                        credentials {
                            username = (project.findProperty("ucloud.mvn.username") as? String?)
                                ?: System.getenv("UCLOUD_MVN_USERNAME")
                            password = (project.findProperty("ucloud.mvn.token") as? String?)
                                ?: System.getenv("UCLOUD_MVN_TOKEN")
                        }
                    }
                }
            }

            publications {
                create<MavenPublication>("api") {
                    from(components["java"])
                }

                all {
                    if (this is MavenPublication) {
                        this.groupId = "dk.sdu.cloud"
                        val metadata = artifactId.substringAfterLast("-")
                        this.artifactId = project.parent!!.name + "-api" + if (metadata == "api") "" else "-$metadata"
                    }
                }
            }
        }

        tasks.withType<Jar> {
            val metadata = archiveBaseName.get().substringAfterLast("-").removeSuffix(".jar")
            val name = if (groupBuilder.isEmpty()) {
                "ucloud"
            } else {
                "ucloud-" + groupBuilder.reversed().joinToString("-")
            }

            archiveBaseName.set("$name-${metadata}")
        }
    }

    if (isUtil) {
        apply(plugin = "org.jetbrains.kotlin.jvm")
        apply(plugin = "org.jetbrains.kotlin.plugin.serialization")

        val utilProject = project
        utilProject.parent?.afterEvaluate {
            utilProject.version = project.version
        }

        extensions.configure<org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension>("kotlin") {
            sourceSets {
                val main by getting {
                    dependencies {
                        implementation(project(":service-lib"))

                        val myApiProject = project.parent?.childProjects?.get("api")
                        if (myApiProject != null) {
                            implementation(myApiProject)
                        }

                        implementation(project(":service-lib-server"))
                    }
                }

                val test by getting {
                    dependencies {
                    }
                }
            }

            jvmToolchain(20)
        }

        tasks.withType<Jar> {
            val metadata = archiveBaseName.get().substringAfterLast("-").removeSuffix(".jar")
            val name = if (groupBuilder.isEmpty()) {
                "ucloud"
            } else {
                "ucloud-" + groupBuilder.reversed().joinToString("-")
            }

            archiveBaseName.set("$name-${metadata}.jar")
        }
    }
}

