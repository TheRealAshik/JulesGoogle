package com.example.jules.sdk

import com.example.jules.sdk.api.JulesApiService
import com.example.jules.sdk.models.Activity
import com.example.jules.sdk.models.ApprovePlanRequest
import com.example.jules.sdk.models.CreateSessionRequest
import com.example.jules.sdk.models.GithubRepoContext
import com.example.jules.sdk.models.ListActivitiesResponse
import com.example.jules.sdk.models.ListSessionsResponse
import com.example.jules.sdk.models.ListSourcesResponse
import com.example.jules.sdk.models.Message
import com.example.jules.sdk.models.SendMessageRequest
import com.example.jules.sdk.models.Session
import com.example.jules.sdk.models.Source
import com.example.jules.sdk.models.SourceContext
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

sealed class JulesResult<out T> {
    data class Success<out T>(val data: T) : JulesResult<T>()
    data class Error(
        val message: String,
        val code: Int? = null,
        val cause: Throwable? = null
    ) : JulesResult<Nothing>()

    val isSuccess: Boolean get() = this is Success<*>
    fun getOrNull(): T? = (this as? Success<T>)?.data
    fun errorMessageOrNull(): String? = (this as? Error)?.message
}

class JulesSdk private constructor(
    private val apiService: JulesApiService
) {
    val sources = SourcesClient(apiService)
    val sessions = SessionsClient(apiService)

    class SourcesClient(private val api: JulesApiService) {
        suspend fun listSources(pageToken: String? = null): JulesResult<ListSourcesResponse> = safeCall {
            api.listSources(pageToken)
        }
    }

    class SessionsClient(private val api: JulesApiService) {
        suspend fun listSessions(
            pageSize: Int = 30,
            pageToken: String? = null
        ): JulesResult<ListSessionsResponse> = safeCall {
            api.listSessions(pageSize, pageToken)
        }

        suspend fun createSession(
            prompt: String,
            sourceName: String,
            branch: String? = null,
            requirePlanApproval: Boolean = false,
            title: String? = null
        ): JulesResult<Session> = safeCall {
            val request = CreateSessionRequest(
                prompt = prompt,
                sourceContext = SourceContext(
                    source = sourceName,
                    githubRepoContext = branch?.let { GithubRepoContext(startingBranch = it) }
                ),
                requirePlanApproval = requirePlanApproval,
                title = title
            )
            api.createSession(request)
        }

        suspend fun getSession(sessionId: String): JulesResult<Session> = safeCall {
            val cleanId = sessionId.substringAfterLast("/")
            api.getSession(cleanId)
        }

        suspend fun deleteSession(sessionId: String): JulesResult<Unit> = safeCall {
            val cleanId = sessionId.substringAfterLast("/")
            val response = api.deleteSession(cleanId)
            if (!response.isSuccessful && response.code() != 200 && response.code() != 204) {
                throw Exception("Failed to delete session: HTTP ${response.code()}")
            }
        }

        suspend fun listActivities(
            sessionId: String,
            pageToken: String? = null
        ): JulesResult<ListActivitiesResponse> = safeCall {
            val cleanId = sessionId.substringAfterLast("/")
            api.listActivities(cleanId, pageToken)
        }

        suspend fun sendMessage(
            sessionId: String,
            content: String
        ): JulesResult<Activity> = safeCall {
            val cleanId = sessionId.substringAfterLast("/")
            api.sendMessage(cleanId, SendMessageRequest(Message(content)))
        }

        suspend fun approvePlan(
            sessionId: String,
            planId: String? = null
        ): JulesResult<Activity> = safeCall {
            val cleanId = sessionId.substringAfterLast("/")
            api.approvePlan(cleanId, ApprovePlanRequest(planId))
        }
    }

    class Builder {
        private var apiKey: String = ""
        private var baseUrl: String = DEFAULT_BASE_URL
        private var timeoutSeconds: Long = 45

        fun apiKey(key: String) = apply { this.apiKey = key }
        fun baseUrl(url: String) = apply { this.baseUrl = url }
        fun timeout(seconds: Long) = apply { this.timeoutSeconds = seconds }

        fun build(): JulesSdk {
            val authInterceptor = Interceptor { chain ->
                val original = chain.request()
                val requestBuilder = original.newBuilder()
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")

                if (apiKey.isNotBlank()) {
                    requestBuilder.header("X-Goog-Api-Key", apiKey.trim())
                }

                chain.proceed(requestBuilder.build())
            }

            val logging = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.NONE
            }

            val okHttpClient = OkHttpClient.Builder()
                .addInterceptor(authInterceptor)
                .addInterceptor(logging)
                .connectTimeout(timeoutSeconds, TimeUnit.SECONDS)
                .readTimeout(timeoutSeconds, TimeUnit.SECONDS)
                .writeTimeout(timeoutSeconds, TimeUnit.SECONDS)
                .build()

            val moshi = Moshi.Builder()
                .addLast(KotlinJsonAdapterFactory())
                .build()

            val retrofit = Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(okHttpClient)
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .build()

            val apiService = retrofit.create(JulesApiService::class.java)
            return JulesSdk(apiService)
        }
    }

    companion object {
        private const val DEFAULT_BASE_URL = "https://jules.googleapis.com/"

        suspend fun <T> safeCall(call: suspend () -> T): JulesResult<T> = withContext(Dispatchers.IO) {
            try {
                JulesResult.Success(call())
            } catch (e: retrofit2.HttpException) {
                val code = e.code()
                val message = when (code) {
                    401, 403 -> "Invalid or missing Jules API key. Please check your key in Settings."
                    404 -> "Requested session or source not found."
                    429 -> "Rate limit reached. Please wait a moment before trying again."
                    500, 502, 503 -> "Jules server error (${code}). Please try again shortly."
                    else -> e.message() ?: "HTTP error $code"
                }
                JulesResult.Error(message = message, code = code, cause = e)
            } catch (e: Exception) {
                JulesResult.Error(
                    message = e.localizedMessage ?: "Network error. Please check your internet connection.",
                    cause = e
                )
            }
        }
    }
}
