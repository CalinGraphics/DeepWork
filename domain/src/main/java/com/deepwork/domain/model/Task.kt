package com.deepwork.domain.model

import java.util.UUID

data class Task(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val category: String,
    val estimatedMinutes: Int,
    val isCompleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
