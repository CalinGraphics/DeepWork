package com.deepwork.data.local.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

data class UserPreferences(
    val onboardingCompleted: Boolean,
    val isPremium: Boolean,
    val sessionDuration: Int,
    val totalXp: Int
)

class UserPreferencesRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    private object PreferencesKeys {
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val IS_PREMIUM = booleanPreferencesKey("is_premium")
        val SESSION_DURATION = intPreferencesKey("session_duration")
        val TOTAL_XP = intPreferencesKey("total_xp")
        val ACTIVE_TASK_ID = stringPreferencesKey("active_task_id")
    }

    val userPreferencesFlow: Flow<UserPreferences> = dataStore.data.map { preferences ->
        UserPreferences(
            onboardingCompleted = preferences[PreferencesKeys.ONBOARDING_COMPLETED] ?: false,
            isPremium = preferences[PreferencesKeys.IS_PREMIUM] ?: false,
            sessionDuration = preferences[PreferencesKeys.SESSION_DURATION] ?: 25,
            totalXp = preferences[PreferencesKeys.TOTAL_XP] ?: 0
        )
    }

    val totalXpFlow: Flow<Int> = dataStore.data.map { preferences ->
        preferences[PreferencesKeys.TOTAL_XP] ?: 0
    }

    /** Task id chosen for the timer; when unset, timer falls back to first incomplete task. */
    val activeTaskIdFlow: Flow<String?> = dataStore.data.map { preferences ->
        preferences[PreferencesKeys.ACTIVE_TASK_ID]?.takeIf { it.isNotBlank() }
    }

    suspend fun setActiveTaskId(id: String?) {
        dataStore.edit { prefs ->
            if (id.isNullOrBlank()) {
                prefs.remove(PreferencesKeys.ACTIVE_TASK_ID)
            } else {
                prefs[PreferencesKeys.ACTIVE_TASK_ID] = id
            }
        }
    }

    suspend fun clearActiveTaskIfMatches(taskId: String) {
        dataStore.edit { prefs ->
            if (prefs[PreferencesKeys.ACTIVE_TASK_ID] == taskId) {
                prefs.remove(PreferencesKeys.ACTIVE_TASK_ID)
            }
        }
    }

    suspend fun updateOnboardingCompleted(completed: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.ONBOARDING_COMPLETED] = completed
        }
    }

    suspend fun updateSessionDuration(duration: Int) {
        val v = duration.coerceIn(5, 120)
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.SESSION_DURATION] = v
        }
    }

    suspend fun addTotalXp(delta: Int) {
        if (delta == 0) return
        dataStore.edit { preferences ->
            val cur = preferences[PreferencesKeys.TOTAL_XP] ?: 0
            preferences[PreferencesKeys.TOTAL_XP] = (cur + delta).coerceAtLeast(0)
        }
    }
}
