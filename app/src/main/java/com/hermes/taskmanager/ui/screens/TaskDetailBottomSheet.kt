package com.hermes.taskmanager.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hermes.taskmanager.api.HermesApiService
import com.hermes.taskmanager.models.Task
import com.hermes.taskmanager.models.TaskAttachment
import com.hermes.taskmanager.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskDetailBottomSheet(
    taskId: String,
    token: String,
    serverUrl: String,
    apiService: HermesApiService,
    onDismiss: () -> Unit,
    onToggleDone: (Task) -> Unit,
    onDeleteTask: (Task) -> Unit,
    onTriggerHermes: (Task) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var task by remember { mutableStateOf<Task?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedEmailText by remember { mutableStateOf<String?>(null) }
    var isReadingEmail by remember { mutableStateOf(false) }

    // Fetch full task details (including attachments and logs)
    LaunchedEffect(taskId) {
        isLoading = true
        try {
            val fullTask = withContext(Dispatchers.IO) {
                apiService.getTask("Bearer $token", taskId)
            }
            task = fullTask
        } catch (e: Exception) {
            // fallback to current task
        } finally {
            isLoading = false
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = TodoistRed)
            }
        } else if (task == null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Task details not available", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
            }
        } else {
            val currentTask = task!!
            val isDone = currentTask.status == "done"

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Top Action Bar in Sheet
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Priority Badge
                            val (pColor, pText) = when (currentTask.priority) {
                                1 -> PriorityP1 to "P1 - Urgent"
                                2 -> PriorityP2 to "P2 - High"
                                3 -> PriorityP3 to "P3 - Medium"
                                else -> PriorityP4 to "P4 - Low"
                            }
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = pColor.copy(alpha = 0.12f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, pColor.copy(alpha = 0.3f))
                            ) {
                                Text(
                                    text = pText,
                                    color = pColor,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }

                            // Hermes Managed Badge
                            if (currentTask.managedByHermes) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = TodoistRed.copy(alpha = 0.1f)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.SmartToy,
                                            contentDescription = null,
                                            tint = TodoistRed,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Hermes Agent",
                                            color = TodoistRed,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }

                        // Delete button
                        IconButton(onClick = {
                            onDeleteTask(currentTask)
                            onDismiss()
                        }) {
                            Icon(
                                Icons.Default.DeleteOutline,
                                contentDescription = "Delete Task",
                                tint = Color(0xFFEF4444)
                            )
                        }
                    }
                }

                // Title
                item {
                    Text(
                        text = currentTask.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Metadata Chips (Due Date, Project)
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (!currentTask.dueDate.isNullOrBlank()) {
                            val due = currentTask.dueDate.split("T")[0]
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Icon(
                                        Icons.Default.CalendarToday,
                                        contentDescription = null,
                                        modifier = Modifier.size(12.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = due,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        if (currentTask.projectId == "email_actions" || currentTask.tags.contains("email")) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFF2563EB).copy(alpha = 0.1f)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Email,
                                        contentDescription = null,
                                        modifier = Modifier.size(12.dp),
                                        tint = Color(0xFF2563EB)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "dharmendrapal@omaxe.com",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF2563EB)
                                    )
                                }
                            }
                        }
                    }
                }

                // Description Box
                if (!currentTask.description.isNullOrBlank()) {
                    item {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "DESCRIPTION",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f))
                            ) {
                                Text(
                                    text = currentTask.description,
                                    fontSize = 14.sp,
                                    lineHeight = 20.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }
                    }
                }

                // Attachments Section
                if (currentTask.attachments.isNotEmpty()) {
                    item {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "ATTACHMENTS (${currentTask.attachments.size})",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }

                    items(currentTask.attachments) { att ->
                        AttachmentCard(
                            attachment = att,
                            serverUrl = serverUrl,
                            token = token,
                            apiService = apiService,
                            onReadEmail = { emailText ->
                                selectedEmailText = emailText
                            }
                        )
                    }
                }

                // Hermes Execution Trigger & Logs
                if (currentTask.managedByHermes) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "AUTONOMOUS EXECUTION",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                    letterSpacing = 1.sp
                                )

                                OutlinedButton(
                                    onClick = { onTriggerHermes(currentTask) },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TodoistRed),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, TodoistRed.copy(alpha = 0.5f)),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Run with Hermes", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            if (currentTask.logs.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFF1E1E24)
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        currentTask.logs.takeLast(6).forEach { log ->
                                            Text(
                                                text = "${log.timestamp.takeLast(8)} [${log.logLevel}] ${log.message}",
                                                fontSize = 11.sp,
                                                fontFamily = FontFamily.Monospace,
                                                color = when (log.logLevel) {
                                                    "ERROR" -> Color(0xFFEF4444)
                                                    "WARN" -> Color(0xFFF59E0B)
                                                    else -> Color(0xFF10B981)
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Completion Toggle Button
                item {
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = {
                            onToggleDone(currentTask)
                            onDismiss()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isDone) Color(0xFF4B5563) else TodoistRed
                        )
                    ) {
                        Icon(
                            imageVector = if (isDone) Icons.Default.Replay else Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isDone) "Reopen Task" else "Mark as Complete",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }

    // Modal to read full email copy
    if (selectedEmailText != null) {
        AlertDialog(
            onDismissRequest = { selectedEmailText = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Email, contentDescription = null, tint = Color(0xFF2563EB))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Email Copy", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp)
                ) {
                    item {
                        Text(
                            text = selectedEmailText ?: "",
                            fontSize = 13.sp,
                            lineHeight = 18.sp,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedEmailText = null }) {
                    Text("Close", color = TodoistRed, fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

@Composable
fun AttachmentCard(
    attachment: TaskAttachment,
    serverUrl: String,
    token: String,
    apiService: HermesApiService,
    onReadEmail: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isLoadingText by remember { mutableStateOf(false) }

    val isEmail = attachment.fileType == "email" || attachment.fileName.endsWith(".txt") || attachment.fileName.endsWith(".eml")
    val fileUrl = "${serverUrl.trimEnd('/')}/api/v1/attachments/${attachment.id}"

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                if (isEmail) {
                    isLoadingText = true
                    scope.launch {
                        try {
                            val res = withContext(Dispatchers.IO) {
                                apiService.getAttachmentContent("Bearer $token", attachment.id)
                            }
                            val text = res["content"]?.toString() ?: "No content available in attachment."
                            onReadEmail(text)
                        } catch (e: Exception) {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(fileUrl))
                            context.startActivity(intent)
                        } finally {
                            isLoadingText = false
                        }
                    }
                } else {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(fileUrl))
                    context.startActivity(intent)
                }
            },
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isEmail) Color(0xFF2563EB).copy(alpha = 0.15f) else TodoistRed.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isEmail) Icons.Default.Email else Icons.Default.Description,
                        contentDescription = null,
                        tint = if (isEmail) Color(0xFF2563EB) else TodoistRed,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = attachment.fileName,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1
                    )
                    Text(
                        text = if (isEmail) "Tap to read full email" else "${(attachment.fileSizeBytes / 1024).coerceAtLeast(1)} KB",
                        fontSize = 11.sp,
                        color = if (isEmail) Color(0xFF2563EB) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }

            Icon(
                imageVector = if (isEmail) Icons.Default.Visibility else Icons.Default.Download,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
