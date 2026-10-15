package com.remoteforge.app.data.api

import com.remoteforge.app.data.model.*
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

interface RemoteForgeApiService {

    @GET("api/health")
    suspend fun getHealth(): Response<HealthResponse>

    @POST("api/auth/login")
    suspend fun login(
        @Body request: LoginRequest
    ): Response<TokenResponse>

    @GET("api/devices")
    suspend fun getDevices(
        @Header("Authorization") authHeader: String
    ): Response<List<Device>>

    @GET("api/devices/{device_id}")
    suspend fun getDevice(
        @Header("Authorization") authHeader: String,
        @Path("device_id") deviceId: String
    ): Response<Device>

    @POST("api/devices/{device_id}/commands")
    suspend fun dispatchCommand(
        @Header("Authorization") authHeader: String,
        @Path("device_id") deviceId: String,
        @Body request: CommandRequest
    ): Response<CommandResponse>

    @GET("api/files/list")
    suspend fun listFiles(
        @Header("Authorization") authHeader: String,
        @Query("device_id") deviceId: String,
        @Query("path") path: String? = null
    ): Response<FileListResponse>

    @GET("api/files/search")
    suspend fun searchFiles(
        @Header("Authorization") authHeader: String,
        @Query("device_id") deviceId: String,
        @Query("query") query: String,
        @Query("directory") directory: String? = null
    ): Response<FileSearchResponse>

    @GET("api/files/preview")
    suspend fun previewFile(
        @Header("Authorization") authHeader: String,
        @Query("device_id") deviceId: String,
        @Query("path") path: String
    ): Response<FilePreviewResponse>

    @Streaming
    @GET("api/files/download")
    suspend fun downloadFile(
        @Header("Authorization") authHeader: String,
        @Query("device_id") deviceId: String,
        @Query("path") path: String,
        @Header("Range") range: String? = null,
        @Query("as_zip") asZip: Boolean = false
    ): Response<ResponseBody>

    @GET("api/files/hash")
    suspend fun getFileHash(
        @Header("Authorization") authHeader: String,
        @Query("device_id") deviceId: String,
        @Query("path") path: String
    ): Response<Map<String, Any>>

    @Multipart
    @POST("api/files/upload")
    suspend fun uploadFile(
        @Header("Authorization") authHeader: String,
        @Part("device_id") deviceId: RequestBody,
        @Part("target_directory") targetDirectory: RequestBody,
        @Part file: MultipartBody.Part
    ): Response<FileUploadResult>

    @GET("api/audit")
    suspend fun getAuditLogs(
        @Header("Authorization") authHeader: String,
        @Query("device_id") deviceId: String? = null,
        @Query("category") category: String? = "all",
        @Query("timeframe") timeframe: String? = null,
        @Query("days") days: Int? = null,
        @Query("limit") limit: Int = 100
    ): Response<List<AuditLogEntry>>

    @GET("api/audit/stats")
    suspend fun getAuditStats(
        @Header("Authorization") authHeader: String,
        @Query("device_id") deviceId: String? = null,
        @Query("days") days: Int? = 14
    ): Response<AuditStatsResponse>

    @POST("api/audit/clean")
    suspend fun cleanAuditLogs(
        @Header("Authorization") authHeader: String,
        @Body request: AuditCleanRequest
    ): Response<AuditCleanResponse>

    @POST("api/ai/chat")
    suspend fun chatWithAI(
        @Header("Authorization") authHeader: String,
        @Body request: AIChatRequest
    ): Response<AIChatResponse>

    @GET("api/tasks")
    suspend fun getTasks(
        @Header("Authorization") authHeader: String,
        @Query("limit") limit: Int = 20
    ): Response<List<TaskItem>>

    @GET("api/tasks/{task_id}")
    suspend fun getTask(
        @Header("Authorization") authHeader: String,
        @Path("task_id") taskId: String
    ): Response<TaskItem>

    @GET("api/automations")
    suspend fun getAutomations(
        @Header("Authorization") authHeader: String
    ): Response<List<AutomationItem>>

    @POST("api/automations")
    suspend fun createAutomation(
        @Header("Authorization") authHeader: String,
        @Body request: AutomationCreateRequest
    ): Response<AutomationItem>

    @PATCH("api/automations/{id}")
    suspend fun updateAutomation(
        @Header("Authorization") authHeader: String,
        @Path("id") id: String,
        @Body request: AutomationUpdateRequest
    ): Response<AutomationItem>

    @DELETE("api/automations/{id}")
    suspend fun discardAutomation(
        @Header("Authorization") authHeader: String,
        @Path("id") id: String
    ): Response<Map<String, Any>>

    @POST("api/automations/{id}/run")
    suspend fun runAutomationNow(
        @Header("Authorization") authHeader: String,
        @Path("id") id: String
    ): Response<Map<String, Any>>

    @GET("api/automations/history")
    suspend fun getAutomationHistory(
        @Header("Authorization") authHeader: String,
        @Query("limit") limit: Int = 50
    ): Response<List<AutomationHistoryItem>>
}
