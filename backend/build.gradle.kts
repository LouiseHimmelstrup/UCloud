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
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.8.21")
        classpath("org.jetbrains.kotlin:kotlin-serialization:1.8.21")
    }
}

plugins {
    `record`
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
        apply(plugin = "jacoco")

        extensions.configure<org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension>("kotlin") {
            jvmToolchain(17)

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
        }

        tasks.withType<Test>().configureEach {
            systemProperty("log4j2.configurationFactory", "dk.sdu.cloud.micro.Log4j2ConfigFactory")
            systemProperty("java.io.tmpdir", System.getProperty("java.io.tmpdir"))

            filter {
                isFailOnNoMatchingTests = false
                excludeTestsMatching("dk.sdu.cloud.integration.*")
            }

            finalizedBy(tasks["jacocoTestReport"])
        }

        extensions.configure<JacocoPluginExtension>("jacoco") {
            toolVersion = "0.8.4"
        }

        tasks.withType<JacocoReport> {
            reports {
                xml.isEnabled= true
                html.isEnabled = true
            }
        }

        tasks.withType<Jar> {
            val name = if (groupBuilder.isEmpty()) {
                "ucloud"
            } else {
                "ucloud-" + groupBuilder.reversed().joinToString("-")
            }

            archiveName = "$name.jar"
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

            jvmToolchain(17)
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
            val metadata = archiveName.substringAfterLast("-").removeSuffix(".jar")
            val name = if (groupBuilder.isEmpty()) {
                "ucloud"
            } else {
                "ucloud-" + groupBuilder.reversed().joinToString("-")
            }

            archiveName = "$name-${metadata}.jar"
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

            jvmToolchain(17)
        }

        tasks.withType<Jar> {
            val metadata = archiveName.substringAfterLast("-").removeSuffix(".jar")
            val name = if (groupBuilder.isEmpty()) {
                "ucloud"
            } else {
                "ucloud-" + groupBuilder.reversed().joinToString("-")
            }

            archiveName = "$name-${metadata}.jar"
        }
    }
}

