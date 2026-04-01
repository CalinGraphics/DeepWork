package com.deepwork.domain.gamification

import com.deepwork.domain.model.Session
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

object StreakCalculator {

    fun activeDays(completedSessions: List<Session>): Set<LocalDate> =
        completedSessions
            .asSequence()
            .filter { it.completedAt > 0L }
            .map { instantToLocalDate(it.completedAt) }
            .toSet()

    fun currentStreak(completedSessions: List<Session>): Int {
        val days = activeDays(completedSessions)
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

    private fun instantToLocalDate(epochMillis: Long): LocalDate =
        Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).toLocalDate()
}
