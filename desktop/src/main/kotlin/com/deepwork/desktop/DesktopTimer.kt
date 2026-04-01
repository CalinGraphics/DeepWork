package com.deepwork.desktop

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

private val DeepIndigo = Color(0xFF5C55E8)
private val DeepTeal = Color(0xFF00C4D4)
private val SurfaceC = Color(0xFF1E1C31)
private val SurfaceVar = Color(0xFF272546)

/**
 * Același tip de arc ca pe Android ([com.deepwork.feature.timer.TimerArc]): progres = timp rămas / durată planificată.
 */
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun DesktopTimerArc(
    phase: DesktopSessionPhase,
    preferredMinutes: Int,
    remainingSeconds: Int
) {
    val totalSec = preferredMinutes.coerceIn(5, 120) * 60
    val remaining = remainingSeconds.coerceIn(0, totalSec)
    val remainingMillis = remaining * 1000L

    val progress = when (phase) {
        DesktopSessionPhase.Running, DesktopSessionPhase.Paused ->
            if (totalSec > 0) remaining.toFloat() / totalSec.toFloat() else 0f
        DesktopSessionPhase.Idle -> 1f
    }

    val minutes = (remainingMillis / 1000) / 60
    val seconds = (remainingMillis / 1000) % 60
    val timeText = String.format("%02d:%02d", minutes, seconds)

    val trackColor = Color.White.copy(alpha = 0.06f)
    val sizeDp = 264.dp

    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(sizeDp)) {
        Canvas(modifier = Modifier.size(sizeDp)) {
            val stroke = 10.dp.toPx()
            val arcSize = Size(size.width - stroke, size.height - stroke)
            val topLeft = Offset(stroke / 2, stroke / 2)

            drawArc(
                color = trackColor,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )

            val sweep = 360f * progress.coerceIn(0f, 1f)
            drawArc(
                brush = Brush.sweepGradient(
                    colors = listOf(DeepIndigo, DeepTeal, DeepIndigo),
                    center = Offset(size.width / 2f, size.height / 2f)
                ),
                startAngle = -90f,
                sweepAngle = sweep,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = stroke * 0.85f, cap = StrokeCap.Round)
            )

            val radius = size.minDimension / 2f - stroke * 0.35f
            val dotCenter = Offset(size.width / 2f, size.height / 2f - radius)
            drawCircle(
                color = DeepTeal,
                radius = 5.dp.toPx(),
                center = dotCenter
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            AnimatedContent(
                targetState = timeText,
                transitionSpec = {
                    if (targetState > initialState) {
                        slideInVertically { h -> h } + fadeIn() togetherWith
                            slideOutVertically { h -> -h } + fadeOut()
                    } else {
                        slideInVertically { h -> -h } + fadeIn() togetherWith
                            slideOutVertically { h -> h } + fadeOut()
                    } using SizeTransform(clip = false)
                },
                label = "desktopTime"
            ) { targetTime ->
                Text(
                    text = targetTime,
                    fontSize = 52.sp,
                    fontWeight = FontWeight.Light,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
            Text(
                text = when (phase) {
                    DesktopSessionPhase.Idle -> "Remaining"
                    DesktopSessionPhase.Running -> "Remaining"
                    DesktopSessionPhase.Paused -> "Paused"
                },
                fontSize = 12.sp,
                letterSpacing = 2.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp)
            )
        }
    }
}

@Composable
fun DesktopTimerActionRow(
    phase: DesktopSessionPhase,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onEnd: () -> Unit,
    onReset: () -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        when (phase) {
            DesktopSessionPhase.Idle -> {
                Button(
                    onClick = onStart,
                    modifier = Modifier.height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = DeepIndigo),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Rounded.PlayArrow, contentDescription = null, tint = Color.White)
                    Text("Start", modifier = Modifier.padding(start = 8.dp), color = Color.White)
                }
                OutlinedButton(
                    onClick = onReset,
                    modifier = Modifier.height(48.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Rounded.Refresh, contentDescription = null)
                    Text("Reset", modifier = Modifier.padding(start = 8.dp))
                }
            }
            DesktopSessionPhase.Running -> {
                Button(
                    onClick = onPause,
                    modifier = Modifier.height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = DeepTeal),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Pause", color = Color.White)
                }
                OutlinedButton(
                    onClick = onReset,
                    modifier = Modifier.height(48.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Rounded.Refresh, contentDescription = null)
                    Text("Reset", modifier = Modifier.padding(start = 8.dp))
                }
            }
            DesktopSessionPhase.Paused -> {
                Button(
                    onClick = onResume,
                    modifier = Modifier.height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = DeepIndigo),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Resume", color = Color.White)
                }
                OutlinedButton(
                    onClick = onEnd,
                    modifier = Modifier.height(48.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("End")
                }
                OutlinedButton(
                    onClick = onReset,
                    modifier = Modifier.height(48.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Rounded.Refresh, contentDescription = null)
                }
            }
        }
    }
}

@Composable
fun DesktopStreakSummaryRow() {
    val gami by DesktopGamification.state.collectAsState()
    val streakDays = remember(gami.completedSessionEpochMillis) {
        val days = gami.completedSessionEpochMillis
            .map { Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate() }
            .toSet()
        if (days.isEmpty()) return@remember 0
        val today = LocalDate.now()
        var anchor = today
        if (!days.contains(today)) {
            anchor = today.minusDays(1)
            if (!days.contains(anchor)) return@remember 0
        }
        var s = 0
        var d = anchor
        while (days.contains(d)) {
            s++
            d = d.minusDays(1)
        }
        s
    }
    val focusScore = (streakDays * 12).coerceIn(0, 100)
    val pct = focusScore / 100f

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = DeepIndigo.copy(alpha = 0.08f)),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    "Current Streak",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    "$streakDays Days",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    modifier = Modifier.padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(Icons.Rounded.TrendingUp, contentDescription = null, modifier = Modifier.size(18.dp), tint = DeepTeal)
                    Text("${gami.totalXp} XP total", fontSize = 12.sp, color = DeepTeal, fontWeight = FontWeight.Bold)
                }
            }
        }
        Card(
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = DeepIndigo.copy(alpha = 0.08f)),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    "Daily Goal",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    "$focusScore% / 100% focus",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .padding(top = 12.dp)
                        .background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(3.dp))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(pct)
                            .height(6.dp)
                            .background(DeepIndigo, RoundedCornerShape(3.dp))
                    )
                }
                Text(
                    "${(pct * 100).toInt()}% complete",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
        }
    }
}

/** Secțiune „Active Task” stil Android — pe desktop afișăm un placeholder scurt. */
@Composable
fun DesktopActiveTaskPlaceholder() {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Active Task",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Pe telefon",
                color = DeepIndigo,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = DeepIndigo.copy(alpha = 0.12f)),
            border = BorderStroke(1.dp, DeepIndigo.copy(alpha = 0.22f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(DeepIndigo.copy(alpha = 0.2f), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.Code, contentDescription = null, tint = DeepIndigo)
                }
                Column {
                    Text("Taskuri pe aplicația Android", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text(
                        "Aici vezi doar sesiunea; lista de taskuri e pe telefon.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
