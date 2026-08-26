package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Chat
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.User
import com.example.data.repository.ChatRepository
import com.example.ui.components.AvatarView
import com.example.ui.theme.BorderLight
import com.example.ui.theme.PrimaryBlue

enum class AppTab(val title: String) {
    GLOBAL("Global Chat"),
    DIRECT("Direct Messages"),
    USERS("Members"),
    PROFILE("Profile")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    repository: ChatRepository,
    onLogout: () -> Unit
) {
    val currentUser by repository.currentUser.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) }
    var activeChatUser by remember { mutableStateOf<User?>(null) }
    var inspectingProfileUser by remember { mutableStateOf<User?>(null) }

    // If active 1-on-1 chat is open
    if (activeChatUser != null) {
        DirectChatScreen(
            repository = repository,
            targetUser = activeChatUser!!,
            onBack = { activeChatUser = null },
            onViewProfile = { user -> inspectingProfileUser = user }
        )
        return
    }

    // If inspecting another user's profile
    if (inspectingProfileUser != null) {
        ProfileScreen(
            repository = repository,
            targetUser = inspectingProfileUser,
            onBack = { inspectingProfileUser = null },
            onStartChat = { user ->
                inspectingProfileUser = null
                activeChatUser = user
            }
        )
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(PrimaryBlue),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Chat,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "BD Chat",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = currentUser?.let { "${it.displayName} (${it.username})" } ?: "Connected",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // My avatar quick badge
                        if (currentUser != null) {
                            AvatarView(
                                photoUrl = currentUser!!.photoUrl,
                                name = currentUser!!.displayName,
                                size = 32.dp,
                                showOnlineIndicator = true,
                                isOnline = true
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                modifier = Modifier.border(width = 0.5.dp, color = BorderLight)
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                modifier = Modifier.border(width = 1.dp, color = BorderLight),
                tonalElevation = 0.dp
            ) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = {
                        Icon(
                            if (selectedTab == 0) Icons.Filled.Forum else Icons.Outlined.Forum,
                            contentDescription = "Global Chat"
                        )
                    },
                    label = { Text("Global Chat", fontSize = 12.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = PrimaryBlue,
                        selectedTextColor = PrimaryBlue,
                        indicatorColor = PrimaryBlue.copy(alpha = 0.12f)
                    ),
                    modifier = Modifier.testTag("tab_global_chat")
                )

                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = {
                        Icon(
                            if (selectedTab == 1) Icons.Filled.Chat else Icons.Outlined.Chat,
                            contentDescription = "Direct Messages"
                        )
                    },
                    label = { Text("Inbox", fontSize = 12.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = PrimaryBlue,
                        selectedTextColor = PrimaryBlue,
                        indicatorColor = PrimaryBlue.copy(alpha = 0.12f)
                    ),
                    modifier = Modifier.testTag("tab_direct_chats")
                )

                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = {
                        Icon(
                            if (selectedTab == 2) Icons.Filled.Group else Icons.Outlined.Group,
                            contentDescription = "Members"
                        )
                    },
                    label = { Text("Members", fontSize = 12.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = PrimaryBlue,
                        selectedTextColor = PrimaryBlue,
                        indicatorColor = PrimaryBlue.copy(alpha = 0.12f)
                    ),
                    modifier = Modifier.testTag("tab_members")
                )

                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    icon = {
                        Icon(
                            if (selectedTab == 3) Icons.Filled.AccountCircle else Icons.Outlined.AccountCircle,
                            contentDescription = "Profile"
                        )
                    },
                    label = { Text("Profile", fontSize = 12.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = PrimaryBlue,
                        selectedTextColor = PrimaryBlue,
                        indicatorColor = PrimaryBlue.copy(alpha = 0.12f)
                    ),
                    modifier = Modifier.testTag("tab_profile")
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                0 -> GlobalChatScreen(repository = repository)
                1 -> DirectChatsListScreen(
                    repository = repository,
                    onOpenChat = { user -> activeChatUser = user },
                    onFindUsers = { selectedTab = 2 }
                )
                2 -> UsersListScreen(
                    repository = repository,
                    onStartChat = { user -> activeChatUser = user },
                    onViewProfile = { user -> inspectingProfileUser = user }
                )
                3 -> ProfileScreen(
                    repository = repository,
                    targetUser = null, // self profile
                    onLogout = onLogout
                )
            }
        }
    }
}
