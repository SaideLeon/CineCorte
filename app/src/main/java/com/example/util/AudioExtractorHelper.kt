package com.example.util

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.net.Uri
import com.example.model.Project
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer

/**
 * Utilitário para extrair uma faixa de áudio leve da timeline para envio à API do Whisper na Groq.
 * Converte/extrai o áudio dos clipes em um arquivo .m4a/.aac ou .mp3 comprimido no cache.
 */
object AudioExtractorHelper {

    /**
     * Extrai o áudio principal da timeline para um arquivo temporário no cache.
     * Retorna o File gerado pronto para envio.
     */
    suspend fun extractTimelineAudio(context: Context, project: Project): File = withContext(Dispatchers.IO) {
        val cacheDir = context.cacheDir
        val outputFile = File(cacheDir, "groq_transcribe_${System.currentTimeMillis()}.m4a")
        if (outputFile.exists()) {
            outputFile.delete()
        }

        // Tenta encontrar o primeiro clipe de vídeo com áudio ou clipe de áudio da timeline
        val firstVideoWithAudio = project.videoClips.firstOrNull { !it.isMuted }
        val firstAudioClip = project.audioTracks.filter { !it.isMuted }.flatMap { it.clips }.firstOrNull()

        val targetUriString = firstVideoWithAudio?.sourceUri ?: firstAudioClip?.sourceUri
        if (targetUriString.isNullOrBlank()) {
            throw IllegalStateException("Nenhum clipe de vídeo ou áudio encontrado na timeline para transcrição.")
        }

        val targetUri = Uri.parse(targetUriString)

        // 1. Tentar extração nativa direta com MediaExtractor + MediaMuxer
        val success = tryExtractAudioTrack(context, targetUri, outputFile)
        if (success && outputFile.exists() && outputFile.length() > 0) {
            return@withContext outputFile
        }

        // 2. Fallback: Se for cópia direta do stream (ex: já é mp3/m4a/wav ou vídeo direto suportado pela Groq)
        try {
            context.contentResolver.openInputStream(targetUri)?.use { input ->
                FileOutputStream(outputFile).use { output ->
                    input.copyTo(output)
                }
            }
            if (outputFile.exists() && outputFile.length() > 0) {
                return@withContext outputFile
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        throw IllegalStateException("Não foi possível extrair o áudio do arquivo selecionado.")
    }

    private fun tryExtractAudioTrack(context: Context, uri: Uri, outputFile: File): Boolean {
        var extractor: MediaExtractor? = null
        var muxer: MediaMuxer? = null
        try {
            extractor = MediaExtractor()
            extractor.setDataSource(context, uri, null)

            var audioTrackIndex = -1
            var audioFormat: MediaFormat? = null

            for (i in 0 until extractor.trackCount) {
                val format = extractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME) ?: ""
                if (mime.startsWith("audio/")) {
                    audioTrackIndex = i
                    audioFormat = format
                    break
                }
            }

            if (audioTrackIndex == -1 || audioFormat == null) {
                return false
            }

            extractor.selectTrack(audioTrackIndex)

            muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            val muxerTrackIndex = muxer.addTrack(audioFormat)
            muxer.start()

            val maxBufferSize = try {
                audioFormat.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE)
            } catch (e: Exception) {
                128 * 1024
            }.coerceAtLeast(64 * 1024)

            val buffer = ByteBuffer.allocate(maxBufferSize)
            val bufferInfo = MediaCodec.BufferInfo()

            while (true) {
                val sampleSize = extractor.readSampleData(buffer, 0)
                if (sampleSize < 0) {
                    break
                }
                bufferInfo.offset = 0
                bufferInfo.size = sampleSize
                bufferInfo.presentationTimeUs = extractor.sampleTime
                bufferInfo.flags = extractor.sampleFlags

                muxer.writeSampleData(muxerTrackIndex, buffer, bufferInfo)
                extractor.advance()
            }

            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        } finally {
            try {
                muxer?.stop()
                muxer?.release()
            } catch (ignored: Exception) {}
            try {
                extractor?.release()
            } catch (ignored: Exception) {}
        }
    }
}
