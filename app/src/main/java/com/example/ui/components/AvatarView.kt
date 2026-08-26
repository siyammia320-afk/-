package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.ui.theme.OnlineGreen
import com.example.ui.theme.PrimaryBlue

val AVATAR_COLORS = listOf(
    Color(0xFFE53935),
    Color(0xFF8E24AA),
    Color(0xFF3949AB),
    Color(0xFF00897B),
    Color(0xFF43A047),
    Color(0xFFFB8C00),
    Color(0xFF6D4C41),
    Color(0xFF00ACC1)
)

fun getAvatarColor(name: String): Color {
    if (name.isBlank()) return PrimaryBlue
    val index = (name.first().code + name.length) % AVATAR_COLORS.size
    return AVATAR_COLORS[index]
}

@Composable
fun AvatarView(
    photoUrl: String?,
    name: String,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    showOnlineIndicator: Boolean = false,
    isOnline: Boolean = false
) {
    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        if (!photoUrl.isNullOrBlank() && (photoUrl.startsWith("http://") || photoUrl.startsWith("https://") || photoUrl.startsWith("data:image"))) {
            AsyncImage(
                model = photoUrl,
                contentDescription = name,
                modifier = Modifier
                    .size(size)
                    .clip(CircleShape)
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), CircleShape),
                contentScale = ContentScale.Crop
            )
        } else {
            val bgColor = getAvatarColor(name)
            val initial = if (name.isNotBlank()) name.first().uppercase() else "U"

            Box(
                modifier = Modifier
                    .size(size)
                    .clip(CircleShape)
                    .background(bgColor)
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (name.isBlank()) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(size * 0.6f)
                    )
                } else {
                    Text(
                        text = initial,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = (size.value * 0.42f).sp
                    )
                }
            }
        }

        if (showOnlineIndicator) {
            Box(
                modifier = Modifier
                    .size(size * 0.28f)
                    .align(Alignment.BottomEnd)
                    .clip(CircleShape)
                    .background(if (isOnline) OnlineGreen else Color.Gray)
                    .border(1.5.dp, MaterialTheme.colorScheme.surface, CircleShape)
            )
        }
    }
}
