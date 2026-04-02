package com.deepwork.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Sensors
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.Smartphone
import androidx.compose.material.icons.outlined.ScreenRotation
import androidx.compose.material.icons.outlined.TouchApp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.painterResource
import kotlin.math.roundToInt

private val DeepIndigo = Color(0xFF5C55E8)
private val DeepTeal = Color(0xFF00C4D4)
private val Bg = Color(0xFF121121)
private val SurfaceC = Color(0xFF1E1C31)
private val SurfaceVar = Color(0xFF272546)

private val weekHeights = listOf(0.6f, 0.35f, 0.85f, 1f, 0.5f, 0.25f, 0.4f)
private val weekLabels = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

private enum class HeatmapRange { WEEKLY, MONTHLY }

@Composable
fun DesktopCompanionAppTheme(content: @Composable () -> Unit) {
    val scheme = darkColorScheme(
        primary = DeepIndigo,
        secondary = DeepTeal,
        background = Bg,
        surface = SurfaceC,
        surfaceVariant = SurfaceVar,
        onPrimary = Color.White,
        onSecondary = Color.Black,
        onBackground = Color(0xFFF6F6F8),
        onSurface = Color(0xFFF6F6F8),
        onSurfaceVariant = Color(0xFFA0A0B0)
    )
    MaterialTheme(
        colorScheme = scheme,
        content = content
    )
}

@Composable
fun DesktopCompanionApp(
    strictFocusEnabled: Boolean,
    onStrictFocusChange: (Boolean) -> Unit
) {
    val status by DesktopState.status.collectAsState()
    val connected by DesktopState.connected.collectAsState()
    val pairingUrl by DesktopState.pairingUrl.collectAsState()
    val usbBridgeStatus by DesktopState.usbBridgeStatus.collectAsState()
    val lastGesture by DesktopState.lastGesture.collectAsState()
    var mainTab by remember { mutableStateOf(DesktopMainTab.Dashboard) }
    var infoDialog by remember { mutableStateOf<DesktopInfoKind?>(null) }
    val dashboardListState = rememberLazyListState()
    val insightsListState = rememberLazyListState()

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val padH = when {
                maxWidth < 600.dp -> 16.dp
                maxWidth < 1000.dp -> 32.dp
                else -> 56.dp
            }
            // Forțăm layout stivuit (o singură coloană) ca să nu existe suprapuneri,
            // indiferent de lățimea ferestrei.
            val stackDashboard = true
            Column(Modifier.fillMaxSize()) {
                DesktopTopBar(
                    onSettingsClick = { infoDialog = DesktopInfoKind.Settings },
                    onNotificationsClick = { infoDialog = DesktopInfoKind.Notifications }
                )
                Spacer(modifier = Modifier.height(16.dp))
                Column(
                    Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(start = padH, end = padH, top = 24.dp, bottom = 88.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    when (mainTab) {
                        DesktopMainTab.Dashboard -> {
                            ScrollableLazyColumn(
                                state = dashboardListState,
                                verticalArrangement = Arrangement.spacedBy(24.dp),
                                contentPadding = PaddingValues(bottom = 24.dp)
                            ) {
                                item {
                                    if (stackDashboard) {
                                        Column(
                                            Modifier.fillMaxWidth(),
                                            verticalArrangement = Arrangement.spacedBy(24.dp)
                                        ) {
                                            SessionTimerCard(status, connected)
                                            ProductivityHeatmapCard()
                                            GestureMapCard(connected, lastGesture)
                                            ConnectionCard(pairingUrl, usbBridgeStatus)
                                        }
                                    } else {
                                        Row(
                                            Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(24.dp)
                                        ) {
                                            Column(
                                                Modifier.weight(2f),
                                                verticalArrangement = Arrangement.spacedBy(24.dp)
                                            ) {
                                                SessionTimerCard(status, connected)
                                                ProductivityHeatmapCard()
                                            }
                                            Column(
                                                Modifier.weight(1f),
                                                verticalArrangement = Arrangement.spacedBy(24.dp)
                                            ) {
                                                GestureMapCard(connected, lastGesture)
                                                ConnectionCard(pairingUrl, usbBridgeStatus)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        DesktopMainTab.History -> {
                        Box(
                            Modifier
                                .fillMaxSize()
                        ) {
                            DesktopHistoryTab()
                        }
                    }
                        DesktopMainTab.Insights -> {
                            ScrollableLazyColumn(
                                state = insightsListState,
                                verticalArrangement = Arrangement.spacedBy(20.dp),
                                contentPadding = PaddingValues(bottom = 24.dp)
                            ) {
                                item {
                                    DesktopInsightsScrollContent()
                                }
                            }
                        }
                    }
                }
            }
                DesktopFooter(
                    modifier = Modifier.align(Alignment.BottomCenter),
                    connected = connected,
                    selectedTab = mainTab,
                    strictFocusEnabled = strictFocusEnabled,
                    onStrictFocusChange = onStrictFocusChange,
                    onTabSelected = { mainTab = it }
                )
        }
    }

    when (val kind = infoDialog) {
        null -> Unit
        else -> {
            AlertDialog(
                onDismissRequest = { infoDialog = null },
                title = {
                    Text(
                        when (kind) {
                            DesktopInfoKind.Settings -> "Setări companion"
                            DesktopInfoKind.Notifications -> "Notificări"
                        }
                    )
                },
                text = {
                    Text(
                        when (kind) {
                            DesktopInfoKind.Settings ->
                                "Serverul WebSocket rulează pe portul 8080. Pornește aplicația înainte de împerechere; pe telefon folosește același rețea sau USB (adb reverse).\n\n" +
                                    "Strict Focus (implicit activ): ecran complet, mereu deasupra, fereastră neredimensionabilă — îți ține Kara în prim-plan. " +
                                    "O aplicație Java nu poate bloca Alt+Tab sau notificările Windows ca un „lock” la nivel de sistem; pentru zero notificări folosește „Asistență pentru focus” / Do Not Disturb din Windows."
                            DesktopInfoKind.Notifications ->
                                "Când telefonul este conectat, gesturile și actualizările de sesiune apar în bara de stare și în jurnalul de conexiune."
                        },
                        color = Color(0xFFB0B0C0)
                    )
                },
                confirmButton = {
                    TextButton(onClick = { infoDialog = null }) {
                        Text("OK")
                    }
                }
            )
        }
    }
}

private enum class DesktopInfoKind { Settings, Notifications }

@Composable
private fun DesktopTopBar(
    onSettingsClick: () -> Unit = {},
    onNotificationsClick: () -> Unit = {}
) {
    Surface(
        color = Bg.copy(alpha = 0.96f),
        shadowElevation = 8.dp
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Image(
                painter = painterResource("kara_logo.png"),
                contentDescription = "Kara",
                modifier = Modifier.height(112.dp)
            )
            Text(
                "DESKTOP",
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = DeepIndigo.copy(alpha = 0.85f),
                letterSpacing = 2.sp,
                modifier = Modifier.padding(start = 4.dp)
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onSettingsClick) {
                Icon(Icons.Outlined.Settings, contentDescription = null, tint = Color(0xFFC8C8D8))
            }
            Box {
                IconButton(onClick = onNotificationsClick) {
                    Icon(Icons.Outlined.Notifications, contentDescription = null, tint = Color(0xFFC8C8D8))
                }
                Box(
                    Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .size(8.dp)
                        .background(DeepIndigo, RoundedCornerShape(50))
                )
            }
        }
    }
}
}

@Composable
private fun SessionTimerCard(status: String, connected: Boolean) {
    val phase by DesktopLocalSession.phase.collectAsState()
    val preferredMinutes by DesktopLocalSession.preferredMinutes.collectAsState()
    val remaining by DesktopLocalSession.remainingSeconds.collectAsState()

    var sliderDraft by remember { mutableFloatStateOf(preferredMinutes.toFloat()) }
    LaunchedEffect(preferredMinutes) {
        sliderDraft = preferredMinutes.toFloat().coerceIn(5f, 120f)
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceC),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceVar),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            Modifier.padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            if (phase == DesktopSessionPhase.Idle) {
                Column(
                    Modifier.fillMaxWidth().widthIn(max = 520.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        "Durată sesiune: ${sliderDraft.roundToInt()} min",
                        fontSize = 12.sp,
                        color = Color(0xFFC8C8D8)
                    )
                    Slider(
                        value = sliderDraft,
                        onValueChange = { sliderDraft = it.coerceIn(5f, 120f) },
                        onValueChangeFinished = {
                            val v = sliderDraft.roundToInt().coerceIn(5, 120)
                            sliderDraft = v.toFloat()
                            DesktopLocalSession.setPreferredMinutes(v)
                        },
                        valueRange = 5f..120f,
                        colors = SliderDefaults.colors(
                            thumbColor = DeepIndigo,
                            activeTrackColor = DeepIndigo,
                            inactiveTrackColor = SurfaceVar
                        )
                    )
                }
            }
            Text(
                "CURRENT SESSION",
                color = DeepIndigo,
                fontWeight = FontWeight.Bold,
                letterSpacing = 3.sp,
                fontSize = 12.sp
            )
            DesktopTimerArc(
                phase = phase,
                preferredMinutes = preferredMinutes,
                remainingSeconds = remaining
            )
            DesktopTimerActionRow(
                phase = phase,
                onStart = { DesktopLocalSession.startSession() },
                onPause = { DesktopLocalSession.pause() },
                onResume = { DesktopLocalSession.startSession() },
                onEnd = { DesktopLocalSession.endSession(SessionEndReason.UserEnded) },
                onReset = { DesktopLocalSession.reset() }
            )
            DesktopStreakSummaryRow()
            DesktopActiveTaskPlaceholder()
            Text(
                text = when {
                    connected -> "Telefon conectat — $status"
                    phase != DesktopSessionPhase.Idle -> "Sesiune locală activă"
                    else -> ""
                },
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun ProductivityHeatmapCard() {
    val gami by DesktopGamification.state.collectAsState()
    var range by remember { mutableStateOf(HeatmapRange.WEEKLY) }
    val millis = gami.completedSessionEpochMillis
    val barHeights: List<Float>
    val barLabels: List<String>
    if (range == HeatmapRange.WEEKLY) {
        barHeights = weeklyHeights(millis)
        barLabels = weekLabels
    } else {
        val monthly = monthlyHeatmapData(millis)
        barHeights = monthly.first
        barLabels = monthly.second
    }
    val avg = ((barHeights.sum() / barHeights.size) * 100).toInt().coerceIn(0, 100)
    val mainPct = "$avg%"
    val deltaLabel = "din date locale"

    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceC),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceVar),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text("Productivity Heatmap", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(mainPct, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                        Text(
                            deltaLabel,
                            color = Color(0xFF34D399),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(start = 8.dp, bottom = 4.dp)
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    HeatmapToggleChip("WEEKLY", range == HeatmapRange.WEEKLY) { range = HeatmapRange.WEEKLY }
                    HeatmapToggleChip("MONTHLY", range == HeatmapRange.MONTHLY) { range = HeatmapRange.MONTHLY }
                }
            }
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(160.dp),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(if (range == HeatmapRange.WEEKLY) 10.dp else 4.dp)
            ) {
                barLabels.forEachIndexed { i, label ->
                    val h = barHeights.getOrElse(i) { 0.3f }
                    Column(
                        Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height((h * 120).dp)
                                .background(
                                    DeepIndigo.copy(alpha = 0.35f + h * 0.55f),
                                    RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)
                                )
                        )
                        Text(
                            label,
                            fontSize = if (range == HeatmapRange.MONTHLY) 9.sp else 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF707080)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HeatmapToggleChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    Text(
        label,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        color = if (selected) Color(0xFFB0B0C0) else Color(0xFF707080),
        modifier = Modifier
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .background(
                if (selected) SurfaceVar else Color.Transparent,
                RoundedCornerShape(8.dp)
            )
            .padding(horizontal = 10.dp, vertical = 6.dp)
    )
}

@Composable
private fun GestureMapCard(connected: Boolean, lastGesture: String?) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceC),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceVar),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Outlined.Sensors, contentDescription = null, tint = DeepIndigo)
                Text("Gesture Map", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(
                    if (connected) "CONNECTED" else "IDLE",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (connected) Color(0xFF34D399) else Color(0xFF707080),
                    modifier = Modifier
                        .background(
                            if (connected) Color(0xFF34D399).copy(alpha = 0.15f) else SurfaceVar,
                            RoundedCornerShape(50)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
            lastGesture?.let {
                Text("Ultimul gest: $it", fontSize = 12.sp, color = DeepTeal)
            }
            GestureRow(Icons.Outlined.ScreenRotation, "Rotate Phone", "Change Ambient Volume")
            GestureRow(Icons.Outlined.Smartphone, "Face Down", "Pune pauză la sesiune")
            GestureRow(Icons.Outlined.Smartphone, "Ridică telefonul", "Reluare din pauză")
            GestureRow(Icons.Outlined.TouchApp, "Shake", "Reset sesiune")
            GestureRow(Icons.Outlined.TouchApp, "Double Tap", "Pornește sesiunea")
        }
    }
}

@Composable
private fun GestureRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String
) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(
            Modifier
                .size(40.dp)
                .background(SurfaceVar, RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = DeepIndigo, modifier = Modifier.size(22.dp))
        }
        Column {
            Text(title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Text(subtitle, fontSize = 12.sp, color = Color(0xFF808090))
        }
    }
}

@Composable
private fun ConnectionCard(pairingUrl: String, usbBridgeStatus: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceC),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceVar),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Outlined.Smartphone, contentDescription = null, tint = DeepIndigo)
                Text("Conectare telefon", fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.padding(start = 8.dp))
            }
            Text(
                pairingUrl.ifBlank { "ws://YOUR_LAN_IP:8080/deepwork" },
                fontSize = 12.sp,
                color = DeepTeal,
                fontWeight = FontWeight.Medium
            )
            Text(
                "Conecteaza manual din telefon folosind IP-ul de mai sus sau USB (adb reverse).",
                fontSize = 12.sp,
                color = Color(0xFF9090A0),
                textAlign = TextAlign.Center
            )
            Text(
                usbBridgeStatus,
                fontSize = 11.sp,
                color = if (usbBridgeStatus.contains("ready", ignoreCase = true)) Color(0xFF34D399) else Color(0xFF9090A0),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun DesktopNavItem(label: String, selected: Boolean, onClick: () -> Unit) {
    val color = if (selected) DeepIndigo else Color(0xFF707080)
    Text(
        label,
        color = color,
        fontWeight = FontWeight.Bold,
        fontSize = 13.sp,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    )
}

@Composable
private fun DesktopHistoryTab() {
    val items by DesktopLocalSession.sessionHistory.collectAsState()
    val historyListState = rememberLazyListState()
    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceC),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceVar),
        modifier = Modifier.fillMaxSize()
    ) {
        if (items.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                Text(
                    "Încă nu ai evenimente în istoric pe acest PC.\n" +
                        "Încheie o sesiune din Dashboard (finalizare la 0:00 sau butonul End) ca să apară în listă.",
                    color = Color(0xFF9090A0),
                    fontSize = 14.sp
                )
            }
        } else {
            ScrollableLazyColumn(
                state = historyListState,
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                item {
                    Text("Istoric sesiuni (local)", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
                itemsIndexed(
                    items,
                    key = { index, entry -> "$index|${entry.time}|${entry.message}" }
                ) { _, entry ->
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .background(SurfaceVar, RoundedCornerShape(8.dp))
                            .padding(12.dp)
                    ) {
                        Text(entry.time, fontSize = 11.sp, color = DeepTeal, fontWeight = FontWeight.Medium)
                        Text(entry.message, fontSize = 13.sp, color = Color(0xFFE8E8F0))
                    }
                }
            }
        }
    }
}

@Composable
private fun ScrollableLazyColumn(
    state: LazyListState,
    modifier: Modifier = Modifier,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    content: androidx.compose.foundation.lazy.LazyListScope.() -> Unit
) {
    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(end = 10.dp),
            state = state,
            verticalArrangement = verticalArrangement,
            contentPadding = contentPadding,
            content = content
        )
    }
}

@Composable
private fun DesktopInsightsScrollContent() {
    val gami by DesktopGamification.state.collectAsState()
    val streakDays = remember(gami.completedSessionEpochMillis) {
        val days = gami.completedSessionEpochMillis
            .map { java.time.Instant.ofEpochMilli(it).atZone(java.time.ZoneId.systemDefault()).toLocalDate() }
            .toSet()
        if (days.isEmpty()) return@remember 0
        val today = java.time.LocalDate.now()
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
    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
    Text("Insights", fontWeight = FontWeight.Bold, fontSize = 22.sp)
    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceC),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceVar),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Gamificare (local PC)", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            Text(
                "Nivel ${DesktopGamification.level(gami.totalXp)} · ${gami.totalXp} XP · streak $streakDays zile · " +
                    "${gami.unlockedAchievementIds.size} realizări",
                fontSize = 13.sp,
                color = Color(0xFFC8C8D8)
            )
            Text(
                "",
                fontSize = 1.sp,
                color = Color.Transparent
            )
        }
    }
    ProductivityHeatmapCard()
    }
}

private fun weeklyHeights(millis: List<Long>): List<Float> {
    val start = java.time.LocalDate.now().with(java.time.DayOfWeek.MONDAY)
    val counts = IntArray(7)
    millis.forEach {
        val d = java.time.Instant.ofEpochMilli(it).atZone(java.time.ZoneId.systemDefault()).toLocalDate()
        val diff = java.time.temporal.ChronoUnit.DAYS.between(start, d).toInt()
        if (diff in 0..6) counts[diff]++
    }
    val max = counts.maxOrNull()?.coerceAtLeast(1) ?: 1
    return counts.map { ((it.toFloat() / max) * 0.8f + 0.2f).coerceIn(0.2f, 1f) }
}

/**
 * Ultimele 12 luni (luna curentă inclusiv), aliniat la stânga → dreapta.
 * Index 0 = acum 11 luni, index 11 = luna curentă. Etichete = numele scurt al lunii (locale).
 */
private fun monthlyHeatmapData(millis: List<Long>): Pair<List<Float>, List<String>> {
    val zone = java.time.ZoneId.systemDefault()
    val today = java.time.LocalDate.now(zone)
    val windowStart = today.withDayOfMonth(1).minusMonths(11)
    val counts = IntArray(12)
    millis.forEach {
        val d = java.time.Instant.ofEpochMilli(it).atZone(zone).toLocalDate()
        val sessionMonthStart = d.withDayOfMonth(1)
        val idx = java.time.temporal.ChronoUnit.MONTHS.between(windowStart, sessionMonthStart).toInt()
        if (idx in 0..11) counts[idx]++
    }
    val max = counts.maxOrNull()?.coerceAtLeast(1) ?: 1
    val heights = counts.map { ((it.toFloat() / max) * 0.8f + 0.2f).coerceIn(0.2f, 1f) }
    val fmt = java.time.format.DateTimeFormatter.ofPattern("LLL", java.util.Locale.getDefault())
    val labels = List(12) { i -> windowStart.plusMonths(i.toLong()).format(fmt) }
    return heights to labels
}

@Composable
private fun DesktopFooter(
    modifier: Modifier = Modifier,
    connected: Boolean,
    selectedTab: DesktopMainTab,
    strictFocusEnabled: Boolean,
    onStrictFocusChange: (Boolean) -> Unit,
    onTabSelected: (DesktopMainTab) -> Unit
) {
    Column(modifier.fillMaxWidth()) {
        HorizontalDivider(color = SurfaceVar)
        Row(
            Modifier
                .fillMaxWidth()
                .background(SurfaceC.copy(alpha = 0.5f))
                .padding(horizontal = 32.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DesktopNavItem("DASHBOARD", selectedTab == DesktopMainTab.Dashboard) {
                    onTabSelected(DesktopMainTab.Dashboard)
                }
                DesktopNavItem("HISTORY", selectedTab == DesktopMainTab.History) {
                    onTabSelected(DesktopMainTab.History)
                }
                DesktopNavItem("INSIGHTS", selectedTab == DesktopMainTab.Insights) {
                    onTabSelected(DesktopMainTab.Insights)
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        Modifier
                            .size(8.dp)
                            .background(
                                if (connected) Color(0xFF34D399) else Color(0xFF707080),
                                RoundedCornerShape(50)
                            )
                    )
                    Text(
                        if (connected) "Telefon conectat (WebSocket activ)"
                        else "Mod local activ — conectarea telefonului este opțională",
                        fontSize = 12.sp,
                        color = Color(0xFF9090A0)
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        "Strict Focus",
                        fontSize = 12.sp,
                        color = if (strictFocusEnabled) DeepIndigo else Color(0xFF9090A0),
                        fontWeight = if (strictFocusEnabled) FontWeight.SemiBold else FontWeight.Normal
                    )
                    Switch(
                        checked = strictFocusEnabled,
                        onCheckedChange = onStrictFocusChange,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = DeepIndigo,
                            checkedTrackColor = DeepIndigo.copy(alpha = 0.5f)
                        )
                    )
                }
            }
        }
    }
}
