package com.deepwork.domain.model

enum class GestureType {
    ROTATE_LEFT_45,
    ROTATE_RIGHT_45,
    FACE_DOWN,
    /** Telefon ridicat după ce a fost cu fața în jos — reluare din pauză. */
    FACE_UP,
    SHAKE,
    TILT_UP_30,
    TILT_DOWN_30,
    DOUBLE_TAP_BACK,
    ROTATE_360_Z
}
