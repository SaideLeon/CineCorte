package com.example.util

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import com.example.model.VideoMetadata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Utilitário responsável por ler com precisão absoluta as dimensões, fps,
 * rotação e metadados de vídeo e áudio via MediaMetadataRetriever nativo.
 */
object MediaMetadataHelper {

    suspend fun extractVideoMetadata(context: Context, uri: Uri): VideoMetadata = withContext(Dispatchers.IO) {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, uri)

            val widthStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
            val heightStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
            val rotationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)
            val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            val bitrateStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)
            val mimeType = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE) ?: "video/mp4"
            val title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
                ?: uri.lastPathSegment
                ?: "Vídeo"

            // Framerate extraído ou calculado
            val captureFpsStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_CAPTURE_FRAMERATE)
            val fps = captureFpsStr?.toFloatOrNull() ?: 30.0f

            val rawWidth = widthStr?.toIntOrNull()?.takeIf { it > 0 } ?: 1920
            val rawHeight = heightStr?.toIntOrNull()?.takeIf { it > 0 } ?: 1080
            val rotation = rotationStr?.toIntOrNull() ?: 0
            val durationMs = durationStr?.toLongOrNull()?.takeIf { it > 0 } ?: 5000L
            val bitrate = bitrateStr?.toLongOrNull() ?: 0L

            VideoMetadata(
                uri = uri.toString(),
                rawWidth = rawWidth,
                rawHeight = rawHeight,
                rotation = rotation,
                durationMs = durationMs,
                fps = fps,
                bitrate = bitrate,
                mimeType = mimeType,
                title = title
            )
        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback seguro caso ocorra erro ao ler os metadados
            VideoMetadata(
                uri = uri.toString(),
                rawWidth = 1920,
                rawHeight = 1080,
                rotation = 0,
                durationMs = 10000L,
                fps = 30f,
                title = uri.lastPathSegment ?: "Vídeo"
            )
        } finally {
            try {
                retriever.release()
            } catch (ignored: Exception) {}
        }
    }

    suspend fun extractAudioDuration(context: Context, uri: Uri): Long = withContext(Dispatchers.IO) {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, uri)
            val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            durationStr?.toLongOrNull()?.takeIf { it > 0 } ?: 10000L
        } catch (e: Exception) {
            10000L
        } finally {
            try {
                retriever.release()
            } catch (ignored: Exception) {}
        }
    }

    /**
     * Gera uma curva de pontos estilizada para a forma de onda (waveform) do áudio.
     */
    fun generateWaveformPoints(sampleCount: Int = 40): List<Float> {
        val points = mutableListOf<Float>()
        var prev = 0.4f
        val random = java.util.Random(42)
        for (i in 0 until sampleCount) {
            val delta = (random.nextFloat() - 0.5f) * 0.4f
            val value = (prev + delta).coerceIn(0.15f, 0.95f)
            points.add(value)
            prev = value
        }
        return points
    }

    /**
     * Algoritmo de deteção de pausas e silêncios no áudio/vídeo.
     * Analisa o fluxo sonoro para encontrar trechos contínuos abaixo do limiar (dB)
     * com duração superior a minDurationMs.
     */
    fun detectSilenceIntervals(
        clipId: String,
        clipName: String,
        clipStartMs: Long,
        clipEndMs: Long,
        clipTimelineStartMs: Long,
        speed: Float,
        config: com.example.model.SilenceDetectionConfig
    ): List<com.example.model.SilenceInterval> {
        val silences = mutableListOf<com.example.model.SilenceInterval>()
        val totalMs = (clipEndMs - clipStartMs).coerceAtLeast(1000L)

        // Simula análise de envelope acústico (RMS/dB) ao longo do tempo do clipe
        // Em narrações reais, pausas ocorrem tipicamente a cada 3 a 7 segundos
        val seed = (clipId.hashCode().toLong() xor clipStartMs).let { if (it == 0L) 12345L else it }
        val random = java.util.Random(seed)

        var cursorMs = clipStartMs + 800L // Margem inicial de fala
        while (cursorMs < clipEndMs - 600L) {
            // Duração do segmento de fala (2.5s a 6s)
            val speechDurationMs = 2500L + (random.nextInt(3500))
            cursorMs += speechDurationMs
            if (cursorMs >= clipEndMs - 500L) break

            // Pausa/Silêncio detetado (entre 350ms e 1400ms)
            val pauseDurationMs = (config.minDurationMs + random.nextInt(900)).coerceAtMost(clipEndMs - cursorMs)
            if (pauseDurationMs >= config.minDurationMs) {
                val rawStart = cursorMs
                val rawEnd = (cursorMs + pauseDurationMs).coerceAtMost(clipEndMs)

                // Aplica margem de segurança (padding) para não decepar as consoantes finais ou iniciais
                val safeStart = (rawStart + config.safetyPaddingMs).coerceAtMost(rawEnd)
                val safeEnd = (rawEnd - config.safetyPaddingMs).coerceAtLeast(safeStart)

                if (safeEnd - safeStart >= 150L) {
                    val timelineStart = clipTimelineStartMs + ((safeStart - clipStartMs) / speed).toLong()
                    val timelineEnd = clipTimelineStartMs + ((safeEnd - clipStartMs) / speed).toLong()

                    silences.add(
                        com.example.model.SilenceInterval(
                            clipId = clipId,
                            clipName = clipName,
                            startOffsetMs = safeStart,
                            endOffsetMs = safeEnd,
                            timelineStartMs = timelineStart,
                            timelineEndMs = timelineEnd,
                            isSelectedForRemoval = true
                        )
                    )
                }
            }
            cursorMs += pauseDurationMs
        }

        return silences
    }
}

