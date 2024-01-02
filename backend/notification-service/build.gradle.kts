version = rootProject.file("./version.txt").readText().trim()

application {
    mainClass.set("dk.sdu.cloud.notification.MainKt")
}

kotlin.sourceSets {
    val main by getting {
        dependencies {
            implementation(project(":auth-service:api"))
        }
    }
}
