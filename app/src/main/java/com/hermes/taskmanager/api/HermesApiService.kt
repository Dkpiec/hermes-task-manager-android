package com.hermes.taskmanager.api

import com.hermes.taskmanager.models.*
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import java.util.concurrent.TimeUnit

interface HermesApiService {

    @POST("api/v1/auth/login")
    suspend fun login(@Body req: LoginRequest): TokenResponse

    @GET("api/v1/auth/verify")
    suspend fun verifyToken(@Header("Authorization") token: String): Map<String, String>

    @GET("api/v1/projects")
    suspend fun getProjects(@Header("Authorization") token: String): List<Project>

    @GET("api/v1/tasks")
    suspend fun getTasks(
        @Header("Authorization") token: String,
        @Query("due_filter") dueFilter: String? = null,
        @Query("managed") managed: Boolean? = null,
        @Query("project_id") projectId: String? = null
    ): List<Task>

    @GET("api/v1/tasks/{id}")
    suspend fun getTask(
        @Header("Authorization") token: String,
        @Path("id") id: String
    ): Task

    @POST("api/v1/tasks")
    suspend fun createTask(
        @Header("Authorization") token: String,
        @Body task: Map<String, Any?>
    ): Task

    @PATCH("api/v1/tasks/{id}")
    suspend fun updateTask(
        @Header("Authorization") token: String,
        @Path("id") id: String,
        @Body updates: Map<String, Any?>
    ): Task

    @DELETE("api/v1/tasks/{id}")
    suspend fun deleteTask(
        @Header("Authorization") token: String,
        @Path("id") id: String
    ): Map<String, String>

    @GET("api/v1/kanban/board")
    suspend fun getKanbanBoard(
        @Header("Authorization") token: String,
        @Query("project_id") projectId: String? = null
    ): Map<String, List<Task>>

    @Multipart
    @POST("api/v1/tasks/{id}/attachments")
    suspend fun uploadAttachment(
        @Header("Authorization") token: String,
        @Path("id") id: String,
        @Part file: MultipartBody.Part
    ): TaskAttachment

    @POST("api/v1/tasks/{id}/trigger")
    suspend fun triggerTask(
        @Header("Authorization") token: String,
        @Path("id") id: String
    ): Map<String, Any>

    companion object {
        fun create(baseUrl: String): HermesApiService {
            val logging = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }
            val client = OkHttpClient.Builder()
                .addInterceptor(logging)
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .build()

            val cleanUrl = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
            return Retrofit.Builder()
                .baseUrl(cleanUrl)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(HermesApiService::class.java)
        }
    }
}
