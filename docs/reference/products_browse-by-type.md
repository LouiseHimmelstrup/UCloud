[UCloud Developer Guide](/docs/developer-guide/README.md) / [Accounting and Project Management](/docs/developer-guide/accounting-and-projects/README.md) / [Products](/docs/developer-guide/accounting-and-projects/products.md)

# Example: Browse products by type

<table>
<tr><th>Frequency of use</th><td>Common</td></tr>
<tr>
<th>Actors</th>
<td><ul>
<li>An authenticated user (<code>user</code>)</li>
</ul></td>
</tr>
</table>
<details>
<summary>
<b>Communication Flow:</b> Kotlin
</summary>

```kotlin
Products.browse.call(
    ProductsBrowseRequest(
        consistency = null, 
        filterArea = ProductType.COMPUTE, 
        filterCategory = null, 
        filterName = null, 
        filterProvider = null, 
        filterVersion = null, 
        includeBalance = null, 
        itemsPerPage = 50, 
        itemsToSkip = null, 
        next = null, 
        showAllVersions = null, 
    ),
    user
).orThrow()

/*
PageV2(
    items = listOf(Product.Compute(
        category = ProductCategoryId(
            id = "example-compute", 
            name = "example-compute", 
            provider = "example", 
        ), 
        chargeType = ChargeType.ABSOLUTE, 
        cpu = 10, 
        description = "An example compute product", 
        freeToUse = false, 
        gpu = 0, 
        hiddenInGrantApplications = false, 
        memoryInGigs = 20, 
        name = "example-compute", 
        pricePerUnit = 1000000, 
        priority = 0, 
        productType = ProductType.COMPUTE, 
        unitOfPrice = ProductPriceUnit.CREDITS_PER_MINUTE, 
        version = 1, 
        balance = null, 
        id = "example-compute", 
    )), 
    itemsPerPage = 50, 
    next = null, 
)
*/
```


</details>

<details>
<summary>
<b>Communication Flow:</b> TypeScript
</summary>

```typescript
// Authenticated as user
await callAPI(ProductsApi.browse(
    {
        "itemsPerPage": 50,
        "next": null,
        "consistency": null,
        "itemsToSkip": null,
        "filterName": null,
        "filterProvider": null,
        "filterArea": "COMPUTE",
        "filterCategory": null,
        "filterVersion": null,
        "showAllVersions": null,
        "includeBalance": null
    }
);

/*
{
    "itemsPerPage": 50,
    "items": [
        {
            "type": "compute",
            "balance": null,
            "name": "example-compute",
            "pricePerUnit": 1000000,
            "category": {
                "name": "example-compute",
                "provider": "example"
            },
            "description": "An example compute product",
            "priority": 0,
            "cpu": 10,
            "memoryInGigs": 20,
            "gpu": 0,
            "version": 1,
            "freeToUse": false,
            "unitOfPrice": "CREDITS_PER_MINUTE",
            "chargeType": "ABSOLUTE",
            "hiddenInGrantApplications": false,
            "productType": "COMPUTE"
        }
    ],
    "next": null
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

# Authenticated as user
curl -XGET -H "Authorization: Bearer $accessToken" "$host/api/products/browse?itemsPerPage=50&filterArea=COMPUTE" 

# {
#     "itemsPerPage": 50,
#     "items": [
#         {
#             "type": "compute",
#             "balance": null,
#             "name": "example-compute",
#             "pricePerUnit": 1000000,
#             "category": {
#                 "name": "example-compute",
#                 "provider": "example"
#             },
#             "description": "An example compute product",
#             "priority": 0,
#             "cpu": 10,
#             "memoryInGigs": 20,
#             "gpu": 0,
#             "version": 1,
#             "freeToUse": false,
#             "unitOfPrice": "CREDITS_PER_MINUTE",
#             "chargeType": "ABSOLUTE",
#             "hiddenInGrantApplications": false,
#             "productType": "COMPUTE"
#         }
#     ],
#     "next": null
# }

```


</details>

<details>
<summary>
<b>Communication Flow:</b> Visual
</summary>

![](/docs/diagrams/products_browse-by-type.png)

</details>


