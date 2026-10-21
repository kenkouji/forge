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

    @POST("api/tasks/{task_id}/cancel")
    suspend fun cancelTask(
        @Header("Authorization") authHeader: String,
        @Path("task_id") taskId: String
    ): Response<TaskItem>

    @POST("api/tasks/{task_id}/retry")
    suspend fun retryTask(
        @Header("Authorization") authHeader: String,
        @Path("task_id") taskId: String
    ): Response<TaskItem>

    @DELETE("api/tasks/cleanup")
    suspend fun cleanupTasks(
        @Header("Authorization") authHeader: String,
        @Query("keep_count") keepCount: Int = 50
    ): Response<Map<String, Any>>


    @GET("api/automations")
    suspend fun getAutomations(
        @Header("Authorization") authHeader: String
    ): Response<List<AutomationItem>>

    @GET("api/automations/history")
    suspend fun getAutomationHistory(
        @Header("Authorization") authHeader: String
    ): Response<List<AutomationHistoryItem>>

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

    @GET("api/automations/templates")
    suspend fun getWorkflowTemplates(
        @Header("Authorization") authHeader: String
    ): Response<List<WorkflowDefinitionItem>>

    @POST("api/automations/dry-run")
    suspend fun dryRunWorkflow(
        @Header("Authorization") authHeader: String,
        @Body workflow: Map<String, Any>
    ): Response<WorkflowDryRunResponse>

    @POST("api/automations/{id}/dry-run")
    suspend fun dryRunSavedAutomation(
        @Header("Authorization") authHeader: String,
        @Path("id") id: String
    ): Response<WorkflowDryRunResponse>

    @POST("api/automations/executions/{id}/cancel")
    suspend fun cancelWorkflowExecution(
        @Header("Authorization") authHeader: String,
        @Path("id") id: String
    ): Response<Map<String, Any>>

    @GET("api/automations/executions")
    suspend fun getWorkflowExecutions(
        @Header("Authorization") authHeader: String
    ): Response<List<WorkflowExecutionItem>>

    @POST("api/stream/control/{id}")
    suspend fun controlScreenStream(
        @Header("Authorization") authHeader: String,
        @Path("id") id: String,
        @Body request: StreamControlRequest
    ): Response<Map<String, Any>>

    @GET("api/stream/status/{id}")
    suspend fun getScreenStreamStatus(
        @Header("Authorization") authHeader: String,
        @Path("id") id: String
    ): Response<StreamStatusResponse>

    // Universal Clipboard History Endpoints
    @GET("api/clipboard/history")
    suspend fun getClipboardHistory(
        @Header("Authorization") authHeader: String,
        @Query("limit") limit: Int = 50,
        @Query("type") type: String? = null
    ): Response<List<ClipboardItem>>

    @POST("api/clipboard/sync")
    suspend fun syncClipboardItem(
        @Header("Authorization") authHeader: String,
        @Body request: ClipboardSyncRequest
    ): Response<Map<String, Any>>

    @POST("api/clipboard/copy")
    suspend fun copyClipboardToDevice(
        @Header("Authorization") authHeader: String,
        @Body request: ClipboardCopyRequest
    ): Response<Map<String, Any>>

    @DELETE("api/clipboard/clear")
    suspend fun clearClipboardHistory(
        @Header("Authorization") authHeader: String
    ): Response<Map<String, Any>>

    @DELETE("api/clipboard/{item_id}")
    suspend fun deleteClipboardItem(
        @Header("Authorization") authHeader: String,
        @Path("item_id") itemId: String
    ): Response<Map<String, Any>>

    // Guest Access, Co-Control & Privacy Shield Endpoints
    @POST("api/guest/invitations")
    suspend fun createGuestInvitation(
        @Header("Authorization") authHeader: String,
        @Body request: GuestInvitationCreateRequest
    ): Response<GuestInvitationResponse>

    @GET("api/guest/invitations/{device_id}")
    suspend fun listGuestInvitations(
        @Header("Authorization") authHeader: String,
        @Path("device_id") deviceId: String
    ): Response<List<GuestInvitationResponse>>

    @DELETE("api/guest/invitations/{invitation_id}")
    suspend fun revokeGuestInvitation(
        @Header("Authorization") authHeader: String,
        @Path("invitation_id") invitationId: String
    ): Response<Map<String, Any>>

    @GET("api/guest/sessions/{device_id}")
    suspend fun listGuestSessions(
        @Header("Authorization") authHeader: String,
        @Path("device_id") deviceId: String
    ): Response<List<GuestSessionResponse>>

    @POST("api/guest/sessions/{session_id}/approve")
    suspend fun approveGuestSession(
        @Header("Authorization") authHeader: String,
        @Path("session_id") sessionId: String,
        @Body request: GuestApprovalRequest
    ): Response<GuestSessionResponse>

    @POST("api/guest/sessions/{session_id}/revoke")
    suspend fun revokeGuestSession(
        @Header("Authorization") authHeader: String,
        @Path("session_id") sessionId: String,
        @Query("reason") reason: String = "Revoked by owner"
    ): Response<Map<String, Any>>

    @GET("api/guest/privacy-shield/{device_id}")
    suspend fun getPrivacyShield(
        @Header("Authorization") authHeader: String,
        @Path("device_id") deviceId: String
    ): Response<PrivacyShieldResponse>

    @POST("api/guest/privacy-shield/{device_id}")
    suspend fun updatePrivacyShield(
        @Header("Authorization") authHeader: String,
        @Path("device_id") deviceId: String,
        @Body request: PrivacyShieldUpdateRequest
    ): Response<PrivacyShieldResponse>

    @GET("api/guest/staging/files")
    suspend fun listStagedFiles(
        @Header("Authorization") authHeader: String,
        @Query("device_id") deviceId: String? = null
    ): Response<List<StagedFileResponse>>

    @POST("api/guest/staging/promote/{file_id}")
    suspend fun promoteStagedFile(
        @Header("Authorization") authHeader: String,
        @Path("file_id") fileId: String,
        @Body request: PromoteFileRequest
    ): Response<StagedFileResponse>

    @DELETE("api/guest/staging/files/{file_id}")
    suspend fun deleteStagedFile(
        @Header("Authorization") authHeader: String,
        @Path("file_id") fileId: String
    ): Response<Map<String, Any>>
}
