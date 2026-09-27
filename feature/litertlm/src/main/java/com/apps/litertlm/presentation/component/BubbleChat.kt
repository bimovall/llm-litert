package com.apps.litertlm.presentation.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun BubbleChat(
    modifier: Modifier = Modifier,
    isMe: Boolean,
    message: String,
    thinking: String = "",
    isLoading: Boolean = false,
    isThinkingAvailable: Boolean = false,
    isExpandThinking: Boolean = false,
    onExpandThinking: (Boolean) -> Unit = {}
) {

    val bubbleColor = if (isMe) Color(0xFF007AFF) else Color(0xFFE5E5EA)
    val textColor = if (isMe) Color.White else Color.Black

    Box(
        modifier = modifier
            .padding(8.dp)
            .drawBehind {
                val bubbleWidth = size.width

                drawRoundRect(
                    color = bubbleColor,
                    cornerRadius = CornerRadius(32f, 32f)
                )

                val path = Path().apply {
                    if (isMe) {
                        // Right side triangle
                        moveTo(bubbleWidth - 48f, 0f)
                        lineTo(bubbleWidth, -24f)
                        lineTo(bubbleWidth, 48f)
                    } else {
                        // Left side triangle
                        moveTo(48f, 0f)
                        lineTo(0f, -24f)
                        lineTo(0f, 48f)
                    }
                    close()
                }
                drawPath(path, color = bubbleColor)
            }
            .padding(12.dp)

    ) {
        Column {
            if (!isMe && isThinkingAvailable) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable(true) {
                        onExpandThinking.invoke(!isExpandThinking)
                    }
                ) {
                    Text(
                        "Show Thinking",
                        color = Color.Black.copy(alpha = 0.7F),
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 12.sp
                        )
                    )
                    Icon(
                        imageVector = if (isExpandThinking) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                        contentDescription = "",
                        tint = Color.Black.copy(alpha = 0.7F),
                        modifier = Modifier.size(14.dp)
                    )
                }
                if (isExpandThinking) {
                    Text(
                        thinking,
                        color = Color.Black.copy(alpha = 0.7F),
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 12.sp
                        ),
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }
            if (isLoading) {
                BounceLoadingIndicator()
            } else {
                Text(message, color = textColor, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
@Preview
fun PreviewBubbleChat() {
    BubbleChat(
        isMe = false,
        message = "test",
        thinking = "Thinking",
        isThinkingAvailable = true
    )
}

