//DEPS dk.sdu.cloud:k8-resources:0.1.2
package dk.sdu.cloud.k8

bundle {
    name = "grant"
    version = "0.2.0-rc0"
    
    withAmbassador() {
        addSimpleMapping("/api/gifts")
    }
    
    val deployment = withDeployment {
        deployment.spec.replicas = 1
    }
    
    withPostgresMigration(deployment)
}
