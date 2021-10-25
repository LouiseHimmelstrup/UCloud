[UCloud Developer Guide](/docs/developer-guide/README.md) / [Orchestration of Resources](/docs/developer-guide/orchestration/README.md) / [Storage](/docs/developer-guide/orchestration/storage/README.md) / [Drives (FileCollection)](/docs/developer-guide/orchestration/storage/filecollections.md)

# Example: Renaming a collection

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

/* In this example, we will see how a user can rename one of their collections. */


/* 📝 NOTE: Renaming must be supported by the provider */

FileCollections.retrieveProducts.call(
    Unit,
    user
).orThrow()

/*
SupportByProvider(
    productsByProvider = mapOf("ucloud" to listOf(ResolvedSupport(
        product = Product.Storage(
            category = ProductCategoryId(
                id = "example-ssd", 
                name = "example-ssd", 
                provider = "example", 
            ), 
            chargeType = ChargeType.DIFFERENTIAL_QUOTA, 
            description = "Fast storage", 
            freeToUse = false, 
            hiddenInGrantApplications = false, 
            name = "example-ssd", 
            pricePerUnit = 1, 
            priority = 0, 
            productType = ProductType.STORAGE, 
            unitOfPrice = ProductPriceUnit.PER_UNIT, 
            version = 1, 
            balance = null, 
            id = "example-ssd", 
        ), 
        support = FSSupport(
            collection = FSCollectionSupport(
                aclModifiable = null, 
                usersCanCreate = true, 
                usersCanDelete = true, 
                usersCanRename = true, 
            ), 
            files = FSFileSupport(
                aclModifiable = false, 
                isReadOnly = false, 
                trashSupported = false, 
            ), 
            product = ProductReference(
                category = "example-ssd", 
                id = "example-ssd", 
                provider = "example", 
            ), 
            stats = FSProductStatsSupport(
                accessedAt = null, 
                createdAt = null, 
                modifiedAt = null, 
                sizeInBytes = null, 
                sizeIncludingChildrenInBytes = null, 
                unixGroup = null, 
                unixOwner = null, 
                unixPermissions = null, 
            ), 
        ), 
    ))), 
)
*/

/* As we can see, the provider does support the rename operation. We now look at our collections. */

FileCollections.browse.call(
    ResourceBrowseRequest(
        consistency = null, 
        flags = FileCollectionIncludeFlags(
            filterCreatedAfter = null, 
            filterCreatedBefore = null, 
            filterCreatedBy = null, 
            filterIds = null, 
            filterProductCategory = null, 
            filterProductId = null, 
            filterProvider = null, 
            filterProviderIds = null, 
            includeOthers = false, 
            includeProduct = false, 
            includeSupport = false, 
            includeUpdates = false, 
        ), 
        itemsPerPage = null, 
        itemsToSkip = null, 
        next = null, 
        sortBy = null, 
        sortDirection = SortDirection.ascending, 
    ),
    user
).orThrow()

/*
PageV2(
    items = listOf(FileCollection(
        acl = null, 
        billing = ResourceBilling.Free, 
        createdAt = 1635151675465, 
        id = "54123", 
        owner = ResourceOwner(
            createdBy = "user", 
            project = null, 
        ), 
        permissions = ResourcePermissions(
            myself = listOf(Permission.ADMIN), 
            others = emptyList(), 
        ), 
        providerGeneratedId = null, 
        specification = FileCollection.Spec(
            product = ProductReference(
                category = "example-ssd", 
                id = "example-ssd", 
                provider = "example", 
            ), 
            title = "Home", 
        ), 
        status = FileCollection.Status(
            resolvedProduct = null, 
            resolvedSupport = null, 
        ), 
        updates = emptyList(), 
    )), 
    itemsPerPage = 50, 
    next = null, 
)
*/

/* Using the unique ID, we can rename the collection */

FileCollections.rename.call(
    bulkRequestOf(FileCollectionsRenameRequestItem(
        id = "54123", 
        newTitle = "My Awesome Drive", 
    )),
    user
).orThrow()

/*
Unit
*/

/* The new title is observed when we browse the collections one more time */

FileCollections.browse.call(
    ResourceBrowseRequest(
        consistency = null, 
        flags = FileCollectionIncludeFlags(
            filterCreatedAfter = null, 
            filterCreatedBefore = null, 
            filterCreatedBy = null, 
            filterIds = null, 
            filterProductCategory = null, 
            filterProductId = null, 
            filterProvider = null, 
            filterProviderIds = null, 
            includeOthers = false, 
            includeProduct = false, 
            includeSupport = false, 
            includeUpdates = false, 
        ), 
        itemsPerPage = null, 
        itemsToSkip = null, 
        next = null, 
        sortBy = null, 
        sortDirection = SortDirection.ascending, 
    ),
    user
).orThrow()

/*
PageV2(
    items = listOf(FileCollection(
        acl = null, 
        billing = ResourceBilling.Free, 
        createdAt = 1635151675465, 
        id = "54123", 
        owner = ResourceOwner(
            createdBy = "user", 
            project = null, 
        ), 
        permissions = ResourcePermissions(
            myself = listOf(Permission.ADMIN), 
            others = emptyList(), 
        ), 
        providerGeneratedId = null, 
        specification = FileCollection.Spec(
            product = ProductReference(
                category = "example-ssd", 
                id = "example-ssd", 
                provider = "example", 
            ), 
            title = "My Awesome Drive", 
        ), 
        status = FileCollection.Status(
            resolvedProduct = null, 
            resolvedSupport = null, 
        ), 
        updates = emptyList(), 
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

/* In this example, we will see how a user can rename one of their collections. */


/* 📝 NOTE: Renaming must be supported by the provider */

// Authenticated as user
await callAPI(FilesCollectionsApi.retrieveProducts(
    {
    }
);

/*
{
    "productsByProvider": {
        "ucloud": [
            {
                "product": {
                    "balance": null,
                    "name": "example-ssd",
                    "pricePerUnit": 1,
                    "category": {
                        "name": "example-ssd",
                        "provider": "example"
                    },
                    "description": "Fast storage",
                    "priority": 0,
                    "version": 1,
                    "freeToUse": false,
                    "unitOfPrice": "PER_UNIT",
                    "chargeType": "DIFFERENTIAL_QUOTA",
                    "hiddenInGrantApplications": false,
                    "productType": "STORAGE"
                },
                "support": {
                    "product": {
                        "id": "example-ssd",
                        "category": "example-ssd",
                        "provider": "example"
                    },
                    "stats": {
                        "sizeInBytes": null,
                        "sizeIncludingChildrenInBytes": null,
                        "modifiedAt": null,
                        "createdAt": null,
                        "accessedAt": null,
                        "unixPermissions": null,
                        "unixOwner": null,
                        "unixGroup": null
                    },
                    "collection": {
                        "aclModifiable": null,
                        "usersCanCreate": true,
                        "usersCanDelete": true,
                        "usersCanRename": true
                    },
                    "files": {
                        "aclModifiable": false,
                        "trashSupported": false,
                        "isReadOnly": false
                    }
                }
            }
        ]
    }
}
*/

/* As we can see, the provider does support the rename operation. We now look at our collections. */

await callAPI(FilesCollectionsApi.browse(
    {
        "flags": {
            "includeOthers": false,
            "includeUpdates": false,
            "includeSupport": false,
            "includeProduct": false,
            "filterCreatedBy": null,
            "filterCreatedAfter": null,
            "filterCreatedBefore": null,
            "filterProvider": null,
            "filterProductId": null,
            "filterProductCategory": null,
            "filterProviderIds": null,
            "filterIds": null
        },
        "itemsPerPage": null,
        "next": null,
        "consistency": null,
        "itemsToSkip": null,
        "sortBy": null,
        "sortDirection": "ascending"
    }
);

/*
{
    "itemsPerPage": 50,
    "items": [
        {
            "id": "54123",
            "specification": {
                "title": "Home",
                "product": {
                    "id": "example-ssd",
                    "category": "example-ssd",
                    "provider": "example"
                }
            },
            "createdAt": 1635151675465,
            "status": {
                "resolvedSupport": null,
                "resolvedProduct": null
            },
            "updates": [
            ],
            "owner": {
                "createdBy": "user",
                "project": null
            },
            "permissions": {
                "myself": [
                    "ADMIN"
                ],
                "others": [
                ]
            },
            "providerGeneratedId": null,
            "acl": null,
            "billing": {
            }
        }
    ],
    "next": null
}
*/

/* Using the unique ID, we can rename the collection */

await callAPI(FilesCollectionsApi.rename(
    {
        "items": [
            {
                "id": "54123",
                "newTitle": "My Awesome Drive"
            }
        ]
    }
);

/*
{
}
*/

/* The new title is observed when we browse the collections one more time */

await callAPI(FilesCollectionsApi.browse(
    {
        "flags": {
            "includeOthers": false,
            "includeUpdates": false,
            "includeSupport": false,
            "includeProduct": false,
            "filterCreatedBy": null,
            "filterCreatedAfter": null,
            "filterCreatedBefore": null,
            "filterProvider": null,
            "filterProductId": null,
            "filterProductCategory": null,
            "filterProviderIds": null,
            "filterIds": null
        },
        "itemsPerPage": null,
        "next": null,
        "consistency": null,
        "itemsToSkip": null,
        "sortBy": null,
        "sortDirection": "ascending"
    }
);

/*
{
    "itemsPerPage": 50,
    "items": [
        {
            "id": "54123",
            "specification": {
                "title": "My Awesome Drive",
                "product": {
                    "id": "example-ssd",
                    "category": "example-ssd",
                    "provider": "example"
                }
            },
            "createdAt": 1635151675465,
            "status": {
                "resolvedSupport": null,
                "resolvedProduct": null
            },
            "updates": [
            ],
            "owner": {
                "createdBy": "user",
                "project": null
            },
            "permissions": {
                "myself": [
                    "ADMIN"
                ],
                "others": [
                ]
            },
            "providerGeneratedId": null,
            "acl": null,
            "billing": {
            }
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

# In this example, we will see how a user can rename one of their collections.

# 📝 NOTE: Renaming must be supported by the provider

# Authenticated as user
curl -XGET -H "Authorization: Bearer $accessToken" "$host/api/files/collections/retrieveProducts" 

# {
#     "productsByProvider": {
#         "ucloud": [
#             {
#                 "product": {
#                     "balance": null,
#                     "name": "example-ssd",
#                     "pricePerUnit": 1,
#                     "category": {
#                         "name": "example-ssd",
#                         "provider": "example"
#                     },
#                     "description": "Fast storage",
#                     "priority": 0,
#                     "version": 1,
#                     "freeToUse": false,
#                     "unitOfPrice": "PER_UNIT",
#                     "chargeType": "DIFFERENTIAL_QUOTA",
#                     "hiddenInGrantApplications": false,
#                     "productType": "STORAGE"
#                 },
#                 "support": {
#                     "product": {
#                         "id": "example-ssd",
#                         "category": "example-ssd",
#                         "provider": "example"
#                     },
#                     "stats": {
#                         "sizeInBytes": null,
#                         "sizeIncludingChildrenInBytes": null,
#                         "modifiedAt": null,
#                         "createdAt": null,
#                         "accessedAt": null,
#                         "unixPermissions": null,
#                         "unixOwner": null,
#                         "unixGroup": null
#                     },
#                     "collection": {
#                         "aclModifiable": null,
#                         "usersCanCreate": true,
#                         "usersCanDelete": true,
#                         "usersCanRename": true
#                     },
#                     "files": {
#                         "aclModifiable": false,
#                         "trashSupported": false,
#                         "isReadOnly": false
#                     }
#                 }
#             }
#         ]
#     }
# }

# As we can see, the provider does support the rename operation. We now look at our collections.

curl -XGET -H "Authorization: Bearer $accessToken" "$host/api/files/collections/browse?includeOthers=false&includeUpdates=false&includeSupport=false&includeProduct=false&sortDirection=ascending" 

# {
#     "itemsPerPage": 50,
#     "items": [
#         {
#             "id": "54123",
#             "specification": {
#                 "title": "Home",
#                 "product": {
#                     "id": "example-ssd",
#                     "category": "example-ssd",
#                     "provider": "example"
#                 }
#             },
#             "createdAt": 1635151675465,
#             "status": {
#                 "resolvedSupport": null,
#                 "resolvedProduct": null
#             },
#             "updates": [
#             ],
#             "owner": {
#                 "createdBy": "user",
#                 "project": null
#             },
#             "permissions": {
#                 "myself": [
#                     "ADMIN"
#                 ],
#                 "others": [
#                 ]
#             },
#             "providerGeneratedId": null,
#             "acl": null,
#             "billing": {
#             }
#         }
#     ],
#     "next": null
# }

# Using the unique ID, we can rename the collection

curl -XPOST -H "Authorization: Bearer $accessToken" -H "Content-Type: content-type: application/json; charset=utf-8" "$host/api/files/collections/rename" -d '{
    "items": [
        {
            "id": "54123",
            "newTitle": "My Awesome Drive"
        }
    ]
}'


# {
# }

# The new title is observed when we browse the collections one more time

curl -XGET -H "Authorization: Bearer $accessToken" "$host/api/files/collections/browse?includeOthers=false&includeUpdates=false&includeSupport=false&includeProduct=false&sortDirection=ascending" 

# {
#     "itemsPerPage": 50,
#     "items": [
#         {
#             "id": "54123",
#             "specification": {
#                 "title": "My Awesome Drive",
#                 "product": {
#                     "id": "example-ssd",
#                     "category": "example-ssd",
#                     "provider": "example"
#                 }
#             },
#             "createdAt": 1635151675465,
#             "status": {
#                 "resolvedSupport": null,
#                 "resolvedProduct": null
#             },
#             "updates": [
#             ],
#             "owner": {
#                 "createdBy": "user",
#                 "project": null
#             },
#             "permissions": {
#                 "myself": [
#                     "ADMIN"
#                 ],
#                 "others": [
#                 ]
#             },
#             "providerGeneratedId": null,
#             "acl": null,
#             "billing": {
#             }
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

![](/docs/diagrams/files.collections_rename.png)

</details>


