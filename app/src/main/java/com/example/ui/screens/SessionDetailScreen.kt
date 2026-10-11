package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gemini.CopilotTurn
import com.example.jules.sdk.models.Activity
import com.example.jules.sdk.models.Artifact
import com.example.jules.sdk.models.Plan
import com.example.ui.components.FloatingChatInput
import com.example.ui.theme.ChatgptBlue
import com.example.ui.theme.ChatgptGreen
import com.example.ui.theme.ChatgptGreenLight
import com.example.ui.theme.ChatgptGreenText
import com.example.ui.theme.UserBubbleDark
import com.example.ui.theme.UserBubbleLight
import com.example.viewmodel.AppScreen
import com.example.viewmodel.JulesAppViewModel

@Composable
fun SessionDetailScreen(
    viewModel: JulesAppViewModel,
    onBack: () -> Unit,
    onOpenPromptGallery: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val listState = rememberLazyListState()

    val selectedSession by viewModel.selectedSession.collectAsState()
    val activities by viewModel.activities.collectAsState()
    val isLoadingActivities by viewModel.isLoadingActivities.collectAsState()
    val isSendingMessage by viewModel.isSendingMessage.collectAsState()
    val isApprovingPlan by viewModel.isApprovingPlan.collectAsState()

    val isCopilotMode by viewModel.isCopilotMode.collectAsState()
    val copilotTurns by viewModel.copilotTurns.collectAsState()
    val isCopilotThinking by viewModel.isCopilotThinking.collectAsState()

    var messageInput by remember { mutableStateOf("") }
    var isMenuOpen by remember { mutableStateOf(false) }

    // Scroll to bottom when new messages arrive
    LaunchedEffect(activities.size, copilotTurns.size) {
        val totalCount = if (isCopilotMode) copilotTurns.size else activities.size
        if (totalCount > 0) {
            listState.animateScrollToItem(totalCount - 1)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            // Top App Bar mirroring Screenshot 4
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.testTag("session_detail_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                Text(
                    text = if (isCopilotMode) "Jules Copilot" else (selectedSession?.displayTitle ?: "Session"),
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp)
                )

                Box {
                    IconButton(
                        onClick = { isMenuOpen = true },
                        modifier = Modifier.testTag("session_detail_menu_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Session options",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    DropdownMenu(
                        expanded = isMenuOpen,
                        onDismissRequest = { isMenuOpen = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Refresh") },
                            leadingIcon = { Icon(Icons.Default.Refresh, contentDescription = null) },
                            onClick = {
                                isMenuOpen = false
                                selectedSession?.cleanId?.let { viewModel.loadActivities(it) }
                            }
                        )

                        DropdownMenuItem(
                            text = { Text(if (isCopilotMode) "Jules Session Mode" else "Gemini Copilot Mode") },
                            leadingIcon = { Icon(Icons.Default.AutoAwesome, contentDescription = null) },
                            onClick = {
                                isMenuOpen = false
                                if (isCopilotMode) {
                                    viewModel.loadSessions()
                                } else {
                                    viewModel.switchToCopilotMode()
                                }
                            }
                        )

                        if (!isCopilotMode && selectedSession != null) {
                            DropdownMenuItem(
                                text = { Text("Delete Session", color = MaterialTheme.colorScheme.error) },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                },
                                onClick = {
                                    isMenuOpen = false
                                    selectedSession?.cleanId?.let { id ->
                                        viewModel.deleteSession(id) {
                                            viewModel.navigateTo(AppScreen.CHAT_HOME)
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        },
        bottomBar = {
            FloatingChatInput(
                value = messageInput,
                onValueChange = { messageInput = it },
                onSend = {
                    if (messageInput.isNotBlank()) {
                        val text = messageInput
                        messageInput = ""
                        if (isCopilotMode) {
                            viewModel.sendCopilotMessage(text)
                        } else {
                            viewModel.sendSessionMessage(text)
                        }
                    }
                },
                placeholder = if (isCopilotMode) "Reply to Copilot" else "Reply to Jules",
                isLoading = if (isCopilotMode) isCopilotThinking else isSendingMessage,
                onPlusClick = onOpenPromptGallery,
                onVoiceClick = {
                    messageInput = "Please examine the plan steps and test cases."
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (isCopilotMode) {
                // Copilot Timeline
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item { Spacer(modifier = Modifier.height(8.dp)) }

                    items(copilotTurns) { turn ->
                        CopilotTurnBubble(turn = turn)
                    }

                    if (isCopilotThinking) {
                        item {
                            ThinkingIndicator(text = "Thinking deeply with Gemini 3.1 Pro (High)...")
                        }
                    }

                    item { Spacer(modifier = Modifier.height(16.dp)) }
                }
            } else {
                // Jules Session Activities Timeline
                if (isLoadingActivities && activities.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(strokeWidth = 2.dp)
                    }
                } else if (activities.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Session Started",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Jules is initializing the workspace and analyzing code...",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        item { Spacer(modifier = Modifier.height(8.dp)) }

                        items(activities, key = { it.cleanId }) { activity ->
                            ActivityItem(
                                activity = activity,
                                isApprovingPlan = isApprovingPlan,
                                onApprovePlan = { planId ->
                                    viewModel.approvePlan(planId)
                                },
                                onOpenUrl = { url ->
                                    try {
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        // Ignore invalid url
                                    }
                                }
                            )
                        }

                        item { Spacer(modifier = Modifier.height(16.dp)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun CopilotTurnBubble(turn: CopilotTurn) {
    var isThinkingExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (turn.isUser) Alignment.End else Alignment.Start
    ) {
        if (turn.isUser) {
            // User Speech Bubble (Right aligned, pill container)
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = if (androidx.compose.foundation.isSystemInDarkTheme()) UserBubbleDark else UserBubbleLight,
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .clip(RoundedCornerShape(20.dp))
            ) {
                Text(
                    text = turn.text,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    lineHeight = 22.sp,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                )
            }
        } else {
            // Assistant Model Message
            Column(modifier = Modifier.fillMaxWidth()) {
                // Collapsible Thinking Section (Screenshot 4: "Worked for 24s >")
                if (turn.thinkingTrace != null) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { isThinkingExpanded = !isThinkingExpanded }
                            .padding(vertical = 4.dp, horizontal = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Worked for 18s",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Icon(
                            imageVector = if (isThinkingExpanded) Icons.Default.KeyboardArrowDown else Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = "Expand thinking",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    AnimatedVisibility(
                        visible = isThinkingExpanded,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp)
                        ) {
                            Text(
                                text = turn.thinkingTrace,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 18.sp,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = turn.text,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onBackground,
                    lineHeight = 22.sp
                )

                // Quick feedback bar (Screenshot 4)
                Row(
                    modifier = Modifier.padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.Default.ThumbUp,
                        contentDescription = "Like",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.Default.ThumbDown,
                        contentDescription = "Dislike",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ActivityItem(
    activity: Activity,
    isApprovingPlan: Boolean,
    onApprovePlan: (String?) -> Unit,
    onOpenUrl: (String) -> Unit
) {
    if (activity.isUser) {
        // User Speech Bubble (Right aligned, pill container)
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.End
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = if (androidx.compose.foundation.isSystemInDarkTheme()) UserBubbleDark else UserBubbleLight,
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .clip(RoundedCornerShape(20.dp))
            ) {
                Text(
                    text = activity.message?.content ?: "",
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    lineHeight = 22.sp,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                )
            }
        }
    } else {
        // Agent Response
        Column(modifier = Modifier.fillMaxWidth()) {
            // Text Message
            if (!activity.message?.content.isNullOrBlank()) {
                Text(
                    text = activity.message?.content ?: "",
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onBackground,
                    lineHeight = 22.sp
                )
                Spacer(modifier = Modifier.height(10.dp))
            }

            // Plan Card (When Jules generates an execution plan)
            if (activity.plan != null) {
                PlanCard(
                    plan = activity.plan,
                    isApproving = isApprovingPlan,
                    onApprove = { onApprovePlan(null) }
                )
                Spacer(modifier = Modifier.height(10.dp))
            }

            // Structured Artifact Card (Screenshot 4: Issue / Pull Request card)
            if (activity.artifact != null) {
                ArtifactCard(
                    artifact = activity.artifact,
                    onOpen = { activity.artifact.url?.let(onOpenUrl) }
                )
                Spacer(modifier = Modifier.height(10.dp))
            }

            // Progress Update
            if (activity.progress != null && !activity.progress.summary.isNullOrBlank()) {
                ThinkingIndicator(text = activity.progress.summary)
                Spacer(modifier = Modifier.height(10.dp))
            }
        }
    }
}

@Composable
private fun PlanCard(
    plan: Plan,
    isApproving: Boolean,
    onApprove: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(20.dp)),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = plan.title ?: "Execution Plan",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                if (plan.isPendingApproval) {
                    Surface(
                        color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f),
                        shape = CircleShape
                    ) {
                        Text(
                            text = "Approval Needed",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            plan.steps.forEachIndexed { index, step ->
                Row(
                    modifier = Modifier.padding(vertical = 4.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = "${index + 1}. ",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = step.description,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = 18.sp
                    )
                }
            }

            if (plan.isPendingApproval) {
                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onApprove,
                    enabled = !isApproving,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .clip(CircleShape)
                        .testTag("approve_plan_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    shape = CircleShape
                ) {
                    if (isApproving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Approve Plan", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

// Artifact Card replicating Screenshot 4 (GitHub Issue / PR card)
@Composable
private fun ArtifactCard(
    artifact: Artifact,
    onOpen: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(20.dp)),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            // Header Row: Checkmark + Title + "Open" Chip (Screenshot 4)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Success",
                        tint = ChatgptGreen,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = artifact.title ?: "Issue #88: Notification System",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Green Open Pill Chip
                Surface(
                    color = ChatgptGreenLight,
                    shape = CircleShape
                ) {
                    Text(
                        text = "Open",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = ChatgptGreenText,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Subheading
            Text(
                text = artifact.summary ?: "Feature: Notify users about Jules events when the app is inactive",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "The issue covers background notifications, actionable alerts, session deep links, privacy, and cross-platform delivery.",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Black Pill Action Button: "View GitHub Issue ↗"
            Button(
                onClick = onOpen,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
                    .clip(CircleShape)
                    .testTag("view_artifact_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                shape = CircleShape
            ) {
                Text(
                    text = if (artifact.type?.contains("PR", true) == true) "View Pull Request" else "View GitHub Issue",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun ThinkingIndicator(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CircularProgressIndicator(
            strokeWidth = 2.dp,
            modifier = Modifier.size(14.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
