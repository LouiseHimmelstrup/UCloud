//DEPS dk.sdu.cloud:k8-resources:0.1.0
package dk.sdu.cloud.k8

bundle {
    name = "file-trash"
    version = "1.3.7"

    withAmbassador("/api/files/trash") {}

    val deployment = withDeployment {
        deployment.spec.replicas = 2
    }
}