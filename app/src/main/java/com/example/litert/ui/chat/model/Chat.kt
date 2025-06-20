package com.example.litert.ui.chat.model

data class Chat(
    val text: String,
    val isFromUser: Boolean
)

fun buildDummyChat(): List<Chat> = listOf(
    Chat("What is the meaning of liar?", isFromUser = true),
    Chat("Liar is someone who always like to lying", isFromUser = false),
)