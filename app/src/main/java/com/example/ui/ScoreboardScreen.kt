package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sports
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.MatchState
import com.example.ui.components.ControlPanel
import com.example.ui.components.HistoryDialog
import com.example.ui.components.NewMatchDialog
import com.example.ui.components.PlayerScoreCard
import com.example.ui.components.RemoteHelpDialog
import com.example.ui.components.ScoresheetDialog
import com.example.ui.components.ShotClockWidget
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.viewmodel.ScoreboardViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScoreboardScreen(
    viewModel: ScoreboardViewModel
) {
    val matchState by viewModel.matchState.collectAsState()
    val inningLogs by viewModel.inningLogs.collectAsState()
    val savedMatches by viewModel.savedMatches.collectAsState()

    var showScoresheet by remember { mutableStateOf(false) }
    var showNewMatchDialog by remember { mutableStateOf(false) }
    var showHistoryDialog by remember { mutableStateOf(false) }
    var showRemoteHelpDialog by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .testTag("scoreboard_root_scaffold"),
        containerColor = DarkBackground,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(EmeraldPrimary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Sports,
                                contentDescription = "Carambole",
                                tint = Color.Black,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "BILLARD CARAMBOLE",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Black,
                                    fontSize = 18.sp,
                                    color = TextPrimary
                                )
                            )
                            Text(
                                text = "${matchState.game_mode} • Objectif: ${matchState.target_score} pts",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = TextMuted,
                                    fontSize = 11.sp
                                )
                            )
                        }
                    }
                },
                actions = {
                    // Badge Live Stream OBS
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF1E293B))
                            .clickable { showRemoteHelpDialog = true }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                            .testTag("obs_live_badge")
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(EmeraldPrimary)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "LIVE OBS : ${viewModel.localIp}:8080",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldPrimary,
                                    fontSize = 11.sp
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Feuille de match
                    IconButton(
                        onClick = { showScoresheet = true },
                        modifier = Modifier.testTag("open_scoresheet_button")
                    ) {
                        Icon(imageVector = Icons.Default.ListAlt, contentDescription = "Feuille de match", tint = TextPrimary)
                    }

                    // Télécommande BT & Aide
                    IconButton(
                        onClick = { showRemoteHelpDialog = true },
                        modifier = Modifier.testTag("open_remote_help_button")
                    ) {
                        Icon(imageVector = Icons.Default.Bluetooth, contentDescription = "Télécommande HID", tint = AmberAccent)
                    }

                    // Nouveau Match / Réglages
                    IconButton(
                        onClick = { showNewMatchDialog = true },
                        modifier = Modifier.testTag("open_new_match_button")
                    ) {
                        Icon(imageVector = Icons.Default.Settings, contentDescription = "Nouveau Match", tint = TextPrimary)
                    }

                    // Historique des Matchs
                    IconButton(
                        onClick = { showHistoryDialog = true },
                        modifier = Modifier.testTag("open_history_button")
                    ) {
                        Icon(imageVector = Icons.Default.History, contentDescription = "Historique", tint = TextPrimary)
                    }

                    // Sauvegarder dans Room DB
                    IconButton(
                        onClick = {
                            viewModel.saveMatchToHistory()
                            scope.launch {
                                snackbarHostState.showSnackbar("Match enregistré dans l'historique local !")
                            }
                        },
                        modifier = Modifier.testTag("save_match_button")
                    ) {
                        Icon(imageVector = Icons.Default.Save, contentDescription = "Sauvegarder", tint = EmeraldPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkSurface)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Section principale Scoreboard : Joueur A (Gauche) | Timer (Centre) | Joueur B (Droite)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Carte Joueur A (Bille Blanche)
                PlayerScoreCard(
                    modifier = Modifier.weight(1f),
                    playerName = matchState.a_name,
                    score = matchState.a_score,
                    currentRun = matchState.a_current_run,
                    innings = matchState.a_innings,
                    highRun = matchState.a_high_run,
                    timeoutsLeft = matchState.a_timeouts_left,
                    ballColorHex = matchState.a_color,
                    isActiveTurn = matchState.current_turn == MatchState.TURN_PLAYER_A,
                    playerKey = MatchState.TURN_PLAYER_A
                )

                // Timer Shot Clock au centre
                ShotClockWidget(
                    timerRemaining = matchState.timer_remaining,
                    shotClockSeconds = matchState.shot_clock_seconds,
                    matchStatus = matchState.match_status,
                    onTogglePause = { viewModel.toggleTimerPause() },
                    onRequestTimeout = { viewModel.requestTimeout() }
                )

                // Carte Joueur B (Bille Jaune)
                PlayerScoreCard(
                    modifier = Modifier.weight(1f),
                    playerName = matchState.b_name,
                    score = matchState.b_score,
                    currentRun = matchState.b_current_run,
                    innings = matchState.b_innings,
                    highRun = matchState.b_high_run,
                    timeoutsLeft = matchState.b_timeouts_left,
                    ballColorHex = matchState.b_color,
                    isActiveTurn = matchState.current_turn == MatchState.TURN_PLAYER_B,
                    playerKey = MatchState.TURN_PLAYER_B
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Panneau de contrôle tactile
            val activeName = if (matchState.current_turn == MatchState.TURN_PLAYER_A) matchState.a_name else matchState.b_name
            ControlPanel(
                activePlayerName = activeName,
                onIncrement = { pts -> viewModel.incrementCurrentRun(pts) },
                onDecrement = { pts -> viewModel.decrementCurrentRun(pts) },
                onCommitTurn = { viewModel.commitTurn() },
                onUndo = { viewModel.undoLastTurn() },
                onSwapPlayers = { viewModel.swapPlayers() }
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Guide rapide Raccourcis Télécommande Bluetooth en bas
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(DarkSurface)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "TÉLÉCOMMANDE BLUETOOTH HID :  [➜ / Vol+] = +1 pt   |   [◄ / Vol-] = -1 pt   |   [Entrée] = Valider Reprise   |   [Espace] = Pause",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = TextMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }
    }

    // Dialogs
    if (showScoresheet) {
        ScoresheetDialog(
            matchState = matchState,
            inningLogs = inningLogs,
            onDismiss = { showScoresheet = false }
        )
    }

    if (showNewMatchDialog) {
        NewMatchDialog(
            initialState = matchState,
            onDismiss = { showNewMatchDialog = false },
            onStartMatch = { aName, bName, targetScore, shotClock, maxInnings, mode ->
                viewModel.startNewMatch(aName, bName, targetScore, shotClock, maxInnings, mode)
                showNewMatchDialog = false
            }
        )
    }

    if (showHistoryDialog) {
        HistoryDialog(
            savedMatches = savedMatches,
            onDismiss = { showHistoryDialog = false },
            onDeleteMatch = { id -> viewModel.deleteMatchFromHistory(id) }
        )
    }

    if (showRemoteHelpDialog) {
        RemoteHelpDialog(
            localIp = viewModel.localIp,
            onDismiss = { showRemoteHelpDialog = false },
            onSimulateKey = { action ->
                when (action) {
                    "RIGHT" -> viewModel.incrementCurrentRun(1)
                    "LEFT" -> viewModel.decrementCurrentRun(1)
                    "ENTER" -> viewModel.commitTurn()
                    "SPACE" -> viewModel.toggleTimerPause()
                }
            }
        )
    }
}
