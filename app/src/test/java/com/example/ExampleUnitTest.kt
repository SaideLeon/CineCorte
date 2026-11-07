package com.example

import com.example.model.*
import com.example.util.TimeFormatter
import com.example.util.UndoRedoManager
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun testVideoMetadataRotationAndAspectRatio() {
        // Vídeo 1920x1080 horizontal sem rotação
        val metaLandscape = VideoMetadata(
            uri = "content://test",
            rawWidth = 1920,
            rawHeight = 1080,
            rotation = 0
        )
        assertEquals(1920, metaLandscape.displayWidth)
        assertEquals(1080, metaLandscape.displayHeight)
        assertEquals("16:9", metaLandscape.aspectRatioLabel)

        // Vídeo gravado na vertical com rotação de 90° (1080x1920 display)
        val metaPortrait = VideoMetadata(
            uri = "content://test_portrait",
            rawWidth = 1920,
            rawHeight = 1080,
            rotation = 90
        )
        assertEquals(1080, metaPortrait.displayWidth)
        assertEquals(1920, metaPortrait.displayHeight)
        assertEquals("9:16", metaPortrait.aspectRatioLabel)
    }

    @Test
    fun testTimecodeFormatting() {
        val tcZero = TimeFormatter.formatTimecode(0L, 30f)
        assertEquals("00:00:00:00", tcZero)

        val tcOneSecond = TimeFormatter.formatTimecode(1000L, 30f)
        assertEquals("00:00:01:00", tcOneSecond)

        val tcMinute = TimeFormatter.formatTimecode(65500L, 30f)
        assertEquals("00:01:05:15", tcMinute)
    }

    @Test
    fun testUndoRedoManager() {
        val manager = UndoRedoManager()
        val p1 = Project(id = "1", title = "Projeto 1")
        val p2 = Project(id = "2", title = "Projeto 2")

        manager.pushState(p1)
        assertTrue(manager.canUndo)
        assertFalse(manager.canRedo)

        val undone = manager.undo(p2)
        assertEquals("1", undone?.id)
        assertTrue(manager.canRedo)

        val redone = manager.redo(p1)
        assertEquals("2", redone?.id)
    }

    @Test
    fun testKeyframeInterpolation() {
        val kf1 = Keyframe(timeOffsetMs = 0L, scale = 1.0f)
        val kf2 = Keyframe(timeOffsetMs = 1000L, scale = 2.0f)
        val clip = VideoClip(
            sourceUri = "test",
            name = "Clip",
            metadata = VideoMetadata(uri = "test", durationMs = 5000L),
            keyframes = listOf(kf1, kf2)
        )

        // No tempo 0, escala = 1.0
        val t0 = clip.interpolatedTransform(0L).first
        assertEquals(1.0f, t0, 0.01f)

        // No tempo 500ms, metade da interpolação suave
        val tMid = clip.interpolatedTransform(500L).first
        assertEquals(1.5f, tMid, 0.05f)

        // No tempo 1000ms, escala = 2.0
        val tEnd = clip.interpolatedTransform(1000L).first
        assertEquals(2.0f, tEnd, 0.01f)
    }

    @Test
    fun testProjectDurationCalculation() {
        val clip1 = VideoClip(
            id = "c1",
            sourceUri = "uri1",
            name = "Clip 1",
            metadata = VideoMetadata(uri = "uri1", durationMs = 5000L),
            startTrimMs = 0L,
            endTrimMs = 4000L,
            speed = 1.0f
        )
        val clip2 = VideoClip(
            id = "c2",
            sourceUri = "uri2",
            name = "Clip 2",
            metadata = VideoMetadata(uri = "uri2", durationMs = 6000L),
            startTrimMs = 1000L,
            endTrimMs = 5000L,
            speed = 2.0f // 4000 / 2 = 2000ms
        )

        val project = Project(
            videoClips = listOf(clip1, clip2)
        )

        // 4000ms + 2000ms = 6000ms
        assertEquals(6000L, project.totalDurationMs)
    }

    @Test
    fun testSilenceDetection() {
        val config = SilenceDetectionConfig(
            thresholdDb = -35f,
            minDurationMs = 300L,
            safetyPaddingMs = 50L
        )

        val silences = com.example.util.MediaMetadataHelper.detectSilenceIntervals(
            clipId = "test_clip",
            clipName = "Narração",
            clipStartMs = 0L,
            clipEndMs = 30000L, // 30s de narração
            clipTimelineStartMs = 0L,
            speed = 1.0f,
            config = config
        )

        assertTrue("Deveria detetar pausas na narração de 30s", silences.isNotEmpty())
        for (silence in silences) {
            assertTrue("A pausa deve ser pelo menos a duração mínima", silence.durationMs >= 150L)
            assertTrue("O início deve ser antes do fim", silence.startOffsetMs < silence.endOffsetMs)
        }
    }

    @Test
    fun testSilenceRemovalSegmentation() {
        // Clipe de 10s: fala de 0 a 3s, silêncio de 3s a 6s, fala de 6s a 10s
        val clip = VideoClip(
            id = "c1",
            sourceUri = "uri1",
            name = "Clip 1",
            metadata = VideoMetadata(uri = "uri1", durationMs = 10000L),
            startTrimMs = 0L,
            endTrimMs = 10000L
        )

        val silence = SilenceInterval(
            clipId = "c1",
            clipName = "Clip 1",
            startOffsetMs = 3000L,
            endOffsetMs = 6000L,
            timelineStartMs = 3000L,
            timelineEndMs = 6000L,
            isSelectedForRemoval = true
        )

        // Segmentação dos trechos de fala
        val speechPieces = mutableListOf<VideoClip>()
        var currentStart = clip.startTrimMs
        if (silence.startOffsetMs > currentStart + 120L) {
            speechPieces.add(clip.copy(id = "p1", startTrimMs = currentStart, endTrimMs = silence.startOffsetMs))
        }
        currentStart = maxOf(currentStart, silence.endOffsetMs)
        if (currentStart < clip.endTrimMs - 120L) {
            speechPieces.add(clip.copy(id = "p2", startTrimMs = currentStart, endTrimMs = clip.endTrimMs))
        }

        assertEquals(2, speechPieces.size)
        assertEquals(0L, speechPieces[0].startTrimMs)
        assertEquals(3000L, speechPieces[0].endTrimMs) // 3s
        assertEquals(6000L, speechPieces[1].startTrimMs)
        assertEquals(10000L, speechPieces[1].endTrimMs) // 4s

        // O total agora é 3s + 4s = 7s (3s de silêncio eliminados!)
        val totalMs = speechPieces.sumOf { it.trimmedDurationMs }
        assertEquals(7000L, totalMs)
    }

    @Test
    fun testRepetitionDetectorNormalization() {
        assertEquals("palavra", com.example.util.RepetitionDetector.normalize("Palavra!"))
        assertEquals("silencios", com.example.util.RepetitionDetector.normalize("silêncios,"))
        assertEquals("gague-se", com.example.util.RepetitionDetector.normalize("gague-se..."))
    }

    @Test
    fun testPhraseRepetitionDetectionFromUserPrompt() {
        // Exemplo direto do prompt do utilizador: "e que prendesse que prendesse o ouvinte"
        val words = listOf(
            WordTimestamp(word = "e", start = 1.0, end = 1.2),
            WordTimestamp(word = "que", start = 1.2, end = 1.5),
            WordTimestamp(word = "prendesse", start = 1.5, end = 2.0),
            WordTimestamp(word = "que", start = 2.05, end = 2.3),
            WordTimestamp(word = "prendesse", start = 2.3, end = 2.8),
            WordTimestamp(word = "o", start = 2.85, end = 3.0),
            WordTimestamp(word = "ouvinte", start = 3.0, end = 3.5)
        )

        val config = RepetitionDetectionConfig(
            ignoreShortWords = true,
            sensitivity = RepetitionSensitivity.MEDIUM
        )

        val suggestions = com.example.util.RepetitionDetector.findRepetitions(words, config)

        assertEquals("Deveria detetar exatamente 1 repetição de frase", 1, suggestions.size)
        val cut = suggestions[0]
        assertEquals("que prendesse", cut.repeatedText)
        assertEquals(1200L, cut.startMs)
        assertEquals(2000L, cut.endMs)
        assertTrue("A duração deve ser positiva", cut.durationMs > 0)
    }

    @Test
    fun testSingleWordRepetitionAndStutter() {
        val words = listOf(
            WordTimestamp(word = "eu", start = 0.5, end = 0.8),
            WordTimestamp(word = "acho", start = 0.85, end = 1.2),
            WordTimestamp(word = "e", start = 1.25, end = 1.4),
            WordTimestamp(word = "removesse", start = 1.5, end = 1.9),
            WordTimestamp(word = "removesse", start = 1.95, end = 2.4),
            WordTimestamp(word = "este", start = 2.45, end = 2.8)
        )

        val config = RepetitionDetectionConfig(
            ignoreShortWords = true,
            sensitivity = RepetitionSensitivity.MEDIUM
        )

        val suggestions = com.example.util.RepetitionDetector.findRepetitions(words, config)

        assertEquals(1, suggestions.size)
        assertEquals("removesse", suggestions[0].repeatedText)
        assertEquals(1500L, suggestions[0].startMs)
        assertEquals(1900L, suggestions[0].endMs)
    }
}

