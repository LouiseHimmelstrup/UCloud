# `NetworkIPState`


[![API: Experimental/Alpha](https://img.shields.io/static/v1?label=API&message=Experimental/Alpha&color=orange&style=flat-square)](/docs/developer-guide/core/api-conventions.md)



```kotlin
enum class NetworkIPState {
    PREPARING,
    READY,
    UNAVAILABLE,
}
```

<details>
<summary>
<b>Properties</b>
</summary>

<details>
<summary>
<code>PREPARING</code> A state indicating that the `NetworkIP` is currently being prepared and is expected to reach `READY` soon.
</summary>





</details>

<details>
<summary>
<code>READY</code> A state indicating that the `NetworkIP` is ready for use or already in use.
</summary>





</details>

<details>
<summary>
<code>UNAVAILABLE</code> A state indicating that the `NetworkIP` is currently unavailable.
</summary>



This state can be used to indicate downtime or service interruptions by the provider.


</details>



</details>

