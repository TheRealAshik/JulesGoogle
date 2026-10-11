package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Surface
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.data.preferences.UserPreferencesManager
import com.example.ui.screens.NavigationDrawerContent
import com.example.ui.screens.PromptGalleryDialog
import com.example.ui.screens.SessionDetailScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.WelcomeAuthScreen
import com.example.ui.screens.WelcomeNewChatScreen
import com.example.ui.theme.JulesTheme
import com.example.viewmodel.AppScreen
import com.example.viewmodel.JulesAppViewModel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val viewModel: JulesAppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val themeMode by viewModel.prefs.themeModeFlow.collectAsState()
            val isDarkTheme = when (themeMode) {
                UserPreferencesManager.THEME_LIGHT -> false
                UserPreferencesManager.THEME_DARK -> true
                else -> isSystemInDarkTheme()
            }

            JulesTheme(darkTheme = isDarkTheme) {
                JulesApp(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun JulesApp(viewModel: JulesAppViewModel) {
    val coroutineScope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val currentScreen by viewModel.currentScreen.collectAsState()
    var showPromptGallery by remember { mutableStateOf(false) }

    // Intercept hardware and gesture back navigation
    BackHandler(enabled = drawerState.isOpen || currentScreen != AppScreen.CHAT_HOME) {
        if (drawerState.isOpen) {
            coroutineScope.launch { drawerState.close() }
        } else if (currentScreen == AppScreen.SESSION_DETAIL || currentScreen == AppScreen.SETTINGS) {
            viewModel.navigateTo(AppScreen.CHAT_HOME)
        }
    }

    if (currentScreen == AppScreen.WELCOME_AUTH) {
        WelcomeAuthScreen(viewModel = viewModel)
    } else {
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                NavigationDrawerContent(
                    viewModel = viewModel,
                    onCloseDrawer = {
                        coroutineScope.launch { drawerState.close() }
                    }
                )
            }
        ) {
            Surface(modifier = Modifier.fillMaxSize()) {
                when (currentScreen) {
                    AppScreen.CHAT_HOME -> {
                        WelcomeNewChatScreen(
                            viewModel = viewModel,
                            onOpenDrawer = {
                                coroutineScope.launch { drawerState.open() }
                            },
                            onOpenPromptGallery = {
                                showPromptGallery = true
                            }
                        )
                    }

                    AppScreen.SESSION_DETAIL -> {
                        SessionDetailScreen(
                            viewModel = viewModel,
                            onBack = {
                                viewModel.navigateTo(AppScreen.CHAT_HOME)
                            },
                            onOpenPromptGallery = {
                                showPromptGallery = true
                            }
                        )
                    }

                    AppScreen.SETTINGS -> {
                        SettingsScreen(
                            viewModel = viewModel,
                            onBack = {
                                viewModel.navigateTo(AppScreen.CHAT_HOME)
                            },
                            onOpenPromptGallery = {
                                showPromptGallery = true
                            }
                        )
                    }

                    AppScreen.WELCOME_AUTH -> {
                        WelcomeAuthScreen(viewModel = viewModel)
                    }
                }
            }
        }
    }

    // Prompt Gallery Dialog
    if (showPromptGallery) {
        PromptGalleryDialog(
            viewModel = viewModel,
            onDismiss = { showPromptGallery = false },
            onSelectPrompt = { selectedPrompt ->
                showPromptGallery = false
                if (currentScreen == AppScreen.SESSION_DETAIL) {
                    viewModel.sendSessionMessage(selectedPrompt)
                } else {
                    viewModel.createNewTask(selectedPrompt)
                }
            }
        )
    }
}
