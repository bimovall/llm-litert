package com.apps.litertlm.data.model

sealed interface ChatProvider {
    data class Thinking(val message: String): ChatProvider
    data class Message(val message: String): ChatProvider
}