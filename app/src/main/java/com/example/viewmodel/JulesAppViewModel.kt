package com.example.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.auth.AuthRepository
import com.example.data.firestore.FirestorePrompt
import com.example.data.firestore.FirestoreSyncManager
import com.example.data.preferences.UserPreferencesManager
import com.example.gemini.CopilotTurn
import com.example.gemini.GeminiCopilotService
import com.example.jules.sdk.JulesResult
import com.example.jules.sdk.JulesSdk
import com.example.jules.sdk.models.Activity
import com.example.jules.sdk.models.Session
import com.example.jules.sdk.models.Source
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class AppScreen {
    WELCOME_AUTH,
    CHAT_HOME,
    SESSION_DETAIL,
    SETTINGS
}

data class CuratedPrompt(
    val title: String,
    val description: String,
    val prompt: String,
    val iconName: String
)

class JulesAppViewModel(application: Application) : AndroidViewModel(application) {

    val prefs = UserPreferencesManager(application)
    val authRepo = AuthRepository(application)
    private val firestoreSync = FirestoreSyncManager(application)
    private val geminiService = GeminiCopilotService()

    private var julesSdk: JulesSdk = JulesSdk.Builder()
        .apiKey(prefs.getApiKey())
        .build()

    // Screen navigation
    private val _currentScreen = MutableStateFlow<AppScreen>(
        if (authRepo.currentUser != null || authRepo.isGuestMode.value) AppScreen.CHAT_HOME else AppScreen.WELCOME_AUTH
    )
    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

    // API Key State
    private val _apiKey = MutableStateFlow(prefs.getApiKey())
    val apiKey: StateFlow<String> = _apiKey.asStateFlow()

    private val _isApiKeyTesting = MutableStateFlow(false)
    val isApiKeyTesting: StateFlow<Boolean> = _isApiKeyTesting.asStateFlow()

    private val _apiKeyValidationMessage = MutableStateFlow<String?>(null)
    val apiKeyValidationMessage: StateFlow<String?> = _apiKeyValidationMessage.asStateFlow()

    val defaultSource = Source(
        name = "sources/github/TheRealAshik/Jules",
        githubRepo = com.example.jules.sdk.models.GithubRepo(
            owner = "TheRealAshik",
            repo = "Jules",
            branches = listOf("main", "dev")
        )
    )

    // Sources
    private val _sources = MutableStateFlow<List<Source>>(listOf(defaultSource))
    val sources: StateFlow<List<Source>> = _sources.asStateFlow()

    private val _selectedSource = MutableStateFlow<Source?>(
        prefs.getLastSource().takeIf { it.isNotBlank() }?.let { savedName ->
            Source(name = savedName)
        } ?: defaultSource
    )
    val selectedSource: StateFlow<Source?> = _selectedSource.asStateFlow()

    private val _selectedBranch = MutableStateFlow(prefs.getLastBranch().ifBlank { "main" })
    val selectedBranch: StateFlow<String> = _selectedBranch.asStateFlow()

    private val _isLoadingSources = MutableStateFlow(false)
    val isLoadingSources: StateFlow<Boolean> = _isLoadingSources.asStateFlow()

    // Sessions
    private val _sessions = MutableStateFlow<List<Session>>(emptyList())
    val sessions: StateFlow<List<Session>> = _sessions.asStateFlow()

    private val _isLoadingSessions = MutableStateFlow(false)
    val isLoadingSessions: StateFlow<Boolean> = _isLoadingSessions.asStateFlow()

    private val _selectedSession = MutableStateFlow<Session?>(null)
    val selectedSession: StateFlow<Session?> = _selectedSession.asStateFlow()

    private val _sessionSearchQuery = MutableStateFlow("")
    val sessionSearchQuery: StateFlow<String> = _sessionSearchQuery.asStateFlow()

    // Activities for Selected Session
    private val _activities = MutableStateFlow<List<Activity>>(emptyList())
    val activities: StateFlow<List<Activity>> = _activities.asStateFlow()

    private val _isLoadingActivities = MutableStateFlow(false)
    val isLoadingActivities: StateFlow<Boolean> = _isLoadingActivities.asStateFlow()

    private val _isSendingMessage = MutableStateFlow(false)
    val isSendingMessage: StateFlow<Boolean> = _isSendingMessage.asStateFlow()

    private val _isApprovingPlan = MutableStateFlow(false)
    val isApprovingPlan: StateFlow<Boolean> = _isApprovingPlan.asStateFlow()

    // Polling job for active session
    private var activityPollingJob: Job? = null

    // Gemini Copilot State
    private val _isCopilotMode = MutableStateFlow(false)
    val isCopilotMode: StateFlow<Boolean> = _isCopilotMode.asStateFlow()

    private val _copilotTurns = MutableStateFlow<List<CopilotTurn>>(emptyList())
    val copilotTurns: StateFlow<List<CopilotTurn>> = _copilotTurns.asStateFlow()

    private val _isCopilotThinking = MutableStateFlow(false)
    val isCopilotThinking: StateFlow<Boolean> = _isCopilotThinking.asStateFlow()

    // Prompt Library
    private val _savedPrompts = MutableStateFlow<List<FirestorePrompt>>(emptyList())
    val savedPrompts: StateFlow<List<FirestorePrompt>> = _savedPrompts.asStateFlow()

    val curatedPrompts = listOf(
        CuratedPrompt(
            title = "Review PR / Code",
            description = "Analyze changes, suggest fixes and improvements",
            prompt = "Review the recent pull request and provide a detailed analysis of code quality, security considerations, and potential edge cases.",
            iconName = "review"
        ),
        CuratedPrompt(
            title = "Create a session",
            description = "Start a new end-to-end coding task",
            prompt = "Implement a new feature to handle background notifications and offline data synchronization in this repository.",
            iconName = "code"
        ),
        CuratedPrompt(
            title = "Search sources",
            description = "Inspect connected GitHub repositories",
            prompt = "List all connected sources and examine the active branches and structure of our primary repository.",
            iconName = "search"
        ),
        CuratedPrompt(
            title = "Fix Issue & Plan",
            description = "Debug bug and create execution plan",
            prompt = "Investigate the reported issue with session state synchronization, outline a multi-step resolution plan, and wait for approval before making edits.",
            iconName = "bug"
        )
    )

    // User State
    private val _currentUser = MutableStateFlow<FirebaseUser?>(authRepo.currentUser)
    val currentUser: StateFlow<FirebaseUser?> = _currentUser.asStateFlow()

    private val _isGuest = MutableStateFlow(authRepo.isGuestMode.value)
    val isGuest: StateFlow<Boolean> = _isGuest.asStateFlow()

    init {
        viewModelScope.launch {
            authRepo.authStateFlow().collect { user ->
                _currentUser.value = user
                if (user != null) {
                    _isGuest.value = false
                    listenToUserPrompts(user.uid)
                }
            }
        }

        viewModelScope.launch {
            authRepo.isGuestMode.collect { guest ->
                _isGuest.value = guest
            }
        }

        viewModelScope.launch {
            prefs.apiKeyFlow.collect { key ->
                _apiKey.value = key
                julesSdk = JulesSdk.Builder().apiKey(key).build()
                if (key.isNotBlank()) {
                    loadSources()
                    loadSessions()
                }
            }
        }
    }

    fun navigateTo(screen: AppScreen) {
        _currentScreen.value = screen
    }

    fun continueAsGuest() {
        authRepo.setGuestMode(true)
        _isGuest.value = true
        _currentScreen.value = AppScreen.CHAT_HOME
    }

    fun setSessionSearchQuery(query: String) {
        _sessionSearchQuery.value = query
    }

    fun updateApiKey(newKey: String) {
        prefs.setApiKey(newKey)
        _apiKey.value = newKey
        julesSdk = JulesSdk.Builder().apiKey(newKey).build()
        _apiKeyValidationMessage.value = null
    }

    fun testApiKey(key: String) {
        val testSdk = JulesSdk.Builder().apiKey(key).build()
        _isApiKeyTesting.value = true
        _apiKeyValidationMessage.value = null

        viewModelScope.launch {
            when (val result = testSdk.sources.listSources()) {
                is JulesResult.Success -> {
                    _isApiKeyTesting.value = false
                    _apiKeyValidationMessage.value = "Success! API key is valid and connected to Google Jules."
                    updateApiKey(key)
                }
                is JulesResult.Error -> {
                    _isApiKeyTesting.value = false
                    _apiKeyValidationMessage.value = "Validation failed: ${result.message}"
                }
            }
        }
    }

    fun loadSources() {
        if (_apiKey.value.isBlank()) return
        _isLoadingSources.value = true
        viewModelScope.launch {
            when (val result = julesSdk.sources.listSources()) {
                is JulesResult.Success -> {
                    val list = result.data.sources
                    if (list.isNotEmpty()) {
                        _sources.value = list
                        val last = prefs.getLastSource()
                        val matched = list.firstOrNull { it.name == last }
                            ?: list.firstOrNull { it.name == _selectedSource.value?.name }
                            ?: list.first()
                        selectSource(matched)
                    } else {
                        // User has no connected repositories in Jules API yet.
                        // Ensure there is always a fallback default source so task creation never crashes or gets blocked!
                        if (_sources.value.isEmpty()) {
                            _sources.value = listOf(defaultSource)
                        }
                        if (_selectedSource.value == null) {
                            selectSource(defaultSource)
                        }
                    }
                }
                is JulesResult.Error -> {
                    Log.w("JulesViewModel", "Failed to load sources: ${result.message}")
                    if (_sources.value.isEmpty()) {
                        _sources.value = listOf(defaultSource)
                    }
                    if (_selectedSource.value == null) {
                        selectSource(defaultSource)
                    }
                }
            }
            _isLoadingSources.value = false
        }
    }

    fun selectSource(source: Source) {
        _selectedSource.value = source
        prefs.setLastSource(source.name)
        val branches = source.githubRepo?.branches ?: emptyList()
        val branch = if (branches.contains("main")) "main" else branches.firstOrNull() ?: "main"
        _selectedBranch.value = branch
        prefs.setLastBranch(branch)
    }

    fun addCustomSource(fullName: String, branch: String = "main") {
        val clean = fullName.trim().removePrefix("https://github.com/").removePrefix("github.com/").trim('/')
        if (clean.isBlank()) return
        val parts = clean.split('/')
        val owner = parts.getOrNull(0) ?: "owner"
        val repo = parts.getOrNull(1) ?: parts[0]
        val newSource = Source(
            name = "sources/github/$clean",
            githubRepo = com.example.jules.sdk.models.GithubRepo(
                owner = owner,
                repo = repo,
                branches = listOf(branch)
            )
        )
        val currentList = _sources.value.toMutableList()
        if (currentList.none { it.name == newSource.name }) {
            currentList.add(0, newSource)
            _sources.value = currentList
        }
        selectSource(newSource)
        selectBranch(branch)
    }

    fun selectBranch(branch: String) {
        _selectedBranch.value = branch
        prefs.setLastBranch(branch)
    }

    fun loadSessions() {
        if (_apiKey.value.isBlank()) return
        _isLoadingSessions.value = true
        viewModelScope.launch {
            when (val result = julesSdk.sessions.listSessions()) {
                is JulesResult.Success -> {
                    _sessions.value = result.data.sessions
                }
                is JulesResult.Error -> {
                    Log.w("JulesViewModel", "Failed to load sessions: ${result.message}")
                }
            }
            _isLoadingSessions.value = false
        }
    }

    fun selectSession(session: Session) {
        _selectedSession.value = session
        _isCopilotMode.value = false
        _currentScreen.value = AppScreen.SESSION_DETAIL
        loadActivities(session.cleanId)
        startActivityPolling(session.cleanId)
    }

    fun loadActivities(sessionId: String) {
        _isLoadingActivities.value = true
        viewModelScope.launch {
            when (val result = julesSdk.sessions.listActivities(sessionId)) {
                is JulesResult.Success -> {
                    _activities.value = result.data.activities
                }
                is JulesResult.Error -> {
                    Log.w("JulesViewModel", "Failed to load activities: ${result.message}")
                }
            }
            _isLoadingActivities.value = false
        }
    }

    private fun startActivityPolling(sessionId: String) {
        activityPollingJob?.cancel()
        activityPollingJob = viewModelScope.launch {
            while (isActive) {
                delay(8000)
                if (_selectedSession.value?.cleanId == sessionId && _currentScreen.value == AppScreen.SESSION_DETAIL) {
                    val result = julesSdk.sessions.listActivities(sessionId)
                    if (result is JulesResult.Success) {
                        _activities.value = result.data.activities
                    }
                } else {
                    break
                }
            }
        }
    }

    fun createNewTask(
        prompt: String,
        requirePlanApproval: Boolean = true,
        customTitle: String? = null,
        onError: (String) -> Unit = {},
        onSuccess: (Session) -> Unit = {}
    ) {
        val source = _selectedSource.value
            ?: _sources.value.firstOrNull()
            ?: defaultSource.also { selectSource(it) }

        if (_selectedSource.value == null) {
            selectSource(source)
        }

        viewModelScope.launch {
            val title = customTitle ?: if (prompt.length > 30) prompt.take(30) + "..." else prompt
            val branch = _selectedBranch.value.ifBlank { "main" }
            when (val result = julesSdk.sessions.createSession(
                prompt = prompt,
                sourceName = source.name,
                branch = branch,
                requirePlanApproval = requirePlanApproval,
                title = title
            )) {
                is JulesResult.Success -> {
                    val session = result.data
                    loadSessions()
                    selectSession(session)
                    onSuccess(session)
                }
                is JulesResult.Error -> {
                    val errorMsg = result.message
                    Log.e("JulesViewModel", "Failed to create session: $errorMsg")
                    onError(errorMsg)
                }
            }
        }
    }

    fun sendSessionMessage(text: String) {
        val session = _selectedSession.value ?: return
        if (text.isBlank() || _isSendingMessage.value) return

        _isSendingMessage.value = true
        viewModelScope.launch {
            // Optimistic message append
            val tempActivity = Activity(
                name = "local_temp",
                id = "temp_${System.currentTimeMillis()}",
                createTime = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(Date()),
                originator = "USER",
                activityType = "MESSAGE",
                message = com.example.jules.sdk.models.Message(content = text)
            )
            _activities.value = _activities.value + tempActivity

            when (val result = julesSdk.sessions.sendMessage(session.cleanId, text)) {
                is JulesResult.Success -> {
                    loadActivities(session.cleanId)
                }
                is JulesResult.Error -> {
                    Log.e("JulesViewModel", "Failed to send message: ${result.message}")
                }
            }
            _isSendingMessage.value = false
        }
    }

    fun approvePlan(planId: String? = null) {
        val session = _selectedSession.value ?: return
        if (_isApprovingPlan.value) return

        _isApprovingPlan.value = true
        viewModelScope.launch {
            when (val result = julesSdk.sessions.approvePlan(session.cleanId, planId)) {
                is JulesResult.Success -> {
                    loadActivities(session.cleanId)
                    // Refresh session details
                    val sessionRefresh = julesSdk.sessions.getSession(session.cleanId)
                    if (sessionRefresh is JulesResult.Success) {
                        _selectedSession.value = sessionRefresh.data
                    }
                }
                is JulesResult.Error -> {
                    Log.e("JulesViewModel", "Plan approval failed: ${result.message}")
                }
            }
            _isApprovingPlan.value = false
        }
    }

    fun deleteSession(sessionId: String, onDeleted: () -> Unit = {}) {
        viewModelScope.launch {
            when (val result = julesSdk.sessions.deleteSession(sessionId)) {
                is JulesResult.Success -> {
                    _sessions.value = _sessions.value.filter { it.cleanId != sessionId }
                    if (_selectedSession.value?.cleanId == sessionId) {
                        _selectedSession.value = null
                        _currentScreen.value = AppScreen.CHAT_HOME
                    }
                    onDeleted()
                }
                is JulesResult.Error -> {
                    Log.e("JulesViewModel", "Failed to delete session: ${result.message}")
                }
            }
        }
    }

    // Gemini Copilot Actions
    fun switchToCopilotMode() {
        _isCopilotMode.value = true
        _selectedSession.value = null
        _currentScreen.value = AppScreen.SESSION_DETAIL
        if (_copilotTurns.value.isEmpty()) {
            _copilotTurns.value = listOf(
                CopilotTurn(
                    isUser = false,
                    text = "Hello! I am your Jules Copilot powered by Gemini 3.1 Pro with High Thinking. " +
                            "I can help you architect solutions, generate prompts for your Jules sessions, or inspect connected repositories. How can I assist with your code today?"
                )
            )
        }
    }

    fun sendCopilotMessage(userText: String) {
        if (userText.isBlank() || _isCopilotThinking.value) return

        val newTurn = CopilotTurn(isUser = true, text = userText)
        _copilotTurns.value = _copilotTurns.value + newTurn
        _isCopilotThinking.value = true

        viewModelScope.launch {
            val responseResult = geminiService.chat(
                history = _copilotTurns.value.dropLast(1),
                userMessage = userText,
                modelName = prefs.getSelectedModel(),
                enableHighThinking = prefs.getHighThinking()
            )

            responseResult.onSuccess { response ->
                val modelTurn = CopilotTurn(
                    isUser = false,
                    text = response.replyText,
                    thinkingTrace = response.thinkingTrace,
                    toolCallName = response.pendingToolCall?.name,
                    toolCallArgs = response.pendingToolCall?.args
                )
                _copilotTurns.value = _copilotTurns.value + modelTurn

                // Handle tool calls if requested
                response.pendingToolCall?.let { toolCall ->
                    executeCopilotTool(toolCall.name, toolCall.args ?: emptyMap())
                }
            }.onFailure { error ->
                _copilotTurns.value = _copilotTurns.value + CopilotTurn(
                    isUser = false,
                    text = "Error communicating with Gemini: ${error.message ?: "Unknown error"}. Please check your GEMINI_API_KEY."
                )
            }
            _isCopilotThinking.value = false
        }
    }

    private fun executeCopilotTool(toolName: String, args: Map<String, Any?>) {
        viewModelScope.launch {
            when (toolName) {
                "create_jules_session" -> {
                    val prompt = args["prompt"] as? String ?: return@launch
                    val customSource = args["source"] as? String
                    if (customSource != null) {
                        addCustomSource(customSource)
                    }
                    val requireApproval = (args["require_plan_approval"] as? Boolean) ?: true

                    createNewTask(
                        prompt = prompt,
                        requirePlanApproval = requireApproval,
                        onError = { err ->
                            _copilotTurns.value = _copilotTurns.value + CopilotTurn(
                                isUser = false,
                                text = "Failed to create Jules session: $err"
                            )
                        },
                        onSuccess = { session ->
                            _copilotTurns.value = _copilotTurns.value + CopilotTurn(
                                isUser = false,
                                text = "Created Jules session: '${session.displayTitle}' (ID: ${session.cleanId}). Switched to session timeline."
                            )
                        }
                    )
                }
                "list_jules_sources" -> {
                    loadSources()
                    val sourcesList = _sources.value.joinToString("\n") { "• ${it.cleanName}" }
                    _copilotTurns.value = _copilotTurns.value + CopilotTurn(
                        isUser = false,
                        text = "Available connected repositories:\n$sourcesList"
                    )
                }
                "approve_jules_plan" -> {
                    val sessionId = args["session_id"] as? String ?: _selectedSession.value?.cleanId
                    if (sessionId != null) {
                        approvePlan()
                        _copilotTurns.value = _copilotTurns.value + CopilotTurn(
                            isUser = false,
                            text = "Execution plan for session '$sessionId' has been approved."
                        )
                    }
                }
            }
        }
    }

    private fun listenToUserPrompts(userId: String) {
        viewModelScope.launch {
            firestoreSync.observeSavedPrompts(userId).collect { list ->
                _savedPrompts.value = list
            }
        }
    }

    fun savePrompt(title: String, prompt: String, category: String = "Coding") {
        val user = _currentUser.value
        if (user != null) {
            viewModelScope.launch {
                firestoreSync.savePrompt(user.uid, title, prompt, category)
            }
        }
    }

    fun deletePrompt(promptId: String) {
        val user = _currentUser.value
        if (user != null) {
            viewModelScope.launch {
                firestoreSync.deletePrompt(user.uid, promptId)
            }
        }
    }

    fun signOut() {
        authRepo.signOut()
        _currentUser.value = null
        _isGuest.value = false
        _currentScreen.value = AppScreen.WELCOME_AUTH
    }

    override fun onCleared() {
        super.onCleared()
        activityPollingJob?.cancel()
    }
}
