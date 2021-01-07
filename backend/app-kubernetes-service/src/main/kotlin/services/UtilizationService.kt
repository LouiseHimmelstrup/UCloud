package dk.sdu.cloud.app.kubernetes.services

import com.github.jasync.sql.db.util.length
import dk.sdu.cloud.app.kubernetes.services.volcano.VolcanoJob
import dk.sdu.cloud.app.kubernetes.services.volcano.volcanoJob
import dk.sdu.cloud.app.orchestrator.api.CpuAndMemory
import dk.sdu.cloud.app.orchestrator.api.QueueStatus
import dk.sdu.cloud.service.k8.*

/**
 * A service responsible for fetching utilization information
 */
class UtilizationService(
    private val k8: K8Dependencies
) {
    suspend fun retrieveCapacity(): CpuAndMemory {
         val namespace = k8.client.getResource<Namespace>(
                KubernetesResources.namespaces.withName("app-kubernetes")
            )

        val computeAnnotation = namespace.metadata?.annotations?.get("scheduler.alpha.kubernetes.io/node-selector")?.toString()

        val nodes = k8.client.listResources<Node>(KubernetesResources.node.withNamespace(NAMESPACE_ANY))
            .items
            .filter { node ->
                computeAnnotation != "ucloud-compute=true" ||
                    node.metadata?.labels?.get("ucloud-compute") == "true"
            }

        val nodeAllocatableCpu = nodes.sumOf { node ->
            node.status?.allocatable?.cpu?.toDouble() ?: 0.0
        }

        val nodeAllocatableMemory = nodes.sumOf { node ->
            memoryStringToBytes(node.status?.allocatable?.memory)
        }

        return CpuAndMemory(nodeAllocatableCpu, nodeAllocatableMemory)
    }

    suspend fun retrieveUsedCapacity(): CpuAndMemory {
        val jobs = k8.client.listResources<VolcanoJob>(
            KubernetesResources.volcanoJob.withNamespace("app-kubernetes")
        ).filter { job ->
            job.status?.state?.phase == "Running"
        }

        val cpuUsage = jobs.sumOf { job ->
            job.spec?.tasks?.sumOf { task ->
                task.template?.spec?.containers?.sumOf { container ->
                    cpuStringToCores(container.resources?.limits?.get("cpu")?.toString()) * (job?.status?.running ?: 0)
                } ?: 0.0
            } ?: 0.0
        }

        val memoryUsage = jobs.sumOf { job ->
            job.spec?.tasks?.sumOf { task ->
                task.template?.spec?.containers?.sumOf { container ->
                    memoryStringToBytes(container.resources?.limits?.get("memory")?.toString()) * (job?.status?.running ?: 0)
                } ?: 0
            } ?: 0
        }

        return CpuAndMemory(cpuUsage, memoryUsage)
    }

    suspend fun retrieveQueueStatus(): QueueStatus{
        val jobs = k8.client.listResources<VolcanoJob>(
            KubernetesResources.volcanoJob.withNamespace("app-kubernetes")
        )

        val runningJobs = jobs.filter { job ->
            job.status?.state?.phase == VolcanoJobPhase.Running
        }

        val pending = jobs.filter { job ->
            (job.status?.running ?: 0) < (job.spec?.minAvailable ?: 0)
        }.length

        return QueueStatus(runningJobs.length, pending)
    }

    private fun memoryStringToBytes(memory: String?): Long {
        val numbersOnly = Regex("[^0-9]")
        val ki = 1024
        val mi = 1048576
        val gi = 1073741824
        val ti = 1099511627776
        val pi = 1125899906842624

        return if (memory.isNullOrBlank()) {
            0
        } else (when {
            memory.contains("Ki") -> {
                numbersOnly.replace(memory, "").toLong() * ki
            }
            memory.contains("Mi") -> {
                numbersOnly.replace(memory, "").toLong() * mi
            }
            memory.contains("Gi") -> {
                numbersOnly.replace(memory, "").toLong() * gi
            }
            memory.contains("Ti") -> {
                numbersOnly.replace(memory, "").toLong() * ti
            }
            memory.contains("Pi") -> {
                numbersOnly.replace(memory, "").toLong() * pi
            }
            else -> {
                numbersOnly.replace(memory, "").toLong()
            }
        })
    }

    private fun cpuStringToCores(cpus: String?): Double {
        val numbersOnly = Regex("[^0-9]")

        return if (cpus.isNullOrBlank()) {
            0.toDouble()
        } else (when {
            cpus.contains("m") -> {
                numbersOnly.replace(cpus, "").toDouble() / 1000
            }
            else -> {
                numbersOnly.replace(cpus, "").toDouble()
            }
        })
    }
}
