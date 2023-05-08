package dk.sdu.cloud.plugins.compute.ucloud

import dk.sdu.cloud.utils.sendTerminalMessage
import java.lang.IllegalStateException
import java.util.*
import kotlin.collections.ArrayList

data class AllocatedReplica<UserData>(
    val jobId: Long,
    val rank: Int,
    val node: String,
    val data: UserData
)

// TODO It would probably make sense to remove the type from the scheduler and simply have multiple schedulers
//  where only one type can be in a single scheduler. This would save a lot of string comparisons.
class Scheduler<UserData> {
    private var time = 0L

    private var nextNodeIdx = 0
    private val nodeNames = arrayOfNulls<String>(MAX_NODES)
    private val nodeEntries = Array(MAX_NODES) { Node() }
    data class Node(
        @JvmField var type: String = "",
        @JvmField var cpu: Int = 0,
        @JvmField var memory: Long = 0,
        @JvmField var nvidiaGpu: Int = 0,
        @JvmField var lastSeen: Long = 0,
    )

    private var nextQueueIdx = 0
    private val queueJobIds = LongArray(MAX_JOBS_IN_QUEUE)
    private val queueEntries = Array(MAX_JOBS_IN_QUEUE) { QueueEntry<UserData>() }
    data class QueueEntry<UserData>(
        @JvmField var type: String = "",
        @JvmField var cpu: Int = 0,
        @JvmField var memory: Long = 0,
        @JvmField var nvidiaGpu: Int = 0,
        @JvmField var replicas: Int = 0,
        @JvmField var lastSeen: Long = 0,
        @JvmField var data: UserData? = null,
    )

    private val replicaJobId = LongArray(MAX_JOBS_IN_QUEUE)
    private val replicaEntries = Array(MAX_JOBS_IN_QUEUE) { ReplicaEntry<UserData>() }
    data class ReplicaEntry<UserData>(
        @JvmField var rank: Int = 0,
        @JvmField var cpu: Int = 0,
        @JvmField var memory: Long = 0,
        @JvmField var nvidiaGpu: Int = 0,
        @JvmField var node: Int = 0,
        @JvmField var lastSeen: Long = 0,
        @JvmField var data: UserData? = null,
    )

    private var activeReplicas = 0
    private var activeNodes = 0
    private var activeQueueEntries = 0

    private val nodesToConsider = IntArray(MAX_NODES_TO_CONSIDER)

    fun registerNode(
        name: String,
        type: String,
        virtualCpuMillis: Int,
        memoryInBytes: Long,
        nvidiaGpus: Int = 0
    ) {
        val existing = nodeNames.indexOf(name)
        if (existing != -1) {
            nodeEntries[existing].lastSeen = time
            return
        }

        var idx = nextNodeIdx
        if (nodeNames[idx] != null) idx = nodeNames.indexOf(null)

        if (idx == -1) {
            throw IllegalStateException("Too many nodes while registering " +
                    "$name $type $virtualCpuMillis $memoryInBytes $nvidiaGpus")
        }

        nextNodeIdx = (idx + 1) % MAX_NODES

        nodeNames[idx] = name
        with(nodeEntries[idx]) {
            this.type = type
            this.cpu = virtualCpuMillis
            this.memory = memoryInBytes
            this.nvidiaGpu = nvidiaGpus
            this.lastSeen = time

            activeNodes++
        }
    }

    fun pruneNodes(): List<String> {
        val result = ArrayList<String>()
        val maxNodes = activeNodes
        var processed = 0
        for (idx in nodeNames.indices) {
            if (maxNodes == processed) break
            if (nodeNames[idx] == null) continue
            processed++
            val entry = nodeEntries[idx]
            if (entry.lastSeen != time) {
                result.add(nodeNames[idx] ?: "???")
                nodeNames[idx] = null

                // NOTE(Dan): Invalidate all replicas which are on the deleted node
                for (rIdx in replicaEntries.indices) {
                    if (replicaJobId[rIdx] != 0L && replicaEntries[rIdx].node == idx) {
                        replicaJobId[rIdx] = 0
                    }
                }

                activeNodes--
            }
        }
        return result
    }

    private fun allocatedReplica(existing: Int): AllocatedReplica<UserData> {
        val entry = replicaEntries[existing]
        @Suppress("UNCHECKED_CAST")
        return AllocatedReplica(
            replicaJobId[existing],
            entry.rank,
            nodeNames[entry.node]!!,
            entry.data as UserData
        )
    }

    fun findRunningReplica(
        jobId: Long,
        rank: Int,
        touch: Boolean
    ): AllocatedReplica<UserData>? {
        val existing = replicaJobId.indexOf(jobId)
        if (existing != -1) {
            if (replicaEntries[existing].rank == rank) {
                if (touch) replicaEntries[existing].lastSeen = time
                return allocatedReplica(existing)
            }

            for (idx in existing until replicaJobId.size) {
                val entry = replicaEntries[idx]
                if (replicaJobId[idx] == jobId && entry.rank == rank) {
                    if (touch) entry.lastSeen = time
                    return allocatedReplica(idx)
                }
            }
        }
        return null
    }

    fun runningReplicas(): Iterator<AllocatedReplica<UserData>> {
        return object : Iterator<AllocatedReplica<UserData>> {
            var idx = 0

            override fun hasNext(): Boolean {
                for (i in idx until replicaEntries.size) {
                    if (replicaJobId[i] != 0L) {
                        idx = i
                        return true
                    }
                }
                return false
            }

            override fun next(): AllocatedReplica<UserData> {
                return allocatedReplica(idx++)
            }
        }
    }

    fun addRunningReplica(
        jobId: Long,
        rank: Int,
        virtualCpuMillis: Int,
        memoryInBytes: Long,
        nvidiaGpus: Int = 0,
        node: String,
        data: UserData,
    ) {
        if (findRunningReplica(jobId, rank, touch = true) != null) return

        val nodeIdx = nodeNames.indexOf(node)
        if (nodeIdx == -1) throw IllegalArgumentException("Unknown node: $node")

        val replicaIdx = replicaJobId.indexOf(0L)
        if (replicaIdx == -1) throw IllegalStateException("Too many running jobs ($jobId $rank)")

        replicaJobId[replicaIdx] = jobId
        with (replicaEntries[replicaIdx]) {
            this.rank = rank
            this.node = nodeIdx
            this.cpu = virtualCpuMillis
            this.memory = memoryInBytes
            this.nvidiaGpu = nvidiaGpus
            this.lastSeen = time
            this.data = data

            val nodeEntry = nodeEntries[this.node]
            nodeEntry.cpu -= this.cpu
            nodeEntry.memory -= this.memory
            nodeEntry.nvidiaGpu -= this.nvidiaGpu

            activeReplicas++
        }
    }

    fun pruneJobs(): List<AllocatedReplica<UserData>> {
        val replicasPruned = ArrayList<AllocatedReplica<UserData>>()
        val maxToProcess = activeReplicas
        var processed = 0
        for (idx in replicaJobId.indices) {
            if (processed == maxToProcess) break
            if (replicaJobId[idx] == 0L) continue
            processed++
            val entry = replicaEntries[idx]
            if (entry.lastSeen != time) {
                val nodeEntry = nodeEntries[entry.node]
                nodeEntry.cpu += entry.cpu
                nodeEntry.memory += entry.memory
                nodeEntry.nvidiaGpu += entry.nvidiaGpu

                replicasPruned.add(allocatedReplica(idx))
                replicaJobId[idx] = 0L
                entry.data = null
                activeReplicas--
            }
        }
        return replicasPruned
    }

    fun removeJob(jobId: Long): List<AllocatedReplica<UserData>> {
        val replicasRemoved = ArrayList<AllocatedReplica<UserData>>()
        for (i in replicaJobId.indices) {
            val replicaId = replicaJobId[i]
            if (replicaId == jobId) {
                replicasRemoved.add(allocatedReplica(i))

                replicaJobId[i] = 0

                val replicaEntry = replicaEntries[i]
                val nodeIdx = replicaEntry.node
                val nodeEntry = nodeEntries[nodeIdx]
                nodeEntry.cpu += replicaEntry.cpu
                nodeEntry.memory += replicaEntry.memory
                nodeEntry.nvidiaGpu += replicaEntry.nvidiaGpu

                replicaEntry.data = null
            }
        }

        removeJobFromQueue(jobId)
        return replicasRemoved
    }

    fun removeJobFromQueue(jobId: Long) {
        for (i in queueJobIds.indices) {
            val queueId = queueJobIds[i]
            if (queueId == jobId) {
                queueJobIds[i] = 0
                queueEntries[i].data = null
                activeQueueEntries--
                break
            }
        }
    }

    fun addJobToQueue(
        id: Long,
        nodeType: String,
        virtualCpuMillis: Int,
        memoryInBytes: Long,
        nvidiaGpus: Int = 0,
        replicas: Int = 1,
        data: UserData,
    ) {
        var idx = nextQueueIdx
        if (queueJobIds[idx] != 0L) idx = queueJobIds.indexOf(0L)
        if (idx == -1) throw IllegalStateException("Too many jobs in the queue: $id")

        queueJobIds[idx] = id
        with(queueEntries[idx]) {
            this.type = nodeType
            this.cpu = virtualCpuMillis
            this.memory = memoryInBytes
            this.nvidiaGpu = nvidiaGpus
            this.replicas = replicas
            this.lastSeen = time
            this.data = data
            activeQueueEntries++
        }

        nextQueueIdx = (idx + 1) % MAX_JOBS_IN_QUEUE
    }

    fun isJobInQueue(id: Long): Boolean {
        return queueJobIds.indexOf(id) != -1
    }

    private fun nodeSatisfiesRequest(node: Int, request: Int): Boolean {
        val nodeEntry = nodeEntries[node]
        val queueEntry = queueEntries[request]
        if (nodeEntry.type != queueEntry.type) return false
        if (nodeEntry.cpu < queueEntry.cpu) return false
        if (nodeEntry.memory < queueEntry.memory) return false
        if (nodeEntry.nvidiaGpu < queueEntry.nvidiaGpu) return false
        return true
    }

    private val nodeComparator = kotlin.Comparator<Int> { a, b ->
        if (a == b) return@Comparator 0
        if (a == -1) return@Comparator 1
        if (b == -1) return@Comparator -1

        val aEntry = nodeEntries[a]
        val bEntry = nodeEntries[a]

        var cmp: Int

        val aGpu = aEntry.nvidiaGpu
        val bGpu = bEntry.nvidiaGpu
        cmp = aGpu.compareTo(bGpu)
        if (cmp != 0) return@Comparator cmp

        val aCpu = aEntry.cpu
        val bCpu = bEntry.cpu
        cmp = aCpu.compareTo(bCpu)
        if (cmp != 0) return@Comparator cmp

        val aMemory = aEntry.memory
        val bMemory = bEntry.memory
        cmp = aMemory.compareTo(bMemory)
        if (cmp != 0) return@Comparator cmp

        return@Comparator 0
    }

    private fun subtractRequestFromNode(node: Int, request: Int) {
        val nodeEntry = nodeEntries[node]
        val queueEntry = queueEntries[request]
        nodeEntry.cpu -= queueEntry.cpu
        nodeEntry.memory -= queueEntry.memory
        nodeEntry.nvidiaGpu -= queueEntry.nvidiaGpu
    }

    private fun returnRequestToNode(node: Int, request: Int) {
        val nodeEntry = nodeEntries[node]
        val queueEntry = queueEntries[request]
        nodeEntry.cpu += queueEntry.cpu
        nodeEntry.memory += queueEntry.memory
        nodeEntry.nvidiaGpu += queueEntry.nvidiaGpu
    }

    fun schedule(): List<AllocatedReplica<UserData>> {
        val scheduledJobs = ArrayList<AllocatedReplica<UserData>>()

        val maxQueueEntriesToProcess = activeQueueEntries
        var queueEntriesProcessed = 0
        jobLoop@for (queueIdx in 0 until MAX_JOBS_IN_QUEUE) {
            if (queueEntriesProcessed == maxQueueEntriesToProcess) break
            val queueJobId = queueJobIds[queueIdx]
            if (queueJobId == 0L) continue
            val queueEntry = queueEntries[queueIdx]
            queueEntriesProcessed++

            var numberOfNodesToConsider = 0
            Arrays.fill(nodesToConsider, -1)

            for (nodeIdx in 0 until MAX_NODES) {
                if (numberOfNodesToConsider >= MAX_NODES_TO_CONSIDER) break
                if (numberOfNodesToConsider >= queueEntry.replicas) break

                if (nodeSatisfiesRequest(nodeIdx, queueIdx)) {
                    nodesToConsider[numberOfNodesToConsider++] = nodeIdx
                }
            }

            if (numberOfNodesToConsider == 0) continue // No nodes satisfy the request, we cannot schedule it yet

            // Sort the list, such that the smallest nodes appear first in the list
            // TODO(Dan): Performance could be improved by using a custom sort implementation. Currently, all of
            //  the primitives are going to be boxed which is a significant enough overhead that it becomes clearly
            //  visible in the profiler.
            nodesToConsider.sortedWith(nodeComparator)

            // Allocate replicas to nodes
            val allocation = IntArray(queueEntry.replicas)
            var rank = 0
            replicaLoop@while (rank < allocation.size) {
                for (considerIdx in 0 until numberOfNodesToConsider) {
                    val nodeIdx = nodesToConsider[considerIdx]
                    if (nodeSatisfiesRequest(nodeIdx, queueIdx)) {
                        allocation[rank++] = nodeIdx
                        subtractRequestFromNode(nodeIdx, queueIdx)
                        continue@replicaLoop
                    }
                }
                break
            }

            if (rank < allocation.size) {
                // If we didn't allocate all the replicas, then return the allocation to the nodes
                for (i in 0 until rank) {
                    returnRequestToNode(allocation[i], queueIdx)
                }
            } else {
                // Otherwise, write them into the active set

                var replicaIdx = 0
                rank = 0
                while (rank < allocation.size) {
                    if (replicaJobId[replicaIdx] != 0L) replicaIdx = replicaJobId.indexOf(0L)
                    if (replicaIdx == -1) throw IllegalStateException("Too many running replicas") // TODO Probably just don't schedule it yet

                    replicaJobId[replicaIdx] = queueJobIds[queueIdx]
                    with(replicaEntries[replicaIdx]) {
                        this.rank = rank
                        this.node = allocation[rank]
                        this.cpu = queueEntry.cpu
                        this.memory = queueEntry.memory
                        this.nvidiaGpu = queueEntry.nvidiaGpu
                        this.lastSeen = time
                        this.data = queueEntry.data
                        activeReplicas++
                        activeQueueEntries--
                    }

                    @Suppress("UNCHECKED_CAST")
                    scheduledJobs.add(
                        AllocatedReplica(
                            queueJobIds[queueIdx],
                            rank,
                            nodeNames[allocation[rank]] ?: error("internal error"),
                            queueEntry.data as UserData
                        )
                    )

                    rank++
                    replicaIdx++
                    replicaIdx %= replicaJobId.size
                }

                queueJobIds[queueIdx] = 0L
                queueEntry.data = null
            }
        }

        time++
        return scheduledJobs
    }

    fun dumpState(replicas: Boolean = true, nodes: Boolean = true) {
        if (replicas) {
            for (i in 0 until replicaJobId.size) {
                val jobId = replicaJobId[i]
                if (jobId == 0L) continue
                sendTerminalMessage { line("$jobId[${replicaEntries[i]}] ${nodeNames[replicaEntries[i].node]}") }
            }
        }

        if (nodes) {
            for (i in 0 until nodeNames.size) {
                val name = nodeNames[i]
                if (name == null) continue
                sendTerminalMessage { line("${name} ${nodeEntries[i]}") }
            }
        }
    }

    companion object {
        const val MAX_NODES = 1024
        const val MAX_JOBS_IN_QUEUE = MAX_NODES * 8
        const val MAX_NODES_TO_CONSIDER = 128
    }
}


fun main() {
    println("Waiting...")
    Scanner(System.`in`).nextLine()
    val scheduler = Scheduler<Unit>()

    scheduler.registerNode(
        "gpu",
        "gpu",
        8000,
        8000,
        8
    )

    /*
    scheduler.addJobToQueue(1, "gpu", 1000, 1000, 1, data = Unit)
    scheduler.addJobToQueue(2, "gpu", 1000, 1000, 4, data = Unit)
    scheduler.addJobToQueue(3, "gpu", 1000, 1000, 1, data = Unit)
    scheduler.addJobToQueue(4, "gpu", 1000, 1000, 1, data = Unit)
    scheduler.addJobToQueue(5, "gpu", 1000, 1000, 1, data = Unit)
    // Now it should be full
    scheduler.addJobToQueue(6, "gpu", 1000, 1000, 1, data = Unit)
    scheduler.addJobToQueue(7, "gpu", 1000, 1000, 1, data = Unit)
    scheduler.addJobToQueue(8, "gpu", 1000, 1000, 1, replicas = 2, data = Unit)
    scheduler.addJobToQueue(9, "gpu", 1000, 1000, 1, replicas = 2, data = Unit)
    scheduler.addJobToQueue(10, "gpu", 1000, 1000, 1, data = Unit)

    println("Iteration")
    scheduler.schedule().forEach { println(it) }

    scheduler.findRunningReplica(1, 0, touch = true)
    scheduler.findRunningReplica(3, 0, touch = true)
    scheduler.findRunningReplica(4, 0, touch = true)
    scheduler.findRunningReplica(5, 0, touch = true)
    scheduler.pruneJobs()

    println("Iteration")
    scheduler.schedule().forEach { println(it) }

    scheduler.findRunningReplica(1, 0, touch = true)
    scheduler.findRunningReplica(3, 0, touch = true)
    scheduler.findRunningReplica(4, 0, touch = true)
    scheduler.findRunningReplica(5, 0, touch = true)
    scheduler.findRunningReplica(6, 0, touch = true)
    scheduler.findRunningReplica(7, 0, touch = true)
    scheduler.findRunningReplica(8, 0, touch = true)
    // 8 1 has died
    scheduler.pruneJobs()

    println("Iteration")
    scheduler.schedule().forEach { println(it) }

    scheduler.findRunningReplica(3, 0, touch = true)
    scheduler.findRunningReplica(4, 0, touch = true)
    scheduler.findRunningReplica(5, 0, touch = true)
    scheduler.findRunningReplica(6, 0, touch = true)
    scheduler.findRunningReplica(7, 0, touch = true)
    scheduler.findRunningReplica(10, 0, touch = true)
    scheduler.pruneJobs()

    println("Iteration")
    scheduler.schedule().forEach { println(it) }

     */

    var scheduleCount = 0
    repeat(100_000) {
        if (it % 1000 == 0) println(it)
        scheduler.pruneJobs()
        scheduler.addJobToQueue(100L + it, "gpu", 8000, 8000, 8, 1, Unit)
        scheduler.schedule().also { scheduleCount += it.size }
    }

    scheduler.dumpState()
    println(scheduleCount)

    /*
    (0 until 5000).forEach {
        scheduler.registerNode(
            "node-${it.toString().padStart(4, '0')}",
            "type-${it % 4}",
            64_000,
            1024 * 1024 * 1024 * 64L,
            0
        )
    }

    val start = System.currentTimeMillis()
    repeat(5000) { rounds ->
        repeat(10) { internalIdx ->
            val jobId = (rounds * 10L + internalIdx)
            scheduler.addJobToQueue(
                jobId,
                "type-0",
                1000,
                1024 * 1024 * 1024L,
                0,
                data = Unit
            )
        }

        val newJobs = scheduler.schedule()
//        println(newJobs)

//        println("Scheduled ${newJobs.size} jobs in rounds $rounds in")
    }
    val end = System.currentTimeMillis()
    println("Took ${end - start} for 50000 jobs")
     */

    scheduler.dumpState()
}

