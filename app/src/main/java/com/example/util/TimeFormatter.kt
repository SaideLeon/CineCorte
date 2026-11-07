package com.example.util

import java.util.Locale

object TimeFormatter {

    /**
     * Formata milissegundos no padrão Timecode profissional: 00:00:00:00 (hh:mm:ss:ff)
     */
    fun formatTimecode(ms: Long, fps: Float = 30f): String {
        val safeMs = ms.coerceAtLeast(0L)
        val totalSeconds = safeMs / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        val remainderMs = safeMs % 1000
        val frame = ((remainderMs / 1000f) * fps).toInt().coerceIn(0, (fps.toInt() - 1).coerceAtLeast(0))

        return String.format(Locale.US, "%02d:%02d:%02d:%02d", hours, minutes, seconds, frame)
    }

    /**
     * Formato simplificado mm:ss para pequenas etiquetas.
     */
    fun formatSimple(ms: Long): String {
        val safeMs = ms.coerceAtLeast(0L)
        val totalSeconds = safeMs / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format(Locale.US, "%02d:%02d", minutes, seconds)
    }
}
