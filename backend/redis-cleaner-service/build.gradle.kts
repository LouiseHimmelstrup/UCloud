version = "0.4.5"

application {
    mainClassName = "dk.sdu.cloud.redis.cleaner.MainKt"
}

kotlin.sourceSets {
    val jvmMain by getting {
        dependencies {
            implementation(project(":auth-service:api"))
        }
    }
}
