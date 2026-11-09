package com.example

import com.example.model.MatchState
import org.junit.Assert.assertEquals
import org.junit.Test

class ExampleRobolectricTest {

    @Test
    fun testMatchState() {
        val state = MatchState(target_score = 40)
        assertEquals(40, state.target_score)
    }
}
