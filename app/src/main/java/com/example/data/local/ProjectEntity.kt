package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entidade Room para persistência local de projetos de edição.
 */
@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey val id: String,
    val title: String,
    val createdAt: Long,
    val lastModified: Long,
    val projectJson: String,
    val previewWidth: Int,
    val previewHeight: Int,
    val durationMs: Long,
    val clipsCount: Int
)
