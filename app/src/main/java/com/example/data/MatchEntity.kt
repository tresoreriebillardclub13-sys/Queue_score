package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "matches")
data class MatchEntity(
    @PrimaryKey val matchId: String,
    val matchStatus: String,
    val gameMode: String,
    val aName: String,
    val aScore: Int,
    val aInnings: Int,
    val aHighRun: Int,
    val aAverage: Double,
    val bName: String,
    val bScore: Int,
    val bInnings: Int,
    val bHighRun: Int,
    val bAverage: Double,
    val targetScore: Int,
    val winnerName: String?,
    val totalTimeSeconds: Long,
    val jsonDetails: String,
    val createdAt: Long = System.currentTimeMillis()
)
