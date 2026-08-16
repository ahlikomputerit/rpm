package com.ahlikomputerit.lumentransfer.domain.protocol

import com.ahlikomputerit.lumentransfer.domain.model.FileMetadata
import com.ahlikomputerit.lumentransfer.domain.model.FrameEnvelope
import com.ahlikomputerit.lumentransfer.domain.model.FrameKind
import java.util.BitSet

class FountainDecoder(private val metadata: FileMetadata) {
    private data class Equation(val bits: BitSet, val payload: ByteArray)

    private val pivots = HashMap<Int, Equation>()
    private var solvedBlocks: Array<ByteArray>? = null

    fun accept(frame: FrameEnvelope): FountainProgress {
        require(frame.transferId == metadata.transferId) { "Transfer ID mismatch" }
        val equation = when (frame.kind) {
            FrameKind.SYSTEMATIC_DATA -> {
                val index = frame.seed.toInt()
                require(index in 0 until metadata.sourceBlockCount) { "Systematic block index is out of range" }
                require(frame.degree == 1) { "Systematic frame degree must be one" }
                Equation(BitSet(metadata.sourceBlockCount).apply { set(index) }, frame.payload.copyOf())
            }
            FrameKind.REPAIR_DATA -> {
                require(frame.degree in 1..metadata.sourceBlockCount) { "Repair degree is invalid" }
                val indices = FountainCodec.chooseIndices(frame.seed, metadata.sourceBlockCount, frame.degree)
                Equation(BitSet(metadata.sourceBlockCount).apply { indices.forEach(::set) }, frame.payload.copyOf())
            }
            else -> error("Frame kind is not a data frame")
        }
        require(equation.payload.size == metadata.blockSize) { "Fountain payload must equal block size" }
        addEquation(equation)
        if (pivots.size == metadata.sourceBlockCount) recover()
        return progress()
    }

    fun isComplete(): Boolean = solvedBlocks != null

    fun block(index: Int): ByteArray {
        require(index in 0 until metadata.sourceBlockCount) { "Block index is out of range" }
        return solvedBlocks?.get(index)?.copyOf() ?: error("Fountain decoder is incomplete")
    }

    fun progress(): FountainProgress = FountainProgress(
        recoveredBlocks = pivots.size,
        totalBlocks = metadata.sourceBlockCount,
        equationCount = pivots.size,
    )

    private fun addEquation(incoming: Equation) {
        val bits = incoming.bits.clone() as BitSet
        val payload = incoming.payload.copyOf()
        reduce(bits, payload)
        if (bits.isEmpty) return

        val pivot = bits.nextSetBit(0)
        val equation = Equation(bits, payload)
        for ((otherPivot, other) in pivots) {
            if (other.bits[pivot]) {
                xorBits(other.bits, bits)
                FountainCodec.xorInto(other.payload, payload)
            }
        }
        pivots[pivot] = equation
    }

    private fun reduce(bits: BitSet, payload: ByteArray) {
        for (pivot in pivots.keys.sorted()) {
            val equation = pivots[pivot] ?: continue
            if (bits[pivot]) {
                xorBits(bits, equation.bits)
                FountainCodec.xorInto(payload, equation.payload)
            }
        }
    }

    private fun recover() {
        val output = arrayOfNulls<ByteArray>(metadata.sourceBlockCount)
        for (pivot in metadata.sourceBlockCount - 1 downTo 0) {
            val equation = pivots[pivot] ?: return
            val value = equation.payload.copyOf()
            var next = equation.bits.nextSetBit(pivot + 1)
            while (next >= 0) {
                output[next]?.let { FountainCodec.xorInto(value, it) }
                next = equation.bits.nextSetBit(next + 1)
            }
            output[pivot] = value
        }
        if (output.all { it != null }) solvedBlocks = Array(metadata.sourceBlockCount) { output[it]!! }
    }

    private fun xorBits(target: BitSet, source: BitSet) {
        target.xor(source)
    }
}

data class FountainProgress(
    val recoveredBlocks: Int,
    val totalBlocks: Int,
    val equationCount: Int,
) {
    val isComplete: Boolean get() = recoveredBlocks >= totalBlocks
}
