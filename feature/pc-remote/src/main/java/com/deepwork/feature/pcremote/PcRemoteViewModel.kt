package com.deepwork.feature.pcremote

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.deepwork.data.remote.client.DeepWorkWebSocketClient
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PcRemoteViewModel @Inject constructor(
    private val webSocketClient: DeepWorkWebSocketClient
) : ViewModel() {

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    fun connectToDesktop(ipAddress: String) {
        val ip = normalizeIpInput(ipAddress)
        if (ip.isEmpty()) {
            _connectionState.value = ConnectionState.Error("Introdu o adresă IP (ex. 192.168.x.x sau 127.0.0.1).")
            return
        }
        _connectionState.value = ConnectionState.Connecting
        viewModelScope.launch {
            val result = webSocketClient.connect(ip)
            if (result.isSuccess) {
                _connectionState.value = ConnectionState.Connected(ip)
            } else {
                val detail = result.exceptionOrNull()?.message?.takeIf { it.isNotBlank() }
                    ?: "Verifică IP-ul, firewall-ul (port 8080) și că aplicația desktop rulează."
                _connectionState.value = ConnectionState.Error(detail)
            }
        }
    }

    fun disconnect() {
        viewModelScope.launch {
            webSocketClient.disconnect()
            _connectionState.value = ConnectionState.Disconnected
        }
    }

    private fun normalizeIpInput(raw: String): String {
        val s = raw.trim()
        if (s.isEmpty()) return ""
        // Acceptă și string-uri din QR gen "ws://192.168.1.10:8080/deepwork"
        return runCatching {
            val withScheme = if (s.startsWith("ws://") || s.startsWith("wss://") || s.startsWith("http://") || s.startsWith("https://")) s
            else "http://$s"
            val uri = java.net.URI(withScheme)
            uri.host ?: s
        }.getOrDefault(s)
    }
}

sealed interface ConnectionState {
    data object Disconnected : ConnectionState
    data object Connecting : ConnectionState
    data class Connected(val ipAddress: String) : ConnectionState
    data class Error(val message: String) : ConnectionState
}
