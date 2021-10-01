# `Share.Spec`


![API: Internal/Beta](https://img.shields.io/static/v1?label=API&message=Internal/Beta&color=red&style=flat-square)



```kotlin
data class Spec(
    val sharedWith: String,
    val sourceFilePath: String,
    val permissions: List<Permission>,
    val product: ProductReference,
)
```

<details>
<summary>
<b>Properties</b>
</summary>

<details>
<summary>
<code>sharedWith</code>: <code><code><a href='https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/'>String</a></code></code>
</summary>





</details>

<details>
<summary>
<code>sourceFilePath</code>: <code><code><a href='https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/'>String</a></code></code>
</summary>





</details>

<details>
<summary>
<code>permissions</code>: <code><code><a href='https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/'>List</a>&lt;<a href='/docs/reference/dk.sdu.cloud.provider.api.Permission.md'>Permission</a>&gt;</code></code>
</summary>





</details>

<details>
<summary>
<code>product</code>: <code><code><a href='/docs/reference/dk.sdu.cloud.accounting.api.ProductReference.md'>ProductReference</a></code></code> A reference to the product which backs this `Resource`
</summary>





</details>



</details>

