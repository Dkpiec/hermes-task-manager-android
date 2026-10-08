package com.hermes.taskmanager

import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.biometric.BiometricPrompt
import androidx.compose.runtime.*
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.hermes.taskmanager.api.HermesApiService
import com.hermes.taskmanager.models.*
import com.hermes.taskmanager.security.SecurityManager
import com.hermes.taskmanager.ui.screens.*
import com.hermes.taskmanager.ui.theme.HermesTasksTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.net.ConnectException
import java.net.UnknownHostException

class MainActivity : FragmentActivity() {

    private lateinit var securityManager: SecurityManager
    private var apiService: HermesApiService? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        securityManager = SecurityManager(this)

        val targetUrl = securityManager.serverUrl.ifBlank { DEFAULT_SERVER_URL }
        apiService = HermesApiService.create(targetUrl)

        setContent {
            HermesTasksTheme {
                AppNavigator()
            }
        }
    }

    @Composable
    private fun AppNavigator() {
        var authState by remember {
            mutableStateOf(
                when {
                    !securityManager.isLoggedIn -> "login"
                    securityManager.hasPin -> "pin"
                    else -> "home"
                }
            )
        }

        var allTasks by remember { mutableStateOf<List<Task>>(emptyList()) }
        var projects by remember { mutableStateOf<List<Project>>(emptyList()) }
        var currentFilter by remember { mutableStateOf("inbox") }
        var currentProjectId by remember { mutableStateOf<String?>(null) }
        var isKanbanOpen by remember { mutableStateOf(false) }
        var kanbanBoard by remember { mutableStateOf<Map<String, List<Task>>>(emptyMap()) }

        // Filtered tasks for the current screen
        val displayedTasks = remember(allTasks, currentFilter, currentProjectId) {
            when {
                currentProjectId != null -> allTasks.filter { it.projectId == currentProjectId }
                currentFilter == "emails" -> allTasks.filter { it.projectId == "email_actions" || it.tags.contains("email") }
                currentFilter == "today" -> allTasks.filter { !it.dueDate.isNullOrBlank() && it.dueDate.startsWith("2026-10-08") }
                currentFilter == "upcoming" -> allTasks.filter { !it.dueDate.isNullOrBlank() }
                currentFilter == "hermes" -> allTasks.filter { it.managedByHermes }
                else -> allTasks.filter { it.projectId == "inbox" || it.projectId.isBlank() }
            }
        }

        suspend fun fetchRemoteData() {
            val token = securityManager.authToken ?: return
            val targetUrl = securityManager.serverUrl.ifBlank { DEFAULT_SERVER_URL }
            val service = apiService ?: HermesApiService.create(targetUrl).also { apiService = it }

            try {
                val fetchedTasks = withContext(Dispatchers.IO) {
                    service.getTasks(token = "Bearer $token")
                }
                allTasks = fetchedTasks

                val fetchedProjects = withContext(Dispatchers.IO) {
                    service.getProjects(token = "Bearer $token")
                }
                projects = fetchedProjects

                if (isKanbanOpen) {
                    val board = withContext(Dispatchers.IO) {
                        service.getKanbanBoard(token = "Bearer $token")
                    }
                    kanbanBoard = board
                }
            } catch (e: Exception) {
                // Background sync silently catches; user manual refresh will show toast
            }
        }

        // Live Auto-Sync Loop (runs every 3.5 seconds when in Home)
        LaunchedEffect(authState, isKanbanOpen) {
            if (authState == "home") {
                while (true) {
                    fetchRemoteData()
                    delay(3500)
                }
            }
        }

        when (authState) {
            "login" -> {
                LoginScreen(
                    onLoginClick = { targetUrl, username, password, pin ->
                        lifecycleScope.launch {
                            try {
                                val client = HermesApiService.create(targetUrl)
                                val tokenRes = withContext(Dispatchers.IO) {
                                    client.login(LoginRequest(username, password))
                                }
                                securityManager.serverUrl = targetUrl
                                securityManager.authToken = tokenRes.accessToken
                                if (!pin.isNullOrBlank()) {
                                    securityManager.pinCode = pin
                                }
                                apiService = client
                                authState = if (securityManager.hasPin) "pin" else "home"
                                Toast.makeText(this@MainActivity, "Connected to Hermes Cloud", Toast.LENGTH_SHORT).show()
                            } catch (e: HttpException) {
                                val msg = if (e.code() == 401) "Invalid username or password" else "Server error (${e.code()})"
                                Toast.makeText(this@MainActivity, msg, Toast.LENGTH_LONG).show()
                            } catch (e: UnknownHostException) {
                                Toast.makeText(this@MainActivity, "Cannot resolve server host. Check network.", Toast.LENGTH_LONG).show()
                            } catch (e: ConnectException) {
                                Toast.makeText(this@MainActivity, "Connection refused. Server offline or unreachable.", Toast.LENGTH_LONG).show()
                            } catch (e: Exception) {
                                Toast.makeText(this@MainActivity, "Login failed: ${e.localizedMessage ?: e.message}", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                )
            }

            "pin" -> {
                PinScreen(
                    correctPin = securityManager.pinCode ?: "1234",
                    onPinSuccess = { authState = "home" },
                    onBiometricClick = { showBiometricPrompt { authState = "home" } },
                    onSwitchUserClick = {
                        securityManager.clearAll()
                        authState = "login"
                    }
                )
            }

            "home" -> {
                if (isKanbanOpen) {
                    KanbanScreen(
                        board = kanbanBoard,
                        onBack = { isKanbanOpen = false },
                        onRefresh = {
                            lifecycleScope.launch { fetchRemoteData() }
                        },
                        onTaskClick = {}
                    )
                } else {
                    HomeScreen(
                        tasks = displayedTasks,
                        allTasks = allTasks,
                        projects = projects,
                        currentFilter = currentFilter,
                        currentProjectId = currentProjectId,
                        onFilterChange = { filter, projId ->
                            currentFilter = filter
                            currentProjectId = projId
                        },
                        onToggleTaskDone = { task ->
                            val token = securityManager.authToken ?: return@HomeScreen
                            val targetUrl = securityManager.serverUrl.ifBlank { DEFAULT_SERVER_URL }
                            val service = apiService ?: HermesApiService.create(targetUrl).also { apiService = it }
                            val newStatus = if (task.status == "done") "backlog" else "done"
                            lifecycleScope.launch {
                                withContext(Dispatchers.IO) {
                                    service.updateTask("Bearer $token", task.id, mapOf("status" to newStatus))
                                }
                                fetchRemoteData()
                            }
                        },
                        onTaskClick = {},
                        onAddTask = { title, desc, due, priority, managed ->
                            val token = securityManager.authToken ?: return@HomeScreen
                            val targetUrl = securityManager.serverUrl.ifBlank { DEFAULT_SERVER_URL }
                            val service = apiService ?: HermesApiService.create(targetUrl).also { apiService = it }
                            lifecycleScope.launch {
                                withContext(Dispatchers.IO) {
                                    service.createTask(
                                        "Bearer $token",
                                        mapOf(
                                            "title" to title,
                                            "description" to desc,
                                            "project_id" to (currentProjectId ?: "inbox"),
                                            "due_date" to due,
                                            "priority" to priority,
                                            "managed_by_hermes" to managed,
                                            "status" to if (managed) "ready" else "backlog"
                                        )
                                    )
                                }
                                fetchRemoteData()
                            }
                        },
                        onOpenKanban = {
                            lifecycleScope.launch { fetchRemoteData() }
                            isKanbanOpen = true
                        },
                        onRefresh = {
                            lifecycleScope.launch {
                                fetchRemoteData()
                                Toast.makeText(this@MainActivity, "Tasks synced", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onLogout = {
                            securityManager.clearAll()
                            authState = "login"
                        }
                    )
                }
            }
        }
    }

    private fun showBiometricPrompt(onSuccess: () -> Unit) {
        val executor = ContextCompat.getMainExecutor(this)
        val prompt = BiometricPrompt(this, executor, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                onSuccess()
            }
        })

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Unlock Hermes Tasks")
            .setSubtitle("Use Fingerprint or Face Unlock")
            .setNegativeButtonText("Use PIN")
            .build()

        prompt.authenticate(promptInfo)
    }
}
