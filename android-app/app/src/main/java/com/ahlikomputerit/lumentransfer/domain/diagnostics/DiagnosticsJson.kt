package com.ahlikomputerit.lumentransfer.domain.diagnostics

import java.util.Locale

object DiagnosticsJson {
    fun encode(snapshot: TransferDiagnostics): String {
        val goodput = String.format(Locale.US, "%.3f", snapshot.goodputBytesPerSecond)
        return buildString {
            appendLine("{")
            appendLine("  \"schemaVersion\": 1,")
            appendLine("  \"role\": \"${snapshot.role}\",")
            appendLine("  \"startedAtMs\": ${snapshot.startedAtMs ?: "null"},")
            appendLine("  \"endedAtMs\": ${snapshot.endedAtMs ?: "null"},")
            appendLine("  \"elapsedMs\": ${snapshot.elapsedMs},")
            appendLine("  \"emittedFrames\": ${snapshot.emittedFrames},")
            appendLine("  \"acceptedFrames\": ${snapshot.acceptedFrames},")
            appendLine("  \"duplicateFrames\": ${snapshot.duplicateFrames},")
            appendLine("  \"rejectedFrames\": ${snapshot.rejectedFrames},")
            appendLine("  \"emittedBytes\": ${snapshot.emittedBytes},")
            appendLine("  \"acceptedBytes\": ${snapshot.acceptedBytes},")
            appendLine("  \"systematicFrames\": ${snapshot.systematicFrames},")
            appendLine("  \"repairFrames\": ${snapshot.repairFrames},")
            appendLine("  \"sourceBlocks\": ${snapshot.sourceBlocks},")
            appendLine("  \"recoveredBlocks\": ${snapshot.recoveredBlocks},")
            appendLine("  \"equationCount\": ${snapshot.equationCount},")
            appendLine("  \"lastSequence\": ${snapshot.lastSequence ?: "null"},")
            appendLine("  \"goodputBytesPerSecond\": $goodput,")
            appendLine("  \"terminalPhase\": \"${snapshot.terminalPhase}\",")
            appendLine("  \"error\": ${snapshot.error?.let { "\"$it\"" } ?: "null"}")
            appendLine("}")
        }
    }
}
