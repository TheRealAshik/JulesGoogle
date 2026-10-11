package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jules.sdk.models.Session
import com.example.ui.theme.ChatgptBlue
import com.example.viewmodel.AppScreen
import com.example.viewmodel.JulesAppViewModel

@Composable
fun NavigationDrawerContent(
    viewModel: JulesAppViewModel,
    onCloseDrawer: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sessions by viewModel.sessions.collectAsState()
    val selectedSession by viewModel.selectedSession.collectAsState()
    val isLoadingSessions by viewModel.isLoadingSessions.collectAsState()
    val searchQuery by viewModel.sessionSearchQuery.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()

    var isSearchExpanded by remember { mutableStateOf(false) }

    val filteredSessions = remember(sessions, searchQuery) {
        if (searchQuery.isBlank()) {
            sessions
        } else {
            sessions.filter {
                it.displayTitle.contains(searchQuery, ignoreCase = true) ||
                        it.cleanId.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    Surface(
        modifier = modifier
            .width(320.dp)
            .fillMaxHeight(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .statusBarsPadding()
                .padding(vertical = 12.dp)
        ) {
            // Top Drawer Header: Title + Search Toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Jules",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                IconButton(
                    onClick = { isSearchExpanded = !isSearchExpanded },
                    modifier = Modifier.testTag("drawer_search_toggle")
                ) {
                    Icon(
                        imageVector = if (isSearchExpanded) Icons.Default.Close else Icons.Default.Search,
                        contentDescription = "Search sessions",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Search Bar Input when expanded
            if (isSearchExpanded) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.setSessionSearchQuery(it) },
                    placeholder = { Text("Search sessions...", fontSize = 14.sp) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .testTag("drawer_search_input"),
                    shape = RoundedCornerShape(20.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    ),
                    singleLine = true
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Sessions List
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                if (isLoadingSessions && sessions.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                } else if (filteredSessions.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (searchQuery.isNotEmpty()) "No matching sessions" else "No coding sessions yet",
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Start a new task below",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(filteredSessions, key = { it.cleanId }) { session ->
                            val isSelected = selectedSession?.cleanId == session.cleanId

                            SessionDrawerItem(
                                session = session,
                                isSelected = isSelected,
                                onClick = {
                                    viewModel.selectSession(session)
                                    onCloseDrawer()
                                },
                                onDelete = {
                                    viewModel.deleteSession(session.cleanId)
                                }
                            )
                        }
                    }
                }
            }

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant,
                thickness = 0.5.dp
            )

            // Bottom Drawer Footer (Screenshot 3 & Screenshot 1 integration)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // User Profile Pill
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(20.dp))
                        .clickable {
                            viewModel.navigateTo(AppScreen.SETTINGS)
                            onCloseDrawer()
                        }
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "User profile",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Text(
                            text = currentUser?.displayName?.takeIf { it.isNotBlank() } ?: "Ashik",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = currentUser?.email ?: "Personal",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // New Chat Blue Pill Action Button (Screenshot 3)
                Button(
                    onClick = {
                        viewModel.navigateTo(AppScreen.CHAT_HOME)
                        onCloseDrawer()
                    },
                    modifier = Modifier
                        .height(40.dp)
                        .testTag("drawer_new_chat_button"),
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ChatgptBlue,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        horizontal = 14.dp,
                        vertical = 6.dp
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "New chat",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Chat",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun SessionDrawerItem(
    session: Session,
    isSelected: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteOption by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = session.displayTitle,
                fontSize = 15.sp,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )

            // Blue Dot Indicator for Selected Session (as seen in Screenshot 3)
            if (isSelected) {
                Spacer(modifier = Modifier.width(10.dp))
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(ChatgptBlue)
                )
            }
        }

        IconButton(
            onClick = onDelete,
            modifier = Modifier
                .size(32.dp)
                .padding(start = 6.dp)
        ) {
            Icon(
                imageVector = Icons.Default.DeleteOutline,
                contentDescription = "Delete session",
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
