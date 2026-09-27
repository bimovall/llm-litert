package com.apps.litertlm.util

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import java.io.File

fun Uri.displayName(context: Context): String {
    if (scheme == "file") return lastPathSegment ?: toString()
    context.contentResolver.query(this, null, null, null, null)?.use { cursor ->
        val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (index >= 0 && cursor.moveToFirst()) {
            cursor.getString(index)?.let { return it }
        }
    }
    return lastPathSegment ?: toString()
}


fun getModelPathFromUri(context: Context, uri: Uri): String {
    val fileName = "model.litertlm"
    val cacheFile = File(context.cacheDir, fileName)

    context.contentResolver.openInputStream(uri)?.use { inputStream ->
        cacheFile.outputStream().use { outputStream ->
            inputStream.copyTo(outputStream)
        }
    }
    return cacheFile.absolutePath
}