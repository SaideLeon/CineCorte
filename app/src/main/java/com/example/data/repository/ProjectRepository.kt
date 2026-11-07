package com.example.data.repository

import com.example.data.local.ProjectDao
import com.example.data.local.ProjectEntity
import com.example.model.Project
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

/**
 * Repositório que abstrai a persistência Room e a conversão do modelo de Projeto.
 */
class ProjectRepository(private val projectDao: ProjectDao) {

    val allProjects: Flow<List<ProjectEntity>> = projectDao.getAllProjects()

    suspend fun loadProject(id: String): Project? = withContext(Dispatchers.IO) {
        val entity = projectDao.getProjectById(id) ?: return@withContext null
        try {
            ProjectJsonAdapter.fromJson(entity.projectJson)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun saveProject(project: Project) = withContext(Dispatchers.IO) {
        val json = ProjectJsonAdapter.toJson(project)
        val entity = ProjectEntity(
            id = project.id,
            title = project.title,
            createdAt = project.createdAt,
            lastModified = System.currentTimeMillis(),
            projectJson = json,
            previewWidth = project.canvasWidth,
            previewHeight = project.canvasHeight,
            durationMs = project.totalDurationMs,
            clipsCount = project.videoClips.size
        )
        projectDao.insertOrUpdate(entity)
    }

    suspend fun deleteProject(id: String) = withContext(Dispatchers.IO) {
        projectDao.deleteProjectById(id)
    }
}
