package com.deepwork.domain.model

data class Achievement(
    val id: String,
    val title: String,
    val xpReward: Int,
    val lottieAnimationUrl: String,
    val isUnlocked: Boolean = false,
    val unlockedAt: Long? = null
)
