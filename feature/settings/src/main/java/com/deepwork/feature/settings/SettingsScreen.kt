package com.deepwork.feature.settings

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
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
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val prefs by viewModel.preferences.collectAsState()
    val blocked by viewModel.blockedPackages.collectAsState()
    val apps by viewModel.launchableApps.collectAsState()
    val accessibilityOn by viewModel.accessibilityServiceEnabled.collectAsState()
    var sliderValue by remember { mutableFloatStateOf(prefs.sessionDuration.toFloat()) }
    var appFilter by remember { mutableStateOf("") }

    LaunchedEffect(prefs.sessionDuration) {
        sliderValue = prefs.sessionDuration.toFloat().coerceIn(MIN_SESSION.toFloat(), MAX_SESSION.toFloat())
    }

    LaunchedEffect(Unit) {
        viewModel.refreshLaunchableApps()
        viewModel.refreshAccessibilityServiceState()
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshAccessibilityServiceState()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val filteredApps = remember(apps, appFilter) {
        if (appFilter.isBlank()) apps
        else apps.filter {
            it.label.contains(appFilter, ignoreCase = true) ||
                it.packageName.contains(appFilter, ignoreCase = true)
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            Text("Setări", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        }

        item {
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
        }

        item {
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
                        "Blocare selectivă aplicații",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        "În timpul unei sesiuni active (timer pornit sau pe pauză), Kara te poate readuce în aplicație dacă deschizi o aplicație pe care ai bifat-o mai jos. " +
                            "Nu blochează întreg telefonul — doar aplicațiile alese. Activează serviciul de accesibilitate Kara pentru ca blocarea să funcționeze.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                    )
                    Text(
                        if (accessibilityOn) "Serviciu accesibilitate: activ" else "Serviciu accesibilitate: inactiv",
                        style = MaterialTheme.typography.labelLarge,
                        color = if (accessibilityOn) DeepTeal else MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = {
                            context.startActivity(
                                Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Deschide setări accesibilitate")
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = appFilter,
                        onValueChange = { appFilter = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Caută aplicație") },
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "${blocked.size} blocate",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        TextButton(onClick = { viewModel.clearAllBlockedApps() }) {
                            Text("Șterge toate")
                        }
                    }
                }
            }
        }

        items(filteredApps, key = { it.packageName }) { app ->
            val checked = app.packageName in blocked
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                ),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.04f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = checked,
                        onCheckedChange = { viewModel.setPackageBlocked(app.packageName, it) },
                        colors = CheckboxDefaults.colors(checkedColor = DeepIndigo)
                    )
                    Column(Modifier.weight(1f)) {
                        Text(app.label, style = MaterialTheme.typography.bodyMedium)
                        Text(
                            app.packageName,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        item {
            Button(onClick = onOpenDesktopPairing, modifier = Modifier.fillMaxWidth()) {
                Text("Conectare remote prin IP sau USB")
            }
        }
        item {
            Text(
                "Deschide ecranul de împerechere când aplicația desktop rulează pe PC.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        item {
            OutlinedButton(onClick = { viewModel.replayOnboarding() }, modifier = Modifier.fillMaxWidth()) {
                Text("Arata din nou turul aplicatiei")
            }
        }
        item {
            Text(
                "Reseteaza onboarding-ul si revine imediat la primul ecran de tur.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
