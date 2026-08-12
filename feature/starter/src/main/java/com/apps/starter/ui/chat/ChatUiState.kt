package com.apps.starter.ui.chat

import com.apps.starter.ui.chat.model.Chat

sealed class ChatUiState {

    data object Initialize : ChatUiState()

    data class Answering(val chat: List<Chat>) : ChatUiState()

    data class Stopped(val chat: List<Chat>) : ChatUiState()

}