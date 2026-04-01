package com.deepwork.desktop

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

private object DesktopAchievementIds {
    const val FIRST_FOCUS = "ach_first_focus"
    const val TEN_SESSIONS = "ach_ten_sessions"
    const val STREAK_3 = "ach_streak_3"
    const val STREAK_7 = "ach_streak_7"
    const val XP_500 = "ach_xp_500"
    const val XP_2000 = "ach_xp_2000"
}

@Serializable
data class DesktopGamificationState(
    val totalXp: Int = 0,
    val unlockedAchievementIds: Set<String> = emptySet(),
    val completedSessionEpochMillis: List<Long> = emptyList()
)

/**
 * Persistență locală (JSON) pentru XP, streak și realizări — același set de ID-uri ca pe Android.
 */
object DesktopGamification {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val dir = File(System.getProperty("user.home") ?: ".", ".deepwork").apply { mkdirs() }
    private val file = File(dir, "gamification.json")
    private val lock = Any()

    private val _state = MutableStateFlow(load())
    val state: StateFlow<DesktopGamificationState> = _state.asStateFlow()

    private fun load(): DesktopGamificationState {
        return try {
            if (!file.exists()) {
                DesktopGamificationState()
            } else {
                json.decodeFromString(
                    DesktopGamificationState.serializer(),
                    file.readText()
                )
            }
        } catch (_: Exception) {
            DesktopGamificationState()
        }
    }

    private fun persist(s: DesktopGamificationState) {
        try {
            file.writeText(json.encodeToString(DesktopGamificationState.serializer(), s))
        } catch (_: Exception) {
        }
    }

    private fun xpForSession(elapsedSeconds: Int, plannedSeconds: Int, completedFull: Boolean): Int {
        val planned = plannedSeconds.coerceAtLeast(1)
        val elapsed = elapsedSeconds.coerceIn(0, planned)
        if (elapsed <= 0) return 0
        val minutesActual = elapsed / 60
        val minutesForXp = when {
            completedFull -> planned / 60
            else -> minutesActual.coerceAtLeast(1)
        }
        return minutesForXp * 2 + if (completedFull) 10 else 0
    }

    private fun activeDays(millis: List<Long>): Set<LocalDate> =
        millis.map { Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate() }.toSet()

    private fun currentStreak(millis: List<Long>): Int {
        val days = activeDays(millis)
        if (days.isEmpty()) return 0
        val today = LocalDate.now()
        var anchor = today
        if (!days.contains(today)) {
            anchor = today.minusDays(1)
            if (!days.contains(anchor)) return 0
        }
        var streak = 0
        var d = anchor
        while (days.contains(d)) {
            streak++
            d = d.minusDays(1)
        }
        return streak
    }

    fun recordSession(elapsedSeconds: Int, plannedSeconds: Int, completedFull: Boolean) {
        synchronized(lock) {
            val base = _state.value
            val delta = xpForSession(elapsedSeconds, plannedSeconds, completedFull)
            val elapsed = elapsedSeconds.coerceIn(0, plannedSeconds.coerceAtLeast(1))
            val completions =
                if (elapsed > 0) {
                    base.completedSessionEpochMillis + System.currentTimeMillis()
                } else {
                    base.completedSessionEpochMillis
                }
            val trimmedCompletions = completions.takeLast(500)

            var totalXp = base.totalXp + delta
            val unlocked = base.unlockedAchievementIds.toMutableSet()
            val completedCount = trimmedCompletions.size
            val streak = currentStreak(trimmedCompletions)

            fun grant(id: String, reward: Int) {
                if (id in unlocked) return
                unlocked.add(id)
                totalXp += reward
            }

            if (completedCount >= 1) grant(DesktopAchievementIds.FIRST_FOCUS, 25)
            if (completedCount >= 10) grant(DesktopAchievementIds.TEN_SESSIONS, 100)
            if (streak >= 3) grant(DesktopAchievementIds.STREAK_3, 50)
            if (streak >= 7) grant(DesktopAchievementIds.STREAK_7, 150)
            if (totalXp >= 500) grant(DesktopAchievementIds.XP_500, 30)
            if (totalXp >= 2000) grant(DesktopAchievementIds.XP_2000, 200)

            val next = DesktopGamificationState(
                totalXp = totalXp,
                unlockedAchievementIds = unlocked,
                completedSessionEpochMillis = trimmedCompletions
            )
            _state.value = next
            persist(next)
        }
    }

    fun level(totalXp: Int): Int = (totalXp / 500) + 1
}
