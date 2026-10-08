package com.hermes.taskmanager.models

import com.google.gson.annotations.SerializedName

data class Task(
    @SerializedName("id") val id: String,
    @SerializedName("project_id") val projectId: String = "inbox",
    @SerializedName("title") val title: String,
    @SerializedName("description") val description: String? = null,
    @SerializedName("author") val author: String = "user",
    @SerializedName("managed_by_hermes") val managedByHermes: Boolean = false,
    @SerializedName("status") val status: String = "backlog", // backlog, ready, in_progress, waiting_for_user, review, done
    @SerializedName("priority") val priority: Int = 4,        // 1: P1, 2: P2, 3: P3, 4: P4
    @SerializedName("due_date") val dueDate: String? = null,
    @SerializedName("tags") val tags: List<String> = emptyList(),
    @SerializedName("execution_step") val executionStep: String? = null,
    @SerializedName("attachments_count") val attachmentsCount: Int = 0,
    @SerializedName("attachments") val attachments: List<TaskAttachment> = emptyList(),
    @SerializedName("logs") val logs: List<TaskLog> = emptyList(),
    @SerializedName("created_at") val createdAt: String? = null,
    @SerializedName("updated_at") val updatedAt: String? = null
)

data class TaskAttachment(
    @SerializedName("id") val id: String,
    @SerializedName("task_id") val taskId: String,
    @SerializedName("file_name") val fileName: String,
    @SerializedName("file_type") val fileType: String,
    @SerializedName("mime_type") val mimeType: String,
    @SerializedName("file_path") val filePath: String,
    @SerializedName("file_size_bytes") val fileSizeBytes: Long
)

data class TaskLog(
    @SerializedName("id") val id: Long,
    @SerializedName("task_id") val taskId: String,
    @SerializedName("log_level") val logLevel: String,
    @SerializedName("message") val message: String,
    @SerializedName("timestamp") val timestamp: String
)

data class Project(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("description") val description: String? = null,
    @SerializedName("color") val color: String = "#DB4C3F",
    @SerializedName("icon") val icon: String = "inbox"
)

data class LoginRequest(
    val username: String,
    val password: String
)

data class TokenResponse(
    @SerializedName("access_token") val accessToken: String,
    @SerializedName("username") val username: String,
    @SerializedName("full_name") val fullName: String? = null
)
