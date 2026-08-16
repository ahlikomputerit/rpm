package com.ahlikomputerit.lumentransfer.domain.protocol

import org.junit.Assert.assertEquals
import org.junit.Test

class FountainCodecTest {
    @Test
    fun `repair budget uses fifty percent with minimum four`() {
        assertEquals(0, FountainCodec.repairFrameBudget(0))
        assertEquals(4, FountainCodec.repairFrameBudget(1))
        assertEquals(8, FountainCodec.repairFrameBudget(16))
        assertEquals(9, FountainCodec.repairFrameBudget(17))
        assertEquals(50, FountainCodec.repairFrameBudget(100))
    }
}
