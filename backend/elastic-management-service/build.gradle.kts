version = "1.3.6"

application {
    mainClassName = "dk.sdu.cloud.elastic.management.MainKt"
}

kotlin.sourceSets {
    val jvmMain by getting {
        dependencies {
            implementation(project(":slack-service:api"))
            implementation(project(":auth-service:api"))
        }
    }
}
