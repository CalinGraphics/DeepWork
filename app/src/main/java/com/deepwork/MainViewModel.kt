package com.deepwork

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.deepwork.core.common.FocusSessionGate
import com.deepwork.data.local.preferences.UserPreferences
import com.deepwork.data.local.preferences.UserPreferencesRepository
import com.deepwork.domain.repository.SessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository,
    sessionRepository: SessionRepository
) : ViewModel() {

    val preferences: StateFlow<UserPreferences> =
        userPreferencesRepository.userPreferencesFlow.stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = UserPreferences(
                onboardingCompleted = false,
                isPremium = false,
                sessionDuration = 25,
                totalXp = 0
            )
        )

    val strictFocusActive: StateFlow<Boolean> = sessionRepository.getActiveSession()
        .map { it != null }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = false
        )

    init {
        viewModelScope.launch {
            strictFocusActive.collect { active ->
                // Sursă globală stabilă pentru Accessibility block când sesiunea e activă.
                FocusSessionGate.setFocusSessionActive(active)
            }
        }
    }

    fun completeOnboarding() {
        viewModelScope.launch {
            userPreferencesRepository.updateOnboardingCompleted(true)
        }
    }
}
