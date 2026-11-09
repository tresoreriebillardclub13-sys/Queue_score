package com.example.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.util.UUID

/**
 * Modèle de données strict pour le score de Billard Carambole.
 * Respecte rigoureusement la nomenclature :
 * - Champs Joueur A : préfixe `a_`
 * - Champs Joueur B : préfixe `b_`
 * - Champs Globaux : sans préfixe
 */
@JsonClass(generateAdapter = true)
data class MatchState(
    @Json(name = "match_id") val match_id: String = UUID.randomUUID().toString(),
    @Json(name = "match_status") val match_status: String = STATUS_EN_COURS, // ECHAUFFEMENT, EN_COURS, PAUSE, TERMINE
    @Json(name = "current_turn") val current_turn: String = TURN_PLAYER_A, // PLAYER_A, PLAYER_B
    @Json(name = "game_mode") val game_mode: String = MODE_3_BANDES, // 3_BANDES, LIBRE, CADRE

    // Champs Joueur A (a_xxx)
    @Json(name = "a_id") val a_id: String = "player_a",
    @Json(name = "a_name") val a_name: String = "Joueur 1",
    @Json(name = "a_score") val a_score: Int = 0,
    @Json(name = "a_current_run") val a_current_run: Int = 0,
    @Json(name = "a_innings") val a_innings: Int = 0,
    @Json(name = "a_high_run") val a_high_run: Int = 0,
    @Json(name = "a_timeouts_left") val a_timeouts_left: Int = 3,
    @Json(name = "a_color") val a_color: String = COLOR_WHITE, // Bille Blanche #FFFFFF

    // Champs Joueur B (b_xxx)
    @Json(name = "b_id") val b_id: String = "player_b",
    @Json(name = "b_name") val b_name: String = "Joueur 2",
    @Json(name = "b_score") val b_score: Int = 0,
    @Json(name = "b_current_run") val b_current_run: Int = 0,
    @Json(name = "b_innings") val b_innings: Int = 0,
    @Json(name = "b_high_run") val b_high_run: Int = 0,
    @Json(name = "b_timeouts_left") val b_timeouts_left: Int = 3,
    @Json(name = "b_color") val b_color: String = COLOR_YELLOW, // Bille Jaune #FACC15

    // Champs Globaux
    @Json(name = "target_score") val target_score: Int = 40,
    @Json(name = "max_innings") val max_innings: Int? = null,
    @Json(name = "shot_clock_seconds") val shot_clock_seconds: Int = 40,
    @Json(name = "timer_remaining") val timer_remaining: Int = 40,
    @Json(name = "updated_at") val updated_at: Long = System.currentTimeMillis()
) {
    companion object {
        const val STATUS_ECHAUFFEMENT = "ECHAUFFEMENT"
        const val STATUS_EN_COURS = "EN_COURS"
        const val STATUS_PAUSE = "PAUSE"
        const val STATUS_TERMINE = "TERMINE"

        const val TURN_PLAYER_A = "PLAYER_A"
        const val TURN_PLAYER_B = "PLAYER_B"

        const val MODE_3_BANDES = "Carambole 3 Bandes"
        const val MODE_LIBRE = "Carambole Partie Libre"
        const val MODE_CADRE = "Carambole Cadre"

        const val COLOR_WHITE = "#FFFFFF"
        const val COLOR_YELLOW = "#FACC15"
    }

    /**
     * Calcul de la moyenne générale du Joueur A
     */
    fun aAverage(): Double {
        if (a_innings == 0) return 0.0
        return a_score.toDouble() / a_innings.toDouble()
    }

    /**
     * Calcul de la moyenne générale du Joueur B
     */
    fun bAverage(): Double {
        if (b_innings == 0) return 0.0
        return b_score.toDouble() / b_innings.toDouble()
    }
}
