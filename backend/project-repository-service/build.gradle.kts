version = "0.3.0"

application {
    mainClassName = "dk.sdu.cloud.project.repository.MainKt"
}

kotlin.sourceSets {
    val jvmMain by getting {
        dependencies {
            implementation(project(":auth-service:api"))
            implementation(project(":storage-service:api"))
            implementation(project(":project-service:api"))
        }
    }
}
