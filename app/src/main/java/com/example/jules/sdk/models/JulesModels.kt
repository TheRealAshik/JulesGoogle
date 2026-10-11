package com.example.jules.sdk.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class GithubRepo(
    @Json(name = "owner") val owner: String? = null,
    @Json(name = "repo") val repo: String? = null,
    @Json(name = "branches") val branches: List<String> = emptyList()
) {
    val fullName: String
        get() = if (owner != null && repo != null) "$owner/$repo" else repo ?: "Repository"
}

@JsonClass(generateAdapter = true)
data class Source(
    @Json(name = "name") val name: String,
    @Json(name = "githubRepo") val githubRepo: GithubRepo? = null
) {
    val cleanName: String
        get() = githubRepo?.fullName ?: name.removePrefix("sources/github/").removePrefix("sources/")
}

@JsonClass(generateAdapter = true)
data class GithubRepoContext(
    @Json(name = "startingBranch") val startingBranch: String? = null
)

@JsonClass(generateAdapter = true)
data class SourceContext(
    @Json(name = "source") val source: String,
    @Json(name = "githubRepoContext") val githubRepoContext: GithubRepoContext? = null
)

@JsonClass(generateAdapter = true)
data class Session(
    @Json(name = "name") val name: String,
    @Json(name = "id") val id: String? = null,
    @Json(name = "title") val title: String? = null,
    @Json(name = "state") val state: String? = null,
    @Json(name = "sourceContext") val sourceContext: SourceContext? = null,
    @Json(name = "requirePlanApproval") val requirePlanApproval: Boolean? = null,
    @Json(name = "createTime") val createTime: String? = null,
    @Json(name = "updateTime") val updateTime: String? = null
) {
    val cleanId: String
        get() = id ?: name.substringAfter("sessions/")

    val displayTitle: String
        get() = title?.takeIf { it.isNotBlank() } ?: "Coding Session"

    val isWaitingForApproval: Boolean
        get() = state?.contains("APPROVAL", ignoreCase = true) == true

    val isCompleted: Boolean
        get() = state?.equals("COMPLETED", ignoreCase = true) == true
}

@JsonClass(generateAdapter = true)
data class CreateSessionRequest(
    @Json(name = "prompt") val prompt: String,
    @Json(name = "sourceContext") val sourceContext: SourceContext,
    @Json(name = "requirePlanApproval") val requirePlanApproval: Boolean = false,
    @Json(name = "title") val title: String? = null
)

@JsonClass(generateAdapter = true)
data class Message(
    @Json(name = "content") val content: String
)

@JsonClass(generateAdapter = true)
data class PlanStep(
    @Json(name = "description") val description: String,
    @Json(name = "status") val status: String? = null
)

@JsonClass(generateAdapter = true)
data class Plan(
    @Json(name = "title") val title: String? = null,
    @Json(name = "steps") val steps: List<PlanStep> = emptyList(),
    @Json(name = "state") val state: String? = null
) {
    val isPendingApproval: Boolean
        get() = state?.contains("WAITING", ignoreCase = true) == true ||
                state?.contains("PENDING", ignoreCase = true) == true
}

@JsonClass(generateAdapter = true)
data class Progress(
    @Json(name = "summary") val summary: String? = null,
    @Json(name = "status") val status: String? = null
)

@JsonClass(generateAdapter = true)
data class Artifact(
    @Json(name = "type") val type: String? = null,
    @Json(name = "url") val url: String? = null,
    @Json(name = "title") val title: String? = null,
    @Json(name = "summary") val summary: String? = null
)

@JsonClass(generateAdapter = true)
data class Activity(
    @Json(name = "name") val name: String,
    @Json(name = "id") val id: String? = null,
    @Json(name = "createTime") val createTime: String? = null,
    @Json(name = "originator") val originator: String? = null,
    @Json(name = "activityType") val activityType: String? = null,
    @Json(name = "message") val message: Message? = null,
    @Json(name = "plan") val plan: Plan? = null,
    @Json(name = "progress") val progress: Progress? = null,
    @Json(name = "artifact") val artifact: Artifact? = null
) {
    val cleanId: String
        get() = id ?: name.substringAfterLast("/")

    val isUser: Boolean
        get() = originator?.equals("USER", ignoreCase = true) == true

    val isAgent: Boolean
        get() = originator?.equals("AGENT", ignoreCase = true) == true ||
                originator?.equals("SYSTEM", ignoreCase = true) == true
}

@JsonClass(generateAdapter = true)
data class SendMessageRequest(
    @Json(name = "message") val message: Message
)

@JsonClass(generateAdapter = true)
data class ApprovePlanRequest(
    @Json(name = "planId") val planId: String? = null
)

@JsonClass(generateAdapter = true)
data class ListSourcesResponse(
    @Json(name = "sources") val sources: List<Source> = emptyList(),
    @Json(name = "nextPageToken") val nextPageToken: String? = null
)

@JsonClass(generateAdapter = true)
data class ListSessionsResponse(
    @Json(name = "sessions") val sessions: List<Session> = emptyList(),
    @Json(name = "nextPageToken") val nextPageToken: String? = null
)

@JsonClass(generateAdapter = true)
data class ListActivitiesResponse(
    @Json(name = "activities") val activities: List<Activity> = emptyList(),
    @Json(name = "nextPageToken") val nextPageToken: String? = null
)
