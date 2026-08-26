package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Reply
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ChatMessage
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.PrimaryBlueDark
import com.example.ui.theme.ReceivedBubbleBg
import com.example.ui.theme.ReceivedBubbleText
import com.example.ui.theme.SentBubbleBg
import com.example.ui.theme.SentBubbleText
import com.example.ui.theme.TickBlue
import com.example.ui.theme.TickGrey
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun MessageItem(
    message: ChatMessage,
    isCurrentUser: Boolean,
    isDirectChat: Boolean = false,
    onReplyClick: (ChatMessage) -> Unit = {}
) {
    val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
    val formattedTime = timeFormat.format(Date(message.timestamp))

    val bubbleShape = if (isCurrentUser) {
        RoundedCornerShape(16.dp, 16.dp, 2.dp, 16.dp)
    } else {
        RoundedCornerShape(16.dp, 16.dp, 16.dp, 2.dp)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 3.dp),
        horizontalArrangement = if (isCurrentUser) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom
    ) {
        if (!isCurrentUser && !isDirectChat) {
            AvatarView(
                photoUrl = message.senderPhoto,
                name = message.senderName,
                size = 32.dp,
                modifier = Modifier.padding(end = 6.dp, bottom = 2.dp)
            )
        }

        Column(
            modifier = Modifier
                .widthIn(max = 290.dp)
                .clip(bubbleShape)
                .background(if (isCurrentUser) SentBubbleBg else ReceivedBubbleBg)
                .combinedClickable(
                    onClick = { onReplyClick(message) },
                    onLongClick = { onReplyClick(message) }
                )
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .testTag("message_bubble_${message.id}")
        ) {
            // Sender Name in Group Chat
            if (!isCurrentUser && !isDirectChat) {
                Text(
                    text = message.senderName,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = PrimaryBlueDark,
                    modifier = Modifier.padding(bottom = 2.dp)
                )
            }

            // Replying Quote Box
            if (message.isReplied) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(
                            if (isCurrentUser) Color.Black.copy(alpha = 0.15f)
                            else Color.Black.copy(alpha = 0.06f)
                        )
                        .border(
                            width = 2.dp,
                            color = if (isCurrentUser) Color.White.copy(alpha = 0.8f) else PrimaryBlue,
                            shape = RoundedCornerShape(6.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Reply,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp),
                                tint = if (isCurrentUser) Color.White else PrimaryBlue
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = message.replyToSender,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = if (isCurrentUser) Color.White else PrimaryBlue
                            )
                        }
                        Text(
                            text = message.replyToText,
                            fontSize = 11.sp,
                            maxLines = 1,
                            color = if (isCurrentUser) Color.White.copy(alpha = 0.9f) else Color.DarkGray
                        )
                    }
                }
            }

            // Message text
            Text(
                text = message.text,
                color = if (isCurrentUser) SentBubbleText else ReceivedBubbleText,
                fontSize = 15.sp,
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(3.dp))

            // Time & Ticks Row
            Row(
                modifier = Modifier.align(Alignment.End),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formattedTime,
                    fontSize = 10.sp,
                    color = if (isCurrentUser) Color.White.copy(alpha = 0.75f) else Color.Gray
                )

                // Message ticks for current user
                if (isCurrentUser) {
                    Spacer(modifier = Modifier.width(4.dp))
                    if (message.seen) {
                        // 🔵 2 Blue Ticks (Seen / Read)
                        Icon(
                            imageVector = Icons.Default.DoneAll,
                            contentDescription = "Seen",
                            tint = Color(0xFF60A5FA), // Crisp Sky Blue Double Tick
                            modifier = Modifier
                                .size(16.dp)
                                .testTag("double_blue_tick_${message.id}")
                        )
                    } else {
                        // 1 Single Tick (Sent / Delivered)
                        Icon(
                            imageVector = Icons.Default.Done,
                            contentDescription = "Sent",
                            tint = TickGrey,
                            modifier = Modifier
                                .size(14.dp)
                                .testTag("single_tick_${message.id}")
                        )
                    }
                }
            }
        }
    }
}
