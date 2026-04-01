package com.deepwork.data.local.repository

import com.deepwork.data.local.dao.TaskDao
import com.deepwork.data.local.entity.toDomain
import com.deepwork.data.local.entity.toEntity
import com.deepwork.domain.model.Task
import com.deepwork.domain.repository.TaskRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class TaskRepositoryImpl @Inject constructor(
    private val taskDao: TaskDao
) : TaskRepository {

    override fun getTasks(): Flow<List<Task>> =
        taskDao.getAllTasks().map { entities -> entities.map { it.toDomain() } }

    override suspend fun insertTask(task: Task) {
        taskDao.insertTask(task.toEntity())
    }

    override suspend fun updateTask(task: Task) {
        taskDao.updateTask(task.toEntity())
    }

    override suspend fun deleteTask(taskId: String) {
        taskDao.deleteTask(taskId)
    }
}
