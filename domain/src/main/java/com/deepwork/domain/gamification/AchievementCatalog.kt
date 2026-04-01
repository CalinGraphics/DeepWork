package com.deepwork.domain.gamification

import com.deepwork.domain.model.Achievement

object AchievementIds {
    const val FIRST_FOCUS = "ach_first_focus"
    const val TEN_SESSIONS = "ach_ten_sessions"
    const val STREAK_3 = "ach_streak_3"
    const val STREAK_7 = "ach_streak_7"
    const val XP_500 = "ach_xp_500"
    const val XP_2000 = "ach_xp_2000"
}

object AchievementCatalog {
    /** XP per full minute of focused time (partial sessions count for elapsed minutes). */
    const val XP_PER_FOCUS_MINUTE = 2

    /** Extra XP when the timer reaches zero (full block). */
    const val XP_BONUS_FULL_SESSION = 10

    private val definitions: List<Achievement> = listOf(
        Achievement(
            id = AchievementIds.FIRST_FOCUS,
            title = "Prima concentrare",
            xpReward = 25,
            lottieAnimationUrl = "",
            isUnlocked = false
        ),
        Achievement(
            id = AchievementIds.TEN_SESSIONS,
            title = "10 sesiuni",
            xpReward = 100,
            lottieAnimationUrl = "",
            isUnlocked = false
        ),
        Achievement(
            id = AchievementIds.STREAK_3,
            title = "Flacără 3 zile",
            xpReward = 50,
            lottieAnimationUrl = "",
            isUnlocked = false
        ),
        Achievement(
            id = AchievementIds.STREAK_7,
            title = "Flacără 7 zile",
            xpReward = 150,
            lottieAnimationUrl = "",
            isUnlocked = false
        ),
        Achievement(
            id = AchievementIds.XP_500,
            title = "500 XP",
            xpReward = 30,
            lottieAnimationUrl = "",
            isUnlocked = false
        ),
        Achievement(
            id = AchievementIds.XP_2000,
            title = "2000 XP",
            xpReward = 200,
            lottieAnimationUrl = "",
            isUnlocked = false
        )
    )

    fun defaultAchievements(): List<Achievement> = definitions

    fun xpRewardFor(id: String): Int =
        definitions.firstOrNull { it.id == id }?.xpReward ?: 0

    fun levelFromTotalXp(totalXp: Int): Int = (totalXp / 500) + 1
}
