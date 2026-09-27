package com.apps.litertlm.data.service

import android.content.Context
import android.net.Uri
import com.apps.litertlm.presentation.prompt.ModelInfo
import com.apps.litertlm.util.displayName
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

class ModelService @Inject constructor(@param:ApplicationContext private val context: Context) {

    suspend fun import(uri: Uri): ModelInfo = withContext(Dispatchers.IO) {
        val name = uri.displayName(context)
        val directory = File(context.filesDir, "models").apply {
            mkdirs()
        }
        val target = File(directory, name)
        if (target.exists() && target.length() > 0) return@withContext ModelInfo(
            name,
            target.absolutePath
        )
        val tmp = File(directory, "$name.part")
        try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                tmp.outputStream().use { outputStream ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    while (true) {
                        coroutineContext.ensureActive()
                        val n = inputStream.read(buffer)
                        if (n < 0) break
                        outputStream.write(buffer, 0, n)
                    }
                }
            } ?: error("Cannot open $uri")
            check(tmp.renameTo(target)) {
                "Could not move model into target"
            }
        } finally {
            tmp.delete()
        }

        ModelInfo(
            name = name,
            filePath = target.absolutePath,
        )
    }
}