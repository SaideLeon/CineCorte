package com.example.model

import androidx.compose.ui.graphics.Color

/**
 * Categorias de faixas de áudio com cores distintas para a linha do tempo.
 */
enum class AudioTrackType(val labelPt: String, val colorHex: Long) {
    VOICE("Voz / Fala", 0xFF00D2FF),         // Ciano elétrico
    MUSIC("Música de Fundo", 0xFF9D4EDD),     // Violeta suave
    AMBIENT("Som Ambiente", 0xFF10B981),      // Verde esmeralda
    SFX("Efeito Sonoro", 0xFFF59E0B);         // Âmbar dourado

    val composeColor: Color
        get() = Color(colorHex)
}

/**
 * Efeitos de áudio avançados: Equalizador de 3 bandas, Normalização e Redução de Ruído.
 */
data class AudioEffects(
    val normalizeVolume: Boolean = false,
    val bassBoostDb: Float = 0f,       // -12dB a +12dB
    val midToneDb: Float = 0f,         // -12dB a +12dB
    val trebleDb: Float = 0f,          // -12dB a +12dB
    val noiseReduction: Boolean = false,
    val pitchShiftSemitones: Float = 0f // -12 a +12 semitons
)

/**
 * Clipe de áudio posicionado numa faixa.
 */
data class AudioClip(
    val id: String = java.util.UUID.randomUUID().toString(),
    val sourceUri: String,
    val name: String,
    val timelineStartMs: Long = 0L,     // Ponto de início na linha do tempo global
    val startTrimMs: Long = 0L,         // Ponto de recorte do ficheiro de áudio
    val endTrimMs: Long = 10000L,       // Ponto final de recorte
    val originalDurationMs: Long = 10000L,
    val speed: Float = 1.0f,
    val volume: Float = 1.0f,           // 0.0f a 2.0f
    val pan: Float = 0.0f,              // -1.0f (esq) a +1.0f (dir)
    val fadeInMs: Long = 0L,
    val fadeOutMs: Long = 0L,
    val effects: AudioEffects = AudioEffects(),
    val waveformPoints: List<Float> = emptyList() // Amostras normalizadas para desenhar forma de onda
) {
    val durationMs: Long
        get() = ((endTrimMs - startTrimMs).coerceAtLeast(0L) / speed).toLong()

    val timelineEndMs: Long
        get() = timelineStartMs + durationMs

    // Calcula o ganho de volume efetivo para um dado timestamp dentro do clipe considerando fade in/out
    fun effectiveVolumeAt(clipOffsetMs: Long): Float {
        var vol = volume
        if (fadeInMs > 0 && clipOffsetMs in 0..fadeInMs) {
            vol *= (clipOffsetMs.toFloat() / fadeInMs.toFloat()).coerceIn(0f, 1f)
        }
        val remaining = durationMs - clipOffsetMs
        if (fadeOutMs > 0 && remaining in 0..fadeOutMs) {
            vol *= (remaining.toFloat() / fadeOutMs.toFloat()).coerceIn(0f, 1f)
        }
        return vol.coerceIn(0f, 2f)
    }
}

/**
 * Faixa de áudio independente com múltiplos clipes.
 */
data class AudioTrack(
    val id: String = java.util.UUID.randomUUID().toString(),
    val type: AudioTrackType = AudioTrackType.MUSIC,
    val name: String = type.labelPt,
    val isMuted: Boolean = false,
    val isSolo: Boolean = false,
    val volume: Float = 1.0f,
    val pan: Float = 0.0f,
    val autoDucking: Boolean = false, // Baixa o volume automaticamente quando a faixa de voz fala
    val clips: List<AudioClip> = emptyList()
)
