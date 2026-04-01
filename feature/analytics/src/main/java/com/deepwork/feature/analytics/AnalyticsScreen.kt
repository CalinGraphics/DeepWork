package com.deepwork.feature.analytics

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.deepwork.core.ui.theme.DeepIndigo
import com.deepwork.core.ui.theme.DeepTeal

@Composable
fun AnalyticsScreen(
    onOpenAchievements: () -> Unit = {},
    viewModel: AnalyticsViewModel = hiltViewModel()
) {
    val result by viewModel.streakResult.collectAsState()
    val heatmapDays by viewModel.heatmapDays.collectAsState()
    val scroll = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scroll)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text("Insights", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        OutlinedButton(onClick = onOpenAchievements, modifier = Modifier.fillMaxWidth()) {
            Text("Realizări & nivel")
        }
        result?.let { r ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SummaryChip(
                    label = "Streak",
                    value = "${r.currentStreak} zile",
                    modifier = Modifier.weight(1f)
                )
                SummaryChip(
                    label = "Focus",
                    value = "${r.focusScore}%",
                    modifier = Modifier.weight(1f)
                )
                SummaryChip(
                    label = "XP",
                    value = "${r.totalXp}",
                    modifier = Modifier.weight(1f)
                )
            }
        }
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Gamificare", fontWeight = FontWeight.SemiBold)
                Text(
                    result?.let { "Nivel ${(it.totalXp / 500) + 1} · ${it.totalXp} XP · streak ${it.currentStreak} zile" }
                        ?: "Nivel 1 · 0 XP · streak 0 zile",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Productivity Heatmap", fontWeight = FontWeight.SemiBold)
                    Text(
                        result?.let { "${it.focusScore}%" } ?: "0%",
                        color = DeepTeal,
                        fontWeight = FontWeight.Bold
                    )
                }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                        .padding(horizontal = 4.dp, vertical = 8.dp),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                heatmapDays.forEach { day ->
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height((day.heightFraction * 120).dp)
                                .background(
                                    DeepIndigo.copy(alpha = 0.30f + day.heightFraction * 0.55f),
                                    RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)
                                )
                        )
                        Text(
                            day.label,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "${day.minutes}m",
                            style = MaterialTheme.typography.labelSmall,
                            color = DeepTeal
                        )
                    }
                }
            }
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun SummaryChip(label: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = DeepIndigo.copy(alpha = 0.1f)
        )
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                value,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
