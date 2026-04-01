package com.deepwork.feature.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.deepwork.data.local.preferences.UserPreferencesRepository
import com.deepwork.domain.model.Task
import com.deepwork.domain.repository.TaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TaskViewModel @Inject constructor(
    private val taskRepository: TaskRepository,
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    val tasks: StateFlow<List<Task>> = taskRepository.getTasks()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val activeTaskIdForTimer: StateFlow<String?> =
        userPreferencesRepository.activeTaskIdFlow.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    fun addTask(title: String, category: String, estimatedMinutes: Int) {
        viewModelScope.launch {
            val task = Task(title = title, category = category, estimatedMinutes = estimatedMinutes)
            taskRepository.insertTask(task)
        }
    }

    fun toggleTaskCompletion(task: Task) {
        viewModelScope.launch {
            val updated = task.copy(isCompleted = !task.isCompleted)
            if (updated.isCompleted) {
                userPreferencesRepository.clearActiveTaskIfMatches(task.id)
            }
            taskRepository.updateTask(updated)
        }
    }

    fun deleteTask(taskId: String) {
        viewModelScope.launch {
            userPreferencesRepository.clearActiveTaskIfMatches(taskId)
            taskRepository.deleteTask(taskId)
        }
    }

    /** Task shown on the timer; must not be completed. */
    fun setActiveTaskForTimer(taskId: String) {
        viewModelScope.launch {
            userPreferencesRepository.setActiveTaskId(taskId)
        }
    }

    fun clearActiveTaskForTimer() {
        viewModelScope.launch {
            userPreferencesRepository.setActiveTaskId(null)
        }
    }
}
