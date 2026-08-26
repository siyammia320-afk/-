package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ChatMessage
import com.example.data.model.User
import com.example.data.repository.ChatRepository
import com.example.ui.components.AvatarView
import com.example.ui.components.ChatInputBar
import com.example.ui.components.MessageItem
import com.example.ui.theme.OnlineGreen
import com.example.ui.theme.PrimaryBlue
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DirectChatScreen(
    repository: ChatRepository,
    targetUser: User,
    onBack: () -> Unit,
    onViewProfile: (User) -> Unit = {}
) {
    val currentUser by repository.currentUser.collectAsState()
    val messages by repository.activeDirectMessages.collectAsState()
    val allUsers by repository.allUsers.collectAsState()

    // Up-to-date target user profile
    val liveTargetUser = allUsers.find { it.userId == targetUser.userId } ?: targetUser

    val chatId = remember(currentUser?.userId, targetUser.userId) {
        currentUser?.let { repository.getDirectChatId(it.userId, targetUser.userId) } ?: ""
    }

    var inputText by remember { mutableStateOf("") }
    var replyingTo by remember { mutableStateOf<ChatMessage?>(null) }

    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    // Listen to direct chat
    DisposableEffect(chatId) {
        if (chatId.isNotBlank()) {
            repository.startListeningToDirectChat(chatId, targetUser.userId)
        }
        onDispose {
            repository.stopListeningToDirectChat()
        }
    }

    // Scroll to bottom on new message
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top App Bar with User Details
        TopAppBar(
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clickable { onViewProfile(liveTargetUser) }
                        .padding(vertical = 4.dp)
                ) {
                    AvatarView(
                        photoUrl = liveTargetUser.photoUrl,
                        name = liveTargetUser.displayName,
                        size = 38.dp,
                        showOnlineIndicator = true,
                        isOnline = liveTargetUser.online
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = liveTargetUser.displayName,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (liveTargetUser.online) "Online (Active)" else "${liveTargetUser.username} • Offline",
                            fontSize = 11.sp,
                            color = if (liveTargetUser.online) OnlineGreen else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            navigationIcon = {
                IconButton(onClick = onBack, modifier = Modifier.testTag("back_button")) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        )

        // Messages List
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            if (messages.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Start a conversation with ${liveTargetUser.displayName}",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "Sent messages show ✓ and read messages show 🔵 ✓✓",
                            color = PrimaryBlue,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(vertical = 8.dp)
                ) {
                    items(messages, key = { it.id }) { msg ->
                        val isMe = msg.senderId == currentUser?.userId
                        MessageItem(
                            message = msg,
                            isCurrentUser = isMe,
                            isDirectChat = true,
                            onReplyClick = { targetMsg ->
                                replyingTo = targetMsg
                            }
                        )
                    }
                }
            }
        }

        // Input Field
        ChatInputBar(
            inputText = inputText,
            onInputChange = { inputText = it },
            replyMessage = replyingTo,
            onCancelReply = { replyingTo = null },
            onSendMessage = {
                if (inputText.isNotBlank()) {
                    val textToSend = inputText
                    val replyToSend = replyingTo
                    inputText = ""
                    replyingTo = null

                    repository.sendDirectMessage(
                        receiver = liveTargetUser,
                        text = textToSend,
                        replyTo = replyToSend
                    )

                    coroutineScope.launch {
                        if (messages.isNotEmpty()) {
                            listState.animateScrollToItem(messages.size - 1)
                        }
                    }
                }
            }
        )
    }
}
