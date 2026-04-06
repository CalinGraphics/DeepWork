package com.deepwork.feature.timer

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.hilt.navigation.compose.hiltViewModel
import com.deepwork.core.ui.theme.DeepIndigo
import com.deepwork.core.ui.theme.DeepTeal
import com.deepwork.domain.model.Task
import com.deepwork.domain.model.TimerState
import com.deepwork.domain.usecase.StreakResult

@Composable
fun TimerScreen(
    onOpenTaskList: () -> Unit = {},
    onOpenNotifications: () -> Unit = {},
    onOpenMenu: () -> Unit = {},
    viewModel: TimerViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val streak by viewModel.streak.collectAsState()
    val activeTask by viewModel.activeTask.collectAsState()
    val sessionDuration by viewModel.sessionDurationMinutes.collectAsState()
    val scroll = rememberScrollState()

    Column(modifier = Modifier.fillMaxSize()) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding(),
            color = MaterialTheme.colorScheme.background.copy(alpha = 0.92f),
            shadowElevation = 0.dp
        ) {
            TimerTopBar(
                onMenuClick = onOpenMenu,
                onNotificationsClick = onOpenNotifications
            )
        }
        HorizontalDivider(
            thickness = 1.dp,
            color = Color.White.copy(alpha = 0.06f)
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .pointerInput(uiState, sessionDuration) {
                    detectTapGestures(
                        onDoubleTap = {
                            if (uiState is TimerState.Idle) {
                                viewModel.startSession(sessionDuration)
                            }
                        }
                    )
                }
        ) {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scroll)
                    .navigationBarsPadding()
            ) {
                val horizontal = when {
                    maxWidth < 600.dp -> 24.dp
                    maxWidth < 900.dp -> 40.dp
                    else -> 56.dp
                }
                Column(
                    modifier = Modifier
                        .widthIn(max = 720.dp)
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .padding(horizontal = horizontal)
                        .padding(top = 32.dp, bottom = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(40.dp)
                ) {
                    Text(
                        "CURRENT SESSION",
                        color = DeepIndigo,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 3.sp,
                        fontSize = 12.sp
                    )
                    TimerArc(uiState = uiState, idleDurationMinutes = sessionDuration)
                    MobileCardioWaveStrip(visible = uiState is TimerState.Running)
                    TimerActionRow(
                        uiState = uiState,
                        onStart = { viewModel.startSession(sessionDuration) },
                        onPause = { viewModel.pauseSession() },
                        onResume = { viewModel.resumeSession() },
                        onStop = { viewModel.stopSession() },
                        onReset = { viewModel.resetSession() }
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        StreakCard(streak, modifier = Modifier.weight(1f))
                        DailyGoalCard(streak, modifier = Modifier.weight(1f))
                    }
                    ActiveTaskSection(
                        task = activeTask,
                        onChangeClick = onOpenTaskList,
                        onCardClick = onOpenTaskList
                    )
                }
            }
        }
    }
}

@Composable
private fun TimerTopBar(
    onMenuClick: () -> Unit = {},
    onNotificationsClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        IconButton(onClick = onMenuClick) {
            Icon(
                Icons.Rounded.Menu,
                contentDescription = "Meniu",
                tint = MaterialTheme.colorScheme.onBackground
            )
        }
        Image(
            painter = painterResource(id = R.drawable.kara_logo),
            contentDescription = "Kara",
            modifier = Modifier
                .height(72.dp)
                .widthIn(max = 300.dp),
            contentScale = ContentScale.Fit
        )
        Box {
            IconButton(onClick = onNotificationsClick) {
                Icon(
                    Icons.Outlined.Notifications,
                    contentDescription = "Notificări",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(10.dp)
                    .size(8.dp)
                    .background(DeepTeal, shape = RoundedCornerShape(percent = 50))
            )
        }
    }
}

@Composable
@OptIn(ExperimentalAnimationApi::class)
fun TimerArc(uiState: TimerState, idleDurationMinutes: Int = 25) {
    val idleMillis = idleDurationMinutes.coerceIn(5, 120) * 60_000L
    val progress = when (uiState) {
        is TimerState.Running -> uiState.progress
        is TimerState.Paused -> uiState.progress
        is TimerState.Completed -> 0f
        else -> 1f
    }

    val remainingMillis = when (uiState) {
        is TimerState.Running -> uiState.remainingMillis
        is TimerState.Paused -> uiState.remainingMillis
        else -> idleMillis
    }

    val minutes = (remainingMillis / 1000) / 60
    val seconds = (remainingMillis / 1000) % 60
    val timeText = String.format("%02d:%02d", minutes, seconds)

    val trackColor = Color.White.copy(alpha = 0.06f)
    val sizeDp = 264.dp

    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(sizeDp)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
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
                        slideInVertically { height -> height } + fadeIn() togetherWith
                            slideOutVertically { height -> -height } + fadeOut()
                    } else {
                        slideInVertically { height -> -height } + fadeIn() togetherWith
                            slideOutVertically { height -> height } + fadeOut()
                    } using SizeTransform(clip = false)
                },
                label = "timeText"
            ) { targetTime ->
                Text(
                    text = targetTime,
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontWeight = FontWeight.Light,
                        fontSize = 52.sp
                    )
                )
            }
            Text(
                text = if (uiState is TimerState.Idle) "Remaining" else when (uiState) {
                    is TimerState.Running -> "Remaining"
                    is TimerState.Paused -> "Paused"
                    is TimerState.Completed -> "Done"
                    else -> "Remaining"
                },
                style = MaterialTheme.typography.labelMedium.copy(
                    letterSpacing = 2.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                modifier = Modifier.padding(top = 6.dp)
            )
        }
    }
}

@Composable
private fun MobileCardioWaveStrip(
    visible: Boolean,
    modifier: Modifier = Modifier
) {
    if (!visible) {
        Spacer(modifier = Modifier.height(22.dp))
        return
    }
    val motion = rememberInfiniteTransition(label = "ecgWave")
    val shift = motion.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1300),
            repeatMode = RepeatMode.Restart
        ),
        label = "ecgShift"
    ).value
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(22.dp)
            .padding(horizontal = 16.dp)
    ) {
        val cycle = size.width * 0.28f
        val base = size.height * 0.58f
        val stroke = 2.2.dp.toPx()
        fun addWave(startX: Float, alphaMul: Float) {
            val p = Path()
            val pts = listOf(
                0.00f to 0f,
                0.10f to 0f,
                0.16f to -2f,
                0.23f to 0f,
                0.34f to 0f,
                0.43f to -11f,
                0.51f to 7f,
                0.60f to 0f,
                1.00f to 0f
            )
            p.moveTo(startX + (pts.first().first * cycle), base + pts.first().second)
            pts.drop(1).forEach { (x, y) ->
                p.lineTo(startX + (x * cycle), base + y)
            }
            drawPath(
                path = p,
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        DeepTeal.copy(alpha = 0.03f),
                        DeepTeal.copy(alpha = 0.92f * alphaMul),
                        DeepTeal.copy(alpha = 0.03f)
                    )
                ),
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
        }
        val start = -shift * cycle
        addWave(start, 1f)
        addWave(start + cycle, 0.55f)
        addWave(start + cycle * 2f, 0.35f)
        addWave(start + cycle * 3f, 0.55f)
    }
}

@Composable
private fun TimerActionRow(
    uiState: TimerState,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onStop: () -> Unit,
    onReset: () -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        when (uiState) {
            is TimerState.Idle -> {
                Button(
                    onClick = onStart,
                    modifier = Modifier.height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = DeepIndigo),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Rounded.PlayArrow, contentDescription = null)
                    Text("Start", modifier = Modifier.padding(start = 8.dp))
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
            is TimerState.Running -> {
                Button(
                    onClick = onPause,
                    modifier = Modifier.height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = DeepTeal),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Pause")
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
            is TimerState.Paused -> {
                Button(
                    onClick = onResume,
                    modifier = Modifier.height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = DeepIndigo),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Resume")
                }
                OutlinedButton(onClick = onStop, modifier = Modifier.height(48.dp), shape = RoundedCornerShape(12.dp)) {
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
            else -> {
                Button(onClick = onStart) { Text("Start new session") }
            }
        }
    }
}

@Composable
private fun StreakCard(streak: StreakResult?, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = DeepIndigo.copy(alpha = 0.08f)
        ),
        border = BorderStroke(
            1.dp,
            Color.White.copy(alpha = 0.05f)
        )
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                "Current Streak",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium
            )
            Text(
                "${streak?.currentStreak ?: 0} Days",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Row(
                modifier = Modifier.padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    Icons.Rounded.TrendingUp,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = DeepTeal
                )
                Text(
                    text = "${streak?.totalXp ?: 0} XP total",
                    style = MaterialTheme.typography.labelMedium,
                    color = DeepTeal,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun DailyGoalCard(streak: StreakResult?, modifier: Modifier = Modifier) {
    val pct = ((streak?.focusScore ?: 0).coerceIn(0, 100)) / 100f
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = DeepIndigo.copy(alpha = 0.08f)
        ),
        border = BorderStroke(
            1.dp,
            Color.White.copy(alpha = 0.05f)
        )
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                "Daily Goal",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium
            )
            Text(
                "${streak?.focusScore ?: 0}% / 100% focus",
                style = MaterialTheme.typography.titleLarge,
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
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp)
            )
        }
    }
}

@Composable
private fun ActiveTaskSection(
    task: Task?,
    onChangeClick: () -> Unit,
    onCardClick: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Active Task",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Change",
                color = DeepIndigo,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clickable(onClick = onChangeClick)
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = DeepIndigo.copy(alpha = 0.12f)
            ),
            border = BorderStroke(1.dp, DeepIndigo.copy(alpha = 0.22f)),
            modifier = Modifier.clickable(onClick = onCardClick)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(DeepIndigo.copy(alpha = 0.2f), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.Code, contentDescription = null, tint = DeepIndigo)
                    }
                    Column(modifier = Modifier.padding(start = 12.dp)) {
                        Text(
                            task?.title ?: "Adaugă un task în tab-ul Tasks",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            task?.let { "Project: ${it.category}" }
                                ?: "Nicio sarcină activă",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Icon(
                    Icons.Rounded.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
