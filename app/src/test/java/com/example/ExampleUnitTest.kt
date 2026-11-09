package com.example

import com.example.model.MatchState
import org.junit.Assert.assertEquals
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun testMatchStateDefaultsAndPrefixes() {
        val state = MatchState(
            a_name = "Joueur A",
            b_name = "Joueur B",
            target_score = 40
        )
        assertEquals("Joueur A", state.a_name)
        assertEquals("Joueur B", state.b_name)
        assertEquals(0, state.a_score)
        assertEquals(0, state.b_score)
        assertEquals(40, state.target_score)
        assertEquals("PLAYER_A", state.current_turn)
        assertEquals("EN_COURS", state.match_status)
    }

    @Test
    fun testAverageCalculations() {
        val state = MatchState(
            a_score = 30,
            a_innings = 20,
            b_score = 15,
            b_innings = 15
        )
        assertEquals(1.5, state.aAverage(), 0.001)
        assertEquals(1.0, state.bAverage(), 0.001)
    }
}
