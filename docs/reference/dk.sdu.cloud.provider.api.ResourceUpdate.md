                            [UCloud Developer Guide](/docs/developer-guide/README.md) / [Core](/docs/developer-guide/core/README.md) / [Resources](/docs/developer-guide/core/resources.md)
                            
                            # `ResourceUpdate`

                            
[![API: Internal/Beta](https://img.shields.io/static/v1?label=API&message=Internal/Beta&color=red&style=flat-square)](/docs/developer-guide/core/api-conventions.md)


_Describes an update to the `Resource`_

```kotlin
data class ResourceUpdate(
    val status: String?,
    val timestamp: Long,
)
```
Updates can optionally be fetched for a `Resource`. The updates describe how the `Resource` changes state over time.
The current state of a `Resource` can typically be read from its `status` field. Thus, it is typically not needed to
use the full update history if you only wish to know the _current_ state of a `Resource`.

An update will typically contain information similar to the `status` field, for example:

- A state value. For example, a compute `Job` might be `RUNNING`.
- Change in key metrics.
- Bindings to related `Resource`s.

<details>
<summary>
<b>Properties</b>
</summary>

<details>
<summary>
<code>status</code>: <code><code><a href='https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/'>String</a>?</code></code> A generic text message describing the current status of the `Resource`
</summary>





</details>

<details>
<summary>
<code>timestamp</code>: <code><code><a href='https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-long/'>Long</a></code></code> A timestamp referencing when UCloud received this update
</summary>





</details>



</details>

