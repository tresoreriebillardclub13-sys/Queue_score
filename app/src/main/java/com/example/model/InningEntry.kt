package com.example.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class InningEntry(
    val inningNumber: Int,
    val playerKey: String, // "PLAYER_A" or "PLAYER_B"
    val playerName: String,
    val pointsScored: Int,
    val aCumulativeScore: Int,
    val bCumulativeScore: Int,
    val timestamp: Long = System.currentTimeMillis()
)
