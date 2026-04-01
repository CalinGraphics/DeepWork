package com.deepwork.domain.model

sealed interface Action {
    data object StartSession : Action
    data object PauseSession : Action
    /** Doar dacă timerul e în pauză (ex. ridicat de pe masă). */
    data object ResumeSession : Action
    /** Anulează sesiunea și revine la Idle (ex. shake). */
    data object ResetSession : Action
    data object StopSession : Action
    data object IncreaseVolume : Action
    data object DecreaseVolume : Action
    data object ShowStats : Action
    data object HideStats : Action
    data object ChangeFocusProfile : Action
    data object None : Action
}
