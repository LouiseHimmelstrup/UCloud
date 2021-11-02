//DEPS dk.sdu.cloud:k8-resources:0.1.0
package dk.sdu.cloud.k8

bundle {
    name = "notification"
    version = "2021.3.0-alpha0"

    withAmbassador("/api/notifications") {}

    val deployment = withDeployment {
        deployment.spec.replicas = Configuration.retrieve("defaultScale", "Default scale", 1)
    }

    withPostgresMigration(deployment)
}
