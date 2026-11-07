package com.example.model

import android.net.Uri

/**
 * Metadados detalhados do vídeo importado, extraídos via MediaMetadataRetriever.
 * Garante preservação de 100% das dimensões originais (sem esticar, achatar ou cortar).
 */
data class VideoMetadata(
    val uri: String,
    val rawWidth: Int = 1920,
    val rawHeight: Int = 1080,
    val rotation: Int = 0,
    val durationMs: Long = 0L,
    val fps: Float = 30f,
    val bitrate: Long = 0L,
    val mimeType: String = "video/mp4",
    val title: String = "Vídeo"
) {
    // Largura corrigida pela orientação de rotação (90° ou 270° invertem largura e altura)
    val displayWidth: Int
        get() = if (rotation == 90 || rotation == 270) rawHeight else rawWidth

    // Altura corrigida pela orientação de rotação
    val displayHeight: Int
        get() = if (rotation == 90 || rotation == 270) rawWidth else rawHeight

    // Aspect ratio real
    val aspectRatio: Float
        get() = if (displayHeight > 0) displayWidth.toFloat() / displayHeight.toFloat() else 16f / 9f

    // Descrição legível da proporção (ex: 16:9, 9:16, 1:1, 4:5, 21:9)
    val aspectRatioLabel: String
        get() {
            val ratio = aspectRatio
            return when {
                ratio in 1.70f..1.85f -> "16:9"
                ratio in 0.54f..0.60f -> "9:16"
                ratio in 0.95f..1.05f -> "1:1"
                ratio in 0.75f..0.85f -> "4:5"
                ratio in 2.30f..2.45f -> "21:9"
                ratio in 1.30f..1.38f -> "4:3"
                else -> "${displayWidth}x${displayHeight}"
            }
        }
}

/**
 * Ponto-chave (Keyframe) para animação de escala, posição e rotação.
 */
data class Keyframe(
    val id: String = java.util.UUID.randomUUID().toString(),
    val timeOffsetMs: Long,
    val scale: Float = 1.0f,
    val offsetX: Float = 0.0f, // Deslocamento relativo (-1.0f a +1.0f)
    val offsetY: Float = 0.0f,
    val rotationDegrees: Float = 0.0f
)

/**
 * Tipo de transição entre clipes de vídeo.
 */
enum class TransitionType(val labelPt: String) {
    NONE("Nenhuma"),
    FADE_BLACK("Esmaecer (Preto)"),
    DISSOLVE("Dissolver"),
    SLIDE_LEFT("Deslizar Esquerda"),
    SLIDE_RIGHT("Deslizar Direita"),
    SLIDE_UP("Deslizar Cima"),
    SLIDE_DOWN("Deslizar Baixo"),
    ZOOM_IN("Zoom In"),
    ZOOM_OUT("Zoom Out"),
    WIPE("Varrimento"),
    FLASH_WHITE("Flash Branco")
}

/**
 * Transição configurada entre clipes.
 */
data class ClipTransition(
    val type: TransitionType = TransitionType.NONE,
    val durationMs: Long = 800L,
    val easing: String = "EaseInOut" // Linear, EaseIn, EaseOut, EaseInOut
)

/**
 * Clipe de vídeo individual na faixa principal.
 */
data class VideoClip(
    val id: String = java.util.UUID.randomUUID().toString(),
    val sourceUri: String,
    val name: String,
    val metadata: VideoMetadata,
    val startTrimMs: Long = 0L,
    val endTrimMs: Long = metadata.durationMs,
    val speed: Float = 1.0f, // 0.25x a 4.0x
    val isMuted: Boolean = false,
    val volume: Float = 1.0f, // 0.0f a 2.0f
    val scale: Float = 1.0f,
    val offsetX: Float = 0.0f,
    val offsetY: Float = 0.0f,
    val rotation: Float = 0.0f,
    val keyframes: List<Keyframe> = emptyList(),
    val transitionIn: ClipTransition = ClipTransition(),
    val transitionOut: ClipTransition = ClipTransition()
) {
    // Duração do conteúdo recortado
    val trimmedDurationMs: Long
        get() = ((endTrimMs - startTrimMs).coerceAtLeast(0L) / speed).toLong()

    // Função de interpolação de keyframes para o tempo atual do clipe
    fun interpolatedTransform(clipTimeMs: Long): Triple<Float, Pair<Float, Float>, Float> {
        if (keyframes.isEmpty()) {
            return Triple(scale, Pair(offsetX, offsetY), rotation)
        }
        val sorted = keyframes.sortedBy { it.timeOffsetMs }
        if (clipTimeMs <= sorted.first().timeOffsetMs) {
            val first = sorted.first()
            return Triple(first.scale, Pair(first.offsetX, first.offsetY), first.rotationDegrees)
        }
        if (clipTimeMs >= sorted.last().timeOffsetMs) {
            val last = sorted.last()
            return Triple(last.scale, Pair(last.offsetX, last.offsetY), last.rotationDegrees)
        }
        // Interpola entre os dois keyframes adjacentes
        for (i in 0 until sorted.size - 1) {
            val kf1 = sorted[i]
            val kf2 = sorted[i + 1]
            if (clipTimeMs in kf1.timeOffsetMs..kf2.timeOffsetMs) {
                val span = (kf2.timeOffsetMs - kf1.timeOffsetMs).toFloat().coerceAtLeast(1f)
                val fraction = ((clipTimeMs - kf1.timeOffsetMs) / span).coerceIn(0f, 1f)
                // Interpolação suave cúbica (Smoothstep)
                val smoothFraction = fraction * fraction * (3f - 2f * fraction)
                val interpScale = kf1.scale + (kf2.scale - kf1.scale) * smoothFraction
                val interpX = kf1.offsetX + (kf2.offsetX - kf1.offsetX) * smoothFraction
                val interpY = kf1.offsetY + (kf2.offsetY - kf1.offsetY) * smoothFraction
                val interpRot = kf1.rotationDegrees + (kf2.rotationDegrees - kf1.rotationDegrees) * smoothFraction
                return Triple(interpScale, Pair(interpX, interpY), interpRot)
            }
        }
        return Triple(scale, Pair(offsetX, offsetY), rotation)
    }
}
