package com.deepwork.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val SurfaceC = Color(0xFF1E1C31)
private val SurfaceVar = Color(0xFF272546)
private val DeepIndigo = Color(0xFF5C55E8)
private val DeepTeal = Color(0xFF00C4D4)

@Composable
fun DesktopBlockAppsCard() {
    val isWin = remember { WindowsForegroundExe.isWindowsOs() }
    val enabled by DesktopBlockedAppsStore.enabled.collectAsState()
    val blocked by DesktopBlockedAppsStore.blocked.collectAsState()
    var showPicker by remember { mutableStateOf(false) }

    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceC),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceVar),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Outlined.Block, contentDescription = null, tint = DeepIndigo)
                Text(
                    "Blocare aplicații (Windows)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    modifier = Modifier.weight(1f)
                )
            }
            if (!isWin) {
                Text(
                    "Disponibil doar pe Windows: citește programele instalate și readuce fereastra Kara când deschizi un .exe blocat în timpul sesiunii.",
                    fontSize = 12.sp,
                    color = Color(0xFF9090A0)
                )
            } else {
                Text(
                    "În timpul sesiunii Pomodoro active, dacă deschizi un program bifat, vei primi overlay de blocare focus.",
                    fontSize = 12.sp,
                    color = Color(0xFF9090A0)
                )
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Activează monitorizarea", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Text(
                            "${blocked.size} .exe blocate",
                            fontSize = 12.sp,
                            color = DeepTeal
                        )
                    }
                    Switch(
                        checked = enabled,
                        onCheckedChange = { DesktopBlockedAppsStore.setEnabled(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = DeepIndigo,
                            checkedTrackColor = DeepIndigo.copy(alpha = 0.45f)
                        )
                    )
                }
                OutlinedButton(
                    onClick = { showPicker = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Alege aplicații instalate")
                }
            }
        }
    }

    if (showPicker) {
        DesktopBlockAppsPickerDialog(onDismiss = { showPicker = false })
    }
}

@Composable
fun DesktopBlockAppsPickerDialog(onDismiss: () -> Unit) {
    val blocked by DesktopBlockedAppsStore.blocked.collectAsState()
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var rows by remember { mutableStateOf<List<DesktopInstalledAppRow>>(emptyList()) }

    LaunchedEffect(Unit) {
        loading = true
        error = null
        val result = DesktopInstalledAppsScanner.scan()
        result.onSuccess {
            rows = it
            loading = false
        }.onFailure {
            error = it.message ?: "Scan eșuat"
            loading = false
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Aplicații instalate", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(
                Modifier
                    .widthIn(min = 520.dp, max = 720.dp)
                    .padding(top = 4.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "Bifează aplicațiile (exe) pe care nu vrei să le poți folosi cât timp Kara este deschisă.",
                    fontSize = 12.sp,
                    color = Color(0xFFB0B0C0)
                )
                when {
                    loading -> {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(320.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(40.dp),
                                color = DeepTeal
                            )
                        }
                    }
                    error != null -> {
                        Text(
                            error!!,
                            color = Color(0xFFFF8A80),
                            fontSize = 13.sp,
                            modifier = Modifier.padding(vertical = 16.dp)
                        )
                    }
                    else -> {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(360.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            items(rows.filter { it.name.isNotBlank() }, key = { it.exe + it.name }) { row ->
                                val checked = row.exe in blocked
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(
                                            SurfaceVar.copy(alpha = 0.35f),
                                            RoundedCornerShape(10.dp)
                                        )
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = checked,
                                        onCheckedChange = { DesktopBlockedAppsStore.setBlocked(row.exe, it) },
                                        colors = CheckboxDefaults.colors(checkedColor = DeepIndigo)
                                    )
                                    Spacer(Modifier.size(6.dp))
                                    Text(row.name, fontSize = 13.sp, color = Color(0xFFE8E8F0), modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }
        },
        dismissButton = {
            TextButton(onClick = { DesktopBlockedAppsStore.clearAll() }) {
                Text("Șterge toate", color = Color(0xFFFF8A80))
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Gata", color = DeepTeal, fontWeight = FontWeight.SemiBold)
            }
        },
        containerColor = SurfaceC
    )
}
