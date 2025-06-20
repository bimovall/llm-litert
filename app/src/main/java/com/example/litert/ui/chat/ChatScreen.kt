package com.example.litert.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel


@Composable
fun ChatScreen(modifier: Modifier = Modifier, viewModel: ChatViewModel = viewModel()) {
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.init(context.assets)
    }

    Column(modifier = modifier.imePadding()) {
        ChatList(
            viewModel,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1F)
                .background(Color(0xFFE3E9F8))
        )
        MessageInput(
            viewModel,
            onSubmit = {
                viewModel.generateText(it, nbTokens = 50)
            },
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFE3E9F8))
                .padding(horizontal = 16.dp, vertical = 16.dp)
        )
    }
}

@Composable
fun ChatList(viewModel: ChatViewModel, modifier: Modifier = Modifier) {
    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
    val uiState by viewModel.chatUiState.collectAsState()

    when (uiState) {
        is ChatUiState.Answering, is ChatUiState.Stopped -> {
            val chatList = when (uiState) {
                is ChatUiState.Answering -> (uiState as ChatUiState.Answering).chat
                is ChatUiState.Stopped -> (uiState as ChatUiState.Stopped).chat
                else -> emptyList()
            }
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = modifier.padding(top = 16.dp)
            ) {
                items(chatList) { chat ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                    ) {
                        Text(
                            chat.text,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier
                                .wrapContentWidth()
                                .padding(
                                    end = if (chat.isFromUser) 8.dp else 0.dp,
                                    start = if (!chat.isFromUser) 8.dp else 0.dp
                                )
                                .widthIn(max = screenWidth * 0.8F)
                                .align(if (chat.isFromUser) Alignment.CenterEnd else Alignment.CenterStart)
                                .background(
                                    if (chat.isFromUser) Color(0xFF99D0FC) else Color.Transparent,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .padding(8.dp)

                        )
                    }
                }
            }
        }

        is ChatUiState.Initialize -> {
            Box(
                modifier = modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
            ) {
                Text(
                    "How can I help you with?",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    }
}

@Composable
fun MessageInput(
    viewModel: ChatViewModel,
    onSubmit: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current
    val uiState by viewModel.chatUiState.collectAsState()

    var inputValue by remember {
        mutableStateOf("")
    }
    val isLoading = uiState is ChatUiState.Answering

    TextField(
        value = inputValue,
        enabled = !isLoading,
        onValueChange = {
            inputValue = it
        },
        placeholder = {
            Text("Any thought?")
        },
        trailingIcon = {
            if (isLoading) {
                IconButton(onClick = {
                    viewModel.stopGenerate()
                }) {
                    CircularProgressIndicator(
                        modifier = Modifier.width(64.dp),
                        color = MaterialTheme.colorScheme.secondary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    )
                    Icon(imageVector = Icons.Filled.Clear, contentDescription = "")
                }
            } else {
                IconButton(onClick = {
                    focusManager.clearFocus()
                    onSubmit.invoke(inputValue)
                    inputValue = ""
                }) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.Send, contentDescription = "")
                }
            }

        },
        shape = RoundedCornerShape(8.dp),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color.White,
            unfocusedContainerColor = Color.White,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            unfocusedPlaceholderColor = Color.Black.copy(alpha = 0.5F)
        ),
        modifier = modifier.shadow(
            shape = RoundedCornerShape(8.dp),
            elevation = 4.dp
        )
    )
}


@Preview
@Composable
fun ChatPreview() {
    ChatScreen()
}