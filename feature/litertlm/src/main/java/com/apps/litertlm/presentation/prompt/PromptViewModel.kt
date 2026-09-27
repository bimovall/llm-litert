package com.apps.litertlm.presentation.prompt

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.apps.litertlm.data.model.ChatProvider
import com.apps.litertlm.data.service.LlmEngineService
import com.apps.litertlm.data.service.ModelService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PromptViewModel @Inject constructor(
    private val engineService: LlmEngineService,
    private val modelService: ModelService
) : ViewModel() {

    private val _uiState = MutableStateFlow(PromptUiState())
    val uiState = _uiState.asStateFlow()

    fun send(prompt: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val generatedId = System.nanoTime()
            _uiState.update {
                it.copy(
                    messages = it.messages +
                            ChatMessage(
                                id = generatedId - 1,
                                isMe = true,
                                text = prompt
                            ) +
                            ChatMessage(
                                id = generatedId,
                                isMe = false,
                                text = "",
                                isStreaming = true
                            )
                )
            }
            engineService.sendMessage(prompt, _uiState.value.enableThinking)
                .onCompletion { cause ->
                    _uiState.update {
                        it.copy(
                            messages = it.messages.map {
                                if (it.id == generatedId) {
                                    it.copy(isStreaming = false)
                                } else {
                                    it
                                }
                            },
                            isGenerating = false,
                        )
                    }
                }
                .collect { text ->
                    _uiState.update {
                        it.copy(
                            messages = it.messages.map {
                                when {
                                    it.id != generatedId -> it
                                    text is ChatProvider.Thinking -> {
                                        it.copy(thinking = it.thinking + text.message)
                                    }
                                    else -> {
                                        it.copy(text = it.text + (text as ChatProvider.Message).message)
                                    }
                                }
                            },
                            isGenerating = true
                        )
                    }
                }

        }
    }

    fun updateModel(uri: Uri) {
        if (_uiState.value.isLoadingModel) return
        _uiState.update {
            it.copy(
                isLoadingModel = true
            )
        }
        viewModelScope.launch {
            runCatching {
                val modelInfo = modelService.import(uri)
                engineService.getOrLoadModel(modelInfo.filePath)
                modelInfo
            }.fold(
                onSuccess = {modelInfo ->
                    _uiState.update {
                        it.copy(
                            currentModel = modelInfo,
                            isLoadingModel = false
                        )
                    }
                },
                onFailure = {throwable ->
                    _uiState.update {
                        it.copy(
                            isLoadingModel = false,
                            errorLoadModel = throwable.message.orEmpty()
                        )
                    }
                }
            )
        }
    }

    fun onEnableThinking(isEnabled: Boolean) {
        _uiState.update {
            it.copy(
                enableThinking = isEnabled
            )
        }
    }

    override fun onCleared() {
        engineService.clear()
    }

}