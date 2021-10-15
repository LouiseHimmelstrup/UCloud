[UCloud Developer Guide](/docs/developer-guide/README.md) / [Built-in Provider](/docs/developer-guide/built-in-provider/README.md) / [UCloud/Compute](/docs/developer-guide/built-in-provider/compute/README.md) / [Jobs](/docs/developer-guide/built-in-provider/compute/jobs.md)

# Example: Declaring minimal support for virtual machines

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

<details>
<summary>
<b>Communication Flow:</b> Visual
</summary>

![](/docs/diagrams/jobs.provider.ucloud_minimalSupport.png)

</details>


