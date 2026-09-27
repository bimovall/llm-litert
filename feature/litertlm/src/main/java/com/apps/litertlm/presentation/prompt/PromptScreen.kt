package com.apps.litertlm.presentation.prompt

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.apps.litertlm.presentation.component.BubbleChat
import com.apps.litertlm.presentation.component.LoadingModelIndicator
import com.apps.litertlm.presentation.component.MessageInputField
import com.apps.litertlm.presentation.component.ModelInfoPopup

@Composable
fun PromptScreen(modifier: Modifier = Modifier) {
    val viewModel: PromptViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    PromptContent(modifier, viewModel::updateModel, viewModel::send, viewModel::onEnableThinking, uiState)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PromptContent(
    modifier: Modifier = Modifier,
    onUpdateModel: (Uri) -> Unit,
    onSendMessage: (String) -> Unit,
    onEnableThinking: (Boolean) -> Unit,
    uiState: PromptUiState
) {

    var showModelPopup by remember { mutableStateOf(false) }
    var inputText by remember { mutableStateOf("") }
    var isThinkingExpanded by remember { mutableStateOf(false) }

    val modelPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        showModelPopup = false
        if (uri != null) {

            onUpdateModel(
                uri
            )
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(text = "Chat") },
                actions = {
                    IconButton(
                        modifier = Modifier.padding(end = 8.dp),
                        onClick = { showModelPopup = true }
                    ) {
                        Icon(
                            Icons.Outlined.MoreVert,
                            contentDescription = "Model options"
                        )
                    }

                    if (showModelPopup) {
                        ModelInfoPopup(
                            model = uiState.currentModel,
                            onDismiss = { showModelPopup = false },
                            onLoadFromStorage = { modelPicker.launch(arrayOf("*/*")) }
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            when {
                uiState.isLoadingModel -> {
                    Spacer(modifier = Modifier.weight(1F))
                    LoadingModelIndicator(
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                    Spacer(modifier = Modifier.weight(1F))
                }
                uiState.messages.isNotEmpty() -> {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        items(uiState.messages, key = {
                            it.id
                        }) {
                            Box(
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                BubbleChat(
                                    modifier = Modifier.align(if (it.isMe) Alignment.CenterEnd else Alignment.CenterStart),
                                    isMe = it.isMe,
                                    message = it.text,
                                    isThinkingAvailable = it.hasThinking,
                                    thinking = it.thinking,
                                    isLoading = it.isLoading,
                                    isExpandThinking = isThinkingExpanded,
                                    onExpandThinking = {
                                        isThinkingExpanded = it
                                    }
                                )
                            }
                        }
                    }
                }

                uiState.currentModel != null -> {
                    Spacer(modifier = Modifier.weight(1F))
                    Image(
                        imageVector = Icons.Filled.Search,
                        contentDescription = null,
                        colorFilter = ColorFilter.tint(Color.Blue),
                        modifier = Modifier
                            .size(48.dp)
                            .align(Alignment.CenterHorizontally)
                    )
                    Text(
                        "Model has been loaded successfully, let's chat",
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                    Spacer(modifier = Modifier.weight(1F))
                }

                else -> {
                    Spacer(modifier = Modifier.weight(1F))
                    Image(
                        imageVector = Icons.Filled.Notifications,
                        contentDescription = null,
                        colorFilter = ColorFilter.tint(Color.Blue),
                        modifier = Modifier
                            .size(48.dp)
                            .align(Alignment.CenterHorizontally)
                    )
                    Text(
                        "Hello, don't forget to choose model before start chatting",
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.weight(1F))
                }
            }

            MessageInputField(
                value = inputText,
                onValueChanged = { inputText = it },
                onSend = {
                    onSendMessage(it)
                    inputText = ""
                },
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .imePadding(),
                enabled = uiState.canSendMessage,
                enableThinking = uiState.enableThinking,
                onEnableThinking = onEnableThinking
            )
        }
    }
}

@Preview(apiLevel = 36)
@Composable
fun PromptScreenPreview() {
    PromptContent(
        onUpdateModel = {},
        onSendMessage = {},
        onEnableThinking = {},
        uiState = PromptUiState()
    )
}
