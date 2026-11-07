package com.example.model

/**
 * Projeto completo com todos os elementos da linha do tempo.
 */
data class Project(
    val id: String = java.util.UUID.randomUUID().toString(),
    val title: String = "Novo Projeto",
    val createdAt: Long = System.currentTimeMillis(),
    val lastModified: Long = System.currentTimeMillis(),
    val videoClips: List<VideoClip> = emptyList(),
    val audioTracks: List<AudioTrack> = listOf(
        AudioTrack(type = AudioTrackType.VOICE, name = AudioTrackType.VOICE.labelPt),
        AudioTrack(type = AudioTrackType.MUSIC, name = AudioTrackType.MUSIC.labelPt),
        AudioTrack(type = AudioTrackType.SFX, name = AudioTrackType.SFX.labelPt)
    ),
    val rippleDelete: Boolean = true, // Fecha o espaço automaticamente ao remover trechos
    val masterVolume: Float = 1.0f
) {
    // A resolução da área de edição é definida pelo primeiro vídeo importado (100% original)
    val canvasWidth: Int
        get() = videoClips.firstOrNull()?.metadata?.displayWidth ?: 1920

    val canvasHeight: Int
        get() = videoClips.firstOrNull()?.metadata?.displayHeight ?: 1080

    val canvasFps: Float
        get() = videoClips.firstOrNull()?.metadata?.fps ?: 30f

    val canvasAspectRatio: Float
        get() = if (canvasHeight > 0) canvasWidth.toFloat() / canvasHeight.toFloat() else 16f / 9f

    val canvasAspectRatioLabel: String
        get() = videoClips.firstOrNull()?.metadata?.aspectRatioLabel ?: "16:9"

    // Duração total do projeto (o máximo entre o fim dos vídeos e o fim dos áudios)
    val totalDurationMs: Long
        get() {
            val videoDuration = videoClips.sumOf { it.trimmedDurationMs }
            val audioMaxEnd = audioTracks.flatMap { it.clips }.maxOfOrNull { it.timelineEndMs } ?: 0L
            return maxOf(videoDuration, audioMaxEnd)
        }

    // Calcula em qual clipe de vídeo e em que offset interno o playhead se encontra
    fun findVideoClipAt(timelineMs: Long): Pair<VideoClip, Long>? {
        var accumulated = 0L
        for (clip in videoClips) {
            val clipDuration = clip.trimmedDurationMs
            if (timelineMs in accumulated until (accumulated + clipDuration)) {
                val clipInternalOffset = ((timelineMs - accumulated) * clip.speed).toLong() + clip.startTrimMs
                return Pair(clip, clipInternalOffset)
            }
            accumulated += clipDuration
        }
        return null
    }

    // Retorna o tempo acumulado de início de um clipe de vídeo na timeline
    fun getClipStartOnTimeline(clipId: String): Long {
        var acc = 0L
        for (clip in videoClips) {
            if (clip.id == clipId) return acc
            acc += clip.trimmedDurationMs
        }
        return acc
    }
}

/**
 * Predefinição de exportação para manter ou adaptar resolução.
 */
enum class ResolutionPreset(val labelPt: String, val width: Int?, val height: Int?) {
    ORIGINAL("Original (100% Dimensões do Vídeo)", null, null),
    UHD_4K("4K Ultra HD (3840x2160)", 3840, 2160),
    FHD_1080P("Full HD 1080p (1920x1080)", 1920, 1080),
    HD_720P("HD 720p (1280x720)", 1280, 720),
    VERTICAL_9_16("Vertical Reels/Shorts (1080x1920)", 1080, 1920),
    SQUARE_1_1("Quadrado 1:1 (1080x1080)", 1080, 1080)
}

/**
 * Formato do ficheiro final de exportação.
 */
enum class ExportFormat(val extension: String, val mimeType: String, val label: String) {
    MP4("mp4", "video/mp4", "MP4 (H.264 / AAC)"),
    MP4_HEVC("mp4", "video/mp4", "MP4 HEVC (H.265 / AAC)"),
    AUDIO_AAC("m4a", "audio/mp4", "Apenas Áudio (AAC / M4A)"),
    AUDIO_WAV("wav", "audio/wav", "Apenas Áudio (WAV Sem Perdas)")
}

/**
 * Configuração detalhada para exportar o projeto.
 */
data class ExportConfig(
    val format: ExportFormat = ExportFormat.MP4,
    val preset: ResolutionPreset = ResolutionPreset.ORIGINAL,
    val targetFps: Int = 30,
    val customBitrateMbps: Int = 25,
    val preserveExactOriginal: Boolean = true
)
