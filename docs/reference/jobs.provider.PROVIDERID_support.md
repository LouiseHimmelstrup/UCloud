                        [UCloud Developer Guide](/docs/developer-guide/README.md) / [Orchestration of Resources](/docs/developer-guide/orchestration/README.md) / [Compute](/docs/developer-guide/orchestration/compute/README.md) / [Provider APIs](/docs/developer-guide/orchestration/compute/providers/README.md) / [Jobs](/docs/developer-guide/orchestration/compute/providers/jobs/README.md) / [Ingoing API](/docs/developer-guide/orchestration/compute/providers/jobs/ingoing.md)
                        
                        # Example: Declaring support full support for containerized applications

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

JobsProvider.retrieveProducts.call(
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
await callAPI(JobsProviderPROVIDERIDApi.retrieveProducts(
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
curl -XGET -H "Authorization: Bearer $accessToken" "$host/ucloud/PROVIDERID/jobs/retrieveProducts" 

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

