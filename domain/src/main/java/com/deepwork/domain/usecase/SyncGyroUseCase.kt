package com.deepwork.domain.usecase

import com.deepwork.domain.model.Action
import com.deepwork.domain.model.GestureType
import javax.inject.Inject

class SyncGyroUseCase @Inject constructor() {

    // Mapare GestureType->Action
    operator fun invoke(gesture: GestureType, isPremium: Boolean): Action {
        // Free tier is limited to 3 gestures, Premium to 8
        val allowedGestures = if (isPremium) {
            GestureType.values().toList()
        } else {
            listOf(GestureType.FACE_DOWN, GestureType.DOUBLE_TAP_BACK, GestureType.SHAKE)
        }

        if (gesture !in allowedGestures) {
            return Action.None
        }

        return when (gesture) {
            GestureType.ROTATE_LEFT_45 -> Action.DecreaseVolume
            GestureType.ROTATE_RIGHT_45 -> Action.IncreaseVolume
            GestureType.FACE_DOWN -> Action.StartSession
            GestureType.SHAKE -> Action.PauseSession
            GestureType.TILT_UP_30 -> Action.ShowStats
            GestureType.TILT_DOWN_30 -> Action.HideStats
            GestureType.DOUBLE_TAP_BACK -> Action.StartSession
            GestureType.ROTATE_360_Z -> Action.ChangeFocusProfile
        }
    }
}
