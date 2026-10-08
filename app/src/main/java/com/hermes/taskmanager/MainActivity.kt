package com.hermes.taskmanager

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
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
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : FragmentActivity() {

    private lateinit var securityManager: SecurityManager
    private var apiService: HermesApiService? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        securityManager = SecurityManager(this)

        if (securityManager.isLoggedIn) {
            apiService = HermesApiService.create(securityManager.serverUrl)
        }

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

        var tasks by remember { mutableStateOf<List<Task>>(emptyList()) }
        var currentFilter by remember { mutableStateOf("inbox") }
        var isKanbanOpen by remember { mutableStateOf(false) }
        var kanbanBoard by remember { mutableStateOf<Map<String, List<Task>>>(emptyMap()) }

        fun refreshTasks() {
            val token = securityManager.authToken ?: return
            val service = apiService ?: return
            lifecycleScope.launch {
                try {
                    val res = withContext(Dispatchers.IO) {
                        service.getTasks(
                            token = "Bearer $token",
                            dueFilter = if (currentFilter in listOf("today", "upcoming")) currentFilter else null,
                            managed = if (currentFilter == "hermes") true else null
                        )
                    }
                    tasks = res
                } catch (e: Exception) {
                    // Fallback
                }
            }
        }

        fun refreshKanban() {
            val token = securityManager.authToken ?: return
            val service = apiService ?: return
            lifecycleScope.launch {
                try {
                    val board = withContext(Dispatchers.IO) {
                        service.getKanbanBoard("Bearer $token")
                    }
                    kanbanBoard = board
                } catch (e: Exception) {}
            }
        }

        LaunchedEffect(authState, currentFilter) {
            if (authState == "home") {
                refreshTasks()
            }
        }

        when (authState) {
            "login" -> {
                LoginScreen(
                    initialServerUrl = securityManager.serverUrl,
                    onLoginSuccess = { serverUrl, triggerPayload, pin ->
                        val parts = triggerPayload.split(":")
                        if (parts.size >= 3) {
                            val user = parts[1]
                            val pass = parts[2]
                            lifecycleScope.launch {
                                try {
                                    val client = HermesApiService.create(serverUrl)
                                    val tokenRes = withContext(Dispatchers.IO) {
                                        client.login(LoginRequest(user, pass))
                                    }
                                    securityManager.serverUrl = serverUrl
                                    securityManager.authToken = tokenRes.accessToken
                                    if (!pin.isNullOrBlank()) {
                                        securityManager.pinCode = pin
                                    }
                                    apiService = client
                                    authState = if (securityManager.hasPin) "pin" else "home"
                                } catch (e: Exception) {
                                    Toast.makeText(this@MainActivity, "Login failed: ${e.message}", Toast.LENGTH_LONG).show()
                                }
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
                        onRefresh = { refreshKanban() },
                        onTaskClick = {}
                    )
                } else {
                    HomeScreen(
                        tasks = tasks,
                        currentFilter = currentFilter,
                        onFilterChange = { currentFilter = it },
                        onToggleTaskDone = { task ->
                            val token = securityManager.authToken ?: return@HomeScreen
                            val service = apiService ?: return@HomeScreen
                            val newStatus = if (task.status == "done") "backlog" else "done"
                            lifecycleScope.launch {
                                withContext(Dispatchers.IO) {
                                    service.updateTask("Bearer $token", task.id, mapOf("status" to newStatus))
                                }
                                refreshTasks()
                            }
                        },
                        onTaskClick = {},
                        onAddTask = { title, desc, due, priority, managed ->
                            val token = securityManager.authToken ?: return@HomeScreen
                            val service = apiService ?: return@HomeScreen
                            lifecycleScope.launch {
                                withContext(Dispatchers.IO) {
                                    service.createTask(
                                        "Bearer $token",
                                        mapOf(
                                            "title" to title,
                                            "description" to desc,
                                            "due_date" to due,
                                            "priority" to priority,
                                            "managed_by_hermes" to managed,
                                            "status" to if (managed) "ready" else "backlog"
                                        )
                                    )
                                }
                                refreshTasks()
                            }
                        },
                        onOpenKanban = {
                            refreshKanban()
                            isKanbanOpen = true
                        },
                        onRefresh = { refreshTasks() }
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
