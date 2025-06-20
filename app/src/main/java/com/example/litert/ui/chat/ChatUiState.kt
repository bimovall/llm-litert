package com.example.litert.ui.chat

import com.example.litert.ui.chat.model.Chat

sealed class ChatUiState {

    data object Initialize : ChatUiState()

    data class Answering(val chat: List<Chat>) : ChatUiState()

    data class Stopped(val chat: List<Chat>) : ChatUiState()

}