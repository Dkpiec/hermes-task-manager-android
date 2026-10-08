package com.hermes.taskmanager.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hermes.taskmanager.models.Task
import com.hermes.taskmanager.ui.theme.TodoistRed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KanbanScreen(
    board: Map<String, List<Task>>,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onTaskClick: (Task) -> Unit
) {
    val columns = listOf(
        "backlog" to "Backlog",
        "ready" to "Ready",
        "in_progress" to "In Progress ⚡",
        "waiting_for_user" to "Action Needed ⚠️",
        "done" to "Done"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Kanban Board", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onRefresh) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        }
    ) { padding ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
                .horizontalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            for ((key, label) in columns) {
                val list = board[key] ?: emptyList()
                KanbanColumnView(
                    label = label,
                    tasks = list,
                    onTaskClick = onTaskClick
                )
            }
        }
    }
}

@Composable
fun KanbanColumnView(
    label: String,
    tasks: List<Task>,
    onTaskClick: (Task) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        modifier = Modifier
            .width(280.dp)
            .fillMaxHeight()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = label.uppercase(),
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.padding(2.dp)
                ) {
                    Text(
                        text = "${tasks.size}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(tasks, key = { it.id }) { task ->
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 1.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp)
                        ) {
                            Text(
                                text = task.title,
                                fontWeight = FontWeight.Medium,
                                fontSize = 13.sp
                            )
                            if (task.managedByHermes) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "🤖 HERMES",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TodoistRed
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
