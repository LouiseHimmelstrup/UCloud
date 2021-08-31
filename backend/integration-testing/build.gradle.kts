import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent

plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    application
}

repositories {
    jcenter()
    mavenCentral()
}

application {
    mainClassName = "dk.sdu.cloud.integration.MainKt"
}

kotlin {
    jvm {
        withJava()
        val main by compilations.getting {
            kotlinOptions {
                jvmTarget = "11"
            }
        }

        val test by compilations.getting {
            kotlinOptions {
                jvmTarget = "11"
            }
        }
    }

    sourceSets {
        val jvmTest by getting {
            dependencies {
                implementation(project(":service-lib"))
                implementation(project(":service-lib-test"))

                implementation("org.testcontainers:testcontainers-bom:1.15.1")
                implementation("org.testcontainers:elasticsearch:1.15.1") {
//                    exclude(group = "junit", module = "junit")
                }
                implementation("it.ozimov:embedded-redis:0.7.3")
                implementation("org.testcontainers:selenium:1.15.1") {
//                    exclude(group = "junit", module = "junit")
                }
                implementation("org.seleniumhq.selenium:selenium-remote-driver:3.141.59")
                implementation("org.seleniumhq.selenium:selenium-chrome-driver:3.141.59")
                implementation("org.seleniumhq.selenium:selenium-firefox-driver:3.141.59")

                rootProject.childProjects.values
                    .filter { it.name.endsWith("-service") }
                    .forEach { p ->
                        implementation(project(":" + p.name))

                        val hasApiProject = rootProject.subprojects
                            .find { it.name == p.name }!!.subprojects
                            .any { it.name == "api" }
                        if (hasApiProject) implementation(project(":" + p.name + ":api"))
                    }
            }
        }

        all {
            languageSettings.enableLanguageFeature("InlineClasses")
            languageSettings.progressiveMode = true
            languageSettings.useExperimentalAnnotation("kotlin.RequiresOptIn")
            languageSettings.useExperimentalAnnotation("kotlin.time.ExperimentalTime")
            languageSettings.useExperimentalAnnotation("kotlin.ExperimentalStdlibApi")
            languageSettings.useExperimentalAnnotation("kotlinx.coroutines.ExperimentalCoroutinesApi")
        }
    }
}

task<Test>("integrationTest") {
    useJUnitPlatform()
    description = "Runs integration test"
    group = "verification"

    systemProperty("log4j2.configurationFactory", "dk.sdu.cloud.micro.Log4j2ConfigFactory")
    systemProperty("java.io.tmpdir", System.getProperty("java.io.tmpdir"))

    filter {
        isFailOnNoMatchingTests = false
        includeTestsMatching("dk.sdu.cloud.integration.backend.*")
    }

    testLogging {
        events(*TestLogEvent.values())
        exceptionFormat = TestExceptionFormat.FULL
        outputs.upToDateWhen { false }
        showExceptions = true
        showCauses = true
        showStackTraces = true

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
                        appendln()
                        repeat(size) { append('-') }
                        appendln()
                        appendln(result.resultType.toString())
                        repeat(size) { append('-') }
                        appendln()

                        append(" TESTS:".padEnd(size - result.testCount.toString().length))
                        appendln(result.testCount)
                        append("PASSED:".padEnd(size - result.successfulTestCount.toString().length))
                        appendln(result.successfulTestCount)
                        append("FAILED:".padEnd(size - result.failedTestCount.toString().length))
                        appendln(result.failedTestCount)
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
}

task<Test>("e2eTest") {
    description = "Runs E2E tests"
    group = "verification"

    systemProperty("log4j2.configurationFactory", "dk.sdu.cloud.micro.Log4j2ConfigFactory")
    systemProperty("java.io.tmpdir", System.getProperty("java.io.tmpdir"))

    filter {
        isFailOnNoMatchingTests = false
        includeTestsMatching("dk.sdu.cloud.integration.backend.e2e.*")
    }
}
