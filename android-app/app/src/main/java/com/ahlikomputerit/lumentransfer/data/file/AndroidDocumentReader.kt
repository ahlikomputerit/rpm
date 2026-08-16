package com.ahlikomputerit.lumentransfer.data.file

import android.content.ContentResolver
import android.net.Uri
import android.provider.OpenableColumns
import com.ahlikomputerit.lumentransfer.domain.integrity.Sha256Hasher
import com.ahlikomputerit.lumentransfer.domain.model.FileMetadata
import com.ahlikomputerit.lumentransfer.domain.model.TransferId
import java.io.ByteArrayOutputStream
import java.util.UUID

class AndroidDocumentReader(private val contentResolver: ContentResolver) {
    data class SelectedDocument(
        val uri: Uri,
        val metadata: FileMetadata,
    )

    fun read(uri: Uri): SelectedDocument {
        val (name, size, mimeType) = queryMetadata(uri)
        require(size <= 10L * 1024L * 1024L) { "File exceeds the MVP 10 MB limit" }
        val checksum = contentResolver.openInputStream(uri)?.use(Sha256Hasher::compute)
            ?: error("Unable to open selected document")
        val blockSize = 1024
        val blocks = if (size == 0L) 0 else ((size + blockSize - 1) / blockSize).toInt()
        return SelectedDocument(
            uri = uri,
            metadata = FileMetadata(
                transferId = TransferId(UUID.randomUUID().toBytes()),
                fileName = name,
                mimeType = mimeType,
                sizeBytes = size,
                sha256 = checksum,
                blockSize = blockSize,
                sourceBlockCount = blocks,
            ),
        )
    }

    fun open(uri: Uri) = contentResolver.openInputStream(uri)
        ?: error("Unable to open selected document")

    fun readPrefix(uri: Uri, maxBytes: Int): ByteArray {
        require(maxBytes > 0) { "Prefix size must be positive" }
        val output = ByteArrayOutputStream(maxBytes)
        open(uri).use { input ->
            val buffer = ByteArray(minOf(16 * 1024, maxBytes))
            while (output.size() < maxBytes) {
                val requested = minOf(buffer.size, maxBytes - output.size())
                val read = input.read(buffer, 0, requested)
                if (read < 0) break
                if (read > 0) output.write(buffer, 0, read)
            }
        } ?: error("Unable to open selected document")
        return output.toByteArray()
    }

    private fun queryMetadata(uri: Uri): Triple<String, Long, String> {
        var name = uri.lastPathSegment?.substringAfterLast('/')?.ifBlank { null } ?: "unnamed-file"
        var size = 0L
        contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)
            ?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (nameIndex >= 0) name = cursor.getString(nameIndex) ?: name
                    if (sizeIndex >= 0 && !cursor.isNull(sizeIndex)) size = cursor.getLong(sizeIndex)
                }
            }
        val mimeType = contentResolver.getType(uri) ?: "application/octet-stream"
        return Triple(name, size, mimeType)
    }
}

private fun UUID.toBytes(): ByteArray {
    val bytes = ByteArray(16)
    val high = mostSignificantBits
    val low = leastSignificantBits
    for (index in 0 until 8) bytes[index] = (high ushr (56 - index * 8)).toByte()
    for (index in 0 until 8) bytes[8 + index] = (low ushr (56 - index * 8)).toByte()
    return bytes
}
