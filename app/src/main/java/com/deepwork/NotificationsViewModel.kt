package com.deepwork

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.deepwork.domain.repository.SessionRepository
import com.deepwork.domain.repository.TaskRepository
import com.deepwork.domain.usecase.CalculateStreakUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@HiltViewModel
class NotificationsViewModel @Inject constructor(
    sessionRepository: SessionRepository,
    taskRepository: TaskRepository,
    calculateStreakUseCase: CalculateStreakUseCase
) : ViewModel() {

    val notifications: StateFlow<List<AppNotification>> = combine(
        sessionRepository.getSessions(),
        taskRepository.getTasks()
    ) { sessions, tasks ->
        val streak = runCatching { calculateStreakUseCase() }.getOrNull()
        val zone = ZoneId.systemDefault()
        val timeFormat = DateTimeFormatter.ofPattern("HH:mm")
        val completed = sessions.filter { it.completedAt > 0L }
        val latestSession = completed.maxByOrNull { it.completedAt }
        val pendingTasks = tasks.count { !it.isCompleted }

        buildList {
            if (latestSession != null) {
                val at = Instant.ofEpochMilli(latestSession.completedAt).atZone(zone)
                add(
                    AppNotification(
                        title = "Sesiune finalizata",
                        body = "Ai terminat ${latestSession.durationMinutes} min focus la ${at.format(timeFormat)}.",
                        priority = NotificationPriority.HIGH
                    )
                )
            }

            add(
                AppNotification(
                    title = "Memento focus",
                    body = if (pendingTasks > 0) {
                        "Ai $pendingTasks task-uri nefinalizate. Porneste o sesiune de 25 min."
                    } else {
                        "Ai inbox-ul curat. Fa o sesiune scurta pentru streak."
                    },
                    priority = NotificationPriority.NORMAL
                )
            )

            if ((streak?.currentStreak ?: 0) > 0) {
                add(
                    AppNotification(
                        title = "Streak activ",
                        body = "Pastreaza streak-ul de ${streak?.currentStreak} zile cu inca o sesiune azi.",
                        priority = NotificationPriority.HIGH
                    )
                )
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )
}

data class AppNotification(
    val title: String,
    val body: String,
    val priority: NotificationPriority
)

enum class NotificationPriority {
    HIGH,
    NORMAL
}
