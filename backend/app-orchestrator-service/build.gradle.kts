version = "2.2.0-projects.8"

application {
    mainClassName = "dk.sdu.cloud.app.orchestrator.MainKt"
}

dependencies {
    implementation(project(":auth-service:api"))
    implementation(project(":app-store-service:api"))
    implementation(project(":app-license-service:api"))
    implementation(project(":storage-service:api"))
    implementation(project(":project-service:api"))
    implementation("com.vladmihalcea:hibernate-types-52:2.4.1")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.9.4")
}