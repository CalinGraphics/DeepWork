package com.deepwork.feature.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.deepwork.domain.repository.SessionRepository
import com.deepwork.domain.usecase.CalculateStreakUseCase
import com.deepwork.domain.usecase.StreakResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale

@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    private val calculateStreakUseCase: CalculateStreakUseCase,
    sessionRepository: SessionRepository
) : ViewModel() {

    private val _streakResult = MutableStateFlow<StreakResult?>(null)
    val streakResult: StateFlow<StreakResult?> = _streakResult.asStateFlow()

    val heatmapDays: StateFlow<List<HeatmapDay>> = sessionRepository.getSessions()
        .combine(_streakResult) { sessions, _ ->
            val zone = ZoneId.systemDefault()
            val today = LocalDate.now(zone)
            val completed = sessions.filter { it.completedAt > 0L }
            val minutesByDay = completed.groupBy {
                Instant.ofEpochMilli(it.completedAt).atZone(zone).toLocalDate()
            }.mapValues { (_, daySessions) -> daySessions.sumOf { it.durationMinutes } }

            val rollingDays = (6 downTo 0).map { delta -> today.minusDays(delta.toLong()) }
            val maxMinutes = (rollingDays.maxOfOrNull { minutesByDay[it] ?: 0 } ?: 0).coerceAtLeast(1)

            rollingDays.map { day ->
                val minutes = minutesByDay[day] ?: 0
                HeatmapDay(
                    label = day.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.ENGLISH),
                    minutes = minutes,
                    heightFraction = (minutes.toFloat() / maxMinutes.toFloat()).coerceIn(0.08f, 1f)
                )
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            _streakResult.value = calculateStreakUseCase()
        }
    }
}

data class HeatmapDay(
    val label: String,
    val minutes: Int,
    val heightFraction: Float
)
