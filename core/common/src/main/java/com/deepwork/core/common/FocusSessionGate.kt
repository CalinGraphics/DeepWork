package com.deepwork.core.common

import java.util.concurrent.atomic.AtomicBoolean

/**
 * Stare sincronă: sesiunea de focus rulează sau e pe pauză (timer activ).
 * Folosit de serviciul de accesibilitate pentru a ști când să blocheze aplicațiile selectate.
 */
object FocusSessionGate {
    private val active = AtomicBoolean(false)

    fun setFocusSessionActive(value: Boolean) {
        active.set(value)
    }

    fun isFocusSessionActive(): Boolean = active.get()
}
