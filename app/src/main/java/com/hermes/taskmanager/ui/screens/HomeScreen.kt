package com.hermes.taskmanager.ui.screens

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hermes.taskmanager.models.Project
import com.hermes.taskmanager.models.Task
import com.hermes.taskmanager.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    tasks: List<Task>,
    allTasks: List<Task>,
    projects: List<Project>,
    currentFilter: String,
    currentProjectId: String?,
    onFilterChange: (filter: String, projectId: String?) -> Unit,
    onToggleTaskDone: (Task) -> Unit,
    onTaskClick: (Task) -> Unit,
    onAddTask: (title: String, desc: String?, dueDate: String?, priority: Int, managed: Boolean) -> Unit,
    onOpenKanban: () -> Unit,
    onRefresh: () -> Unit,
    onLogout: () -> Unit
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var showAddSheet by remember { mutableStateOf(false) }

    // Live counts for sidebar badges
    val inboxCount = allTasks.count { it.status != "done" && it.projectId == "inbox" }
    val todayCount = allTasks.count { it.status != "done" && !it.dueDate.isNullOrBlank() && it.dueDate.startsWith("2026-10-08") }
    val upcomingCount = allTasks.count { it.status != "done" && !it.dueDate.isNullOrBlank() }
    val hermesCount = allTasks.count { it.status != "done" && it.managedByHermes }
    val emailCount = allTasks.count { it.status != "done" && (it.projectId == "email_actions" || it.tags.contains("email")) }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.width(300.dp),
                drawerContainerColor = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    // Drawer Header
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(TodoistRed),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Bolt,
                                contentDescription = "Logo",
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Hermes Tasks",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF10B981))
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Live Cloud Sync",
                                    fontSize = 10.sp,
                                    color = Color(0xFF10B981),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                    Spacer(modifier = Modifier.height(12.dp))

                    // Core Navigation Views
                    DrawerItem(
                        icon = Icons.Default.Inbox,
                        title = "Inbox",
                        count = inboxCount,
                        isSelected = currentFilter == "inbox" && currentProjectId == null,
                        onClick = {
                            onFilterChange("inbox", null)
                            scope.launch { drawerState.close() }
                        }
                    )
                    DrawerItem(
                        icon = Icons.Default.Today,
                        title = "Today",
                        count = todayCount,
                        isSelected = currentFilter == "today",
                        onClick = {
                            onFilterChange("today", null)
                            scope.launch { drawerState.close() }
                        }
                    )
                    DrawerItem(
                        icon = Icons.Default.DateRange,
                        title = "Upcoming",
                        count = upcomingCount,
                        isSelected = currentFilter == "upcoming",
                        onClick = {
                            onFilterChange("upcoming", null)
                            scope.launch { drawerState.close() }
                        }
                    )
                    DrawerItem(
                        icon = Icons.Default.SmartToy,
                        title = "Managed by Hermes",
                        count = hermesCount,
                        isSelected = currentFilter == "hermes",
                        accentTint = TodoistRed,
                        onClick = {
                            onFilterChange("hermes", null)
                            scope.launch { drawerState.close() }
                        }
                    )
                    DrawerItem(
                        icon = Icons.Default.Email,
                        title = "Email Action Items",
                        count = emailCount,
                        isSelected = currentFilter == "emails" || currentProjectId == "email_actions",
                        accentTint = Color(0xFF2563EB),
                        onClick = {
                            onFilterChange("emails", "email_actions")
                            scope.launch { drawerState.close() }
                        }
                    )
                    DrawerItem(
                        icon = Icons.Default.ViewKanban,
                        title = "Kanban Board",
                        badgeText = "BOARD",
                        isSelected = false,
                        onClick = {
                            scope.launch { drawerState.close() }
                            onOpenKanban()
                        }
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "PROJECTS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )

                    projects.forEach { proj ->
                        DrawerItem(
                            icon = Icons.Default.Folder,
                            title = proj.name,
                            isSelected = currentProjectId == proj.id,
                            onClick = {
                                onFilterChange("project", proj.id)
                                scope.launch { drawerState.close() }
                            }
                        )
                    }

                    Spacer(modifier = Modifier.weight(1f))
                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                    Spacer(modifier = Modifier.height(12.dp))

                    // Bottom User / Logout item
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onLogout() }
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("D", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("dhar", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            Text("Lock / Logout", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                        }
                        Icon(Icons.Default.Logout, contentDescription = "Logout", tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f), modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = when {
                                    currentProjectId != null -> projects.firstOrNull { it.id == currentProjectId }?.name ?: "Project"
                                    currentFilter == "today" -> "Today"
                                    currentFilter == "upcoming" -> "Upcoming"
                                    currentFilter == "hermes" -> "Managed by Hermes"
                                    currentFilter == "emails" -> "Email Action Items"
                                    else -> "Inbox"
                                },
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                            Text(
                                text = "${tasks.count { it.status != "done" }} pending tasks",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Open Navigation Menu", tint = TodoistRed)
                        }
                    },
                    actions = {
                        IconButton(onClick = onOpenKanban) {
                            Icon(Icons.Default.ViewKanban, contentDescription = "Kanban Board", tint = TodoistRed)
                        }
                        IconButton(onClick = onRefresh) {
                            Icon(Icons.Default.Refresh, contentDescription = "Sync")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = { showAddSheet = true },
                    containerColor = TodoistRed,
                    contentColor = Color.White,
                    shape = CircleShape
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Task")
                }
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(MaterialTheme.colorScheme.background)
            ) {
                if (tasks.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "All clear in this view",
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(tasks, key = { it.id }) { task ->
                            TodoistTaskItem(
                                task = task,
                                onToggleDone = { onToggleTaskDone(task) },
                                onClick = { onTaskClick(task) }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAddSheet) {
        QuickAddTaskBottomSheet(
            onDismiss = { showAddSheet = false },
            onSave = { title, desc, due, priority, managed ->
                onAddTask(title, desc, due, priority, managed)
                showAddSheet = false
            }
        )
    }
}

@Composable
fun DrawerItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    count: Int? = null,
    badgeText: String? = null,
    isSelected: Boolean,
    accentTint: Color? = null,
    onClick: () -> Unit
) {
    val bg = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) else Color.Transparent
    val textCol = if (isSelected) TodoistRed else MaterialTheme.colorScheme.onSurface

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = accentTint ?: (if (isSelected) TodoistRed else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)),
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = title,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            fontSize = 14.sp,
            color = textCol,
            modifier = Modifier.weight(1f)
        )
        if (count != null && count > 0) {
            Text(
                text = count.toString(),
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
        } else if (badgeText != null) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFFFEF3C7))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = badgeText,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFD97706)
                )
            }
        }
    }
}

@Composable
fun TodoistTaskItem(
    task: Task,
    onToggleDone: () -> Unit,
    onClick: () -> Unit
) {
    val isDone = task.status == "done"
    val priorityColor = when (task.priority) {
        1 -> PriorityP1
        2 -> PriorityP2
        3 -> PriorityP3
        else -> PriorityP4
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Circular Checkbox
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .border(2.dp, priorityColor, CircleShape)
                    .background(if (isDone) priorityColor else Color.Transparent)
                    .clickable { onToggleDone() },
                contentAlignment = Alignment.Center
            ) {
                if (isDone) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Done",
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.title,
                    fontWeight = FontWeight.Medium,
                    fontSize = 15.sp,
                    color = if (isDone) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f) else MaterialTheme.colorScheme.onSurface,
                    textDecoration = if (isDone) TextDecoration.LineThrough else TextDecoration.None
                )

                if (!task.description.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = task.description,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Metadata Chips row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Due date chip
                    if (!task.dueDate.isNullOrBlank()) {
                        val isOverdue = task.dueDate.compareTo("2026-10-08") < 0 && !isDone
                        val isToday = task.dueDate.startsWith("2026-10-08")
                        val chipBg = if (isOverdue) Color(0xFFFEE2E2) else if (isToday) Color(0xFFFEF3C7) else Color(0xFFF3F4F6)
                        val chipText = if (isOverdue) Color(0xFFDC2626) else if (isToday) Color(0xFFD97706) else Color(0xFF4B5563)

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(chipBg)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = if (isToday) "Today" else task.dueDate,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = chipText
                            )
                        }
                    }

                    // Hermes Managed Badge
                    if (task.managedByHermes) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFFEDE9FE))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "🤖 Hermes: ${task.status.replace("_", " ").uppercase()}",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF7C3AED)
                            )
                        }
                    }

                    // Attachments counter
                    if (task.attachmentsCount > 0) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AttachFile, contentDescription = null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                            Text(text = task.attachmentsCount.toString(), fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickAddTaskBottomSheet(
    onDismiss: () -> Unit,
    onSave: (title: String, desc: String?, due: String?, priority: Int, managed: Boolean) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var priority by remember { mutableStateOf(4) }
    var dueDate by remember { mutableStateOf<String?>(null) }
    var managedByHermes by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
                .navigationBarsPadding()
        ) {
            Text("Add Task", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                placeholder = { Text("Task name e.g., Verify BOQ quantities") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = desc,
                onValueChange = { desc = it },
                placeholder = { Text("Description or instructions for Hermes...") },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 3
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Quick Filters / Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = dueDate != null,
                    onClick = { dueDate = if (dueDate == null) "2026-10-08" else null },
                    label = { Text(if (dueDate != null) "Today" else "Set Due") },
                    leadingIcon = { Icon(Icons.Default.Today, contentDescription = null, modifier = Modifier.size(16.dp)) }
                )

                FilterChip(
                    selected = managedByHermes,
                    onClick = { managedByHermes = !managedByHermes },
                    label = { Text("🤖 Hermes Auto-Run") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFFEDE9FE),
                        selectedLabelColor = Color(0xFF7C3AED)
                    )
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        if (title.isNotBlank()) {
                            onSave(title, desc.ifBlank { null }, dueDate, priority, managedByHermes)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TodoistRed),
                    enabled = title.isNotBlank()
                ) {
                    Text("Add Task", color = Color.White)
                }
            }
        }
    }
}
