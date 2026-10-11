package com.example.data.preferences

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class UserPreferencesManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("jules_user_prefs", Context.MODE_PRIVATE)

    private val _apiKeyFlow = MutableStateFlow(getApiKey())
    val apiKeyFlow: StateFlow<String> = _apiKeyFlow.asStateFlow()

    private val _themeModeFlow = MutableStateFlow(getThemeMode())
    val themeModeFlow: StateFlow<String> = _themeModeFlow.asStateFlow()

    private val _selectedModelFlow = MutableStateFlow(getSelectedModel())
    val selectedModelFlow: StateFlow<String> = _selectedModelFlow.asStateFlow()

    private val _highThinkingFlow = MutableStateFlow(getHighThinking())
    val highThinkingFlow: StateFlow<Boolean> = _highThinkingFlow.asStateFlow()

    fun getApiKey(): String {
        val encoded = prefs.getString(KEY_API_KEY, "") ?: ""
        return if (encoded.isNotEmpty()) {
            try {
                String(Base64.decode(encoded, Base64.DEFAULT))
            } catch (e: Exception) {
                ""
            }
        } else ""
    }

    fun setApiKey(key: String) {
        val trimmed = key.trim()
        val encoded = if (trimmed.isNotEmpty()) {
            Base64.encodeToString(trimmed.toByteArray(), Base64.DEFAULT)
        } else ""
        prefs.edit().putString(KEY_API_KEY, encoded).apply()
        _apiKeyFlow.value = trimmed
    }

    fun getThemeMode(): String = prefs.getString(KEY_THEME_MODE, THEME_SYSTEM) ?: THEME_SYSTEM

    fun setThemeMode(mode: String) {
        prefs.edit().putString(KEY_THEME_MODE, mode).apply()
        _themeModeFlow.value = mode
    }

    fun getSelectedModel(): String {
        val model = prefs.getString(KEY_MODEL, null)
        if (model == null || model == "gemini-3.1-pro-preview") {
            return "gemini-3.5-flash"
        }
        return model
    }

    fun setSelectedModel(model: String) {
        prefs.edit().putString(KEY_MODEL, model).apply()
        _selectedModelFlow.value = model
    }

    fun getHighThinking(): Boolean = prefs.getBoolean(KEY_HIGH_THINKING, true)

    fun setHighThinking(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_HIGH_THINKING, enabled).apply()
        _highThinkingFlow.value = enabled
    }

    fun getLastSource(): String = prefs.getString(KEY_LAST_SOURCE, "") ?: ""

    fun setLastSource(source: String) {
        prefs.edit().putString(KEY_LAST_SOURCE, source).apply()
    }

    fun getLastBranch(): String = prefs.getString(KEY_LAST_BRANCH, "main") ?: "main"

    fun setLastBranch(branch: String) {
        prefs.edit().putString(KEY_LAST_BRANCH, branch).apply()
    }

    fun getRequireApproval(): Boolean = prefs.getBoolean(KEY_REQUIRE_APPROVAL, true)

    fun setRequireApproval(require: Boolean) {
        prefs.edit().putBoolean(KEY_REQUIRE_APPROVAL, require).apply()
    }

    companion object {
        private const val KEY_API_KEY = "enc_jules_api_key"
        private const val KEY_THEME_MODE = "app_theme_mode"
        private const val KEY_MODEL = "gemini_model_name"
        private const val KEY_HIGH_THINKING = "gemini_high_thinking"
        private const val KEY_LAST_SOURCE = "last_selected_source"
        private const val KEY_LAST_BRANCH = "last_selected_branch"
        private const val KEY_REQUIRE_APPROVAL = "require_plan_approval"

        const val THEME_SYSTEM = "SYSTEM"
        const val THEME_LIGHT = "LIGHT"
        const val THEME_DARK = "DARK"
    }
}
