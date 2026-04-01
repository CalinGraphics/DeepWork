package com.deepwork.feature.pcremote

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.deepwork.core.ui.theme.DeepIndigo
import com.deepwork.core.ui.theme.DeepTeal

@Composable
fun PcRemoteScreen(
    viewModel: PcRemoteViewModel = hiltViewModel()
) {
    val connectionState by viewModel.connectionState.collectAsState()
    var ipInput by remember { mutableStateOf("") }
    val scroll = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scroll)
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("PC Companion", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.28f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Gesture Map", fontWeight = FontWeight.SemiBold)
                }
                GestureRow("Rotate Phone", "Change Ambient Volume")
                GestureRow("Face Down", "Deep Focus Mode")
                GestureRow("Double Tap", "Log Quick Task")
            }
        }

        when (connectionState) {
            is ConnectionState.Disconnected, is ConnectionState.Error -> {
                if (connectionState is ConnectionState.Error) {
                    Text(
                        text = (connectionState as ConnectionState.Error).message,
                        color = Color(0xFFCF6679),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                TextField(
                    value = ipInput,
                    onValueChange = { ipInput = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Adresă PC (ex. 192.168.x.x sau 127.0.0.1 pentru USB)") },
                    singleLine = true,
                    placeholder = { Text("192.168.1.10") }
                )
                OutlinedButton(
                    onClick = {
                        ipInput = "127.0.0.1"
                        viewModel.connectToDesktop("127.0.0.1")
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Conectează prin USB (127.0.0.1)")
                }
                Button(
                    onClick = { viewModel.connectToDesktop(ipInput) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Conectează")
                }
            }
            is ConnectionState.Connecting -> {
                Text(
                    "Se conectează la ${ipInput.ifBlank { "…" }}…",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
            is ConnectionState.Connected -> {
                val connected = connectionState as ConnectionState.Connected
                Text(
                    "Conectat la PC: ${connected.ipAddress}",
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF34D399)
                )
                Text("Gestiuni HCI active: rotire telefon, față în jos, shake etc. — mesajele merg pe WebSocket către companion.")
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { viewModel.disconnect() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Deconectează")
                }
            }
        }
    }
}

@Composable
private fun GestureRow(title: String, subtitle: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(
            Modifier
                .size(32.dp)
                .padding(2.dp)
        ) {
            Box(
                Modifier
                    .size(20.dp)
                    .background(DeepIndigo.copy(alpha = 0.35f), RoundedCornerShape(6.dp))
            )
        }
        Column {
            Text(title, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            Text(subtitle, fontSize = 12.sp, color = Color(0xFF9090A0))
        }
    }
}
