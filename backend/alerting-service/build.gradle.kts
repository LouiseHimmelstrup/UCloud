version = "2021.3.0-alpha0"

application {
    mainClassName = "dk.sdu.cloud.alerting.MainKt"
}

kotlin.sourceSets {
    val jvmMain by getting {
        dependencies {
            implementation(project(":auth-service:api"))
            implementation("io.fabric8:kubernetes-client:4.1.3")
            implementation(project(":slack-service:api"))
        }
    }
}
