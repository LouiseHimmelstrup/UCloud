# `WalletAllocation`


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

