package com.deepwork.feature.timer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.deepwork.data.local.preferences.UserPreferencesRepository
import com.deepwork.data.remote.client.DeepWorkWebSocketClient
import com.deepwork.data.remote.model.DeepWorkMessage
import com.deepwork.data.remote.model.MessageType
import com.deepwork.domain.model.Action
import com.deepwork.domain.model.GestureType
import com.deepwork.domain.model.Task
import com.deepwork.domain.model.TimerState
import com.deepwork.domain.repository.SensorRepository
import com.deepwork.domain.repository.SessionRepository
import com.deepwork.domain.repository.TaskRepository
import com.deepwork.domain.usecase.CalculateStreakUseCase
import com.deepwork.domain.usecase.CompleteSessionUseCase
import com.deepwork.domain.usecase.StartTimerUseCase
import com.deepwork.domain.usecase.StreakResult
import com.deepwork.domain.usecase.SyncGyroUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonPrimitive
import javax.inject.Inject

@HiltViewModel
class TimerViewModel @Inject constructor(
    private val startTimerUseCase: StartTimerUseCase,
    private val completeSessionUseCase: CompleteSessionUseCase,
    private val syncGyroUseCase: SyncGyroUseCase,
    private val webSocketClient: DeepWorkWebSocketClient,
    private val sensorRepository: SensorRepository,
    private val taskRepository: TaskRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val sessionRepository: SessionRepository,
    private val calculateStreakUseCase: CalculateStreakUseCase,
    private val timerNotifier: TimerNotifier
) : ViewModel() {

    private val _uiState = MutableStateFlow<TimerState>(TimerState.Idle)
    val uiState: StateFlow<TimerState> = _uiState.asStateFlow()

    private val _streak = MutableStateFlow<StreakResult?>(null)
    val streak: StateFlow<StreakResult?> = _streak.asStateFlow()

    private val _sessionDurationMinutes = MutableStateFlow(25)
    val sessionDurationMinutes: StateFlow<Int> = _sessionDurationMinutes.asStateFlow()

    val activeTask: StateFlow<Task?> = combine(
        taskRepository.getTasks(),
        userPreferencesRepository.activeTaskIdFlow
    ) { tasks, preferredId ->
        val byPreference = preferredId?.let { id ->
            tasks.find { it.id == id && !it.isCompleted }
        }
        byPreference ?: tasks.firstOrNull { !it.isCompleted }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = null
    )

    private var timerJob: Job? = null
    private var currentSessionId: String? = null
    private var plannedSessionMinutes: Int = 25
    private var sessionTotalMillis: Long = plannedSessionMinutes * 60_000L

    init {
        viewModelScope.launch {
            userPreferencesRepository.userPreferencesFlow.collect { prefs ->
                _sessionDurationMinutes.value = prefs.sessionDuration.coerceIn(5, 120)
            }
        }
        viewModelScope.launch {
            _streak.value = calculateStreakUseCase()
        }
        viewModelScope.launch {
            sensorRepository.getGestureFlow().collect { gesture ->
                handleGyroAction(gesture)
            }
        }
    }

    fun resetSession() {
        timerJob?.cancel()
        when (val s = _uiState.value) {
            is TimerState.Running, is TimerState.Paused ->
                completeCurrentSession(completedFull = false)
            is TimerState.Completed -> {
                /* XP deja înregistrat în countdown */
            }
            is TimerState.Idle -> abandonDraftSession()
            is TimerState.Error -> abandonDraftSession()
        }
        _uiState.value = TimerState.Idle
        notifyDesktop(MessageType.TIMER_STOP)
    }

    fun startSession(durationMinutes: Int) {
        viewModelScope.launch {
            plannedSessionMinutes = durationMinutes
            sessionTotalMillis = durationMinutes * 60_000L
            val result = startTimerUseCase(durationMinutes)
            result.onSuccess { session ->
                currentSessionId = session.id
                val durationMillis = sessionTotalMillis
                _uiState.update { TimerState.Running(durationMillis, 1f) }
                startCountdown(durationMillis)
                notifyDesktop(MessageType.TIMER_START)
                timerNotifier.showSessionStarted(durationMinutes)
            }
        }
    }

    fun pauseSession() {
        val currentState = _uiState.value
        if (currentState is TimerState.Running) {
            timerJob?.cancel()
            _uiState.update { TimerState.Paused(currentState.remainingMillis, currentState.progress) }
            notifyDesktop(MessageType.TIMER_PAUSE)
            timerNotifier.showSessionPaused()
        }
    }

    fun resumeSession() {
        val currentState = _uiState.value
        if (currentState is TimerState.Paused) {
            _uiState.update { TimerState.Running(currentState.remainingMillis, currentState.progress) }
            startCountdown(currentState.remainingMillis)
            notifyDesktop(MessageType.TIMER_START)
        }
    }

    fun stopSession() {
        timerJob?.cancel()
        when (_uiState.value) {
            is TimerState.Running, is TimerState.Paused ->
                completeCurrentSession(completedFull = false)
            is TimerState.Error -> abandonDraftSession()
            else -> {}
        }
        _uiState.update { TimerState.Idle }
        notifyDesktop(MessageType.TIMER_STOP)
    }

    private fun startCountdown(remainingMillis: Long) {
        timerJob?.cancel()
        val totalMillis = sessionTotalMillis
        timerJob = viewModelScope.launch {
            var currentRemaining = remainingMillis
            while (currentRemaining > 0) {
                delay(1000)
                currentRemaining -= 1000
                val progress = currentRemaining.toFloat() / totalMillis.toFloat()
                _uiState.update { TimerState.Running(currentRemaining, progress) }

                if (currentRemaining % 10000L == 0L) {
                    notifyDesktop(MessageType.SESSION_UPDATE)
                }
            }
            completeCurrentSession(completedFull = true)
            _uiState.update { TimerState.Completed(plannedSessionMinutes, null) }
            timerNotifier.showSessionCompleted(plannedSessionMinutes)
        }
    }

    private fun completeCurrentSession(completedFull: Boolean) {
        val id = currentSessionId ?: return
        val state = _uiState.value
        val remaining = when (state) {
            is TimerState.Running -> state.remainingMillis
            is TimerState.Paused -> state.remainingMillis
            is TimerState.Completed -> 0L
            is TimerState.Error -> sessionTotalMillis
            else -> sessionTotalMillis
        }
        val elapsed = (sessionTotalMillis - remaining).coerceAtLeast(0L)
        viewModelScope.launch {
            completeSessionUseCase(
                sessionId = id,
                plannedMinutes = plannedSessionMinutes,
                elapsedMillis = elapsed,
                completedFull = completedFull
            )
            currentSessionId = null
            _streak.value = calculateStreakUseCase()
        }
    }

    private fun abandonDraftSession() {
        val id = currentSessionId ?: return
        viewModelScope.launch {
            sessionRepository.deleteSession(id)
            currentSessionId = null
        }
    }

    private fun handleGyroAction(gesture: GestureType) {
        val action = syncGyroUseCase(gesture, isPremium = true)

        if (webSocketClient.isConnected) {
            viewModelScope.launch {
                webSocketClient.sendMessage(
                    DeepWorkMessage(
                        type = MessageType.GESTURE_ACTION,
                        payload = JsonPrimitive(gesture.name),
                        deviceId = "android_client"
                    )
                )
            }
        }

        when (action) {
            Action.StartSession ->
                if (_uiState.value is TimerState.Idle) startSession(_sessionDurationMinutes.value) else resumeSession()
            Action.PauseSession -> pauseSession()
            Action.ResumeSession -> resumeSession()
            Action.ResetSession -> resetSession()
            Action.StopSession -> stopSession()
            else -> {}
        }
    }

    private fun notifyDesktop(type: MessageType) {
        viewModelScope.launch {
            webSocketClient.sendMessage(
                DeepWorkMessage(
                    type = type,
                    payload = JsonPrimitive(_uiState.value.javaClass.simpleName),
                    deviceId = "android_client"
                )
            )
        }
    }
}
