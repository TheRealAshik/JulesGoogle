package com.example.jules.sdk.api

import com.example.jules.sdk.models.Activity
import com.example.jules.sdk.models.ApprovePlanRequest
import com.example.jules.sdk.models.CreateSessionRequest
import com.example.jules.sdk.models.ListActivitiesResponse
import com.example.jules.sdk.models.ListSessionsResponse
import com.example.jules.sdk.models.ListSourcesResponse
import com.example.jules.sdk.models.SendMessageRequest
import com.example.jules.sdk.models.Session
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface JulesApiService {

    @GET("v1alpha/sources")
    suspend fun listSources(
        @Query("pageToken") pageToken: String? = null
    ): ListSourcesResponse

    @GET("v1alpha/sessions")
    suspend fun listSessions(
        @Query("pageSize") pageSize: Int = 30,
        @Query("pageToken") pageToken: String? = null
    ): ListSessionsResponse

    @POST("v1alpha/sessions")
    suspend fun createSession(
        @Body request: CreateSessionRequest
    ): Session

    @GET("v1alpha/sessions/{sessionId}")
    suspend fun getSession(
        @Path("sessionId") sessionId: String
    ): Session

    @DELETE("v1alpha/sessions/{sessionId}")
    suspend fun deleteSession(
        @Path("sessionId") sessionId: String
    ): Response<Unit>

    @GET("v1alpha/sessions/{sessionId}/activities")
    suspend fun listActivities(
        @Path("sessionId") sessionId: String,
        @Query("pageToken") pageToken: String? = null
    ): ListActivitiesResponse

    @POST("v1alpha/sessions/{sessionId}:sendMessage")
    suspend fun sendMessage(
        @Path("sessionId") sessionId: String,
        @Body request: SendMessageRequest
    ): Activity

    @POST("v1alpha/sessions/{sessionId}:approvePlan")
    suspend fun approvePlan(
        @Path("sessionId") sessionId: String,
        @Body request: ApprovePlanRequest
    ): Activity
}
