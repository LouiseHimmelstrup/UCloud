<p align='center'>
<a href='/docs/developer-guide/built-in-provider/storage/shares.md'>« Previous section</a>
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<a href='/docs/developer-guide/built-in-provider/compute/ingress.md'>Next section »</a>
</p>


[UCloud Developer Guide](/docs/developer-guide/README.md) / [Built-in Provider](/docs/developer-guide/built-in-provider/README.md) / [UCloud/Compute](/docs/developer-guide/built-in-provider/compute/README.md) / Jobs
# Jobs

[![API: Experimental/Alpha](https://img.shields.io/static/v1?label=API&message=Experimental/Alpha&color=orange&style=flat-square)](/docs/developer-guide/core/api-conventions.md)

_The ingoing provider API for Jobs_

## Rationale

[`Job`](/docs/reference/dk.sdu.cloud.app.kubernetes.api.Job.md)  s in UCloud are the core abstraction used to describe units of computation.

This document describes the API which providers receive to implement Jobs. We recommend that you read the
documentation for the end-user API first. Most of this API is a natural extension of the end-user APIs. 
Almost all RPCs in this API have a direct match in the end-user API. Most endpoints in the provider API
receives a Job along with extra call details. This is the main difference from the end-user API. In the
end-user API the request is mainly a reference or a specification plus call details.

It is not required that you, as a provider, implement all calls. However, you must implement all the calls
which you support. This level of support is controlled by your response to the [`retrieveProducts`](/docs/reference/retrieveProducts.md) 
call (See below for an example).

---

__📝 Provider Note:__ This is the API exposed to providers. See the table below for other relevant APIs.

| End-User | Provider (Ingoing) | Control (Outgoing) |
|----------|--------------------|--------------------|
| [`Jobs`](/docs/developer-guide/orchestration/compute/jobs.md) | [`JobsProvider`](/docs/developer-guide/orchestration/compute/providers/jobs/ingoing.md) | [`JobsControl`](/docs/developer-guide/orchestration/compute/providers/jobs/outgoing.md) |

---

## Multi-replica Jobs


A `Job` can be scheduled on more than one replica. The orchestrator requires that backends execute the exact same
command on all the nodes. Information about other nodes will be mounted at `/etc/ucloud`. This information allows jobs
to configure themselves accordingly.

Each node is given a rank. The rank is 0-indexed. By convention index 0 is used as a primary point of contact.

The table below summarizes the files mounted at `/etc/ucloud` and their contents:

| **Name**              | **Description**                                           |
|-----------------------|-----------------------------------------------------------|
| `node-$rank.txt`      | Single line containing hostname/ip address of the 'node'. |
| `rank.txt`            | Single line containing the rank of this node.             |
| `cores.txt`           | Single line containing the amount of cores allocated.     |
| `number_of_nodes.txt` | Single line containing the number of nodes allocated.     |
| `job_id.txt`          | Single line containing the id of this job.                |

---

__📝 NOTE:__ We expect that the mount location will become more flexible in a future release. See
issue [#2124](https://github.com/SDU-eScience/UCloud/issues/2124).

---

## Networking and Peering with Other Applications

`Job`s are, by default, only allowed to perform networking with other nodes in the same `Job`. A user can override this
by requesting, at `Job` startup, networking with an existing job. This will configure the firewall accordingly and allow
networking between the two `Job`s. This will also automatically provide user-friendly hostnames for the `Job`.

## The `/work`ing directory

UCloud assumes that the `/work` directory is available for data which needs to be persisted. It is expected
that files left directly in this directory is placed in the `output` folder of the `Job`. 

## Ephemeral Resources

Every `Job` has some resources which exist only as long as the `Job` is `RUNNING`. These types of resources are said to
be ephemeral resources. Examples of this includes temporary working storage included as part of the `Job`. Such
storage is _not_ guaranteed to be persisted across `Job` runs and `Application`s should not rely on this behavior.

## Job Scheduler

The job scheduler is responsible for running `Job`s on behalf of users. The provider can tweak which features the
scheduler is able to support using the provider manifest.

UCloud puts no strict requirements on how the job scheduler runs job and leaves this to the provider. For example, this
means that there are no strict requirements on how jobs are queued. Jobs can be run in any order which the provider sees
fit.

## Table of Contents
<details>
<summary>
<a href='#example-declaring-support-full-support-for-containerized-applications'>1. Examples</a>
</summary>

<table><thead><tr>
<th>Description</th>
</tr></thread>
<tbody>
<tr><td><a href='#example-declaring-support-full-support-for-containerized-applications'>Declaring support full support for containerized applications</a></td></tr>
<tr><td><a href='#example-declaring-minimal-support-for-virtual-machines'>Declaring minimal support for virtual machines</a></td></tr>
<tr><td><a href='#example-simple-batch-job-with-life-cycle-events'>Simple batch Job with life-cycle events</a></td></tr>
<tr><td><a href='#example-accounting'>Accounting</a></td></tr>
<tr><td><a href='#example-ensuring-ucloud/core-and-provider-are-in-sync'>Ensuring UCloud/Core and Provider are in-sync</a></td></tr>
</tbody></table>


</details>

<details>
<summary>
<a href='#remote-procedure-calls'>2. Remote Procedure Calls</a>
</summary>

<table><thead><tr>
<th>Name</th>
<th>Description</th>
</tr></thread>
<tbody>
<tr>
<td><a href='#follow'><code>follow</code></a></td>
<td>Follow the progress of a job</td>
</tr>
<tr>
<td><a href='#retrieveproducts'><code>retrieveProducts</code></a></td>
<td>Retrieve product support for this providers</td>
</tr>
<tr>
<td><a href='#retrieveutilization'><code>retrieveUtilization</code></a></td>
<td>Retrieve information about how busy the provider's cluster currently is</td>
</tr>
<tr>
<td><a href='#create'><code>create</code></a></td>
<td>Creates one or more resources</td>
</tr>
<tr>
<td><a href='#extend'><code>extend</code></a></td>
<td>Extend the duration of one or more jobs</td>
</tr>
<tr>
<td><a href='#openinteractivesession'><code>openInteractiveSession</code></a></td>
<td>Opens an interactive session (e.g. terminal, web or VNC)</td>
</tr>
<tr>
<td><a href='#suspend'><code>suspend</code></a></td>
<td>Suspend a job</td>
</tr>
<tr>
<td><a href='#terminate'><code>terminate</code></a></td>
<td>Request job cancellation and destruction</td>
</tr>
<tr>
<td><a href='#updateacl'><code>updateAcl</code></a></td>
<td>Callback received by the Provider when permissions are updated</td>
</tr>
<tr>
<td><a href='#verify'><code>verify</code></a></td>
<td>Invoked by UCloud/Core to trigger verification of a single batch</td>
</tr>
</tbody></table>


</details>

## Example: Declaring support full support for containerized applications
<table>
<tr><th>Frequency of use</th><td>Common</td></tr>
<tr>
<th>Actors</th>
<td><ul>
<li>The UCloud/Core service user (<code>ucloud</code>)</li>
</ul></td>
</tr>
</table>
<details>
<summary>
<b>Communication Flow:</b> Kotlin
</summary>

```kotlin

/* In this example we will show how you, as a provider, can declare full support for containerized
applications. This example assumes that you have already registered two compute products with
UCloud/Core. */


/* The retrieveProducts call will be invoked by the UCloud/Core service account. UCloud will generally
cache this response for a period of time before re-querying for information. As a result, changes
in your response might not be immediately visible in UCloud. */

KubernetesCompute.retrieveProducts.call(
    Unit,
    ucloud
).orThrow()

/*
BulkResponse(
    responses = listOf(ComputeSupport(
        docker = ComputeSupport.Docker(
            enabled = true, 
            logs = true, 
            peers = true, 
            terminal = true, 
            timeExtension = true, 
            utilization = true, 
            vnc = true, 
            web = true, 
        ), 
        product = ProductReference(
            category = "example-compute", 
            id = "example-compute-1", 
            provider = "example", 
        ), 
        virtualMachine = ComputeSupport.VirtualMachine(
            enabled = null, 
            logs = null, 
            suspension = null, 
            terminal = null, 
            timeExtension = null, 
            utilization = null, 
            vnc = null, 
        ), 
    ), ComputeSupport(
        docker = ComputeSupport.Docker(
            enabled = true, 
            logs = true, 
            peers = true, 
            terminal = true, 
            timeExtension = true, 
            utilization = true, 
            vnc = true, 
            web = true, 
        ), 
        product = ProductReference(
            category = "example-compute", 
            id = "example-compute-2", 
            provider = "example", 
        ), 
        virtualMachine = ComputeSupport.VirtualMachine(
            enabled = null, 
            logs = null, 
            suspension = null, 
            terminal = null, 
            timeExtension = null, 
            utilization = null, 
            vnc = null, 
        ), 
    )), 
)
*/

/* 📝 Note: The support information must be repeated for every Product you support. */


/* 📝 Note: The Products mentioned in this response must already be registered with UCloud. */

```


</details>

<details>
<summary>
<b>Communication Flow:</b> TypeScript
</summary>

```typescript

/* In this example we will show how you, as a provider, can declare full support for containerized
applications. This example assumes that you have already registered two compute products with
UCloud/Core. */


/* The retrieveProducts call will be invoked by the UCloud/Core service account. UCloud will generally
cache this response for a period of time before re-querying for information. As a result, changes
in your response might not be immediately visible in UCloud. */

// Authenticated as ucloud
await callAPI(JobsProviderUcloudApi.retrieveProducts(
    {
    }
);

/*
{
    "responses": [
        {
            "product": {
                "id": "example-compute-1",
                "category": "example-compute",
                "provider": "example"
            },
            "docker": {
                "enabled": true,
                "web": true,
                "vnc": true,
                "logs": true,
                "terminal": true,
                "peers": true,
                "timeExtension": true,
                "utilization": true
            },
            "virtualMachine": {
                "enabled": null,
                "logs": null,
                "vnc": null,
                "terminal": null,
                "timeExtension": null,
                "suspension": null,
                "utilization": null
            }
        },
        {
            "product": {
                "id": "example-compute-2",
                "category": "example-compute",
                "provider": "example"
            },
            "docker": {
                "enabled": true,
                "web": true,
                "vnc": true,
                "logs": true,
                "terminal": true,
                "peers": true,
                "timeExtension": true,
                "utilization": true
            },
            "virtualMachine": {
                "enabled": null,
                "logs": null,
                "vnc": null,
                "terminal": null,
                "timeExtension": null,
                "suspension": null,
                "utilization": null
            }
        }
    ]
}
*/

/* 📝 Note: The support information must be repeated for every Product you support. */


/* 📝 Note: The Products mentioned in this response must already be registered with UCloud. */

```


</details>

<details>
<summary>
<b>Communication Flow:</b> Curl
</summary>

```bash
# ------------------------------------------------------------------------------------------------------
# $host is the UCloud instance to contact. Example: 'http://localhost:8080' or 'https://cloud.sdu.dk'
# $accessToken is a valid access-token issued by UCloud
# ------------------------------------------------------------------------------------------------------

# In this example we will show how you, as a provider, can declare full support for containerized
# applications. This example assumes that you have already registered two compute products with
# UCloud/Core.

# The retrieveProducts call will be invoked by the UCloud/Core service account. UCloud will generally
# cache this response for a period of time before re-querying for information. As a result, changes
# in your response might not be immediately visible in UCloud.

# Authenticated as ucloud
curl -XGET -H "Authorization: Bearer $accessToken" "$host/ucloud/ucloud/jobs/retrieveProducts" 

# {
#     "responses": [
#         {
#             "product": {
#                 "id": "example-compute-1",
#                 "category": "example-compute",
#                 "provider": "example"
#             },
#             "docker": {
#                 "enabled": true,
#                 "web": true,
#                 "vnc": true,
#                 "logs": true,
#                 "terminal": true,
#                 "peers": true,
#                 "timeExtension": true,
#                 "utilization": true
#             },
#             "virtualMachine": {
#                 "enabled": null,
#                 "logs": null,
#                 "vnc": null,
#                 "terminal": null,
#                 "timeExtension": null,
#                 "suspension": null,
#                 "utilization": null
#             }
#         },
#         {
#             "product": {
#                 "id": "example-compute-2",
#                 "category": "example-compute",
#                 "provider": "example"
#             },
#             "docker": {
#                 "enabled": true,
#                 "web": true,
#                 "vnc": true,
#                 "logs": true,
#                 "terminal": true,
#                 "peers": true,
#                 "timeExtension": true,
#                 "utilization": true
#             },
#             "virtualMachine": {
#                 "enabled": null,
#                 "logs": null,
#                 "vnc": null,
#                 "terminal": null,
#                 "timeExtension": null,
#                 "suspension": null,
#                 "utilization": null
#             }
#         }
#     ]
# }

# 📝 Note: The support information must be repeated for every Product you support.

# 📝 Note: The Products mentioned in this response must already be registered with UCloud.

```


</details>


## Example: Declaring minimal support for virtual machines
<table>
<tr><th>Frequency of use</th><td>Common</td></tr>
<tr>
<th>Actors</th>
<td><ul>
<li>The UCloud/Core service user (<code>ucloud</code>)</li>
</ul></td>
</tr>
</table>
<details>
<summary>
<b>Communication Flow:</b> Kotlin
</summary>

```kotlin

/* In this example we will show how you, as a provider, can declare minimal support for virtual
machines. This example assumes that you have already registered two compute products with
UCloud/Core. */


/* The retrieveProducts call will be invoked by the UCloud/Core service account. UCloud will generally
cache this response for a period of time before re-querying for information. As a result, changes
in your response might not be immediately visible in UCloud. */

KubernetesCompute.retrieveProducts.call(
    Unit,
    ucloud
).orThrow()

/*
BulkResponse(
    responses = listOf(ComputeSupport(
        docker = ComputeSupport.Docker(
            enabled = null, 
            logs = null, 
            peers = null, 
            terminal = null, 
            timeExtension = null, 
            utilization = null, 
            vnc = null, 
            web = null, 
        ), 
        product = ProductReference(
            category = "example-compute", 
            id = "example-compute-1", 
            provider = "example", 
        ), 
        virtualMachine = ComputeSupport.VirtualMachine(
            enabled = true, 
            logs = null, 
            suspension = null, 
            terminal = null, 
            timeExtension = null, 
            utilization = null, 
            vnc = null, 
        ), 
    ), ComputeSupport(
        docker = ComputeSupport.Docker(
            enabled = null, 
            logs = null, 
            peers = null, 
            terminal = null, 
            timeExtension = null, 
            utilization = null, 
            vnc = null, 
            web = null, 
        ), 
        product = ProductReference(
            category = "example-compute", 
            id = "example-compute-2", 
            provider = "example", 
        ), 
        virtualMachine = ComputeSupport.VirtualMachine(
            enabled = true, 
            logs = null, 
            suspension = null, 
            terminal = null, 
            timeExtension = null, 
            utilization = null, 
            vnc = null, 
        ), 
    )), 
)
*/

/* 📝 Note: If a support feature is not explicitly mentioned, then no support is assumed. */


/* 📝 Note: The support information must be repeated for every Product you support. */


/* 📝 Note: The Products mentioned in this response must already be registered with UCloud. */

```


</details>

<details>
<summary>
<b>Communication Flow:</b> TypeScript
</summary>

```typescript

/* In this example we will show how you, as a provider, can declare minimal support for virtual
machines. This example assumes that you have already registered two compute products with
UCloud/Core. */


/* The retrieveProducts call will be invoked by the UCloud/Core service account. UCloud will generally
cache this response for a period of time before re-querying for information. As a result, changes
in your response might not be immediately visible in UCloud. */

// Authenticated as ucloud
await callAPI(JobsProviderUcloudApi.retrieveProducts(
    {
    }
);

/*
{
    "responses": [
        {
            "product": {
                "id": "example-compute-1",
                "category": "example-compute",
                "provider": "example"
            },
            "docker": {
                "enabled": null,
                "web": null,
                "vnc": null,
                "logs": null,
                "terminal": null,
                "peers": null,
                "timeExtension": null,
                "utilization": null
            },
            "virtualMachine": {
                "enabled": true,
                "logs": null,
                "vnc": null,
                "terminal": null,
                "timeExtension": null,
                "suspension": null,
                "utilization": null
            }
        },
        {
            "product": {
                "id": "example-compute-2",
                "category": "example-compute",
                "provider": "example"
            },
            "docker": {
                "enabled": null,
                "web": null,
                "vnc": null,
                "logs": null,
                "terminal": null,
                "peers": null,
                "timeExtension": null,
                "utilization": null
            },
            "virtualMachine": {
                "enabled": true,
                "logs": null,
                "vnc": null,
                "terminal": null,
                "timeExtension": null,
                "suspension": null,
                "utilization": null
            }
        }
    ]
}
*/

/* 📝 Note: If a support feature is not explicitly mentioned, then no support is assumed. */


/* 📝 Note: The support information must be repeated for every Product you support. */


/* 📝 Note: The Products mentioned in this response must already be registered with UCloud. */

```


</details>

<details>
<summary>
<b>Communication Flow:</b> Curl
</summary>

```bash
# ------------------------------------------------------------------------------------------------------
# $host is the UCloud instance to contact. Example: 'http://localhost:8080' or 'https://cloud.sdu.dk'
# $accessToken is a valid access-token issued by UCloud
# ------------------------------------------------------------------------------------------------------

# In this example we will show how you, as a provider, can declare minimal support for virtual
# machines. This example assumes that you have already registered two compute products with
# UCloud/Core.

# The retrieveProducts call will be invoked by the UCloud/Core service account. UCloud will generally
# cache this response for a period of time before re-querying for information. As a result, changes
# in your response might not be immediately visible in UCloud.

# Authenticated as ucloud
curl -XGET -H "Authorization: Bearer $accessToken" "$host/ucloud/ucloud/jobs/retrieveProducts" 

# {
#     "responses": [
#         {
#             "product": {
#                 "id": "example-compute-1",
#                 "category": "example-compute",
#                 "provider": "example"
#             },
#             "docker": {
#                 "enabled": null,
#                 "web": null,
#                 "vnc": null,
#                 "logs": null,
#                 "terminal": null,
#                 "peers": null,
#                 "timeExtension": null,
#                 "utilization": null
#             },
#             "virtualMachine": {
#                 "enabled": true,
#                 "logs": null,
#                 "vnc": null,
#                 "terminal": null,
#                 "timeExtension": null,
#                 "suspension": null,
#                 "utilization": null
#             }
#         },
#         {
#             "product": {
#                 "id": "example-compute-2",
#                 "category": "example-compute",
#                 "provider": "example"
#             },
#             "docker": {
#                 "enabled": null,
#                 "web": null,
#                 "vnc": null,
#                 "logs": null,
#                 "terminal": null,
#                 "peers": null,
#                 "timeExtension": null,
#                 "utilization": null
#             },
#             "virtualMachine": {
#                 "enabled": true,
#                 "logs": null,
#                 "vnc": null,
#                 "terminal": null,
#                 "timeExtension": null,
#                 "suspension": null,
#                 "utilization": null
#             }
#         }
#     ]
# }

# 📝 Note: If a support feature is not explicitly mentioned, then no support is assumed.

# 📝 Note: The support information must be repeated for every Product you support.

# 📝 Note: The Products mentioned in this response must already be registered with UCloud.

```


</details>


## Example: Simple batch Job with life-cycle events
<table>
<tr><th>Frequency of use</th><td>Common</td></tr>
<tr><th>Pre-conditions</th><td><ul>
<li>You should understand Products, Applications and Jobs before reading this</li>
<li>The provider must support containerized Applications</li>
<li>The provider must implement the retrieveProducts call</li>
</ul></td></tr>
<tr>
<th>Actors</th>
<td><ul>
<li>The UCloud/Core service user (<code>ucloud</code>)</li>
<li>The provider (<code>provider</code>)</li>
</ul></td>
</tr>
</table>
<details>
<summary>
<b>Communication Flow:</b> Kotlin
</summary>

```kotlin

/* In this example we will show the creation of a simple batch Job. The procedure starts with the
Provider receives a create request from UCloud/Core */


/* The request below contains a lot of information. We recommend that you read about and understand
Products, Applications and Jobs before you continue. We will attempt to summarize the information
below:

- The request contains one or more Jobs. The Provider should schedule each of them on their
  infrastructure.
- The `id` of a Job is unique, globally, in UCloud.
- The `owner` references the UCloud identity and workspace of the creator
- The `specification` contains the user's request
- The `status` contains UCloud's view of the Job _AND_ resolved resources required for the Job

In this example:

- Exactly one Job will be created. 
  - `items` contains only one Job
  
- This Job will run a `BATCH` application
  - See `status.resolvedApplication.invocation.applicationType`
  
- It will run on the `example-compute-1` machine-type
  - See `specification.product` and `status.resolvedProduct`
  
- The application should launch the `acme/batch:1.0.0` container
  - `status.resolvedApplication.invocation.tool.tool.description.backend`
  - `status.resolvedApplication.invocation.tool.tool.description.image`
  
- It will be invoked with `acme-batch --debug "Hello, World!"`. 
  - The invocation is created from `status.resolvedApplication.invocation.invocation`
  - With parameters defined in `status.resolvedApplication.invocation.parameters`
  - And values defined in `specification.parameters`
  
- The Job should be scheduled with a max wall-time of 1 hour 
  - See `specification.timeAllocation`
  
- ...on exactly 1 node.
  - See `specification.replicas` */

KubernetesCompute.create.call(
    bulkRequestOf(Job(
        acl = null, 
        billing = ResourceBilling.Free, 
        createdAt = 1633329776235, 
        id = "54112", 
        output = null, 
        owner = ResourceOwner(
            createdBy = "user", 
            project = null, 
        ), 
        permissions = null, 
        specification = JobSpecification(
            allowDuplicateJob = false, 
            application = NameAndVersion(
                name = "acme-batch", 
                version = "1.0.0", 
            ), 
            name = null, 
            parameters = mapOf("debug" to AppParameterValue.Bool(
                value = true, 
            ), "value" to AppParameterValue.Text(
                value = "Hello, World!", 
            )), 
            product = ProductReference(
                category = "example-compute", 
                id = "example-compute-1", 
                provider = "example", 
            ), 
            replicas = 1, 
            resources = null, 
            timeAllocation = SimpleDuration(
                hours = 1, 
                minutes = 0, 
                seconds = 0, 
            ), 
        ), 
        status = JobStatus(
            expiresAt = null, 
            jobParametersJson = null, 
            resolvedApplication = Application(
                invocation = ApplicationInvocationDescription(
                    allowAdditionalMounts = null, 
                    allowAdditionalPeers = null, 
                    allowMultiNode = false, 
                    applicationType = ApplicationType.BATCH, 
                    container = null, 
                    environment = null, 
                    fileExtensions = emptyList(), 
                    invocation = listOf(WordInvocationParameter(
                        word = "acme-batch", 
                    ), VariableInvocationParameter(
                        isPrefixVariablePartOfArg = false, 
                        isSuffixVariablePartOfArg = false, 
                        prefixGlobal = "--debug ", 
                        prefixVariable = "", 
                        suffixGlobal = "", 
                        suffixVariable = "", 
                        variableNames = listOf("debug"), 
                    ), VariableInvocationParameter(
                        isPrefixVariablePartOfArg = false, 
                        isSuffixVariablePartOfArg = false, 
                        prefixGlobal = "", 
                        prefixVariable = "", 
                        suffixGlobal = "", 
                        suffixVariable = "", 
                        variableNames = listOf("value"), 
                    )), 
                    licenseServers = emptyList(), 
                    outputFileGlobs = listOf("*"), 
                    parameters = listOf(ApplicationParameter.Bool(
                        defaultValue = null, 
                        description = "Should debug be enabled?", 
                        falseValue = "false", 
                        name = "debug", 
                        optional = false, 
                        title = "", 
                        trueValue = "true", 
                    ), ApplicationParameter.Text(
                        defaultValue = null, 
                        description = "The value for the batch application", 
                        name = "value", 
                        optional = false, 
                        title = "", 
                    )), 
                    shouldAllowAdditionalMounts = false, 
                    shouldAllowAdditionalPeers = true, 
                    tool = ToolReference(
                        name = "acme-batch", 
                        tool = Tool(
                            createdAt = 1633329776235, 
                            description = NormalizedToolDescription(
                                authors = listOf("UCloud"), 
                                backend = ToolBackend.DOCKER, 
                                container = null, 
                                defaultNumberOfNodes = 1, 
                                defaultTimeAllocation = SimpleDuration(
                                    hours = 1, 
                                    minutes = 0, 
                                    seconds = 0, 
                                ), 
                                description = "An example tool", 
                                image = "acme/batch:1.0.0", 
                                info = NameAndVersion(
                                    name = "acme-batch", 
                                    version = "1.0.0", 
                                ), 
                                license = "None", 
                                requiredModules = emptyList(), 
                                supportedProviders = null, 
                                title = "Acme batch", 
                            ), 
                            modifiedAt = 1633329776235, 
                            owner = "_ucloud", 
                        ), 
                        version = "1.0.0", 
                    ), 
                    vnc = null, 
                    web = null, 
                ), 
                metadata = ApplicationMetadata(
                    authors = listOf("UCloud"), 
                    description = "An example application", 
                    isPublic = true, 
                    name = "acme-batch", 
                    public = true, 
                    title = "Acme batch", 
                    version = "1.0.0", 
                    website = null, 
                ), 
            ), 
            resolvedProduct = Product.Compute(
                category = ProductCategoryId(
                    id = "example-compute", 
                    name = "example-compute", 
                    provider = "example", 
                ), 
                chargeType = ChargeType.ABSOLUTE, 
                cpu = 1, 
                description = "An example machine", 
                freeToUse = false, 
                gpu = 0, 
                hiddenInGrantApplications = false, 
                memoryInGigs = 2, 
                name = "example-compute-1", 
                pricePerUnit = 1000000, 
                priority = 0, 
                productType = ProductType.COMPUTE, 
                unitOfPrice = ProductPriceUnit.CREDITS_PER_MINUTE, 
                version = 1, 
                balance = null, 
                id = "example-compute-1", 
            ), 
            resolvedSupport = ResolvedSupport(
                product = Product.Compute(
                    category = ProductCategoryId(
                        id = "example-compute", 
                        name = "example-compute", 
                        provider = "example", 
                    ), 
                    chargeType = ChargeType.ABSOLUTE, 
                    cpu = 1, 
                    description = "An example machine", 
                    freeToUse = false, 
                    gpu = 0, 
                    hiddenInGrantApplications = false, 
                    memoryInGigs = 2, 
                    name = "example-compute-1", 
                    pricePerUnit = 1000000, 
                    priority = 0, 
                    productType = ProductType.COMPUTE, 
                    unitOfPrice = ProductPriceUnit.CREDITS_PER_MINUTE, 
                    version = 1, 
                    balance = null, 
                    id = "example-compute-1", 
                ), 
                support = ComputeSupport(
                    docker = ComputeSupport.Docker(
                        enabled = true, 
                        logs = null, 
                        peers = null, 
                        terminal = null, 
                        timeExtension = null, 
                        utilization = null, 
                        vnc = null, 
                        web = null, 
                    ), 
                    product = ProductReference(
                        category = "example-compute", 
                        id = "example-compute-1", 
                        provider = "example", 
                    ), 
                    virtualMachine = ComputeSupport.VirtualMachine(
                        enabled = null, 
                        logs = null, 
                        suspension = null, 
                        terminal = null, 
                        timeExtension = null, 
                        utilization = null, 
                        vnc = null, 
                    ), 
                ), 
            ), 
            startedAt = null, 
            state = JobState.IN_QUEUE, 
        ), 
        updates = emptyList(), 
        providerGeneratedId = "54112", 
    )),
    ucloud
).orThrow()

/*
BulkResponse(
    responses = listOf(null), 
)
*/

/* 📝 Note: The response in this case indicates that the Provider chose not to generate an internal ID
for this Job. If an ID was provided, then on subsequent requests the `providerGeneratedId` of this
Job would be set accordingly. This feature can help providers keep track of their internal state
without having to actively maintain a mapping. */


/* The Provider will use this information to schedule the Job on their infrastructure. Through
background processing, the Provider will keep track of this Job. The Provider notifies UCloud of
state changes as they occur. This happens through the outgoing Control API. */

JobsControl.update.call(
    bulkRequestOf(ResourceUpdateAndId(
        id = "54112", 
        update = JobUpdate(
            expectedDifferentState = null, 
            expectedState = null, 
            outputFolder = null, 
            state = JobState.RUNNING, 
            status = "The job is now running!", 
            timestamp = 0, 
        ), 
    )),
    provider
).orThrow()

/*
Unit
*/

/* 📝 Note: The timestamp field will be filled out by UCloud/Core */


/* ~ Some time later ~ */

JobsControl.update.call(
    bulkRequestOf(ResourceUpdateAndId(
        id = "54112", 
        update = JobUpdate(
            expectedDifferentState = null, 
            expectedState = null, 
            outputFolder = null, 
            state = JobState.SUCCESS, 
            status = "The job has finished processing!", 
            timestamp = 0, 
        ), 
    )),
    provider
).orThrow()

/*
Unit
*/
```


</details>

<details>
<summary>
<b>Communication Flow:</b> TypeScript
</summary>

```typescript

/* In this example we will show the creation of a simple batch Job. The procedure starts with the
Provider receives a create request from UCloud/Core */


/* The request below contains a lot of information. We recommend that you read about and understand
Products, Applications and Jobs before you continue. We will attempt to summarize the information
below:

- The request contains one or more Jobs. The Provider should schedule each of them on their
  infrastructure.
- The `id` of a Job is unique, globally, in UCloud.
- The `owner` references the UCloud identity and workspace of the creator
- The `specification` contains the user's request
- The `status` contains UCloud's view of the Job _AND_ resolved resources required for the Job

In this example:

- Exactly one Job will be created. 
  - `items` contains only one Job
  
- This Job will run a `BATCH` application
  - See `status.resolvedApplication.invocation.applicationType`
  
- It will run on the `example-compute-1` machine-type
  - See `specification.product` and `status.resolvedProduct`
  
- The application should launch the `acme/batch:1.0.0` container
  - `status.resolvedApplication.invocation.tool.tool.description.backend`
  - `status.resolvedApplication.invocation.tool.tool.description.image`
  
- It will be invoked with `acme-batch --debug "Hello, World!"`. 
  - The invocation is created from `status.resolvedApplication.invocation.invocation`
  - With parameters defined in `status.resolvedApplication.invocation.parameters`
  - And values defined in `specification.parameters`
  
- The Job should be scheduled with a max wall-time of 1 hour 
  - See `specification.timeAllocation`
  
- ...on exactly 1 node.
  - See `specification.replicas` */

// Authenticated as ucloud
await callAPI(JobsProviderUcloudApi.create(
    {
        "items": [
            {
                "id": "54112",
                "owner": {
                    "createdBy": "user",
                    "project": null
                },
                "updates": [
                ],
                "specification": {
                    "application": {
                        "name": "acme-batch",
                        "version": "1.0.0"
                    },
                    "product": {
                        "id": "example-compute-1",
                        "category": "example-compute",
                        "provider": "example"
                    },
                    "name": null,
                    "replicas": 1,
                    "allowDuplicateJob": false,
                    "parameters": {
                        "debug": {
                            "type": "boolean",
                            "value": true
                        },
                        "value": {
                            "type": "text",
                            "value": "Hello, World!"
                        }
                    },
                    "resources": null,
                    "timeAllocation": {
                        "hours": 1,
                        "minutes": 0,
                        "seconds": 0
                    }
                },
                "status": {
                    "state": "IN_QUEUE",
                    "jobParametersJson": null,
                    "startedAt": null,
                    "expiresAt": null,
                    "resolvedApplication": {
                        "metadata": {
                            "name": "acme-batch",
                            "version": "1.0.0",
                            "authors": [
                                "UCloud"
                            ],
                            "title": "Acme batch",
                            "description": "An example application",
                            "website": null,
                            "public": true
                        },
                        "invocation": {
                            "tool": {
                                "name": "acme-batch",
                                "version": "1.0.0",
                                "tool": {
                                    "owner": "_ucloud",
                                    "createdAt": 1633329776235,
                                    "modifiedAt": 1633329776235,
                                    "description": {
                                        "info": {
                                            "name": "acme-batch",
                                            "version": "1.0.0"
                                        },
                                        "container": null,
                                        "defaultNumberOfNodes": 1,
                                        "defaultTimeAllocation": {
                                            "hours": 1,
                                            "minutes": 0,
                                            "seconds": 0
                                        },
                                        "requiredModules": [
                                        ],
                                        "authors": [
                                            "UCloud"
                                        ],
                                        "title": "Acme batch",
                                        "description": "An example tool",
                                        "backend": "DOCKER",
                                        "license": "None",
                                        "image": "acme/batch:1.0.0",
                                        "supportedProviders": null
                                    }
                                }
                            },
                            "invocation": [
                                {
                                    "type": "word",
                                    "word": "acme-batch"
                                },
                                {
                                    "type": "var",
                                    "variableNames": [
                                        "debug"
                                    ],
                                    "prefixGlobal": "--debug ",
                                    "suffixGlobal": "",
                                    "prefixVariable": "",
                                    "suffixVariable": "",
                                    "isPrefixVariablePartOfArg": false,
                                    "isSuffixVariablePartOfArg": false
                                },
                                {
                                    "type": "var",
                                    "variableNames": [
                                        "value"
                                    ],
                                    "prefixGlobal": "",
                                    "suffixGlobal": "",
                                    "prefixVariable": "",
                                    "suffixVariable": "",
                                    "isPrefixVariablePartOfArg": false,
                                    "isSuffixVariablePartOfArg": false
                                }
                            ],
                            "parameters": [
                                {
                                    "type": "boolean",
                                    "name": "debug",
                                    "optional": false,
                                    "defaultValue": null,
                                    "title": "",
                                    "description": "Should debug be enabled?",
                                    "trueValue": "true",
                                    "falseValue": "false"
                                },
                                {
                                    "type": "text",
                                    "name": "value",
                                    "optional": false,
                                    "defaultValue": null,
                                    "title": "",
                                    "description": "The value for the batch application"
                                }
                            ],
                            "outputFileGlobs": [
                                "*"
                            ],
                            "applicationType": "BATCH",
                            "vnc": null,
                            "web": null,
                            "container": null,
                            "environment": null,
                            "allowAdditionalMounts": null,
                            "allowAdditionalPeers": null,
                            "allowMultiNode": false,
                            "fileExtensions": [
                            ],
                            "licenseServers": [
                            ]
                        }
                    },
                    "resolvedSupport": {
                        "product": {
                            "balance": null,
                            "name": "example-compute-1",
                            "pricePerUnit": 1000000,
                            "category": {
                                "name": "example-compute",
                                "provider": "example"
                            },
                            "description": "An example machine",
                            "priority": 0,
                            "cpu": 1,
                            "memoryInGigs": 2,
                            "gpu": 0,
                            "version": 1,
                            "freeToUse": false,
                            "unitOfPrice": "CREDITS_PER_MINUTE",
                            "chargeType": "ABSOLUTE",
                            "hiddenInGrantApplications": false,
                            "productType": "COMPUTE"
                        },
                        "support": {
                            "product": {
                                "id": "example-compute-1",
                                "category": "example-compute",
                                "provider": "example"
                            },
                            "docker": {
                                "enabled": true,
                                "web": null,
                                "vnc": null,
                                "logs": null,
                                "terminal": null,
                                "peers": null,
                                "timeExtension": null,
                                "utilization": null
                            },
                            "virtualMachine": {
                                "enabled": null,
                                "logs": null,
                                "vnc": null,
                                "terminal": null,
                                "timeExtension": null,
                                "suspension": null,
                                "utilization": null
                            }
                        }
                    },
                    "resolvedProduct": {
                        "balance": null,
                        "name": "example-compute-1",
                        "pricePerUnit": 1000000,
                        "category": {
                            "name": "example-compute",
                            "provider": "example"
                        },
                        "description": "An example machine",
                        "priority": 0,
                        "cpu": 1,
                        "memoryInGigs": 2,
                        "gpu": 0,
                        "version": 1,
                        "freeToUse": false,
                        "unitOfPrice": "CREDITS_PER_MINUTE",
                        "chargeType": "ABSOLUTE",
                        "hiddenInGrantApplications": false,
                        "productType": "COMPUTE"
                    }
                },
                "createdAt": 1633329776235,
                "output": null,
                "permissions": null,
                "acl": null,
                "billing": {
                }
            }
        ]
    }
);

/*
{
    "responses": [
        null
    ]
}
*/

/* 📝 Note: The response in this case indicates that the Provider chose not to generate an internal ID
for this Job. If an ID was provided, then on subsequent requests the `providerGeneratedId` of this
Job would be set accordingly. This feature can help providers keep track of their internal state
without having to actively maintain a mapping. */


/* The Provider will use this information to schedule the Job on their infrastructure. Through
background processing, the Provider will keep track of this Job. The Provider notifies UCloud of
state changes as they occur. This happens through the outgoing Control API. */

// Authenticated as provider
await callAPI(JobsControlApi.update(
    {
        "items": [
            {
                "id": "54112",
                "update": {
                    "state": "RUNNING",
                    "outputFolder": null,
                    "status": "The job is now running!",
                    "expectedState": null,
                    "expectedDifferentState": null,
                    "timestamp": 0
                }
            }
        ]
    }
);

/*
{
}
*/

/* 📝 Note: The timestamp field will be filled out by UCloud/Core */


/* ~ Some time later ~ */

await callAPI(JobsControlApi.update(
    {
        "items": [
            {
                "id": "54112",
                "update": {
                    "state": "SUCCESS",
                    "outputFolder": null,
                    "status": "The job has finished processing!",
                    "expectedState": null,
                    "expectedDifferentState": null,
                    "timestamp": 0
                }
            }
        ]
    }
);

/*
{
}
*/
```


</details>

<details>
<summary>
<b>Communication Flow:</b> Curl
</summary>

```bash
# ------------------------------------------------------------------------------------------------------
# $host is the UCloud instance to contact. Example: 'http://localhost:8080' or 'https://cloud.sdu.dk'
# $accessToken is a valid access-token issued by UCloud
# ------------------------------------------------------------------------------------------------------

# In this example we will show the creation of a simple batch Job. The procedure starts with the
# Provider receives a create request from UCloud/Core

# The request below contains a lot of information. We recommend that you read about and understand
# Products, Applications and Jobs before you continue. We will attempt to summarize the information
# below:
# 
# - The request contains one or more Jobs. The Provider should schedule each of them on their
#   infrastructure.
# - The `id` of a Job is unique, globally, in UCloud.
# - The `owner` references the UCloud identity and workspace of the creator
# - The `specification` contains the user's request
# - The `status` contains UCloud's view of the Job _AND_ resolved resources required for the Job
# 
# In this example:
# 
# - Exactly one Job will be created. 
#   - `items` contains only one Job
#   
# - This Job will run a `BATCH` application
#   - See `status.resolvedApplication.invocation.applicationType`
#   
# - It will run on the `example-compute-1` machine-type
#   - See `specification.product` and `status.resolvedProduct`
#   
# - The application should launch the `acme/batch:1.0.0` container
#   - `status.resolvedApplication.invocation.tool.tool.description.backend`
#   - `status.resolvedApplication.invocation.tool.tool.description.image`
#   
# - It will be invoked with `acme-batch --debug "Hello, World!"`. 
#   - The invocation is created from `status.resolvedApplication.invocation.invocation`
#   - With parameters defined in `status.resolvedApplication.invocation.parameters`
#   - And values defined in `specification.parameters`
#   
# - The Job should be scheduled with a max wall-time of 1 hour 
#   - See `specification.timeAllocation`
#   
# - ...on exactly 1 node.
#   - See `specification.replicas`

# Authenticated as ucloud
curl -XPOST -H "Authorization: Bearer $accessToken" -H "Content-Type: content-type: application/json; charset=utf-8" "$host/ucloud/ucloud/jobs" -d '{
    "items": [
        {
            "id": "54112",
            "owner": {
                "createdBy": "user",
                "project": null
            },
            "updates": [
            ],
            "specification": {
                "application": {
                    "name": "acme-batch",
                    "version": "1.0.0"
                },
                "product": {
                    "id": "example-compute-1",
                    "category": "example-compute",
                    "provider": "example"
                },
                "name": null,
                "replicas": 1,
                "allowDuplicateJob": false,
                "parameters": {
                    "debug": {
                        "type": "boolean",
                        "value": true
                    },
                    "value": {
                        "type": "text",
                        "value": "Hello, World!"
                    }
                },
                "resources": null,
                "timeAllocation": {
                    "hours": 1,
                    "minutes": 0,
                    "seconds": 0
                }
            },
            "status": {
                "state": "IN_QUEUE",
                "jobParametersJson": null,
                "startedAt": null,
                "expiresAt": null,
                "resolvedApplication": {
                    "metadata": {
                        "name": "acme-batch",
                        "version": "1.0.0",
                        "authors": [
                            "UCloud"
                        ],
                        "title": "Acme batch",
                        "description": "An example application",
                        "website": null,
                        "public": true
                    },
                    "invocation": {
                        "tool": {
                            "name": "acme-batch",
                            "version": "1.0.0",
                            "tool": {
                                "owner": "_ucloud",
                                "createdAt": 1633329776235,
                                "modifiedAt": 1633329776235,
                                "description": {
                                    "info": {
                                        "name": "acme-batch",
                                        "version": "1.0.0"
                                    },
                                    "container": null,
                                    "defaultNumberOfNodes": 1,
                                    "defaultTimeAllocation": {
                                        "hours": 1,
                                        "minutes": 0,
                                        "seconds": 0
                                    },
                                    "requiredModules": [
                                    ],
                                    "authors": [
                                        "UCloud"
                                    ],
                                    "title": "Acme batch",
                                    "description": "An example tool",
                                    "backend": "DOCKER",
                                    "license": "None",
                                    "image": "acme/batch:1.0.0",
                                    "supportedProviders": null
                                }
                            }
                        },
                        "invocation": [
                            {
                                "type": "word",
                                "word": "acme-batch"
                            },
                            {
                                "type": "var",
                                "variableNames": [
                                    "debug"
                                ],
                                "prefixGlobal": "--debug ",
                                "suffixGlobal": "",
                                "prefixVariable": "",
                                "suffixVariable": "",
                                "isPrefixVariablePartOfArg": false,
                                "isSuffixVariablePartOfArg": false
                            },
                            {
                                "type": "var",
                                "variableNames": [
                                    "value"
                                ],
                                "prefixGlobal": "",
                                "suffixGlobal": "",
                                "prefixVariable": "",
                                "suffixVariable": "",
                                "isPrefixVariablePartOfArg": false,
                                "isSuffixVariablePartOfArg": false
                            }
                        ],
                        "parameters": [
                            {
                                "type": "boolean",
                                "name": "debug",
                                "optional": false,
                                "defaultValue": null,
                                "title": "",
                                "description": "Should debug be enabled?",
                                "trueValue": "true",
                                "falseValue": "false"
                            },
                            {
                                "type": "text",
                                "name": "value",
                                "optional": false,
                                "defaultValue": null,
                                "title": "",
                                "description": "The value for the batch application"
                            }
                        ],
                        "outputFileGlobs": [
                            "*"
                        ],
                        "applicationType": "BATCH",
                        "vnc": null,
                        "web": null,
                        "container": null,
                        "environment": null,
                        "allowAdditionalMounts": null,
                        "allowAdditionalPeers": null,
                        "allowMultiNode": false,
                        "fileExtensions": [
                        ],
                        "licenseServers": [
                        ]
                    }
                },
                "resolvedSupport": {
                    "product": {
                        "balance": null,
                        "name": "example-compute-1",
                        "pricePerUnit": 1000000,
                        "category": {
                            "name": "example-compute",
                            "provider": "example"
                        },
                        "description": "An example machine",
                        "priority": 0,
                        "cpu": 1,
                        "memoryInGigs": 2,
                        "gpu": 0,
                        "version": 1,
                        "freeToUse": false,
                        "unitOfPrice": "CREDITS_PER_MINUTE",
                        "chargeType": "ABSOLUTE",
                        "hiddenInGrantApplications": false,
                        "productType": "COMPUTE"
                    },
                    "support": {
                        "product": {
                            "id": "example-compute-1",
                            "category": "example-compute",
                            "provider": "example"
                        },
                        "docker": {
                            "enabled": true,
                            "web": null,
                            "vnc": null,
                            "logs": null,
                            "terminal": null,
                            "peers": null,
                            "timeExtension": null,
                            "utilization": null
                        },
                        "virtualMachine": {
                            "enabled": null,
                            "logs": null,
                            "vnc": null,
                            "terminal": null,
                            "timeExtension": null,
                            "suspension": null,
                            "utilization": null
                        }
                    }
                },
                "resolvedProduct": {
                    "balance": null,
                    "name": "example-compute-1",
                    "pricePerUnit": 1000000,
                    "category": {
                        "name": "example-compute",
                        "provider": "example"
                    },
                    "description": "An example machine",
                    "priority": 0,
                    "cpu": 1,
                    "memoryInGigs": 2,
                    "gpu": 0,
                    "version": 1,
                    "freeToUse": false,
                    "unitOfPrice": "CREDITS_PER_MINUTE",
                    "chargeType": "ABSOLUTE",
                    "hiddenInGrantApplications": false,
                    "productType": "COMPUTE"
                }
            },
            "createdAt": 1633329776235,
            "output": null,
            "permissions": null,
            "acl": null,
            "billing": {
            }
        }
    ]
}'


# {
#     "responses": [
#         null
#     ]
# }

# 📝 Note: The response in this case indicates that the Provider chose not to generate an internal ID
# for this Job. If an ID was provided, then on subsequent requests the `providerGeneratedId` of this
# Job would be set accordingly. This feature can help providers keep track of their internal state
# without having to actively maintain a mapping.

# The Provider will use this information to schedule the Job on their infrastructure. Through
# background processing, the Provider will keep track of this Job. The Provider notifies UCloud of
# state changes as they occur. This happens through the outgoing Control API.

# Authenticated as provider
curl -XPOST -H "Authorization: Bearer $accessToken" -H "Content-Type: content-type: application/json; charset=utf-8" "$host/api/jobs/control/update" -d '{
    "items": [
        {
            "id": "54112",
            "update": {
                "state": "RUNNING",
                "outputFolder": null,
                "status": "The job is now running!",
                "expectedState": null,
                "expectedDifferentState": null,
                "timestamp": 0
            }
        }
    ]
}'


# {
# }

# 📝 Note: The timestamp field will be filled out by UCloud/Core

# ~ Some time later ~

curl -XPOST -H "Authorization: Bearer $accessToken" -H "Content-Type: content-type: application/json; charset=utf-8" "$host/api/jobs/control/update" -d '{
    "items": [
        {
            "id": "54112",
            "update": {
                "state": "SUCCESS",
                "outputFolder": null,
                "status": "The job has finished processing!",
                "expectedState": null,
                "expectedDifferentState": null,
                "timestamp": 0
            }
        }
    ]
}'


# {
# }

```


</details>


## Example: Accounting
<table>
<tr><th>Frequency of use</th><td>Common</td></tr>
<tr><th>Pre-conditions</th><td><ul>
<li>One or more active Jobs running at the Provider</li>
</ul></td></tr>
<tr>
<th>Actors</th>
<td><ul>
<li>The UCloud/Core service user (<code>ucloud</code>)</li>
<li>The provider (<code>provider</code>)</li>
</ul></td>
</tr>
</table>
<details>
<summary>
<b>Communication Flow:</b> Kotlin
</summary>

```kotlin

/* In this example, we show how a Provider can implement accounting. Accounting is done, periodically,
by the provider in a background process. We recommend that Providers combine this with the same
background processing required for state changes. */


/* You should read understand how Products work in UCloud. UCloud supports multiple ways of accounting
for usage. The most normal one, which we show here, is the `CREDITS_PER_MINUTE` policy. This policy
requires that a Provider charges credits (1 credit = 1/1_000_000 DKK) for every minute of usage. */


/* We assume that the Provider has just determined that Jobs "51231" (single replica) and "63489"
(23 replicas) each have used 15 minutes of compute time since last accounting iteration. */

JobsControl.chargeCredits.call(
    bulkRequestOf(ResourceChargeCredits(
        chargeId = "51231-charge-04-oct-2021-12:30", 
        description = null, 
        id = "51231", 
        numberOfProducts = 1, 
        performedBy = null, 
        units = 15, 
    ), ResourceChargeCredits(
        chargeId = "63489-charge-04-oct-2021-12:30", 
        description = null, 
        id = "63489", 
        numberOfProducts = 23, 
        performedBy = null, 
        units = 15, 
    )),
    provider
).orThrow()

/*
ResourceChargeCreditsResponse(
    duplicateCharges = emptyList(), 
    insufficientFunds = emptyList(), 
)
*/

/* 📝 Note: Because the ProductPriceUnit, of the Product associated with the Job, is
`CREDITS_PER_MINUTE` each unit corresponds to minutes of usage. A different ProductPriceUnit, for
example `CREDITS_PER_HOUR` would alter the definition of this unit. */


/* 📝 Note: The chargeId is an identifier which must be unique for any charge made by the Provider.
If the Provider makes a different charge request with this ID then the request will be ignored. We
recommend that Providers use this to their advantage and include, for example, a timestamp from
the last iteration. This means that you, as a Provider, cannot accidentally charge twice for the
same usage. */


/* In the next iteration, the Provider also determines that 15 minutes has passed for these Jobs. */

JobsControl.chargeCredits.call(
    bulkRequestOf(ResourceChargeCredits(
        chargeId = "51231-charge-04-oct-2021-12:45", 
        description = null, 
        id = "51231", 
        numberOfProducts = 1, 
        performedBy = null, 
        units = 15, 
    ), ResourceChargeCredits(
        chargeId = "63489-charge-04-oct-2021-12:45", 
        description = null, 
        id = "63489", 
        numberOfProducts = 23, 
        performedBy = null, 
        units = 15, 
    )),
    provider
).orThrow()

/*
ResourceChargeCreditsResponse(
    duplicateCharges = emptyList(), 
    insufficientFunds = listOf(FindByStringId(
        id = "63489", 
    )), 
)
*/

/* However, this time UCloud has told us that 63489 no longer has enough credits to pay for this.
The Provider should respond to this by immediately cancelling the Job, UCloud/Core does not perform
this step for you! */


/* 📝 Note: This request should be triggered by the normal life-cycle handler. */

JobsControl.update.call(
    bulkRequestOf(ResourceUpdateAndId(
        id = "63489", 
        update = JobUpdate(
            expectedDifferentState = null, 
            expectedState = null, 
            outputFolder = null, 
            state = JobState.SUCCESS, 
            status = "The job was terminated (No credits)", 
            timestamp = 0, 
        ), 
    )),
    provider
).orThrow()

/*
Unit
*/
```


</details>

<details>
<summary>
<b>Communication Flow:</b> TypeScript
</summary>

```typescript

/* In this example, we show how a Provider can implement accounting. Accounting is done, periodically,
by the provider in a background process. We recommend that Providers combine this with the same
background processing required for state changes. */


/* You should read understand how Products work in UCloud. UCloud supports multiple ways of accounting
for usage. The most normal one, which we show here, is the `CREDITS_PER_MINUTE` policy. This policy
requires that a Provider charges credits (1 credit = 1/1_000_000 DKK) for every minute of usage. */


/* We assume that the Provider has just determined that Jobs "51231" (single replica) and "63489"
(23 replicas) each have used 15 minutes of compute time since last accounting iteration. */

// Authenticated as provider
await callAPI(JobsControlApi.chargeCredits(
    {
        "items": [
            {
                "id": "51231",
                "chargeId": "51231-charge-04-oct-2021-12:30",
                "units": 15,
                "numberOfProducts": 1,
                "performedBy": null,
                "description": null
            },
            {
                "id": "63489",
                "chargeId": "63489-charge-04-oct-2021-12:30",
                "units": 15,
                "numberOfProducts": 23,
                "performedBy": null,
                "description": null
            }
        ]
    }
);

/*
{
    "insufficientFunds": [
    ],
    "duplicateCharges": [
    ]
}
*/

/* 📝 Note: Because the ProductPriceUnit, of the Product associated with the Job, is
`CREDITS_PER_MINUTE` each unit corresponds to minutes of usage. A different ProductPriceUnit, for
example `CREDITS_PER_HOUR` would alter the definition of this unit. */


/* 📝 Note: The chargeId is an identifier which must be unique for any charge made by the Provider.
If the Provider makes a different charge request with this ID then the request will be ignored. We
recommend that Providers use this to their advantage and include, for example, a timestamp from
the last iteration. This means that you, as a Provider, cannot accidentally charge twice for the
same usage. */


/* In the next iteration, the Provider also determines that 15 minutes has passed for these Jobs. */

await callAPI(JobsControlApi.chargeCredits(
    {
        "items": [
            {
                "id": "51231",
                "chargeId": "51231-charge-04-oct-2021-12:45",
                "units": 15,
                "numberOfProducts": 1,
                "performedBy": null,
                "description": null
            },
            {
                "id": "63489",
                "chargeId": "63489-charge-04-oct-2021-12:45",
                "units": 15,
                "numberOfProducts": 23,
                "performedBy": null,
                "description": null
            }
        ]
    }
);

/*
{
    "insufficientFunds": [
        {
            "id": "63489"
        }
    ],
    "duplicateCharges": [
    ]
}
*/

/* However, this time UCloud has told us that 63489 no longer has enough credits to pay for this.
The Provider should respond to this by immediately cancelling the Job, UCloud/Core does not perform
this step for you! */


/* 📝 Note: This request should be triggered by the normal life-cycle handler. */

await callAPI(JobsControlApi.update(
    {
        "items": [
            {
                "id": "63489",
                "update": {
                    "state": "SUCCESS",
                    "outputFolder": null,
                    "status": "The job was terminated (No credits)",
                    "expectedState": null,
                    "expectedDifferentState": null,
                    "timestamp": 0
                }
            }
        ]
    }
);

/*
{
}
*/
```


</details>

<details>
<summary>
<b>Communication Flow:</b> Curl
</summary>

```bash
# ------------------------------------------------------------------------------------------------------
# $host is the UCloud instance to contact. Example: 'http://localhost:8080' or 'https://cloud.sdu.dk'
# $accessToken is a valid access-token issued by UCloud
# ------------------------------------------------------------------------------------------------------

# In this example, we show how a Provider can implement accounting. Accounting is done, periodically,
# by the provider in a background process. We recommend that Providers combine this with the same
# background processing required for state changes.

# You should read understand how Products work in UCloud. UCloud supports multiple ways of accounting
# for usage. The most normal one, which we show here, is the `CREDITS_PER_MINUTE` policy. This policy
# requires that a Provider charges credits (1 credit = 1/1_000_000 DKK) for every minute of usage.

# We assume that the Provider has just determined that Jobs "51231" (single replica) and "63489"
# (23 replicas) each have used 15 minutes of compute time since last accounting iteration.

# Authenticated as provider
curl -XPOST -H "Authorization: Bearer $accessToken" -H "Content-Type: content-type: application/json; charset=utf-8" "$host/api/jobs/control/chargeCredits" -d '{
    "items": [
        {
            "id": "51231",
            "chargeId": "51231-charge-04-oct-2021-12:30",
            "units": 15,
            "numberOfProducts": 1,
            "performedBy": null,
            "description": null
        },
        {
            "id": "63489",
            "chargeId": "63489-charge-04-oct-2021-12:30",
            "units": 15,
            "numberOfProducts": 23,
            "performedBy": null,
            "description": null
        }
    ]
}'


# {
#     "insufficientFunds": [
#     ],
#     "duplicateCharges": [
#     ]
# }

# 📝 Note: Because the ProductPriceUnit, of the Product associated with the Job, is
# `CREDITS_PER_MINUTE` each unit corresponds to minutes of usage. A different ProductPriceUnit, for
# example `CREDITS_PER_HOUR` would alter the definition of this unit.

# 📝 Note: The chargeId is an identifier which must be unique for any charge made by the Provider.
# If the Provider makes a different charge request with this ID then the request will be ignored. We
# recommend that Providers use this to their advantage and include, for example, a timestamp from
# the last iteration. This means that you, as a Provider, cannot accidentally charge twice for the
# same usage.

# In the next iteration, the Provider also determines that 15 minutes has passed for these Jobs.

curl -XPOST -H "Authorization: Bearer $accessToken" -H "Content-Type: content-type: application/json; charset=utf-8" "$host/api/jobs/control/chargeCredits" -d '{
    "items": [
        {
            "id": "51231",
            "chargeId": "51231-charge-04-oct-2021-12:45",
            "units": 15,
            "numberOfProducts": 1,
            "performedBy": null,
            "description": null
        },
        {
            "id": "63489",
            "chargeId": "63489-charge-04-oct-2021-12:45",
            "units": 15,
            "numberOfProducts": 23,
            "performedBy": null,
            "description": null
        }
    ]
}'


# {
#     "insufficientFunds": [
#         {
#             "id": "63489"
#         }
#     ],
#     "duplicateCharges": [
#     ]
# }

# However, this time UCloud has told us that 63489 no longer has enough credits to pay for this.
# The Provider should respond to this by immediately cancelling the Job, UCloud/Core does not perform
# this step for you!

# 📝 Note: This request should be triggered by the normal life-cycle handler.

curl -XPOST -H "Authorization: Bearer $accessToken" -H "Content-Type: content-type: application/json; charset=utf-8" "$host/api/jobs/control/update" -d '{
    "items": [
        {
            "id": "63489",
            "update": {
                "state": "SUCCESS",
                "outputFolder": null,
                "status": "The job was terminated (No credits)",
                "expectedState": null,
                "expectedDifferentState": null,
                "timestamp": 0
            }
        }
    ]
}'


# {
# }

```


</details>


## Example: Ensuring UCloud/Core and Provider are in-sync
<table>
<tr><th>Frequency of use</th><td>Common</td></tr>
<tr><th>Pre-conditions</th><td><ul>
<li>One or more active Jobs for this Provider</li>
</ul></td></tr>
<tr>
<th>Actors</th>
<td><ul>
<li>The UCloud/Core service user (<code>ucloud</code>)</li>
<li>The provider (<code>provider</code>)</li>
</ul></td>
</tr>
</table>
<details>
<summary>
<b>Communication Flow:</b> Kotlin
</summary>

```kotlin

/* In this example, we will explore the mechanism that UCloud/Core uses to ensure that the Provider
is synchronized with the core. */


/* UCloud/Core will periodically send the Provider a batch of active Jobs. If the Provider is unable
to recognize one or more of these Jobs, it should respond by updating the state of the affected
Job(s). */

KubernetesCompute.verify.call(
    bulkRequestOf(Job(
        acl = null, 
        billing = ResourceBilling.Free, 
        createdAt = 1633329776235, 
        id = "54112", 
        output = null, 
        owner = ResourceOwner(
            createdBy = "user", 
            project = null, 
        ), 
        permissions = null, 
        specification = JobSpecification(
            allowDuplicateJob = false, 
            application = NameAndVersion(
                name = "acme-batch", 
                version = "1.0.0", 
            ), 
            name = null, 
            parameters = mapOf("debug" to AppParameterValue.Bool(
                value = true, 
            ), "value" to AppParameterValue.Text(
                value = "Hello, World!", 
            )), 
            product = ProductReference(
                category = "example-compute", 
                id = "example-compute-1", 
                provider = "example", 
            ), 
            replicas = 1, 
            resources = null, 
            timeAllocation = SimpleDuration(
                hours = 1, 
                minutes = 0, 
                seconds = 0, 
            ), 
        ), 
        status = JobStatus(
            expiresAt = null, 
            jobParametersJson = null, 
            resolvedApplication = null, 
            resolvedProduct = null, 
            resolvedSupport = null, 
            startedAt = null, 
            state = JobState.RUNNING, 
        ), 
        updates = emptyList(), 
        providerGeneratedId = "54112", 
    )),
    ucloud
).orThrow()

/*
Unit
*/

/* In this case, the Provider does not recognize 54112 */

JobsControl.update.call(
    bulkRequestOf(ResourceUpdateAndId(
        id = "54112", 
        update = JobUpdate(
            expectedDifferentState = null, 
            expectedState = null, 
            outputFolder = null, 
            state = JobState.FAILURE, 
            status = "Your job is no longer available", 
            timestamp = 0, 
        ), 
    )),
    provider
).orThrow()

/*
Unit
*/
```


</details>

<details>
<summary>
<b>Communication Flow:</b> TypeScript
</summary>

```typescript

/* In this example, we will explore the mechanism that UCloud/Core uses to ensure that the Provider
is synchronized with the core. */


/* UCloud/Core will periodically send the Provider a batch of active Jobs. If the Provider is unable
to recognize one or more of these Jobs, it should respond by updating the state of the affected
Job(s). */

// Authenticated as ucloud
await callAPI(JobsProviderUcloudApi.verify(
    {
        "items": [
            {
                "id": "54112",
                "owner": {
                    "createdBy": "user",
                    "project": null
                },
                "updates": [
                ],
                "specification": {
                    "application": {
                        "name": "acme-batch",
                        "version": "1.0.0"
                    },
                    "product": {
                        "id": "example-compute-1",
                        "category": "example-compute",
                        "provider": "example"
                    },
                    "name": null,
                    "replicas": 1,
                    "allowDuplicateJob": false,
                    "parameters": {
                        "debug": {
                            "type": "boolean",
                            "value": true
                        },
                        "value": {
                            "type": "text",
                            "value": "Hello, World!"
                        }
                    },
                    "resources": null,
                    "timeAllocation": {
                        "hours": 1,
                        "minutes": 0,
                        "seconds": 0
                    }
                },
                "status": {
                    "state": "RUNNING",
                    "jobParametersJson": null,
                    "startedAt": null,
                    "expiresAt": null,
                    "resolvedApplication": null,
                    "resolvedSupport": null,
                    "resolvedProduct": null
                },
                "createdAt": 1633329776235,
                "output": null,
                "permissions": null,
                "acl": null,
                "billing": {
                }
            }
        ]
    }
);

/*
{
}
*/

/* In this case, the Provider does not recognize 54112 */

// Authenticated as provider
await callAPI(JobsControlApi.update(
    {
        "items": [
            {
                "id": "54112",
                "update": {
                    "state": "FAILURE",
                    "outputFolder": null,
                    "status": "Your job is no longer available",
                    "expectedState": null,
                    "expectedDifferentState": null,
                    "timestamp": 0
                }
            }
        ]
    }
);

/*
{
}
*/
```


</details>

<details>
<summary>
<b>Communication Flow:</b> Curl
</summary>

```bash
# ------------------------------------------------------------------------------------------------------
# $host is the UCloud instance to contact. Example: 'http://localhost:8080' or 'https://cloud.sdu.dk'
# $accessToken is a valid access-token issued by UCloud
# ------------------------------------------------------------------------------------------------------

# In this example, we will explore the mechanism that UCloud/Core uses to ensure that the Provider
# is synchronized with the core.

# UCloud/Core will periodically send the Provider a batch of active Jobs. If the Provider is unable
# to recognize one or more of these Jobs, it should respond by updating the state of the affected
# Job(s).

# Authenticated as ucloud
curl -XPOST -H "Authorization: Bearer $accessToken" -H "Content-Type: content-type: application/json; charset=utf-8" "$host/ucloud/ucloud/jobs/verify" -d '{
    "items": [
        {
            "id": "54112",
            "owner": {
                "createdBy": "user",
                "project": null
            },
            "updates": [
            ],
            "specification": {
                "application": {
                    "name": "acme-batch",
                    "version": "1.0.0"
                },
                "product": {
                    "id": "example-compute-1",
                    "category": "example-compute",
                    "provider": "example"
                },
                "name": null,
                "replicas": 1,
                "allowDuplicateJob": false,
                "parameters": {
                    "debug": {
                        "type": "boolean",
                        "value": true
                    },
                    "value": {
                        "type": "text",
                        "value": "Hello, World!"
                    }
                },
                "resources": null,
                "timeAllocation": {
                    "hours": 1,
                    "minutes": 0,
                    "seconds": 0
                }
            },
            "status": {
                "state": "RUNNING",
                "jobParametersJson": null,
                "startedAt": null,
                "expiresAt": null,
                "resolvedApplication": null,
                "resolvedSupport": null,
                "resolvedProduct": null
            },
            "createdAt": 1633329776235,
            "output": null,
            "permissions": null,
            "acl": null,
            "billing": {
            }
        }
    ]
}'


# {
# }

# In this case, the Provider does not recognize 54112

# Authenticated as provider
curl -XPOST -H "Authorization: Bearer $accessToken" -H "Content-Type: content-type: application/json; charset=utf-8" "$host/api/jobs/control/update" -d '{
    "items": [
        {
            "id": "54112",
            "update": {
                "state": "FAILURE",
                "outputFolder": null,
                "status": "Your job is no longer available",
                "expectedState": null,
                "expectedDifferentState": null,
                "timestamp": 0
            }
        }
    ]
}'


# {
# }

```


</details>



## Remote Procedure Calls

### `follow`

[![API: Experimental/Alpha](https://img.shields.io/static/v1?label=API&message=Experimental/Alpha&color=orange&style=flat-square)](/docs/developer-guide/core/api-conventions.md)
[![Auth: Services](https://img.shields.io/static/v1?label=Auth&message=Services&color=informational&style=flat-square)](/docs/developer-guide/core/types.md#role)


_Follow the progress of a job_

| Request | Response | Error |
|---------|----------|-------|
|<code><a href='/docs/reference/dk.sdu.cloud.app.orchestrator.api.JobsProviderFollowRequest.md'>JobsProviderFollowRequest</a></code>|<code><a href='/docs/reference/dk.sdu.cloud.app.orchestrator.api.JobsProviderFollowResponse.md'>JobsProviderFollowResponse</a></code>|<code><a href='/docs/reference/dk.sdu.cloud.CommonErrorMessage.md'>CommonErrorMessage</a></code>|

__Implementation requirements:__ 
 - [`docker.logs = true`](/docs/reference/dk.sdu.cloud.app.kubernetes.api.ComputeSupport.Docker.md) or
 - [`virtualMachine.logs = true`](/docs/reference/dk.sdu.cloud.app.kubernetes.api.ComputeSupport.VirtualMachine.md)

For more information, see the end-user API ([`jobs.follow`](/docs/reference/jobs.follow.md))



### `retrieveProducts`

[![API: Experimental/Alpha](https://img.shields.io/static/v1?label=API&message=Experimental/Alpha&color=orange&style=flat-square)](/docs/developer-guide/core/api-conventions.md)
[![Auth: Services](https://img.shields.io/static/v1?label=Auth&message=Services&color=informational&style=flat-square)](/docs/developer-guide/core/types.md#role)


_Retrieve product support for this providers_

| Request | Response | Error |
|---------|----------|-------|
|<code><a href='https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/'>Unit</a></code>|<code><a href='/docs/reference/dk.sdu.cloud.calls.BulkResponse.md'>BulkResponse</a>&lt;<a href='/docs/reference/dk.sdu.cloud.app.orchestrator.api.ComputeSupport.md'>ComputeSupport</a>&gt;</code>|<code><a href='/docs/reference/dk.sdu.cloud.CommonErrorMessage.md'>CommonErrorMessage</a></code>|

This endpoint responds with the [`Product`](/docs/reference/dk.sdu.cloud.accounting.api.Product.md)  s supported by
this provider along with details for how [`Product`](/docs/reference/dk.sdu.cloud.accounting.api.Product.md)  is
supported. The [`Product`](/docs/reference/dk.sdu.cloud.accounting.api.Product.md)  s must be registered with
UCloud/Core already.


### `retrieveUtilization`

[![API: Experimental/Alpha](https://img.shields.io/static/v1?label=API&message=Experimental/Alpha&color=orange&style=flat-square)](/docs/developer-guide/core/api-conventions.md)
[![Auth: Services](https://img.shields.io/static/v1?label=Auth&message=Services&color=informational&style=flat-square)](/docs/developer-guide/core/types.md#role)


_Retrieve information about how busy the provider's cluster currently is_

| Request | Response | Error |
|---------|----------|-------|
|<code><a href='https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/'>Unit</a></code>|<code><a href='/docs/reference/dk.sdu.cloud.app.orchestrator.api.JobsRetrieveUtilizationResponse.md'>JobsRetrieveUtilizationResponse</a></code>|<code><a href='/docs/reference/dk.sdu.cloud.CommonErrorMessage.md'>CommonErrorMessage</a></code>|

__Implementation requirements:__ 
 - [`docker.utilization = true`](/docs/reference/dk.sdu.cloud.app.kubernetes.api.ComputeSupport.Docker.md) or
 - [`virtualMachine.utilization = true`](/docs/reference/dk.sdu.cloud.app.kubernetes.api.ComputeSupport.VirtualMachine.md)

For more information, see the end-user API ([`jobs.retrieveUtilization`](/docs/reference/jobs.retrieveUtilization.md))



### `create`

[![API: Experimental/Alpha](https://img.shields.io/static/v1?label=API&message=Experimental/Alpha&color=orange&style=flat-square)](/docs/developer-guide/core/api-conventions.md)
[![Auth: Services](https://img.shields.io/static/v1?label=Auth&message=Services&color=informational&style=flat-square)](/docs/developer-guide/core/types.md#role)


_Creates one or more resources_

| Request | Response | Error |
|---------|----------|-------|
|<code><a href='/docs/reference/dk.sdu.cloud.calls.BulkRequest.md'>BulkRequest</a>&lt;<a href='/docs/reference/dk.sdu.cloud.app.orchestrator.api.Job.md'>Job</a>&gt;</code>|<code><a href='/docs/reference/dk.sdu.cloud.calls.BulkResponse.md'>BulkResponse</a>&lt;<a href='/docs/reference/dk.sdu.cloud.FindByStringId.md'>FindByStringId</a>&gt;</code>|<code><a href='/docs/reference/dk.sdu.cloud.CommonErrorMessage.md'>CommonErrorMessage</a></code>|

__Implementation requirements:__ 
 - [`docker.enabled = true`](/docs/reference/dk.sdu.cloud.app.kubernetes.api.ComputeSupport.Docker.md) or
 - [`virtualMachine.enabled = true`](/docs/reference/dk.sdu.cloud.app.kubernetes.api.ComputeSupport.VirtualMachine.md)

For more information, see the end-user API ([`jobs.create`](/docs/reference/jobs.create.md))


### `extend`

[![API: Experimental/Alpha](https://img.shields.io/static/v1?label=API&message=Experimental/Alpha&color=orange&style=flat-square)](/docs/developer-guide/core/api-conventions.md)
[![Auth: Services](https://img.shields.io/static/v1?label=Auth&message=Services&color=informational&style=flat-square)](/docs/developer-guide/core/types.md#role)


_Extend the duration of one or more jobs_

| Request | Response | Error |
|---------|----------|-------|
|<code><a href='/docs/reference/dk.sdu.cloud.calls.BulkRequest.md'>BulkRequest</a>&lt;<a href='/docs/reference/dk.sdu.cloud.app.orchestrator.api.JobsProviderExtendRequestItem.md'>JobsProviderExtendRequestItem</a>&gt;</code>|<code><a href='/docs/reference/dk.sdu.cloud.calls.BulkResponse.md'>BulkResponse</a>&lt;<a href='https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/'>Unit</a>&gt;</code>|<code><a href='/docs/reference/dk.sdu.cloud.CommonErrorMessage.md'>CommonErrorMessage</a></code>|

__Implementation requirements:__ 
 - [`docker.timeExtension = true`](/docs/reference/dk.sdu.cloud.app.kubernetes.api.ComputeSupport.Docker.md) or 
 - [`virtualMachine.timeExtension = true`](/docs/reference/dk.sdu.cloud.app.kubernetes.api.ComputeSupport.VirtualMachine.md)

For more information, see the end-user API ([`jobs.extend`](/docs/reference/jobs.extend.md))



### `openInteractiveSession`

[![API: Experimental/Alpha](https://img.shields.io/static/v1?label=API&message=Experimental/Alpha&color=orange&style=flat-square)](/docs/developer-guide/core/api-conventions.md)
[![Auth: Services](https://img.shields.io/static/v1?label=Auth&message=Services&color=informational&style=flat-square)](/docs/developer-guide/core/types.md#role)


_Opens an interactive session (e.g. terminal, web or VNC)_

| Request | Response | Error |
|---------|----------|-------|
|<code><a href='/docs/reference/dk.sdu.cloud.calls.BulkRequest.md'>BulkRequest</a>&lt;<a href='/docs/reference/dk.sdu.cloud.app.orchestrator.api.JobsProviderOpenInteractiveSessionRequestItem.md'>JobsProviderOpenInteractiveSessionRequestItem</a>&gt;</code>|<code><a href='/docs/reference/dk.sdu.cloud.calls.BulkResponse.md'>BulkResponse</a>&lt;<a href='/docs/reference/dk.sdu.cloud.app.orchestrator.api.OpenSession.md'>OpenSession</a>&gt;</code>|<code><a href='/docs/reference/dk.sdu.cloud.CommonErrorMessage.md'>CommonErrorMessage</a></code>|

__Implementation requirements:__ 
 - [`docker.vnc = true`](/docs/reference/dk.sdu.cloud.app.kubernetes.api.ComputeSupport.Docker.md) or
 - [`docker.terminal = true`](/docs/reference/dk.sdu.cloud.app.kubernetes.api.ComputeSupport.Docker.md) or
 - [`docker.web = true`](/docs/reference/dk.sdu.cloud.app.kubernetes.api.ComputeSupport.Docker.md) or
 - [`virtualMachine.vnc = true`](/docs/reference/dk.sdu.cloud.app.kubernetes.api.ComputeSupport.VirtualMachine.md) or
 - [`virtualMachine.terminal = true`](/docs/reference/dk.sdu.cloud.app.kubernetes.api.ComputeSupport.VirtualMachine.md)

For more information, see the end-user API ([`jobs.openInteractiveSession`](/docs/reference/jobs.openInteractiveSession.md))



### `suspend`

[![API: Experimental/Alpha](https://img.shields.io/static/v1?label=API&message=Experimental/Alpha&color=orange&style=flat-square)](/docs/developer-guide/core/api-conventions.md)
[![Auth: Services](https://img.shields.io/static/v1?label=Auth&message=Services&color=informational&style=flat-square)](/docs/developer-guide/core/types.md#role)


_Suspend a job_

| Request | Response | Error |
|---------|----------|-------|
|<code><a href='/docs/reference/dk.sdu.cloud.calls.BulkRequest.md'>BulkRequest</a>&lt;<a href='/docs/reference/dk.sdu.cloud.app.orchestrator.api.Job.md'>Job</a>&gt;</code>|<code><a href='https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/'>Unit</a></code>|<code><a href='/docs/reference/dk.sdu.cloud.CommonErrorMessage.md'>CommonErrorMessage</a></code>|

__Implementation requirements:__ 
 - [`virtualMachine.suspension = true`](/docs/reference/dk.sdu.cloud.app.kubernetes.api.ComputeSupport.VirtualMachine.md)

For more information, see the end-user API ([`jobs.suspend`](/docs/reference/jobs.suspend.md))



### `terminate`

[![API: Experimental/Alpha](https://img.shields.io/static/v1?label=API&message=Experimental/Alpha&color=orange&style=flat-square)](/docs/developer-guide/core/api-conventions.md)
[![Auth: Services](https://img.shields.io/static/v1?label=Auth&message=Services&color=informational&style=flat-square)](/docs/developer-guide/core/types.md#role)


_Request job cancellation and destruction_

| Request | Response | Error |
|---------|----------|-------|
|<code><a href='/docs/reference/dk.sdu.cloud.calls.BulkRequest.md'>BulkRequest</a>&lt;<a href='/docs/reference/dk.sdu.cloud.app.orchestrator.api.Job.md'>Job</a>&gt;</code>|<code><a href='/docs/reference/dk.sdu.cloud.calls.BulkResponse.md'>BulkResponse</a>&lt;<a href='https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/'>Unit</a>&gt;</code>|<code><a href='/docs/reference/dk.sdu.cloud.CommonErrorMessage.md'>CommonErrorMessage</a></code>|

__Implementation requirements:__ Mandatory

For more information, see the end-user API ([`jobs.terminate`](/docs/reference/jobs.terminate.md))



### `updateAcl`

[![API: Experimental/Alpha](https://img.shields.io/static/v1?label=API&message=Experimental/Alpha&color=orange&style=flat-square)](/docs/developer-guide/core/api-conventions.md)
[![Auth: Services](https://img.shields.io/static/v1?label=Auth&message=Services&color=informational&style=flat-square)](/docs/developer-guide/core/types.md#role)


_Callback received by the Provider when permissions are updated_

| Request | Response | Error |
|---------|----------|-------|
|<code><a href='/docs/reference/dk.sdu.cloud.calls.BulkRequest.md'>BulkRequest</a>&lt;<a href='/docs/reference/dk.sdu.cloud.provider.api.UpdatedAclWithResource.md'>UpdatedAclWithResource</a>&lt;<a href='/docs/reference/dk.sdu.cloud.app.orchestrator.api.Job.md'>Job</a>&gt;&gt;</code>|<code><a href='/docs/reference/dk.sdu.cloud.calls.BulkResponse.md'>BulkResponse</a>&lt;<a href='https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/'>Unit</a>&gt;</code>|<code><a href='/docs/reference/dk.sdu.cloud.CommonErrorMessage.md'>CommonErrorMessage</a></code>|

This endpoint is mandatory for Providers to implement. If the Provider does not need to keep
internal state, then they may simply ignore this request by responding with `200 OK`. The
Provider _MUST_ reply with an OK status. UCloud/Core will fail the request if the Provider does
not acknowledge the request.


### `verify`

[![API: Experimental/Alpha](https://img.shields.io/static/v1?label=API&message=Experimental/Alpha&color=orange&style=flat-square)](/docs/developer-guide/core/api-conventions.md)
[![Auth: Services](https://img.shields.io/static/v1?label=Auth&message=Services&color=informational&style=flat-square)](/docs/developer-guide/core/types.md#role)


_Invoked by UCloud/Core to trigger verification of a single batch_

| Request | Response | Error |
|---------|----------|-------|
|<code><a href='/docs/reference/dk.sdu.cloud.calls.BulkRequest.md'>BulkRequest</a>&lt;<a href='/docs/reference/dk.sdu.cloud.app.orchestrator.api.Job.md'>Job</a>&gt;</code>|<code><a href='https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/'>Unit</a></code>|<code><a href='/docs/reference/dk.sdu.cloud.CommonErrorMessage.md'>CommonErrorMessage</a></code>|

This endpoint is periodically invoked by UCloud/Core for resources which are deemed active. The
Provider should immediately determine if these are still valid and recognized by the Provider.
If any of the resources are not valid, then the Provider should notify UCloud/Core by issuing
an update for each affected resource.


