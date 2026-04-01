package com.deepwork.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.deepwork.data.local.preferences.UserPreferences
import com.deepwork.data.local.preferences.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferencesRepository: UserPreferencesRepository
) : ViewModel() {

    val preferences: StateFlow<UserPreferences> = preferencesRepository.userPreferencesFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = UserPreferences(false, false, 25, 0)
        )

    fun updateSessionDuration(durationMinutes: Int) {
        viewModelScope.launch {
            preferencesRepository.updateSessionDuration(durationMinutes)
        }
    }

    /** Reseteaza onboarding-ul pentru retestare. */
    fun replayOnboarding() {
        viewModelScope.launch {
            preferencesRepository.updateOnboardingCompleted(false)
        }
    }
}
