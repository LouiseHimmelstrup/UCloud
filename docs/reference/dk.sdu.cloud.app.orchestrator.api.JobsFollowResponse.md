# `JobsFollowResponse`


![API: Internal/Beta](https://img.shields.io/static/v1?label=API&message=Internal/Beta&color=red&style=flat-square)



```kotlin
data class JobsFollowResponse(
    val updates: List<JobUpdate>,
    val log: List<JobsLog>,
    val newStatus: JobStatus?,
)
```

<details>
<summary>
<b>Properties</b>
</summary>

<details>
<summary>
<code>updates</code>: <code><code><a href='https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/'>List</a>&lt;<a href='/docs/reference/dk.sdu.cloud.app.orchestrator.api.JobUpdate.md'>JobUpdate</a>&gt;</code></code>
</summary>





</details>

<details>
<summary>
<code>log</code>: <code><code><a href='https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/'>List</a>&lt;<a href='#jobslog'>JobsLog</a>&gt;</code></code>
</summary>





</details>

<details>
<summary>
<code>newStatus</code>: <code><code><a href='#jobstatus'>JobStatus</a>?</code></code>
</summary>





</details>



</details>

