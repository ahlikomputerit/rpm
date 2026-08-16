package com.ahlikomputerit.lumentransfer.data.file

import android.content.ContentResolver
import android.net.Uri
import com.ahlikomputerit.lumentransfer.domain.diagnostics.DiagnosticsJson
import com.ahlikomputerit.lumentransfer.domain.diagnostics.TransferDiagnostics

object DiagnosticsFileWriter {
    fun write(contentResolver: ContentResolver, uri: Uri, snapshot: TransferDiagnostics) {
        contentResolver.openOutputStream(uri, "w")?.use { output ->
            output.writer(Charsets.UTF_8).use { writer ->
                writer.write(DiagnosticsJson.encode(snapshot))
            }
        } ?: error("Unable to open diagnostics output")
    }
}
