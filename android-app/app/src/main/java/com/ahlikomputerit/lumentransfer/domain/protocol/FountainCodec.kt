package com.ahlikomputerit.lumentransfer.domain.protocol

import kotlin.math.ceil
import kotlin.math.max

object FountainCodec {
    fun repairFrameBudget(sourceBlockCount: Int): Int =
        if (sourceBlockCount == 0) 0 else max(4, ceil(sourceBlockCount * 0.50).toInt())

    fun degree(seed: Long, sourceBlockCount: Int): Int {
        if (sourceBlockCount <= 1) return sourceBlockCount
        val bucket = ((mix(seed) ushr 1) % 100).toInt()
        return when {
            bucket < 35 -> 1
            bucket < 65 -> minOf(2, sourceBlockCount)
            bucket < 85 -> minOf(3, sourceBlockCount)
            else -> minOf(4 + ((mix(seed + 1) ushr 1) % 5).toInt(), sourceBlockCount)
        }
    }

    fun chooseIndices(seed: Long, sourceBlockCount: Int, degree: Int): IntArray {
        require(sourceBlockCount > 0) { "Cannot choose fountain blocks from an empty source" }
        require(degree in 1..sourceBlockCount) { "Fountain degree is out of range" }
        val chosen = LinkedHashSet<Int>(degree)
        var state = mix(seed)
        while (chosen.size < degree) {
            state = nextState(state)
            chosen += ((state ushr 1) % sourceBlockCount).toInt()
        }
        return chosen.toIntArray()
    }

    fun xorInto(target: ByteArray, source: ByteArray) {
        require(target.size == source.size) { "Fountain vectors must have equal sizes" }
        for (index in target.indices) target[index] = (target[index].toInt() xor source[index].toInt()).toByte()
    }

    private fun mix(value: Long): Long {
        var state = value xor -7046029254386353131L
        state = (state xor (state ushr 30)) * -4658895280553007687L
        state = (state xor (state ushr 27)) * -7723592293110705685L
        return state xor (state ushr 31)
    }

    private fun nextState(state: Long): Long {
        var value = state
        value = value xor (value shl 13)
        value = value xor (value ushr 7)
        return value xor (value shl 17)
    }
}
