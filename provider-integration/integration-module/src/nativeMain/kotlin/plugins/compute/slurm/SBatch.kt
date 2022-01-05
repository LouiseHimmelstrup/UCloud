package dk.sdu.cloud.plugins.compute.slurm

import dk.sdu.cloud.app.orchestrator.api.*

private fun escapeBash(value: String): String {
    return buildString {
        for (char in value) {
            append(
                when (char) {
                    '\\' -> "\\\\"
                    '\'' -> "'\"'\"'"
                    '\"' -> "\\\""
                    '`' -> "\\`"
                    '$' -> "\\$"
                    else -> char
                }
            )
        }
    }
}

suspend fun createSbatchFile(job: Job, config: SlurmConfiguration): String {
    @Suppress("DEPRECATION") val timeAllocation = job.specification.timeAllocation
        ?: job.status.resolvedApplication!!.invocation.tool.tool!!.description.defaultTimeAllocation

    val formattedTime = "${timeAllocation.hours}:${timeAllocation.minutes}:${timeAllocation.seconds}"
    val resolvedProduct = job.status.resolvedProduct!!

    //sbatch will stop processing further #SBATCH directives once the first non-comment non-whitespace line has been reached in the script.
    // remove whitespaces
    val app = job.status.resolvedApplication!!.invocation
    val givenParameters =
        job.specification.parameters!!.mapNotNull { (paramName, value) ->
            app.parameters.find { it.name == paramName }!! to value
        }.toMap()
    val cliInvocation = app.invocation.flatMap { parameter ->
        parameter.buildInvocationList(givenParameters)
    }.joinToString(separator = " ") { "'" + escapeBash(it) + "'" }

    val memoryAllocation = if (config.useFakeMemoryAllocations) {
        "50M"
    } else {
        "${resolvedProduct.memoryInGigs ?: 1}G"
    }

    return buildString {
        appendLine("#!/usr/bin/env bash")
        appendLine("#")
        appendLine("# POSTFIX START")
        appendLine("#")
        appendLine("#SBATCH --chdir ${config.mountpoint}/${job.id}")
        appendLine("#SBATCH --cpus-per-task ${resolvedProduct.cpu ?: 1}")
        appendLine("#SBATCH --mem $memoryAllocation")
        appendLine("#SBATCH --gpus-per-node ${resolvedProduct.gpu ?: 0}")
        appendLine("#SBATCH --time $formattedTime")
        appendLine("#SBATCH --nodes ${job.specification.replicas}")
        appendLine("#SBATCH --job-name ${job.id}")
        appendLine("#SBATCH --partition ${config.partition}")
        appendLine("#SBATCH --parsable")
        appendLine("#SBATCH --output=std.out")
        appendLine("#SBATCH --error=std.err")
        appendLine("#")
        appendLine("# POSTFIX END")
        appendLine("#")
        appendLine(cliInvocation)
        appendLine("#EOF")
    }
}