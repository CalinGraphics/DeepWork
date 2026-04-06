package com.deepwork.desktop

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.JsonPrimitive

object DesktopState {
    private val _status = MutableStateFlow("Waiting for Phone connection...")
    val status: StateFlow<String> = _status.asStateFlow()

    private val _connected = MutableStateFlow(false)
    val connected: StateFlow<Boolean> = _connected.asStateFlow()

    private val _lastGesture = MutableStateFlow<String?>(null)
    val lastGesture: StateFlow<String?> = _lastGesture.asStateFlow()

    private val _pairingUrl = MutableStateFlow("")
    val pairingUrl: StateFlow<String> = _pairingUrl.asStateFlow()
    private val _usbBridgeStatus = MutableStateFlow("USB bridge: pending")
    val usbBridgeStatus: StateFlow<String> = _usbBridgeStatus.asStateFlow()

    private val _blockingOverlayAppName = MutableStateFlow<String?>(null)
    val blockingOverlayAppName: StateFlow<String?> = _blockingOverlayAppName.asStateFlow()
    private var suppressedOverlayAppName: String? = null

    fun setPairingUrl(url: String) {
        _pairingUrl.value = url
    }

    fun setUsbBridgeStatus(status: String) {
        _usbBridgeStatus.value = status
    }

    fun updateStatus(message: String) {
        _status.value = message
    }

    fun setConnected(value: Boolean) {
        _connected.value = value
    }

    fun showBlockingOverlay(appName: String) {
        if (suppressedOverlayAppName == appName) return
        _blockingOverlayAppName.value = appName
    }

    fun clearBlockingOverlay() {
        _blockingOverlayAppName.value = null
        suppressedOverlayAppName = null
    }

    fun dismissBlockingOverlay() {
        val cur = _blockingOverlayAppName.value
        if (!cur.isNullOrBlank()) {
            suppressedOverlayAppName = cur
        }
        _blockingOverlayAppName.value = null
    }

    fun onMessage(msg: DeepWorkMessage) {
        when (msg.type) {
            MessageType.GESTURE_ACTION -> {
                val p = msg.payload
                _lastGesture.value = when (p) {
                    is JsonPrimitive -> p.content
                    else -> p.toString()
                }
            }
            else -> { }
        }
        _status.value = msg.type.name
    }
}
