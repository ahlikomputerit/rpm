package com.ahlikomputerit.lumentransfer.data.file

import android.content.ContentResolver
import android.net.Uri
import java.io.File

interface DocumentSaver {
    fun save(source: File, target: Uri)
}

class AndroidDocumentSaver(private val contentResolver: ContentResolver) : DocumentSaver {
    override fun save(source: File, target: Uri) {
        contentResolver.openOutputStream(target, "w")?.use { output ->
            source.inputStream().use { input ->
                val buffer = ByteArray(16 * 1024)
                while (true) {
                    val read = input.read(buffer)
                    if (read < 0) break
                    if (read > 0) output.write(buffer, 0, read)
                }
                output.flush()
            }
        } ?: error("Unable to open output document")
    }
}

object UnavailableDocumentSaver : DocumentSaver {
    override fun save(source: File, target: Uri): Nothing =
        error("Document saver is unavailable in this test environment")
}

fun sanitizeDocumentName(rawName: String, fallback: String = "lumen-transfer.bin"): String {
    val sanitized = rawName
        .trim()
        .replace(Regex("[\\\\/:*?\"<>|\\p{Cntrl}]"), "_")
        .trim('.')
        .take(120)
    return sanitized.ifBlank { fallback }
}
