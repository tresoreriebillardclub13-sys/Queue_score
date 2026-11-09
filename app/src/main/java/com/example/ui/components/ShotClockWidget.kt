package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.MatchState
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.CrimsonAlert
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary

@Composable
fun ShotClockWidget(
    timerRemaining: Int,
    shotClockSeconds: Int,
    matchStatus: String,
    onTogglePause: () -> Unit,
    onRequestTimeout: () -> Unit
) {
    val progress = if (shotClockSeconds > 0) {
        (timerRemaining.toFloat() / shotClockSeconds.toFloat()).coerceIn(0f, 1f)
    } else 1f

    val ringColor by animateColorAsState(
        targetValue = when {
            timerRemaining <= 10 -> CrimsonAlert
            timerRemaining <= 20 -> AmberAccent
            else -> EmeraldPrimary
        },
        label = "timer_color"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(130.dp)
                .testTag("shot_clock_widget"),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokeWidth = 10.dp.toPx()
                // Fond de l'anneau
                drawCircle(
                    color = DarkBorder,
                    style = Stroke(width = strokeWidth)
                )

                // Arc de décompte du timer
                drawArc(
                    color = ringColor,
                    startAngle = -90f,
                    sweepAngle = 360f * progress,
                    useCenter = false,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = timerRemaining.toString(),
                    style = MaterialTheme.typography.displayMedium.copy(
                        fontSize = 42.sp,
                        fontWeight = FontWeight.Black,
                        color = if (timerRemaining <= 10) CrimsonAlert else TextPrimary
                    ),
                    modifier = Modifier.testTag("timer_remaining_text")
                )
                Text(
                    text = if (matchStatus == MatchState.STATUS_PAUSE) "PAUSE" else "SECONDES",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = if (matchStatus == MatchState.STATUS_PAUSE) AmberAccent else TextMuted,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Boutons sous le timer : Pause/Play & Extension +40s
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Pause / Play
            IconButton(
                onClick = onTogglePause,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF1E293B))
                    .testTag("toggle_pause_button")
            ) {
                Icon(
                    imageVector = if (matchStatus == MatchState.STATUS_EN_COURS) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = "Pause/Reprise Timer",
                    tint = EmeraldPrimary
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Demande de temps mort / Extension (+40s)
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFF1E293B))
                    .clickable { onRequestTimeout() }
                    .padding(horizontal = 14.dp, vertical = 8.dp)
                    .testTag("request_timeout_button"),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.HourglassTop,
                        contentDescription = "Extension",
                        tint = AmberAccent,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "+40s",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = AmberAccent
                        )
                    )
                }
            }
        }
    }
}
