package com.example.model

/**
 * Intervalo de silêncio ou pausa detetado num clipe ou faixa da timeline.
 */
data class SilenceInterval(
    val id: String = java.util.UUID.randomUUID().toString(),
    val clipId: String,
    val clipName: String,
    val startOffsetMs: Long,     // Início da pausa relativo ao clipe original
    val endOffsetMs: Long,       // Fim da pausa relativo ao clipe original
    val timelineStartMs: Long,   // Início da pausa na linha do tempo global
    val timelineEndMs: Long,     // Fim da pausa na linha do tempo global
    val isSelectedForRemoval: Boolean = true
) {
    val durationMs: Long
        get() = (endOffsetMs - startOffsetMs).coerceAtLeast(0L)
}

/**
 * Parâmetros de calibração para a auto-remoção de pausas.
 */
data class SilenceDetectionConfig(
    val thresholdDb: Float = -35f,     // Limiar em dB (-50dB a -20dB)
    val minDurationMs: Long = 400L,    // Pausas menores que isto são pausas naturais entre palavras (200ms a 1500ms)
    val safetyPaddingMs: Long = 80L,   // Margem de segurança antes e depois da fala (20ms a 200ms)
    val applyToAllClips: Boolean = false
)
