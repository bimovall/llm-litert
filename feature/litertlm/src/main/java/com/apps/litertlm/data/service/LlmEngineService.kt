package com.apps.litertlm.data.service

import android.content.Context
import com.apps.litertlm.data.model.ChatProvider
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Channel
import com.google.ai.edge.litertlm.Conversation
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.ExperimentalApi
import com.google.ai.edge.litertlm.Message
import com.google.ai.edge.litertlm.MessageCallback
import com.google.ai.edge.litertlm.ThinkingConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LlmEngineService @Inject constructor(@param:ApplicationContext private val context: Context) {
    private var engine: Engine? = null
    private var conversation: Conversation? = null

    suspend fun getOrLoadModel(path: String): Engine = withContext(Dispatchers.IO){

        val cacheDir = File("${context.cacheDir}/model/")
        if (!cacheDir.exists()) {
            cacheDir.mkdirs()
        }
        val engineConfig = EngineConfig(
            modelPath = path,
            backend = Backend.CPU(),
            cacheDir = cacheDir.absolutePath

        )

        engine = Engine(engineConfig)
        engine?.initialize()
        return@withContext engine!!

    }

    @OptIn(ExperimentalApi::class)
    fun createConversation(): Conversation? {
        return engine?.createConversation(
            ConversationConfig(
                channels = listOf(
                    Channel("thinking", start = "<|channel>", end = "<channel|>"),
                    Channel("thinking", start = "<think>", end = "</think>")
                ),
                thinkingConfig = ThinkingConfig(enableThinking = true),
                initialMessages = listOf(
                    Message.user("What is the color of an apple?"),
                    Message.model("It's red"),
                    Message.user("What is the color of an orange?"),
                    Message.model("It's orange"),
                ),
            )
        )
    }

    @OptIn(ExperimentalApi::class)
    fun sendMessage(prompt: String, isEnableThinking: Boolean = false): Flow<ChatProvider> {
        val conversation =
            conversation ?: createConversation() ?: error("Error creating conversation")
        return callbackFlow {
            val callback = object : MessageCallback {
                override fun onMessage(message: Message) {
                    trySend(message)
                }

                override fun onDone() {
                    close()
                }

                override fun onError(throwable: Throwable) {
                    throwable.printStackTrace()
                    close(throwable)
                }

            }
            conversation.sendMessageAsync(prompt, callback, thinkingConfig = ThinkingConfig(enableThinking = isEnableThinking))
            awaitClose { conversation.cancelProcess() }
        }
            .map {
                val thought = it.channels["thinking"]
                if (!thought.isNullOrEmpty()) {
                    ChatProvider.Thinking(thought)
                } else {
                    ChatProvider.Message(it.toString())
                }
            }
            .flowOn(Dispatchers.Default)

    }

    fun clear() {
        engine?.close()
    }
}