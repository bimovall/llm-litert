package com.example.litert.ui.chat

import android.content.res.AssetFileDescriptor
import android.content.res.AssetManager
import android.util.JsonReader
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.litert.tokenization.GPT2Tokenizer
import com.example.litert.ui.chat.model.Chat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.tensorflow.lite.Interpreter
import java.io.BufferedReader
import java.io.FileInputStream
import java.io.InputStreamReader
import java.nio.channels.FileChannel

private const val VOCAB_SIZE = 49152
private const val NUM_LITE_THREADS = 2
private const val MODEL_PATH = "SmolLM-135M-Instruct_seq128_q8_ekv1280.tflite"
private const val VOCAB_PATH = "vocab.json"
private const val MERGES_PATH = "merges.txt"
private const val EOS_TOKEN_ID = 2

class ChatViewModel : ViewModel() {
    private var initJob: Job? = null
    private var generateJob: Job? = null
    private lateinit var tokenizer: GPT2Tokenizer
    private lateinit var tflite: Interpreter

    private var maxSeqLen: Int = 0
    private var maxKvCacheSeqLen: Int = 0
    private lateinit var prefillSignatureName: String
    private var isInitialized = false

    private var historyChat = arrayListOf<Chat>()

    private var _chatUiState = MutableStateFlow<ChatUiState>(ChatUiState.Initialize)
    val chatUiState get() = _chatUiState.asStateFlow()

    fun init(asset: AssetManager) {
        if (isInitialized) return
        initJob?.cancel()
        initJob = viewModelScope.launch {
            val encoder = loadEncoder(asset)
            val decoder = encoder.entries.associateBy({ it.value }, { it.key })
            val bpeRanks = loadBpeRanks(asset)

            tokenizer = GPT2Tokenizer(encoder, decoder, bpeRanks)
            tflite = loadModel(asset.openFd(MODEL_PATH))
            isInitialized = true
        }
    }

    private suspend fun loadModel(assetFileDescriptor: AssetFileDescriptor): Interpreter =
        withContext(Dispatchers.IO) {
            assetFileDescriptor.use {
                val fileChannel = FileInputStream(assetFileDescriptor.fileDescriptor).channel
                val modelBuffer = fileChannel.map(
                    FileChannel.MapMode.READ_ONLY,
                    it.startOffset,
                    it.declaredLength
                )

                val opts = Interpreter.Options()
                opts.useXNNPACK = true
                opts.setNumThreads(NUM_LITE_THREADS)
                return@use Interpreter(modelBuffer, opts)
            }
        }

    private suspend fun loadEncoder(asset: AssetManager): Map<String, Int> =
        withContext(Dispatchers.IO) {
            hashMapOf<String, Int>().apply {
                val vocabStream = asset.open(VOCAB_PATH)
                vocabStream.use {
                    val vocabReader = JsonReader(InputStreamReader(it, "UTF-8"))
                    vocabReader.beginObject()
                    while (vocabReader.hasNext()) {
                        val key = vocabReader.nextName()
                        val value = vocabReader.nextInt()
                        put(key, value)
                    }
                    vocabReader.close()
                }
            }
        }

    private suspend fun loadBpeRanks(asset: AssetManager): Map<Pair<String, String>, Int> =
        withContext(Dispatchers.IO) {
            hashMapOf<Pair<String, String>, Int>().apply {
                val mergesStream = asset.open(MERGES_PATH)
                mergesStream.use { stream ->
                    val mergesReader = BufferedReader(InputStreamReader(stream))
                    mergesReader.useLines { seq ->
                        seq.drop(1).forEachIndexed { i, s ->
                            val list = s.split(" ")
                            val keyTuple = list[0] to list[1]
                            put(keyTuple, i)
                        }
                    }
                }
            }
        }

    fun stopGenerate() {
        _chatUiState.update {
            ChatUiState.Stopped(historyChat.toList())
        }
        generateJob?.cancel()
    }

    fun generateText(text: String, nbTokens: Int? = null) {
        generateJob?.cancel()
        generateJob = viewModelScope.launch(Dispatchers.Default) {
            val chat = Chat(
                text,
                true
            )
            historyChat.add(chat)

            _chatUiState.update {
                ChatUiState.Answering(historyChat.toList())
            }

            historyChat.add(Chat("", false))

            val tokenIds = tokenizer.encodeFullText(text).toIntArray()
            initPrefillRunner(tokenIds.size)

            val prefillTokenLength = tokenIds.size - 1

            val kvCache = runPrefill(tokenIds.copyOf(prefillTokenLength), prefillSignatureName)

            var actualMaxDecodeSteps = maxKvCacheSeqLen - prefillTokenLength - 1

            if (nbTokens != null) {
                actualMaxDecodeSteps = nbTokens
            }
            runDecode(
                prefillTokenLength,
                tokenIds[prefillTokenLength],
                kvCache,
                actualMaxDecodeSteps,
                EOS_TOKEN_ID,
                decodeToken = {
                    tokenizer.decode(listOf(it))
                },
                onAnswered = {
                    historyChat[historyChat.size - 1] = historyChat.last().copy(it)
                    _chatUiState.update {
                        ChatUiState.Answering(historyChat.toList())
                    }
                },
                onFinished = {
                    _chatUiState.update {
                        ChatUiState.Stopped(historyChat.toList())
                    }
                },
                tflite
            )
        }
    }

    private fun initPrefillRunner(numInputTokens: Int) {
        prefillSignatureName = getPrefillRunner(numInputTokens)
        // Get max sequence length
        val tokensShape = tflite.getInputTensorFromSignature("tokens", prefillSignatureName).shape()
        maxSeqLen = if (tokensShape.size == 1) tokensShape[0] else tokensShape[1]

        // Get max kv cache sequence length
        val kvShape =
            tflite.getInputTensorFromSignature("kv_cache_k_0", prefillSignatureName).shape()
        maxKvCacheSeqLen = kvShape[1]
    }

    private fun getPrefillRunner(numInputTokens: Int): String {
        //val allSignatures = tflite.signatureKeys // Assumes this returns a List<String>
        val allSignatures = listOf("decode", "prefill_128")
        var bestSignature: String? = null
        var delta = Int.MAX_VALUE
        var maxPrefillLen = -1

        for (key in allSignatures) {
            if (!key.contains("prefill")) continue

            val inputNames = tflite.getSignatureInputs(key)
            if (!inputNames.contains("input_pos")) continue

            val inputPosTensor = tflite.getInputTensorFromSignature("input_pos", key)
            val shape = inputPosTensor.shape() // e.g., [128]
            val seqSize = shape[0]
            maxPrefillLen = maxOf(maxPrefillLen, seqSize)

            if (numInputTokens <= seqSize && seqSize - numInputTokens < delta) {
                delta = seqSize - numInputTokens
                bestSignature = key
            }
        }

        requireNotNull(bestSignature) {
            "The largest prefill length supported is $maxPrefillLen, but we have $numInputTokens input tokens"
        }

        return bestSignature
    }

    private fun initKvCache(prefillSignature: String): MutableMap<String, Any> {
        val kvCache = mutableMapOf<String, Any>()
        val inputNames = tflite.getSignatureInputs(prefillSignature)

        for (inputName in inputNames) {
            if (inputName.contains("kv_cache")) {
                val tensor = tflite.getInputTensorFromSignature(inputName, prefillSignature)
                val shape = tensor.shape()

                val buffer = when (shape.size) {
                    4 -> Array(shape[0]) {
                        Array(shape[1]) {
                            Array(shape[2]) {
                                FloatArray(shape[3])
                            }
                        }
                    }

                    3 -> Array(shape[0]) {
                        Array(shape[1]) {
                            FloatArray(shape[2])
                        }
                    }

                    else -> error("Unsupported kv_cache shape: ${shape.joinToString()}")
                }

                kvCache[inputName] = buffer
            }
        }

        return kvCache
    }

    private fun runPrefill(
        prefillTokenIds: IntArray,
        prefillSignature: String
    ): MutableMap<String, Any> {
        val tokenLength = prefillTokenIds.size

        // Explicitly padded to maxSeqLen exactly like Python
        val inputTokens = arrayOf(IntArray(maxSeqLen) { i ->
            if (i < tokenLength) prefillTokenIds[i] else 0
        })

        // Explicit position padding to exactly match Python
        val inputPos = IntArray(maxSeqLen) { i ->
            if (i < tokenLength) i else 0
        }

        // Initialize kv cache
        val prefillInputs = initKvCache(prefillSignature).apply {
            this["tokens"] = inputTokens
            this["input_pos"] = inputPos
        }

        val prefillOutputs = mutableMapOf<String, Any>()
        val outputNames = tflite.getSignatureOutputs(prefillSignature)
        for (name in outputNames) {
            val tensor = tflite.getOutputTensorFromSignature(name, prefillSignature)
            val shape = tensor.shape()
            prefillOutputs[name] = createZeroArray(shape)
        }

        tflite.runSignature(prefillInputs, prefillOutputs, "prefill_128")
        prefillOutputs.remove("logits")

        return prefillOutputs
    }

    private fun createZeroArray(shape: IntArray): Any {
        return when (shape.size) {
            1 -> FloatArray(shape[0])
            2 -> Array(shape[0]) { FloatArray(shape[1]) }
            3 -> Array(shape[0]) { Array(shape[1]) { FloatArray(shape[2]) } }
            4 -> Array(shape[0]) { Array(shape[1]) { Array(shape[2]) { FloatArray(shape[3]) } } }
            else -> error("Unsupported tensor shape: ${shape.joinToString()}")
        }
    }

    private fun greedySampler(logits: FloatArray): Int {
        return logits.indices.maxByOrNull { logits[it] } ?: 0
    }

    private fun runDecode(
        startPos: Int,
        startTokenId: Int,
        kvCache: MutableMap<String, Any>,
        maxDecodeSteps: Int,
        eosTokenId: Int,
        decodeToken: (Int) -> String,
        onAnswered: (String) -> Unit,
        onFinished: (String) -> Unit,
        tflite: Interpreter
    ) {
        var nextPos = startPos
        var nextToken = startTokenId
        val decodeText = StringBuilder()
        val signatureName = "decode"

        // Prepare logits buffer once
        val logitsBuffer =
            Array(1) { Array(1) { FloatArray(VOCAB_SIZE) } }  // ensure correct vocab size
        val decodeOutputs = mutableMapOf<String, Any>("logits" to logitsBuffer)

        // Allocate KV-cache once and reuse
        val outputNames = tflite.getSignatureOutputs(signatureName)
        outputNames.filter { it != "logits" }.forEach { name ->
            val shape = tflite.getOutputTensorFromSignature(name, signatureName).shape()
            decodeOutputs[name] = Array(shape[0]) {
                Array(shape[1]) {
                    Array(shape[2]) { FloatArray(shape[3]) }
                }
            }
        }

        var decodeInputs = kvCache.toMutableMap()

        repeat(maxDecodeSteps) {
            if (generateJob?.isCancelled == true) {
                onFinished.invoke(decodeText.toString())
                return
            }
            decodeInputs["tokens"] = arrayOf(intArrayOf(nextToken))
            decodeInputs["input_pos"] = intArrayOf(nextPos)

            tflite.runSignature(decodeInputs, decodeOutputs, signatureName)

            val logits = logitsBuffer[0][0]
            nextToken = greedySampler(logits)

            if (nextToken == eosTokenId) {
                onFinished.invoke(decodeText.toString())
                return
            }

            decodeText.append(decodeToken(nextToken))

            onAnswered.invoke(decodeText.toString())

            // Update cache
            decodeInputs = decodeOutputs.filterKeys { it != "logits" }.toMutableMap()
            nextPos++
        }

        onFinished.invoke(decodeText.toString())
    }

    override fun onCleared() {
        super.onCleared()
        tflite.close()
    }

}