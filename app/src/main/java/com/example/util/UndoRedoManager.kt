package com.example.util

import com.example.model.Project
import java.util.ArrayDeque

/**
 * Gestor de histórico ilimitado de Desfazer (Undo) e Refazer (Redo).
 */
class UndoRedoManager(private val maxHistory: Int = 100) {

    private val undoStack = ArrayDeque<Project>()
    private val redoStack = ArrayDeque<Project>()

    val canUndo: Boolean
        get() = undoStack.isNotEmpty()

    val canRedo: Boolean
        get() = redoStack.isNotEmpty()

    fun pushState(currentState: Project) {
        if (undoStack.size >= maxHistory) {
            undoStack.removeLast()
        }
        undoStack.push(currentState)
        redoStack.clear()
    }

    fun undo(currentState: Project): Project? {
        if (!canUndo) return null
        redoStack.push(currentState)
        return undoStack.pop()
    }

    fun redo(currentState: Project): Project? {
        if (!canRedo) return null
        undoStack.push(currentState)
        return redoStack.pop()
    }

    fun clear() {
        undoStack.clear()
        redoStack.clear()
    }
}
