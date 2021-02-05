version = "3.3.0-rc1"

application {
    mainClassName = "dk.sdu.cloud.project.MainKt"
}

dependencies {
    implementation(project(":auth-service:api"))
    implementation(project(":contact-book-service:api"))
    implementation(project(":mail-service:api"))
    implementation(project(":notification-service:api"))
}
