package com.deepwork.feature.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.deepwork.core.ui.theme.DeepIndigo
import com.deepwork.core.ui.theme.DeepTeal
import kotlin.math.roundToInt

private const val MIN_SESSION = 5
private const val MAX_SESSION = 120

@Composable
fun SettingsScreen(
    onOpenDesktopPairing: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val prefs by viewModel.preferences.collectAsState()
    var sliderValue by remember { mutableFloatStateOf(prefs.sessionDuration.toFloat()) }

    LaunchedEffect(prefs.sessionDuration) {
        sliderValue = prefs.sessionDuration.toFloat().coerceIn(MIN_SESSION.toFloat(), MAX_SESSION.toFloat())
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Text("Setări", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
            ),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.06f))
        ) {
            Column(Modifier.padding(20.dp)) {
                Text(
                    "Durată sesiune",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    "Folosită pentru Start pe Timer și pentru gesturi HCI.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "${sliderValue.roundToInt()} minute",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = DeepTeal
                    )
                    Text(
                        "$MIN_SESSION–$MAX_SESSION min",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Slider(
                    value = sliderValue,
                    onValueChange = { sliderValue = it },
                    onValueChangeFinished = {
                        val v = sliderValue.roundToInt().coerceIn(MIN_SESSION, MAX_SESSION)
                        sliderValue = v.toFloat()
                        viewModel.updateSessionDuration(v)
                    },
                    valueRange = MIN_SESSION.toFloat()..MAX_SESSION.toFloat(),
                    steps = 0,
                    colors = SliderDefaults.colors(
                        thumbColor = DeepIndigo,
                        activeTrackColor = DeepIndigo,
                        inactiveTrackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                    )
                )
            }
        }

        Text(
            if (prefs.isPremium) "Cont Premium" else "Cont Free",
            style = MaterialTheme.typography.bodyLarge
        )
        Button(onClick = onOpenDesktopPairing, modifier = Modifier.fillMaxWidth()) {
            Text("Conectare companion desktop (IP / USB)")
        }
        Text(
            "Deschide ecranul de împerechere când aplicația desktop rulează pe PC.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        OutlinedButton(onClick = { viewModel.replayOnboarding() }, modifier = Modifier.fillMaxWidth()) {
            Text("Arata din nou turul aplicatiei")
        }
        Text(
            "Reseteaza onboarding-ul si revine imediat la primul ecran de tur.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
