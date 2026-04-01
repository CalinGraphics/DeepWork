package com.deepwork.feature.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.deepwork.domain.gamification.AchievementCatalog
import com.deepwork.domain.model.Achievement
import com.deepwork.domain.repository.AchievementRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AchievementsViewModel @Inject constructor(
    private val achievementRepository: AchievementRepository
) : ViewModel() {

    init {
        viewModelScope.launch {
            achievementRepository.ensureAchievementsSeeded()
        }
    }

    val achievements: StateFlow<List<Achievement>> =
        achievementRepository.getAchievements().stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    val totalXp: StateFlow<Int> =
        achievementRepository.getTotalXp().stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = 0
        )

    val level: StateFlow<Int> =
        totalXp.map { AchievementCatalog.levelFromTotalXp(it) }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = 1
        )
}
