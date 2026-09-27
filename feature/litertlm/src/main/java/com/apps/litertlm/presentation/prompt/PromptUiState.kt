package com.apps.litertlm.presentation.prompt

import androidx.compose.runtime.Immutable

@Immutable
data class PromptUiState(
    val currentModel: ModelInfo? = null,
    val messages: List<ChatMessage> = emptyList(),
    val isGenerating: Boolean = false,
    val enableThinking: Boolean = false,
    val isLoadingModel: Boolean = false,
    val errorLoadModel: String = ""
) {
    val canSendMessage: Boolean
        get() = currentModel != null && !isGenerating
}

@Immutable
data class ModelInfo(
    val name: String,
    val filePath: String,
)

@Immutable
data class ChatMessage(
    val id: Long,
    val isMe: Boolean,
    val text: String,
    val isStreaming: Boolean = false,
    val thinking: String = ""
) {
    val hasThinking get() = thinking.isNotEmpty()
    val isLoading = isStreaming && text.isEmpty() && !isMe
}

