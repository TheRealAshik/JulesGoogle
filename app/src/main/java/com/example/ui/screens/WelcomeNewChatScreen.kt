package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.FloatingChatInput
import com.example.ui.theme.ChatgptBlue
import com.example.viewmodel.AppScreen
import com.example.viewmodel.JulesAppViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WelcomeNewChatScreen(
    viewModel: JulesAppViewModel,
    onOpenDrawer: () -> Unit,
    onOpenPromptGallery: () -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val apiKey by viewModel.apiKey.collectAsState()
    val sources by viewModel.sources.collectAsState()
    val selectedSource by viewModel.selectedSource.collectAsState()
    val selectedBranch by viewModel.selectedBranch.collectAsState()
    val isLoadingSources by viewModel.isLoadingSources.collectAsState()

    var promptInput by remember { mutableStateOf("") }
    var isCreatingSession by remember { mutableStateOf(false) }
    var requireApproval by remember { mutableStateOf(viewModel.prefs.getRequireApproval()) }

    var isSourceMenuOpen by remember { mutableStateOf(false) }
    var isAddRepoDialogOpen by remember { mutableStateOf(false) }
    var customRepoInput by remember { mutableStateOf("") }
    var customBranchInput by remember { mutableStateOf("main") }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            // Clean Top Bar inspired by ChatGPT (Screenshot 2)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Hamburger Menu Button (Screenshot 2 top-left)
                IconButton(
                    onClick = onOpenDrawer,
                    modifier = Modifier.testTag("home_menu_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = "Open session history",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Title / Model indicator
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { viewModel.switchToCopilotMode() }
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "Jules",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "Gemini Copilot",
                        tint = ChatgptBlue,
                        modifier = Modifier.size(16.dp)
                    )
                }

                // Top Right: Settings & Copilot actions
                Row {
                    IconButton(
                        onClick = { viewModel.switchToCopilotMode() },
                        modifier = Modifier.testTag("home_copilot_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "Switch to Gemini Copilot",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    IconButton(
                        onClick = { viewModel.navigateTo(AppScreen.SETTINGS) },
                        modifier = Modifier.testTag("home_settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        },
        bottomBar = {
            FloatingChatInput(
                value = promptInput,
                onValueChange = { promptInput = it },
                onSend = {
                    if (apiKey.isBlank()) {
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("Please configure your Jules API key in Settings first.")
                        }
                        viewModel.navigateTo(AppScreen.SETTINGS)
                        return@FloatingChatInput
                    }

                    // Guarantee a valid active source is selected
                    val activeSource = selectedSource ?: sources.firstOrNull() ?: viewModel.defaultSource
                    if (selectedSource == null) {
                        viewModel.selectSource(activeSource)
                    }

                    isCreatingSession = true
                    viewModel.createNewTask(
                        prompt = promptInput,
                        requirePlanApproval = requireApproval,
                        onError = { error ->
                            isCreatingSession = false
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("Failed to create task: $error")
                            }
                        },
                        onSuccess = {
                            isCreatingSession = false
                            promptInput = ""
                        }
                    )
                },
                placeholder = "Ask Jules",
                isLoading = isCreatingSession,
                onPlusClick = onOpenPromptGallery,
                onVoiceClick = {
                    promptInput = "Investigate recent commit changes and review unit test coverage."
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.weight(1f))

            // Connected Repository / Source Pill Badge
            Box(
                modifier = Modifier
                    .fillMaxWidth(),
                contentAlignment = Alignment.CenterStart
            ) {
                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { isSourceMenuOpen = true }
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp)),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    tonalElevation = 2.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Code,
                            contentDescription = "Source repo",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = selectedSource?.cleanName ?: if (isLoadingSources) "Loading sources..." else "Select Repository",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = " : $selectedBranch",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                DropdownMenu(
                    expanded = isSourceMenuOpen,
                    onDismissRequest = { isSourceMenuOpen = false }
                ) {
                    if (sources.isNotEmpty()) {
                        sources.forEach { src ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = src.cleanName,
                                        fontWeight = if (src.name == selectedSource?.name) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                onClick = {
                                    viewModel.selectSource(src)
                                    isSourceMenuOpen = false
                                }
                            )
                        }
                        HorizontalDivider()
                    }

                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Add Custom Repository...",
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        },
                        onClick = {
                            isSourceMenuOpen = false
                            customRepoInput = ""
                            isAddRepoDialogOpen = true
                        }
                    )

                    DropdownMenuItem(
                        text = { Text("Refresh Connected Sources") },
                        onClick = {
                            isSourceMenuOpen = false
                            viewModel.loadSources()
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Plan approval toggle chip
            FilterChip(
                selected = requireApproval,
                onClick = {
                    requireApproval = !requireApproval
                    viewModel.prefs.setRequireApproval(requireApproval)
                },
                label = {
                    Text(
                        text = if (requireApproval) "Plan approval: Required" else "Plan approval: Auto-execute",
                        fontSize = 12.sp
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    selectedLabelColor = MaterialTheme.colorScheme.onSurface
                ),
                shape = RoundedCornerShape(14.dp)
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Minimalist Heading (Screenshot 2)
            Text(
                text = "What should Jules work on?",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(18.dp))

            // ChatGPT-style Quick Action Suggestions (Screenshot 2)
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                QuickActionRow(
                    icon = Icons.Default.Edit,
                    label = "Write or edit code",
                    onClick = {
                        promptInput = "Create a pull request to optimize database query performance and add cache."
                    }
                )

                QuickActionRow(
                    icon = Icons.Default.BugReport,
                    label = "Review PR or debug issue",
                    onClick = {
                        promptInput = "Review pull request changes and create an issue covering edge cases and regression."
                    }
                )

                QuickActionRow(
                    icon = Icons.Default.Search,
                    label = "Search and inspect sources",
                    onClick = {
                        promptInput = "Examine the repository directory structure and list potential architectural improvements."
                    }
                )
            }

            Spacer(modifier = Modifier.weight(1.5f))
        }
    }

    if (isAddRepoDialogOpen) {
        AlertDialog(
            onDismissRequest = { isAddRepoDialogOpen = false },
            title = {
                Text("Add GitHub Repository", fontWeight = FontWeight.SemiBold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "Enter the GitHub repository name (e.g. owner/repo) or URL to connect Jules tasks to.",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = customRepoInput,
                        onValueChange = { customRepoInput = it },
                        placeholder = { Text("TheRealAshik/Jules") },
                        label = { Text("Repository (owner/repo)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = customBranchInput,
                        onValueChange = { customBranchInput = it },
                        placeholder = { Text("main") },
                        label = { Text("Starting Branch") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (customRepoInput.isNotBlank()) {
                            viewModel.addCustomSource(customRepoInput, customBranchInput.ifBlank { "main" })
                            isAddRepoDialogOpen = false
                        }
                    }
                ) {
                    Text("Add & Select")
                }
            },
            dismissButton = {
                TextButton(onClick = { isAddRepoDialogOpen = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun QuickActionRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp)),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(14.dp))
            Text(
                text = label,
                fontSize = 15.sp,
                fontWeight = FontWeight.Normal,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
