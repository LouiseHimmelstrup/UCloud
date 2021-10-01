# `AppParameterValue.License`


![API: Experimental/Alpha](https://img.shields.io/static/v1?label=API&message=Experimental/Alpha&color=orange&style=flat-square)


_A reference to a software license, registered locally at the provider_

```kotlin
data class License(
    val id: String,
    val type: String /* "license_server" */,
)
```
- __Compatible with:__ `ApplicationParameter.LicenseServer`
- __Mountable as a resource:__ ❌ No
- __Expands to:__ `${license.address}:${license.port}/${license.key}` or 
  `${license.address}:${license.port}` if no key is provided
- __Side effects:__ None

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
<code>type</code>: <code><code>String /* "license_server" */</code></code> The type discriminator
</summary>

![API: Stable](https://img.shields.io/static/v1?label=API&message=Stable&color=green&style=flat-square)




</details>



</details>

