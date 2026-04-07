package com.deepwork.desktop

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

enum class DesktopSessionPhase {
    Idle,
    Running,
    Paused
}

enum class SessionEndReason {
    /** Do not write history line */
    None,
    Completed,
    UserEnded,
    Remote
}

@Serializable
data class DesktopSessionLog(
    val time: String,
    val message: String
)

@Serializable
private data class DesktopLocalSnapshot(
    val preferredMinutes: Int = 25,
    val history: List<DesktopSessionLog> = emptyList()
)

/**
 * Local focus timer on desktop — works without a phone. Optional sync when phone sends TIMER_* over WebSocket.
 */
object DesktopLocalSession {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var tickJob: Job? = null
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val storeDir = File(System.getProperty("user.home") ?: ".", ".deepwork").apply { mkdirs() }
    private val snapshotFile = File(storeDir, "desktop-session.json")
    private val timeFmt = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")
    private val lock = Any()
    private val loaded = loadSnapshot()

    private val _phase = MutableStateFlow(DesktopSessionPhase.Idle)
    val phase: StateFlow<DesktopSessionPhase> = _phase.asStateFlow()

    private val _preferredMinutes = MutableStateFlow(loaded.preferredMinutes.coerceIn(1, 360))
    val preferredMinutes: StateFlow<Int> = _preferredMinutes.asStateFlow()

    private val _remainingSeconds = MutableStateFlow(_preferredMinutes.value * 60)
    val remainingSeconds: StateFlow<Int> = _remainingSeconds.asStateFlow()

    private val _sessionHistory = MutableStateFlow<List<DesktopSessionLog>>(loaded.history.take(100))
    val sessionHistory: StateFlow<List<DesktopSessionLog>> = _sessionHistory.asStateFlow()

    private val _sessionJustCompletedMinutes = MutableSharedFlow<Int>(extraBufferCapacity = 1)
    val sessionJustCompletedMinutes: SharedFlow<Int> = _sessionJustCompletedMinutes.asSharedFlow()

    fun setPreferredMinutes(minutes: Int, fromRemote: Boolean = false) {
        val v = minutes.coerceIn(1, 360)
        _preferredMinutes.value = v
        if (_phase.value == DesktopSessionPhase.Idle) {
            _remainingSeconds.value = v * 60
        }
        persistSnapshot()
        if (!fromRemote && DesktopWebSocketRelay.hasClient()) {
            DesktopWebSocketRelay.broadcastTimerSyncMinutes(v)
        }
    }

    fun startSession() {
        val plannedSeconds = _preferredMinutes.value * 60
        when (_phase.value) {
            DesktopSessionPhase.Idle -> {
                _remainingSeconds.value = plannedSeconds
                _phase.value = DesktopSessionPhase.Running
                startTick()
            }
            DesktopSessionPhase.Paused -> {
                _phase.value = DesktopSessionPhase.Running
                startTick()
            }
            DesktopSessionPhase.Running -> {}
        }
    }

    fun pause() {
        if (_phase.value != DesktopSessionPhase.Running) return
        tickJob?.cancel()
        tickJob = null
        _phase.value = DesktopSessionPhase.Paused
    }

    /** Revine la idle cu timpul complet, fără a înregistra sesiune (ca Reset pe Android). */
    fun reset() {
        tickJob?.cancel()
        tickJob = null
        _phase.value = DesktopSessionPhase.Idle
        _remainingSeconds.value = _preferredMinutes.value * 60
    }

    fun endSession(reason: SessionEndReason = SessionEndReason.UserEnded) {
        tickJob?.cancel()
        tickJob = null
        val wasActive = _phase.value != DesktopSessionPhase.Idle
        val planned = _preferredMinutes.value * 60
        val remainingSnapshot = _remainingSeconds.value
        _phase.value = DesktopSessionPhase.Idle
        _remainingSeconds.value = planned
        if (!wasActive || reason == SessionEndReason.None) return

        val elapsedSec = when (reason) {
            SessionEndReason.Completed -> planned
            else -> (planned - remainingSnapshot).coerceIn(0, planned)
        }
        val completedFull = reason == SessionEndReason.Completed
        scope.launch(Dispatchers.IO) {
            DesktopGamification.recordSession(elapsedSec, planned, completedFull)
        }

        val msg = when (reason) {
            SessionEndReason.Completed ->
                "Sesiune finalizată (${planned / 60} min focus)."
            SessionEndReason.UserEnded ->
                "Sesiune încheiată manual (End)."
            SessionEndReason.Remote ->
                "Sesiune oprită din sync cu telefonul."
            SessionEndReason.None -> return
        }
        logSession(msg)
        if (reason == SessionEndReason.Completed) {
            scope.launch {
                _sessionJustCompletedMinutes.emit(planned / 60)
            }
        }
    }

    fun applyRemoteMessage(msg: DeepWorkMessage) {
        when (msg.type) {
            MessageType.TIMER_START -> startSession()
            MessageType.TIMER_PAUSE -> pause()
            MessageType.TIMER_STOP -> endSession(SessionEndReason.Remote)
            MessageType.TIMER_COMPLETED -> {
                val minutes = (msg.payload as? JsonPrimitive)?.content?.toIntOrNull()?.coerceIn(1, 360)
                    ?: _preferredMinutes.value
                DesktopSessionAlerts.ensureTray()
                DesktopSessionAlerts.notifySessionCompleted(minutes)
            }
            MessageType.TIMER_SYNC -> {
                val minutes = (msg.payload as? JsonPrimitive)?.content?.toIntOrNull()?.coerceIn(1, 360)
                    ?: return
                setPreferredMinutes(minutes, fromRemote = true)
            }
            else -> {}
        }
    }

    private fun logSession(message: String) {
        val line = DesktopSessionLog(LocalDateTime.now().format(timeFmt), message)
        _sessionHistory.update { (listOf(line) + it).take(100) }
        persistSnapshot()
    }

    private fun startTick() {
        tickJob?.cancel()
        tickJob = scope.launch {
            while (isActive && _phase.value == DesktopSessionPhase.Running) {
                delay(1000)
                val next = _remainingSeconds.value - 1
                if (next <= 0) {
                    _remainingSeconds.value = 0
                    endSession(SessionEndReason.Completed)
                    return@launch
                }
                _remainingSeconds.value = next
            }
        }
    }

    private fun loadSnapshot(): DesktopLocalSnapshot {
        return try {
            if (!snapshotFile.exists()) DesktopLocalSnapshot()
            else json.decodeFromString(DesktopLocalSnapshot.serializer(), snapshotFile.readText())
        } catch (_: Exception) {
            DesktopLocalSnapshot()
        }
    }

    private fun persistSnapshot() {
        synchronized(lock) {
            runCatching {
                snapshotFile.writeText(
                    json.encodeToString(
                        DesktopLocalSnapshot.serializer(),
                        DesktopLocalSnapshot(
                            preferredMinutes = _preferredMinutes.value,
                            history = _sessionHistory.value.take(100)
                        )
                    )
                )
            }
        }
    }

    /** Timer pornit sau pe pauză — folosit pentru blocarea altor aplicații pe desktop. */
    fun isFocusSessionActive(): Boolean =
        _phase.value == DesktopSessionPhase.Running || _phase.value == DesktopSessionPhase.Paused
}

enum class DesktopMainTab {
    Dashboard,
    History,
    Insights
}
