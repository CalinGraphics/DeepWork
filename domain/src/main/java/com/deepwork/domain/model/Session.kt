package com.deepwork.domain.model

import java.util.UUID

data class Session(
    val id: String = UUID.randomUUID().toString(),
    val durationMinutes: Int,
    /** 0 = session în curs (draft). */
    val completedAt: Long = 0L,
    val xpEarned: Int = 0,
    val focusScore: Int = 0,
    val isSynced: Boolean = false
)
