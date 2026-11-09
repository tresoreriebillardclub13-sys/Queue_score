package com.example.viewmodel

import android.app.Application
import android.util.Log
import android.view.KeyEvent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.MatchEntity
import com.example.model.InningEntry
import com.example.model.MatchState
import com.example.server.NetworkUtils
import com.example.server.StreamServer
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.ArrayDeque

class ScoreboardViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val matchDao = db.matchDao()

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()
    private val matchStateAdapter = moshi.adapter(MatchState::class.java)

    // Serveur HTTP/WebSocket embarqué pour streaming OBS
    val streamServer = StreamServer(port = 8080)
    val localIp: String = NetworkUtils.getLocalIpAddress()

    private val _matchState = MutableStateFlow(MatchState())
    val matchState: StateFlow<MatchState> = _matchState.asStateFlow()

    private val _inningLogs = MutableStateFlow<List<InningEntry>>(emptyList())
    val inningLogs: StateFlow<List<InningEntry>> = _inningLogs.asStateFlow()

    private val _savedMatches = MutableStateFlow<List<MatchEntity>>(emptyList())
    val savedMatches: StateFlow<List<MatchEntity>> = _savedMatches.asStateFlow()

    // Événements d'alerte sonore / visuelle
    private val _audioAlertEvent = MutableSharedFlow<String>()
    val audioAlertEvent: SharedFlow<String> = _audioAlertEvent.asSharedFlow()

    // Historique d'annulation (Undo)
    private val undoStack = ArrayDeque<Pair<MatchState, List<InningEntry>>>()

    private var timerJob: Job? = null
    private var matchStartTimeMillis: Long = System.currentTimeMillis()

    init {
        // Démarrage du serveur local
        val initialJson = matchStateAdapter.toJson(_matchState.value)
        streamServer.start(initialJson)

        // Observation de l'historique Room
        viewModelScope.launch {
            matchDao.getAllMatches().collect { list ->
                _savedMatches.value = list
            }
        }

        // Lancement du timer de coup
        startTimerJob()
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
        streamServer.stop()
    }

    private fun pushUndoState() {
        if (undoStack.size > 20) {
            undoStack.removeLast()
        }
        undoStack.push(Pair(_matchState.value, _inningLogs.value))
    }

    private fun broadcastCurrentState() {
        val json = matchStateAdapter.toJson(_matchState.value)
        streamServer.broadcastState(json)
    }

    private fun startTimerJob() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (isActive) {
                delay(1000)
                val currentState = _matchState.value
                if (currentState.match_status == MatchState.STATUS_EN_COURS && currentState.shot_clock_seconds > 0) {
                    val remaining = currentState.timer_remaining - 1
                    if (remaining >= 0) {
                        _matchState.value = currentState.copy(
                            timer_remaining = remaining,
                            updated_at = System.currentTimeMillis()
                        )
                        broadcastCurrentState()

                        if (remaining == 10) {
                            _audioAlertEvent.emit("ALERT_10S")
                        } else if (remaining == 0) {
                            _audioAlertEvent.emit("ALERT_TIMEOUT")
                            // Temps écoulé : Pause auto du timer
                            _matchState.value = _matchState.value.copy(
                                match_status = MatchState.STATUS_PAUSE
                            )
                            broadcastCurrentState()
                        }
                    }
                }
            }
        }
    }

    // --- ACTIONS SCOREBOARD ---

    /**
     * +1 point pour le joueur actif
     */
    fun incrementCurrentRun(points: Int = 1) {
        if (_matchState.value.match_status == MatchState.STATUS_TERMINE) return
        pushUndoState()

        val current = _matchState.value
        _matchState.value = if (current.current_turn == MatchState.TURN_PLAYER_A) {
            current.copy(
                a_current_run = current.a_current_run + points,
                updated_at = System.currentTimeMillis()
            )
        } else {
            current.copy(
                b_current_run = current.b_current_run + points,
                updated_at = System.currentTimeMillis()
            )
        }
        broadcastCurrentState()
    }

    /**
     * -1 point pour le joueur actif (correction)
     */
    fun decrementCurrentRun(points: Int = 1) {
        if (_matchState.value.match_status == MatchState.STATUS_TERMINE) return
        pushUndoState()

        val current = _matchState.value
        _matchState.value = if (current.current_turn == MatchState.TURN_PLAYER_A) {
            val newRun = (current.a_current_run - points).coerceAtLeast(0)
            current.copy(a_current_run = newRun, updated_at = System.currentTimeMillis())
        } else {
            val newRun = (current.b_current_run - points).coerceAtLeast(0)
            current.copy(b_current_run = newRun, updated_at = System.currentTimeMillis())
        }
        broadcastCurrentState()
    }

    /**
     * Validation de la reprise : Ajoute la série au score total, met à jour les séries et reprises,
     * puis passe la main au joueur suivant.
     */
    fun commitTurn() {
        if (_matchState.value.match_status == MatchState.STATUS_TERMINE) return
        pushUndoState()

        val current = _matchState.value
        val isPlayerA = current.current_turn == MatchState.TURN_PLAYER_A

        val updatedAInnings = if (isPlayerA) current.a_innings + 1 else current.a_innings
        val updatedBInnings = if (!isPlayerA) current.b_innings + 1 else current.b_innings

        val updatedAScore = if (isPlayerA) current.a_score + current.a_current_run else current.a_score
        val updatedBScore = if (!isPlayerA) current.b_score + current.b_current_run else current.b_score

        val updatedAHighRun = if (isPlayerA) maxOf(current.a_high_run, current.a_current_run) else current.a_high_run
        val updatedBHighRun = if (!isPlayerA) maxOf(current.b_high_run, current.b_current_run) else current.b_high_run

        // Log de la feuille de match
        val inningNum = if (isPlayerA) updatedAInnings else updatedBInnings
        val entry = InningEntry(
            inningNumber = inningNum,
            playerKey = current.current_turn,
            playerName = if (isPlayerA) current.a_name else current.b_name,
            pointsScored = if (isPlayerA) current.a_current_run else current.b_current_run,
            aCumulativeScore = updatedAScore,
            bCumulativeScore = updatedBScore
        )
        _inningLogs.value = _inningLogs.value + entry

        // Passage de la main
        val nextTurn = if (isPlayerA) MatchState.TURN_PLAYER_B else MatchState.TURN_PLAYER_A

        // Vérification de fin de match
        val isTargetReached = updatedAScore >= current.target_score || updatedBScore >= current.target_score
        val isInningsReached = current.max_innings != null && updatedAInnings >= current.max_innings && updatedBInnings >= current.max_innings

        val nextStatus = if (isTargetReached || isInningsReached) MatchState.STATUS_TERMINE else current.match_status

        _matchState.value = current.copy(
            a_score = updatedAScore,
            a_current_run = 0,
            a_innings = updatedAInnings,
            a_high_run = updatedAHighRun,

            b_score = updatedBScore,
            b_current_run = 0,
            b_innings = updatedBInnings,
            b_high_run = updatedBHighRun,

            current_turn = nextTurn,
            timer_remaining = current.shot_clock_seconds,
            match_status = nextStatus,
            updated_at = System.currentTimeMillis()
        )

        broadcastCurrentState()

        if (nextStatus == MatchState.STATUS_TERMINE) {
            saveMatchToHistory()
        }
    }

    /**
     * Demander un temps mort / extension de chronomètre (+30s / +40s)
     */
    fun requestTimeout() {
        val current = _matchState.value
        val isPlayerA = current.current_turn == MatchState.TURN_PLAYER_A

        if (isPlayerA && current.a_timeouts_left > 0) {
            pushUndoState()
            _matchState.value = current.copy(
                a_timeouts_left = current.a_timeouts_left - 1,
                timer_remaining = current.timer_remaining + 40,
                updated_at = System.currentTimeMillis()
            )
            broadcastCurrentState()
        } else if (!isPlayerA && current.b_timeouts_left > 0) {
            pushUndoState()
            _matchState.value = current.copy(
                b_timeouts_left = current.b_timeouts_left - 1,
                timer_remaining = current.timer_remaining + 40,
                updated_at = System.currentTimeMillis()
            )
            broadcastCurrentState()
        }
    }

    /**
     * Pause / Reprise du chronomètre
     */
    fun toggleTimerPause() {
        val current = _matchState.value
        if (current.match_status == MatchState.STATUS_TERMINE) return

        val newStatus = if (current.match_status == MatchState.STATUS_EN_COURS) {
            MatchState.STATUS_PAUSE
        } else {
            MatchState.STATUS_EN_COURS
        }

        _matchState.value = current.copy(
            match_status = newStatus,
            updated_at = System.currentTimeMillis()
        )
        broadcastCurrentState()
    }

    /**
     * Annulation de la dernière action (Undo)
     */
    fun undoLastTurn() {
        if (undoStack.isNotEmpty()) {
            val (previousState, previousLogs) = undoStack.pop()
            _matchState.value = previousState
            _inningLogs.value = previousLogs
            broadcastCurrentState()
        }
    }

    /**
     * Inverser Joueur A et Joueur B
     */
    fun swapPlayers() {
        pushUndoState()
        val c = _matchState.value
        _matchState.value = c.copy(
            a_name = c.b_name,
            a_score = c.b_score,
            a_current_run = c.b_current_run,
            a_innings = c.b_innings,
            a_high_run = c.b_high_run,
            a_timeouts_left = c.b_timeouts_left,
            a_color = c.b_color,

            b_name = c.a_name,
            b_score = c.a_score,
            b_current_run = c.a_current_run,
            b_innings = c.a_innings,
            b_high_run = c.a_high_run,
            b_timeouts_left = c.a_timeouts_left,
            b_color = c.a_color,

            current_turn = if (c.current_turn == MatchState.TURN_PLAYER_A) MatchState.TURN_PLAYER_B else MatchState.TURN_PLAYER_A,
            updated_at = System.currentTimeMillis()
        )
        broadcastCurrentState()
    }

    /**
     * Démarrer une nouvelle partie
     */
    fun startNewMatch(
        aName: String,
        bName: String,
        targetScore: Int,
        shotClockSeconds: Int,
        maxInnings: Int?,
        gameMode: String
    ) {
        matchStartTimeMillis = System.currentTimeMillis()
        undoStack.clear()
        _inningLogs.value = emptyList()

        _matchState.value = MatchState(
            match_status = MatchState.STATUS_EN_COURS,
            game_mode = gameMode,
            current_turn = MatchState.TURN_PLAYER_A,

            a_name = aName.ifBlank { "Joueur 1" },
            a_score = 0,
            a_current_run = 0,
            a_innings = 0,
            a_high_run = 0,
            a_timeouts_left = 3,
            a_color = MatchState.COLOR_WHITE,

            b_name = bName.ifBlank { "Joueur 2" },
            b_score = 0,
            b_current_run = 0,
            b_innings = 0,
            b_high_run = 0,
            b_timeouts_left = 3,
            b_color = MatchState.COLOR_YELLOW,

            target_score = targetScore.coerceAtLeast(10),
            max_innings = maxInnings,
            shot_clock_seconds = shotClockSeconds,
            timer_remaining = shotClockSeconds,
            updated_at = System.currentTimeMillis()
        )

        broadcastCurrentState()
    }

    /**
     * Sauvegarder la partie en cours dans la base de données Room
     */
    fun saveMatchToHistory() {
        val s = _matchState.value
        val durationSec = (System.currentTimeMillis() - matchStartTimeMillis) / 1000

        val winner = when {
            s.a_score > s.b_score -> s.a_name
            s.b_score > s.a_score -> s.b_name
            else -> "Égalité"
        }

        val entity = MatchEntity(
            matchId = s.match_id,
            matchStatus = s.match_status,
            gameMode = s.game_mode,
            aName = s.a_name,
            aScore = s.a_score,
            aInnings = s.a_innings,
            aHighRun = s.a_high_run,
            aAverage = s.aAverage(),
            bName = s.b_name,
            bScore = s.b_score,
            bInnings = s.b_innings,
            bHighRun = s.b_high_run,
            bAverage = s.bAverage(),
            targetScore = s.target_score,
            winnerName = winner,
            totalTimeSeconds = durationSec,
            jsonDetails = matchStateAdapter.toJson(s),
            createdAt = System.currentTimeMillis()
        )

        viewModelScope.launch {
            matchDao.insertMatch(entity)
        }
    }

    /**
     * Supprimer un match de l'historique
     */
    fun deleteMatchFromHistory(matchId: String) {
        viewModelScope.launch {
            matchDao.deleteMatchById(matchId)
        }
    }

    /**
     * Intercepteur d'événements Télécommande Bluetooth (HID)
     * - Flèche Droite / Vol+ : +1 Point
     * - Flèche Gauche / Vol- : -1 Point
     * - Entrée / DPAD_CENTER : Fin de reprise (Passer la main)
     * - Espace / Media Play-Pause : Pause/Reprise Timer
     */
    fun handleRemoteKeyEvent(event: KeyEvent): Boolean {
        if (event.action != KeyEvent.ACTION_DOWN) return false

        return when (event.keyCode) {
            KeyEvent.KEYCODE_DPAD_RIGHT,
            KeyEvent.KEYCODE_VOLUME_UP,
            KeyEvent.KEYCODE_NUMPAD_6 -> {
                incrementCurrentRun(1)
                true
            }
            KeyEvent.KEYCODE_DPAD_LEFT,
            KeyEvent.KEYCODE_VOLUME_DOWN,
            KeyEvent.KEYCODE_NUMPAD_4 -> {
                decrementCurrentRun(1)
                true
            }
            KeyEvent.KEYCODE_ENTER,
            KeyEvent.KEYCODE_NUMPAD_ENTER,
            KeyEvent.KEYCODE_DPAD_CENTER,
            KeyEvent.KEYCODE_BUTTON_A -> {
                commitTurn()
                true
            }
            KeyEvent.KEYCODE_SPACE,
            KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> {
                toggleTimerPause()
                true
            }
            else -> false
        }
    }
}
