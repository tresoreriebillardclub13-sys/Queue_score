package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.TextPrimary

@Composable
fun ControlPanel(
    activePlayerName: String,
    onIncrement: (Int) -> Unit,
    onDecrement: (Int) -> Unit,
    onCommitTurn: () -> Unit,
    onUndo: () -> Unit,
    onSwapPlayers: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(DarkSurfaceVariant, RoundedCornerShape(20.dp))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Rangée principale : +1 POINT & VALIDER REPRISE
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Bouton Géant +1 Point
            Button(
                onClick = { onIncrement(1) },
                modifier = Modifier
                    .weight(1.3f)
                    .height(68.dp)
                    .testTag("increment_point_button"),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = EmeraldPrimary,
                    contentColor = Color.Black
                )
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "+1",
                        modifier = Modifier.padding(end = 6.dp)
                    )
                    Text(
                        text = "+1 POINT",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Black,
                            fontSize = 22.sp
                        )
                    )
                }
            }

            // Bouton Valider la Reprise / Passer la main
            Button(
                onClick = onCommitTurn,
                modifier = Modifier
                    .weight(1.7f)
                    .height(68.dp)
                    .testTag("commit_turn_button"),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AmberAccent,
                    contentColor = Color.Black
                )
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Valider",
                        modifier = Modifier.padding(end = 6.dp)
                    )
                    Column {
                        Text(
                            text = "VALIDER REPRISE",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Black,
                                fontSize = 16.sp
                            )
                        )
                        Text(
                            text = "Passer la main à l'adversaire",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                color = Color(0xFF451A03)
                            )
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Rangée secondaire : Actions rapides (+2, +5, -1, Undo, Swap)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // +2
            OutlinedButton(
                onClick = { onIncrement(2) },
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("plus_two_button"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "+2",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                )
            }

            // +5
            OutlinedButton(
                onClick = { onIncrement(5) },
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("plus_five_button"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "+5",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                )
            }

            // -1 Point
            OutlinedButton(
                onClick = { onDecrement(1) },
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("decrement_point_button"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Remove,
                        contentDescription = "-1",
                        tint = TextPrimary
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = "-1",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    )
                }
            }

            // Annuler (Undo)
            OutlinedButton(
                onClick = onUndo,
                modifier = Modifier
                    .weight(1.2f)
                    .height(48.dp)
                    .testTag("undo_button"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Undo,
                        contentDescription = "Undo",
                        tint = TextPrimary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "ANNULER",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    )
                }
            }

            // Inverser Joueurs
            OutlinedButton(
                onClick = onSwapPlayers,
                modifier = Modifier
                    .weight(1.2f)
                    .height(48.dp)
                    .testTag("swap_players_button"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.SwapHoriz,
                        contentDescription = "Inverser",
                        tint = TextPrimary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "INVERSER",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    )
                }
            }
        }
    }
}
