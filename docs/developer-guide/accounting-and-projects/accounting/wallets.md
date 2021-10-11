<p align='center'>
<a href='/docs/developer-guide/accounting-and-projects/products.md'>« Previous section</a>
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<a href='/docs/developer-guide/accounting-and-projects/accounting/allocations.md'>Next section »</a>
</p>


[UCloud Developer Guide](/docs/developer-guide/README.md) / [Accounting and Project Management](/docs/developer-guide/accounting-and-projects/README.md) / [Accounting](/docs/developer-guide/accounting-and-projects/accounting/README.md) / Wallets
# Wallets

[![API: Internal/Beta](https://img.shields.io/static/v1?label=API&message=Internal/Beta&color=red&style=flat-square)](/docs/developer-guide/core/api-conventions.md)

_Wallets hold allocations which grant access to a provider's resources._

## Rationale

[`Wallet`](/docs/reference/dk.sdu.cloud.accounting.api.Wallet.md)s are the core abstraction used in the accounting system of UCloud. This feature builds
on top of various other features of UCloud. Here is a quick recap:

- The users of UCloud are members of 
  [Workspaces and Projects](/docs/developer-guide/accounting-and-projects/projects/projects.md). These form 
  the foundation of all collaboration in UCloud.
- UCloud is an orchestrator of [`Resource`](/docs/reference/dk.sdu.cloud.provider.api.Resource.md)s. UCloud delegates the responsibility of hosting [`Resource`](/docs/reference/dk.sdu.cloud.provider.api.Resource.md)s to 
  [`Provider`](/docs/reference/dk.sdu.cloud.provider.api.Provider.md)s.
- [`Provider`](/docs/reference/dk.sdu.cloud.provider.api.Provider.md)s define which services they support using [`Product`](/docs/reference/dk.sdu.cloud.accounting.api.Product.md)s.
- All [`Product`](/docs/reference/dk.sdu.cloud.accounting.api.Product.md)s belong in a [`ProductCategory`](/docs/reference/dk.sdu.cloud.accounting.api.ProductCategory.md)  . The category contains similar 
  [`Product`](/docs/reference/dk.sdu.cloud.accounting.api.Product.md)s. Under normal circumstances, all products in a category run on the same system.
- [`Product`](/docs/reference/dk.sdu.cloud.accounting.api.Product.md)s define a payment model. The model supports quotas (`DIFFERENTIAL_QUOTA`), one-time 
  payments and periodic payments (`ABSOLUTE`). All absolute payment models support paying in a 
  product-specific unit or in DKK.
- All [`Product`](/docs/reference/dk.sdu.cloud.accounting.api.Product.md)s in a category share the exact same payment model

![](/backend/accounting-service/wiki/allocations.png)

## Table of Contents
<details>
<summary>
<a href='#remote-procedure-calls'>1. Remote Procedure Calls</a>
</summary>

<table><thead><tr>
<th>Name</th>
<th>Description</th>
</tr></thread>
<tbody>
<tr>
<td><a href='#browse'><code>browse</code></a></td>
<td><i>No description</i></td>
</tr>
<tr>
<td><a href='#browsesuballocations'><code>browseSubAllocations</code></a></td>
<td><i>No description</i></td>
</tr>
<tr>
<td><a href='#retrieverecipient'><code>retrieveRecipient</code></a></td>
<td><i>No description</i></td>
</tr>
<tr>
<td><a href='#push'><code>push</code></a></td>
<td><i>No description</i></td>
</tr>
</tbody></table>


</details>

<details>
<summary>
<a href='#data-models'>2. Data Models</a>
</summary>

<table><thead><tr>
<th>Name</th>
<th>Description</th>
</tr></thread>
<tbody>
<tr>
<td><a href='#allocationselectorpolicy'><code>AllocationSelectorPolicy</code></a></td>
<td><i>No description</i></td>
</tr>
<tr>
<td><a href='#sortsuballocationsby'><code>SortSubAllocationsBy</code></a></td>
<td><i>No description</i></td>
</tr>
<tr>
<td><a href='#suballocation'><code>SubAllocation</code></a></td>
<td>A parent allocator's view of a `WalletAllocation`</td>
</tr>
<tr>
<td><a href='#wallet'><code>Wallet</code></a></td>
<td><i>No description</i></td>
</tr>
<tr>
<td><a href='#walletallocation'><code>WalletAllocation</code></a></td>
<td><i>No description</i></td>
</tr>
<tr>
<td><a href='#walletowner'><code>WalletOwner</code></a></td>
<td><i>No description</i></td>
</tr>
<tr>
<td><a href='#walletowner.project'><code>WalletOwner.Project</code></a></td>
<td><i>No description</i></td>
</tr>
<tr>
<td><a href='#walletowner.user'><code>WalletOwner.User</code></a></td>
<td><i>No description</i></td>
</tr>
<tr>
<td><a href='#pushwalletchangerequestitem'><code>PushWalletChangeRequestItem</code></a></td>
<td><i>No description</i></td>
</tr>
<tr>
<td><a href='#walletbrowserequest'><code>WalletBrowseRequest</code></a></td>
<td>The base type for requesting paginated content.</td>
</tr>
<tr>
<td><a href='#walletsbrowsesuballocationsrequest'><code>WalletsBrowseSubAllocationsRequest</code></a></td>
<td>The base type for requesting paginated content.</td>
</tr>
<tr>
<td><a href='#walletsretrieverecipientrequest'><code>WalletsRetrieveRecipientRequest</code></a></td>
<td><i>No description</i></td>
</tr>
<tr>
<td><a href='#walletsretrieverecipientresponse'><code>WalletsRetrieveRecipientResponse</code></a></td>
<td><i>No description</i></td>
</tr>
</tbody></table>


</details>


## Remote Procedure Calls

### `browse`

[![API: Internal/Beta](https://img.shields.io/static/v1?label=API&message=Internal/Beta&color=red&style=flat-square)](/docs/developer-guide/core/api-conventions.md)
[![Auth: Users](https://img.shields.io/static/v1?label=Auth&message=Users&color=informational&style=flat-square)](/docs/developer-guide/core/types.md#role)



| Request | Response | Error |
|---------|----------|-------|
|<code><a href='#walletbrowserequest'>WalletBrowseRequest</a></code>|<code><a href='/docs/reference/dk.sdu.cloud.PageV2.md'>PageV2</a>&lt;<a href='#wallet'>Wallet</a>&gt;</code>|<code><a href='/docs/reference/dk.sdu.cloud.CommonErrorMessage.md'>CommonErrorMessage</a></code>|



### `browseSubAllocations`

[![API: Internal/Beta](https://img.shields.io/static/v1?label=API&message=Internal/Beta&color=red&style=flat-square)](/docs/developer-guide/core/api-conventions.md)
[![Auth: Users](https://img.shields.io/static/v1?label=Auth&message=Users&color=informational&style=flat-square)](/docs/developer-guide/core/types.md#role)



| Request | Response | Error |
|---------|----------|-------|
|<code><a href='#walletsbrowsesuballocationsrequest'>WalletsBrowseSubAllocationsRequest</a></code>|<code><a href='/docs/reference/dk.sdu.cloud.PageV2.md'>PageV2</a>&lt;<a href='#suballocation'>SubAllocation</a>&gt;</code>|<code><a href='/docs/reference/dk.sdu.cloud.CommonErrorMessage.md'>CommonErrorMessage</a></code>|



### `retrieveRecipient`

[![API: Internal/Beta](https://img.shields.io/static/v1?label=API&message=Internal/Beta&color=red&style=flat-square)](/docs/developer-guide/core/api-conventions.md)
[![Auth: Users](https://img.shields.io/static/v1?label=Auth&message=Users&color=informational&style=flat-square)](/docs/developer-guide/core/types.md#role)



| Request | Response | Error |
|---------|----------|-------|
|<code><a href='#walletsretrieverecipientrequest'>WalletsRetrieveRecipientRequest</a></code>|<code><a href='#walletsretrieverecipientresponse'>WalletsRetrieveRecipientResponse</a></code>|<code><a href='/docs/reference/dk.sdu.cloud.CommonErrorMessage.md'>CommonErrorMessage</a></code>|



### `push`

[![API: Internal/Beta](https://img.shields.io/static/v1?label=API&message=Internal/Beta&color=red&style=flat-square)](/docs/developer-guide/core/api-conventions.md)
[![Auth: Services](https://img.shields.io/static/v1?label=Auth&message=Services&color=informational&style=flat-square)](/docs/developer-guide/core/types.md#role)



| Request | Response | Error |
|---------|----------|-------|
|<code><a href='/docs/reference/dk.sdu.cloud.calls.BulkRequest.md'>BulkRequest</a>&lt;<a href='#pushwalletchangerequestitem'>PushWalletChangeRequestItem</a>&gt;</code>|<code><a href='https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/'>Unit</a></code>|<code><a href='/docs/reference/dk.sdu.cloud.CommonErrorMessage.md'>CommonErrorMessage</a></code>|




## Data Models

### `AllocationSelectorPolicy`

[![API: Internal/Beta](https://img.shields.io/static/v1?label=API&message=Internal/Beta&color=red&style=flat-square)](/docs/developer-guide/core/api-conventions.md)



```kotlin
enum class AllocationSelectorPolicy {
    EXPIRE_FIRST,
}
```

<details>
<summary>
<b>Properties</b>
</summary>

<details>
<summary>
<code>EXPIRE_FIRST</code>
</summary>





</details>



</details>



---

### `SortSubAllocationsBy`

[![API: Internal/Beta](https://img.shields.io/static/v1?label=API&message=Internal/Beta&color=red&style=flat-square)](/docs/developer-guide/core/api-conventions.md)



```kotlin
enum class SortSubAllocationsBy {
    GRANT_ALLOCATION,
    PRODUCT_CATEGORY,
}
```

<details>
<summary>
<b>Properties</b>
</summary>

<details>
<summary>
<code>GRANT_ALLOCATION</code>
</summary>





</details>

<details>
<summary>
<code>PRODUCT_CATEGORY</code>
</summary>





</details>



</details>



---

### `SubAllocation`

[![API: Experimental/Alpha](https://img.shields.io/static/v1?label=API&message=Experimental/Alpha&color=orange&style=flat-square)](/docs/developer-guide/core/api-conventions.md)


_A parent allocator's view of a `WalletAllocation`_

```kotlin
data class SubAllocation(
    val id: String,
    val startDate: Long,
    val endDate: Long?,
    val productCategoryId: ProductCategoryId,
    val productType: ProductType,
    val chargeType: ChargeType,
    val unit: ProductPriceUnit,
    val workspaceId: String,
    val workspaceTitle: String,
    val workspaceIsProject: Boolean,
    val remaining: Long,
)
```

<details>
<summary>
<b>Properties</b>
</summary>

<details>
<summary>
<code>id</code>: <code><code><a href='https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/'>String</a></code></code>
</summary>





</details>

<details>
<summary>
<code>startDate</code>: <code><code><a href='https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-long/'>Long</a></code></code>
</summary>





</details>

<details>
<summary>
<code>endDate</code>: <code><code><a href='https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-long/'>Long</a>?</code></code>
</summary>





</details>

<details>
<summary>
<code>productCategoryId</code>: <code><code><a href='/docs/reference/dk.sdu.cloud.accounting.api.ProductCategoryId.md'>ProductCategoryId</a></code></code>
</summary>





</details>

<details>
<summary>
<code>productType</code>: <code><code><a href='/docs/reference/dk.sdu.cloud.accounting.api.ProductType.md'>ProductType</a></code></code>
</summary>





</details>

<details>
<summary>
<code>chargeType</code>: <code><code><a href='/docs/reference/dk.sdu.cloud.accounting.api.ChargeType.md'>ChargeType</a></code></code>
</summary>





</details>

<details>
<summary>
<code>unit</code>: <code><code><a href='/docs/reference/dk.sdu.cloud.accounting.api.ProductPriceUnit.md'>ProductPriceUnit</a></code></code>
</summary>





</details>

<details>
<summary>
<code>workspaceId</code>: <code><code><a href='https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/'>String</a></code></code>
</summary>





</details>

<details>
<summary>
<code>workspaceTitle</code>: <code><code><a href='https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/'>String</a></code></code>
</summary>





</details>

<details>
<summary>
<code>workspaceIsProject</code>: <code><code><a href='https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/'>Boolean</a></code></code>
</summary>





</details>

<details>
<summary>
<code>remaining</code>: <code><code><a href='https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-long/'>Long</a></code></code>
</summary>





</details>



</details>



---

### `Wallet`

[![API: Internal/Beta](https://img.shields.io/static/v1?label=API&message=Internal/Beta&color=red&style=flat-square)](/docs/developer-guide/core/api-conventions.md)



```kotlin
data class Wallet(
    val owner: WalletOwner,
    val paysFor: ProductCategoryId,
    val allocations: List<WalletAllocation>,
    val chargePolicy: AllocationSelectorPolicy,
    val productType: ProductType?,
    val chargeType: ChargeType?,
    val unit: ProductPriceUnit?,
)
```

<details>
<summary>
<b>Properties</b>
</summary>

<details>
<summary>
<code>owner</code>: <code><code><a href='#walletowner'>WalletOwner</a></code></code>
</summary>





</details>

<details>
<summary>
<code>paysFor</code>: <code><code><a href='/docs/reference/dk.sdu.cloud.accounting.api.ProductCategoryId.md'>ProductCategoryId</a></code></code>
</summary>





</details>

<details>
<summary>
<code>allocations</code>: <code><code><a href='https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/'>List</a>&lt;<a href='#walletallocation'>WalletAllocation</a>&gt;</code></code>
</summary>





</details>

<details>
<summary>
<code>chargePolicy</code>: <code><code><a href='#allocationselectorpolicy'>AllocationSelectorPolicy</a></code></code>
</summary>





</details>

<details>
<summary>
<code>productType</code>: <code><code><a href='/docs/reference/dk.sdu.cloud.accounting.api.ProductType.md'>ProductType</a>?</code></code>
</summary>





</details>

<details>
<summary>
<code>chargeType</code>: <code><code><a href='/docs/reference/dk.sdu.cloud.accounting.api.ChargeType.md'>ChargeType</a>?</code></code>
</summary>





</details>

<details>
<summary>
<code>unit</code>: <code><code><a href='/docs/reference/dk.sdu.cloud.accounting.api.ProductPriceUnit.md'>ProductPriceUnit</a>?</code></code>
</summary>





</details>



</details>



---

### `WalletAllocation`

[![API: Internal/Beta](https://img.shields.io/static/v1?label=API&message=Internal/Beta&color=red&style=flat-square)](/docs/developer-guide/core/api-conventions.md)



```kotlin
data class WalletAllocation(
    val id: String,
    val allocationPath: List<String>,
    val balance: Long,
    val initialBalance: Long,
    val localBalance: Long,
    val startDate: Long,
    val endDate: Long?,
)
```

<details>
<summary>
<b>Properties</b>
</summary>

<details>
<summary>
<code>id</code>: <code><code><a href='https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/'>String</a></code></code> A unique ID of this allocation
</summary>





</details>

<details>
<summary>
<code>allocationPath</code>: <code><code><a href='https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/'>List</a>&lt;<a href='https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/'>String</a>&gt;</code></code> A path, starting from the top, through the allocations that will be charged, when a charge is made
</summary>



Note that this allocation path will always include, as its last element, this allocation.


</details>

<details>
<summary>
<code>balance</code>: <code><code><a href='https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-long/'>Long</a></code></code> The current balance of this wallet allocation's subtree
</summary>





</details>

<details>
<summary>
<code>initialBalance</code>: <code><code><a href='https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-long/'>Long</a></code></code> The initial balance which was granted to this allocation
</summary>





</details>

<details>
<summary>
<code>localBalance</code>: <code><code><a href='https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-long/'>Long</a></code></code> The current balance of this wallet allocation
</summary>





</details>

<details>
<summary>
<code>startDate</code>: <code><code><a href='https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-long/'>Long</a></code></code> Timestamp for when this allocation becomes valid
</summary>





</details>

<details>
<summary>
<code>endDate</code>: <code><code><a href='https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-long/'>Long</a>?</code></code> Timestamp for when this allocation becomes invalid, null indicates that this allocation does not expire automatically
</summary>





</details>



</details>



---

### `WalletOwner`

[![API: Experimental/Alpha](https://img.shields.io/static/v1?label=API&message=Experimental/Alpha&color=orange&style=flat-square)](/docs/developer-guide/core/api-conventions.md)



```kotlin
sealed class WalletOwner {
    class User : WalletOwner()
    class Project : WalletOwner()
}
```



---

### `WalletOwner.Project`

[![API: Experimental/Alpha](https://img.shields.io/static/v1?label=API&message=Experimental/Alpha&color=orange&style=flat-square)](/docs/developer-guide/core/api-conventions.md)



```kotlin
data class Project(
    val projectId: String,
    val type: String /* "project" */,
)
```

<details>
<summary>
<b>Properties</b>
</summary>

<details>
<summary>
<code>projectId</code>: <code><code><a href='https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/'>String</a></code></code>
</summary>





</details>

<details>
<summary>
<code>type</code>: <code><code>String /* "project" */</code></code> The type discriminator
</summary>

[![API: Stable](https://img.shields.io/static/v1?label=API&message=Stable&color=green&style=flat-square)](/docs/developer-guide/core/api-conventions.md)




</details>



</details>



---

### `WalletOwner.User`

[![API: Experimental/Alpha](https://img.shields.io/static/v1?label=API&message=Experimental/Alpha&color=orange&style=flat-square)](/docs/developer-guide/core/api-conventions.md)



```kotlin
data class User(
    val username: String,
    val type: String /* "user" */,
)
```

<details>
<summary>
<b>Properties</b>
</summary>

<details>
<summary>
<code>username</code>: <code><code><a href='https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/'>String</a></code></code>
</summary>





</details>

<details>
<summary>
<code>type</code>: <code><code>String /* "user" */</code></code> The type discriminator
</summary>

[![API: Stable](https://img.shields.io/static/v1?label=API&message=Stable&color=green&style=flat-square)](/docs/developer-guide/core/api-conventions.md)




</details>



</details>



---

### `PushWalletChangeRequestItem`

[![API: Internal/Beta](https://img.shields.io/static/v1?label=API&message=Internal/Beta&color=red&style=flat-square)](/docs/developer-guide/core/api-conventions.md)



```kotlin
data class PushWalletChangeRequestItem(
    val owner: WalletOwner,
    val amount: Long,
    val productId: ProductReference,
)
```

<details>
<summary>
<b>Properties</b>
</summary>

<details>
<summary>
<code>owner</code>: <code><code><a href='#walletowner'>WalletOwner</a></code></code>
</summary>





</details>

<details>
<summary>
<code>amount</code>: <code><code><a href='https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-long/'>Long</a></code></code>
</summary>





</details>

<details>
<summary>
<code>productId</code>: <code><code><a href='/docs/reference/dk.sdu.cloud.accounting.api.ProductReference.md'>ProductReference</a></code></code>
</summary>





</details>



</details>



---

### `WalletBrowseRequest`

[![API: Internal/Beta](https://img.shields.io/static/v1?label=API&message=Internal/Beta&color=red&style=flat-square)](/docs/developer-guide/core/api-conventions.md)


_The base type for requesting paginated content._

```kotlin
data class WalletBrowseRequest(
    val itemsPerPage: Int?,
    val next: String?,
    val consistency: PaginationRequestV2Consistency?,
    val itemsToSkip: Long?,
    val filterType: ProductType?,
)
```
Paginated content can be requested with one of the following `consistency` guarantees, this greatly changes the
semantics of the call:

| Consistency | Description |
|-------------|-------------|
| `PREFER` | Consistency is preferred but not required. An inconsistent snapshot might be returned. |
| `REQUIRE` | Consistency is required. A request will fail if consistency is no longer guaranteed. |

The `consistency` refers to if collecting all the results via the pagination API are _consistent_. We consider the
results to be consistent if it contains a complete view at some point in time. In practice this means that the results
must contain all the items, in the correct order and without duplicates.

If you use the `PREFER` consistency then you may receive in-complete results that might appear out-of-order and can
contain duplicate items. UCloud will still attempt to serve a snapshot which appears mostly consistent. This is helpful
for user-interfaces which do not strictly depend on consistency but would still prefer something which is mostly
consistent.

The results might become inconsistent if the client either takes too long, or a service instance goes down while
fetching the results. UCloud attempts to keep each `next` token alive for at least one minute before invalidating it.
This does not mean that a client must collect all results within a minute but rather that they must fetch the next page
within a minute of the last page. If this is not feasible and consistency is not required then `PREFER` should be used.

---

__📝 NOTE:__ Services are allowed to ignore extra criteria of the request if the `next` token is supplied. This is
needed in order to provide a consistent view of the results. Clients _should_ provide the same criterion as they
paginate through the results.

---

<details>
<summary>
<b>Properties</b>
</summary>

<details>
<summary>
<code>itemsPerPage</code>: <code><code><a href='https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/'>Int</a>?</code></code> Requested number of items per page. Supported values: 10, 25, 50, 100, 250.
</summary>





</details>

<details>
<summary>
<code>next</code>: <code><code><a href='https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/'>String</a>?</code></code> A token requesting the next page of items
</summary>





</details>

<details>
<summary>
<code>consistency</code>: <code><code><a href='/docs/reference/dk.sdu.cloud.PaginationRequestV2Consistency.md'>PaginationRequestV2Consistency</a>?</code></code> Controls the consistency guarantees provided by the backend
</summary>





</details>

<details>
<summary>
<code>itemsToSkip</code>: <code><code><a href='https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-long/'>Long</a>?</code></code> Items to skip ahead
</summary>





</details>

<details>
<summary>
<code>filterType</code>: <code><code><a href='/docs/reference/dk.sdu.cloud.accounting.api.ProductType.md'>ProductType</a>?</code></code>
</summary>





</details>



</details>



---

### `WalletsBrowseSubAllocationsRequest`

[![API: Experimental/Alpha](https://img.shields.io/static/v1?label=API&message=Experimental/Alpha&color=orange&style=flat-square)](/docs/developer-guide/core/api-conventions.md)


_The base type for requesting paginated content._

```kotlin
data class WalletsBrowseSubAllocationsRequest(
    val sortBy: SortSubAllocationsBy?,
    val filterType: ProductType?,
    val itemsPerPage: Int?,
    val next: String?,
    val consistency: PaginationRequestV2Consistency?,
    val itemsToSkip: Long?,
)
```
Paginated content can be requested with one of the following `consistency` guarantees, this greatly changes the
semantics of the call:

| Consistency | Description |
|-------------|-------------|
| `PREFER` | Consistency is preferred but not required. An inconsistent snapshot might be returned. |
| `REQUIRE` | Consistency is required. A request will fail if consistency is no longer guaranteed. |

The `consistency` refers to if collecting all the results via the pagination API are _consistent_. We consider the
results to be consistent if it contains a complete view at some point in time. In practice this means that the results
must contain all the items, in the correct order and without duplicates.

If you use the `PREFER` consistency then you may receive in-complete results that might appear out-of-order and can
contain duplicate items. UCloud will still attempt to serve a snapshot which appears mostly consistent. This is helpful
for user-interfaces which do not strictly depend on consistency but would still prefer something which is mostly
consistent.

The results might become inconsistent if the client either takes too long, or a service instance goes down while
fetching the results. UCloud attempts to keep each `next` token alive for at least one minute before invalidating it.
This does not mean that a client must collect all results within a minute but rather that they must fetch the next page
within a minute of the last page. If this is not feasible and consistency is not required then `PREFER` should be used.

---

__📝 NOTE:__ Services are allowed to ignore extra criteria of the request if the `next` token is supplied. This is
needed in order to provide a consistent view of the results. Clients _should_ provide the same criterion as they
paginate through the results.

---

<details>
<summary>
<b>Properties</b>
</summary>

<details>
<summary>
<code>sortBy</code>: <code><code><a href='#sortsuballocationsby'>SortSubAllocationsBy</a>?</code></code>
</summary>





</details>

<details>
<summary>
<code>filterType</code>: <code><code><a href='/docs/reference/dk.sdu.cloud.accounting.api.ProductType.md'>ProductType</a>?</code></code>
</summary>





</details>

<details>
<summary>
<code>itemsPerPage</code>: <code><code><a href='https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/'>Int</a>?</code></code> Requested number of items per page. Supported values: 10, 25, 50, 100, 250.
</summary>





</details>

<details>
<summary>
<code>next</code>: <code><code><a href='https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/'>String</a>?</code></code> A token requesting the next page of items
</summary>





</details>

<details>
<summary>
<code>consistency</code>: <code><code><a href='/docs/reference/dk.sdu.cloud.PaginationRequestV2Consistency.md'>PaginationRequestV2Consistency</a>?</code></code> Controls the consistency guarantees provided by the backend
</summary>





</details>

<details>
<summary>
<code>itemsToSkip</code>: <code><code><a href='https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-long/'>Long</a>?</code></code> Items to skip ahead
</summary>





</details>



</details>



---

### `WalletsRetrieveRecipientRequest`

[![API: Internal/Beta](https://img.shields.io/static/v1?label=API&message=Internal/Beta&color=red&style=flat-square)](/docs/developer-guide/core/api-conventions.md)



```kotlin
data class WalletsRetrieveRecipientRequest(
    val query: String,
)
```

<details>
<summary>
<b>Properties</b>
</summary>

<details>
<summary>
<code>query</code>: <code><code><a href='https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/'>String</a></code></code>
</summary>





</details>



</details>



---

### `WalletsRetrieveRecipientResponse`

[![API: Internal/Beta](https://img.shields.io/static/v1?label=API&message=Internal/Beta&color=red&style=flat-square)](/docs/developer-guide/core/api-conventions.md)



```kotlin
data class WalletsRetrieveRecipientResponse(
    val id: String,
    val isProject: Boolean,
    val title: String,
    val principalInvestigator: String,
    val numberOfMembers: Int,
)
```

<details>
<summary>
<b>Properties</b>
</summary>

<details>
<summary>
<code>id</code>: <code><code><a href='https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/'>String</a></code></code>
</summary>





</details>

<details>
<summary>
<code>isProject</code>: <code><code><a href='https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/'>Boolean</a></code></code>
</summary>





</details>

<details>
<summary>
<code>title</code>: <code><code><a href='https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/'>String</a></code></code>
</summary>





</details>

<details>
<summary>
<code>principalInvestigator</code>: <code><code><a href='https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/'>String</a></code></code>
</summary>





</details>

<details>
<summary>
<code>numberOfMembers</code>: <code><code><a href='https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/'>Int</a></code></code>
</summary>





</details>



</details>



---

