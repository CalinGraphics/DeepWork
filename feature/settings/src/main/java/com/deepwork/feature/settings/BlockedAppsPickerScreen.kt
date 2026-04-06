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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BlockedAppsPickerScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val blocked by viewModel.blockedPackages.collectAsState()
    val apps by viewModel.launchableApps.collectAsState()
    val showSystem by viewModel.showSystemApps.collectAsState()
    val accessibilityOn by viewModel.accessibilityServiceEnabled.collectAsState()

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

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Aplicații de blocat",
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Înapoi"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { innerPadding ->
        val visibleApps = if (showSystem) apps else apps.filter { !it.isSystem }
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Spacer(modifier = Modifier.padding(top = 8.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                    ),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.06f))
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text(
                            "Bifează aplicațiile pe care nu vrei să le folosești cât timp rulează sesiunea Kara (timer pornit sau pe pauză).",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.padding(top = 8.dp))
                        Text(
                            if (accessibilityOn) "Serviciu accesibilitate: activ"
                            else "Serviciu accesibilitate: inactiv — activează Kara din setările sistemului.",
                            style = MaterialTheme.typography.labelLarge,
                            color = if (accessibilityOn) DeepTeal else MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.padding(top = 10.dp))
                        OutlinedButton(
                            onClick = {
                                context.startActivity(
                                    Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                )
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Deschide setări accesibilitate")
                        }
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "${blocked.size} blocate · ${visibleApps.size} afișate",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    TextButton(onClick = { viewModel.clearAllBlockedApps() }) {
                        Text("Șterge toate")
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Arată aplicații de sistem", fontWeight = FontWeight.SemiBold)
                        Text(
                            "De obicei nu ai nevoie de ele (overlay-uri, module).",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = showSystem,
                        onCheckedChange = { viewModel.setShowSystemApps(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = DeepIndigo,
                            checkedTrackColor = DeepIndigo.copy(alpha = 0.45f)
                        )
                    )
                }
            }

            item {
                if (!showSystem && visibleApps.size < 30) {
                    Text(
                        "Notă: dacă aici vezi foarte puține aplicații, e posibil să rulezi Kara într-un profil diferit (Work Profile / Secure Folder). " +
                            "Android nu îți permite să gestionezi aplicații din alt profil. Instalează și rulează Kara în același profil în care ai Instagram/Facebook/YouTube.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF9090A0)
                    )
                }
            }

            items(visibleApps, key = { it.packageName }) { app ->
                val checked = app.packageName in blocked
                Card(
                    modifier = Modifier.fillMaxWidth(),
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
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.padding(bottom = 32.dp))
            }
        }
    }
}
