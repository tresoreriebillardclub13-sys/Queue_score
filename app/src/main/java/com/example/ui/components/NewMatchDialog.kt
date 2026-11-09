package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Sports
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.MatchState
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun NewMatchDialog(
    initialState: MatchState,
    onDismiss: () -> Unit,
    onStartMatch: (aName: String, bName: String, targetScore: Int, shotClock: Int, maxInnings: Int?, mode: String) -> Unit
) {
    var aName by remember { mutableStateOf(initialState.a_name) }
    var bName by remember { mutableStateOf(initialState.b_name) }
    var targetScoreText by remember { mutableStateOf(initialState.target_score.toString()) }
    var shotClockSeconds by remember { mutableIntStateOf(initialState.shot_clock_seconds) }
    var selectedMode by remember { mutableStateOf(initialState.game_mode) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .clip(RoundedCornerShape(24.dp))
                .testTag("new_match_dialog"),
            color = DarkBackground
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Sports,
                            contentDescription = "Match",
                            tint = EmeraldPrimary
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "NOUVEAU MATCH DE CARAMBOLE",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Black,
                                color = TextPrimary
                            )
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_new_match_button")
                    ) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Fermer", tint = TextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Mode de Jeu
                Text(
                    text = "DISCIPLINE",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = TextMuted)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(MatchState.MODE_3_BANDES, MatchState.MODE_LIBRE, MatchState.MODE_CADRE).forEach { mode ->
                        FilterChip(
                            selected = selectedMode == mode,
                            onClick = { selectedMode = mode },
                            label = { Text(mode) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = EmeraldPrimary,
                                selectedLabelColor = Color.Black
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Noms des Joueurs
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = aName,
                        onValueChange = { aName = it },
                        label = { Text("Nom Joueur A (Blanche)") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("input_player_a_name"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = EmeraldPrimary,
                            unfocusedBorderColor = DarkBorder,
                            focusedLabelColor = EmeraldPrimary
                        ),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = bName,
                        onValueChange = { bName = it },
                        label = { Text("Nom Joueur B (Jaune)") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("input_player_b_name"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = EmeraldPrimary,
                            unfocusedBorderColor = DarkBorder,
                            focusedLabelColor = EmeraldPrimary
                        ),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Distance / Score à atteindre
                OutlinedTextField(
                    value = targetScoreText,
                    onValueChange = { targetScoreText = it.filter { char -> char.isDigit() } },
                    label = { Text("Score à atteindre (ex: 40 pts)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_target_score"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = EmeraldPrimary,
                        unfocusedBorderColor = DarkBorder
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Chronomètre de tir (Shot Clock)
                Text(
                    text = "CHRONOMÈTRE DE TIR (SHOT CLOCK)",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = TextMuted)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(30, 40, 45, 50, 60, 0).forEach { sec ->
                        val label = if (sec == 0) "DÉSACTIVÉ" else "${sec}s"
                        FilterChip(
                            selected = shotClockSeconds == sec,
                            onClick = { shotClockSeconds = sec },
                            label = { Text(label) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = EmeraldPrimary,
                                selectedLabelColor = Color.Black
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Boutons Valider / Annuler
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(
                        onClick = {
                            val target = targetScoreText.toIntOrNull() ?: 40
                            onStartMatch(aName, bName, target, shotClockSeconds, null, selectedMode)
                        },
                        modifier = Modifier
                            .height(52.dp)
                            .testTag("confirm_start_match_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary, contentColor = Color.Black)
                    ) {
                        Text(
                            text = "DÉMARRER LE MATCH",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black)
                        )
                    }
                }
            }
        }
    }
}
