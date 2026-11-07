package com.example.ui

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.repository.ProjectRepository
import com.example.model.*
import com.example.service.ExportState
import com.example.service.VideoExportService
import com.example.util.MediaMetadataHelper
import com.example.util.UndoRedoManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

import com.example.service.GroqWhisperService
import com.example.util.AudioExtractorHelper
import com.example.util.RepetitionDetector
import com.example.util.SecurePreferencesHelper

enum class EditorSheet {
    NONE,
    PROPERTIES,
    AUDIO_EFFECTS,
    TRANSITIONS,
    KEYFRAMES,
    EXPORT,
    PROJECTS,
    SILENCE_REMOVAL,
    AUTO_CUT_WIZARD,
    SETTINGS
}

class EditorViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ProjectRepository(AppDatabase.getDatabase(application).projectDao())
    private val undoRedoManager = UndoRedoManager()

    val savedProjects = repository.allProjects
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val exportState = VideoExportService.exportState

    private val _project = MutableStateFlow(Project())
    val project = _project.asStateFlow()

    private val _currentTimeMs = MutableStateFlow(0L)
    val currentTimeMs = _currentTimeMs.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying = _isPlaying.asStateFlow()

    private val _selectedVideoClipId = MutableStateFlow<String?>(null)
    val selectedVideoClipId = _selectedVideoClipId.asStateFlow()

    private val _selectedAudioClipId = MutableStateFlow<String?>(null)
    val selectedAudioClipId = _selectedAudioClipId.asStateFlow()

    private val _selectedTrackId = MutableStateFlow<String?>(null)
    val selectedTrackId = _selectedTrackId.asStateFlow()

    private val _timelineZoom = MutableStateFlow(1.0f) // 0.2f a 5.0f
    val timelineZoom = _timelineZoom.asStateFlow()

    private val _activeSheet = MutableStateFlow(EditorSheet.NONE)
    val activeSheet = _activeSheet.asStateFlow()

    private val _canUndo = MutableStateFlow(false)
    val canUndo = _canUndo.asStateFlow()

    private val _canRedo = MutableStateFlow(false)
    val canRedo = _canRedo.asStateFlow()

    private val _snackMessage = MutableStateFlow<String?>(null)
    val snackMessage = _snackMessage.asStateFlow()

    private val _detectedSilences = MutableStateFlow<List<SilenceInterval>>(emptyList())
    val detectedSilences = _detectedSilences.asStateFlow()

    private val _silenceConfig = MutableStateFlow(SilenceDetectionConfig())
    val silenceConfig = _silenceConfig.asStateFlow()

    private val _isDetectingSilences = MutableStateFlow(false)
    val isDetectingSilences = _isDetectingSilences.asStateFlow()

    private val securePrefs = SecurePreferencesHelper(application)

    private val _groqApiKey = MutableStateFlow(securePrefs.getGroqApiKey())
    val groqApiKey = _groqApiKey.asStateFlow()

    private val _hasAcceptedGroqConsent = MutableStateFlow(securePrefs.hasAcceptedGroqConsent())
    val hasAcceptedGroqConsent = _hasAcceptedGroqConsent.asStateFlow()

    private val _autoCutStep = MutableStateFlow(AutoCutStep.STEP_A_SILENCE)
    val autoCutStep = _autoCutStep.asStateFlow()

    private val _repetitionConfig = MutableStateFlow(RepetitionDetectionConfig())
    val repetitionConfig = _repetitionConfig.asStateFlow()

    private val _repetitionSuggestions = MutableStateFlow<List<RepetitionCutSuggestion>>(emptyList())
    val repetitionSuggestions = _repetitionSuggestions.asStateFlow()

    private val _isTranscribing = MutableStateFlow(false)
    val isTranscribing = _isTranscribing.asStateFlow()

    private val _transcriptionProgressText = MutableStateFlow("")
    val transcriptionProgressText = _transcriptionProgressText.asStateFlow()

    private var cachedTranscriptionWords: List<WordTimestamp>? = null

    private var playbackJob: Job? = null

    init {
        // Inicializa projeto de exemplo vazio ou salvo
        viewModelScope.launch {
            repository.allProjects.collect { list ->
                if (list.isNotEmpty() && _project.value.videoClips.isEmpty()) {
                    val first = repository.loadProject(list.first().id)
                    if (first != null) {
                        _project.value = first
                        _selectedVideoClipId.value = first.videoClips.firstOrNull()?.id
                    }
                }
            }
        }
    }

    private fun pushHistory() {
        undoRedoManager.pushState(_project.value)
        _canUndo.value = undoRedoManager.canUndo
        _canRedo.value = undoRedoManager.canRedo
    }

    fun undo() {
        val prev = undoRedoManager.undo(_project.value)
        if (prev != null) {
            _project.value = prev
            _canUndo.value = undoRedoManager.canUndo
            _canRedo.value = undoRedoManager.canRedo
            autoSave()
        }
    }

    fun redo() {
        val next = undoRedoManager.redo(_project.value)
        if (next != null) {
            _project.value = next
            _canUndo.value = undoRedoManager.canUndo
            _canRedo.value = undoRedoManager.canRedo
            autoSave()
        }
    }

    fun openSheet(sheet: EditorSheet) {
        _activeSheet.value = sheet
    }

    fun closeSheet() {
        _activeSheet.value = sheetToClose()
    }

    private fun sheetToClose(): EditorSheet = EditorSheet.NONE

    fun clearSnack() {
        _snackMessage.value = null
    }

    fun setZoom(newZoom: Float) {
        _timelineZoom.value = newZoom.coerceIn(0.2f, 8.0f)
    }

    fun seekTo(timeMs: Long) {
        val clamped = timeMs.coerceIn(0L, _project.value.totalDurationMs.coerceAtLeast(1000L))
        _currentTimeMs.value = clamped
    }

    fun stepFrame(forward: Boolean) {
        val fps = _project.value.canvasFps.coerceAtLeast(1f)
        val frameMs = (1000f / fps).toLong().coerceAtLeast(16L)
        val delta = if (forward) frameMs else -frameMs
        seekTo(_currentTimeMs.value + delta)
    }

    fun togglePlayPause() {
        if (_isPlaying.value) {
            pause()
        } else {
            play()
        }
    }

    private fun play() {
        _isPlaying.value = true
        playbackJob?.cancel()
        playbackJob = viewModelScope.launch {
            val total = _project.value.totalDurationMs
            if (_currentTimeMs.value >= total && total > 0) {
                _currentTimeMs.value = 0L
            }
            while (_isPlaying.value) {
                val current = _currentTimeMs.value
                val max = _project.value.totalDurationMs
                if (max > 0 && current >= max) {
                    _isPlaying.value = false
                    break
                }
                delay(33) // ~30 fps tick
                _currentTimeMs.value = (current + 33).coerceAtMost(max.coerceAtLeast(1000L))
            }
        }
    }

    fun pause() {
        _isPlaying.value = false
        playbackJob?.cancel()
    }

    fun selectVideoClip(id: String?) {
        _selectedVideoClipId.value = id
        if (id != null) {
            _selectedAudioClipId.value = null
        }
    }

    fun selectAudioClip(id: String?, trackId: String?) {
        _selectedAudioClipId.value = id
        _selectedTrackId.value = trackId
        if (id != null) {
            _selectedVideoClipId.value = null
        }
    }

    fun importVideo(context: Context, uri: Uri) {
        viewModelScope.launch {
            pushHistory()
            val metadata = MediaMetadataHelper.extractVideoMetadata(context, uri)
            val newClip = VideoClip(
                sourceUri = uri.toString(),
                name = metadata.title,
                metadata = metadata,
                startTrimMs = 0L,
                endTrimMs = metadata.durationMs
            )
            val updatedClips = _project.value.videoClips + newClip
            _project.value = _project.value.copy(
                videoClips = updatedClips,
                lastModified = System.currentTimeMillis()
            )
            _selectedVideoClipId.value = newClip.id
            _snackMessage.value = "Vídeo importado: ${metadata.displayWidth}x${metadata.displayHeight} (${metadata.aspectRatioLabel})"
            autoSave()
        }
    }

    fun importAudio(context: Context, uri: Uri, trackType: AudioTrackType) {
        viewModelScope.launch {
            pushHistory()
            val duration = MediaMetadataHelper.extractAudioDuration(context, uri)
            val name = uri.lastPathSegment ?: "Áudio"
            val newAudioClip = AudioClip(
                sourceUri = uri.toString(),
                name = name,
                timelineStartMs = _currentTimeMs.value,
                startTrimMs = 0L,
                endTrimMs = duration,
                originalDurationMs = duration,
                waveformPoints = MediaMetadataHelper.generateWaveformPoints(32)
            )

            val updatedTracks = _project.value.audioTracks.map { track ->
                if (track.type == trackType) {
                    track.copy(clips = track.clips + newAudioClip)
                } else track
            }

            _project.value = _project.value.copy(
                audioTracks = updatedTracks,
                lastModified = System.currentTimeMillis()
            )
            _selectedAudioClipId.value = newAudioClip.id
            _snackMessage.value = "Áudio adicionado à faixa ${trackType.labelPt}"
            autoSave()
        }
    }

    /**
     * Cortar (Split) no ponto atual do playhead.
     */
    fun splitClipAtPlayhead() {
        val selectedVid = _selectedVideoClipId.value
        val playhead = _currentTimeMs.value

        if (selectedVid != null) {
            val clipIndex = _project.value.videoClips.indexOfFirst { it.id == selectedVid }
            if (clipIndex == -1) return
            val clip = _project.value.videoClips[clipIndex]
            val clipStartOnTimeline = _project.value.getClipStartOnTimeline(clip.id)
            val offsetInClipTimeline = playhead - clipStartOnTimeline

            if (offsetInClipTimeline <= 100L || offsetInClipTimeline >= clip.trimmedDurationMs - 100L) {
                _snackMessage.value = "Posiciona o cursor no interior do clipe para cortar."
                return
            }

            pushHistory()
            val splitOffsetMs = clip.startTrimMs + (offsetInClipTimeline * clip.speed).toLong()

            val firstPart = clip.copy(
                id = java.util.UUID.randomUUID().toString(),
                endTrimMs = splitOffsetMs
            )
            val secondPart = clip.copy(
                id = java.util.UUID.randomUUID().toString(),
                startTrimMs = splitOffsetMs
            )

            val newClips = _project.value.videoClips.toMutableList()
            newClips.removeAt(clipIndex)
            newClips.add(clipIndex, secondPart)
            newClips.add(clipIndex, firstPart)

            _project.value = _project.value.copy(videoClips = newClips)
            _selectedVideoClipId.value = secondPart.id
            _snackMessage.value = "Clipe cortado em dois trechos."
            autoSave()
            return
        }

        // Cortar clipe de áudio selecionado
        val selectedAud = _selectedAudioClipId.value
        if (selectedAud != null) {
            for (track in _project.value.audioTracks) {
                val clipIndex = track.clips.indexOfFirst { it.id == selectedAud }
                if (clipIndex != -1) {
                    val ac = track.clips[clipIndex]
                    if (playhead in (ac.timelineStartMs + 100L) until (ac.timelineEndMs - 100L)) {
                        pushHistory()
                        val offsetMs = playhead - ac.timelineStartMs
                        val splitTrim = ac.startTrimMs + (offsetMs * ac.speed).toLong()

                        val part1 = ac.copy(id = java.util.UUID.randomUUID().toString(), endTrimMs = splitTrim)
                        val part2 = ac.copy(
                            id = java.util.UUID.randomUUID().toString(),
                            timelineStartMs = playhead,
                            startTrimMs = splitTrim
                        )

                        val newClips = track.clips.toMutableList()
                        newClips.removeAt(clipIndex)
                        newClips.add(clipIndex, part2)
                        newClips.add(clipIndex, part1)

                        val newTracks = _project.value.audioTracks.map {
                            if (it.id == track.id) it.copy(clips = newClips) else it
                        }
                        _project.value = _project.value.copy(audioTracks = newTracks)
                        _selectedAudioClipId.value = part2.id
                        _snackMessage.value = "Áudio cortado no cursor."
                        autoSave()
                        return
                    }
                }
            }
        }

        _snackMessage.value = "Seleciona um clipe para cortar."
    }

    /**
     * Eliminar clipe selecionado (com ripple delete opcional).
     */
    fun deleteSelectedClip() {
        val selectedVid = _selectedVideoClipId.value
        if (selectedVid != null) {
            pushHistory()
            val newClips = _project.value.videoClips.filterNot { it.id == selectedVid }
            _project.value = _project.value.copy(videoClips = newClips)
            _selectedVideoClipId.value = newClips.firstOrNull()?.id
            _snackMessage.value = "Clipe removido (espaço fechado)."
            autoSave()
            return
        }

        val selectedAud = _selectedAudioClipId.value
        if (selectedAud != null) {
            pushHistory()
            val newTracks = _project.value.audioTracks.map { track ->
                track.copy(clips = track.clips.filterNot { it.id == selectedAud })
            }
            _project.value = _project.value.copy(audioTracks = newTracks)
            _selectedAudioClipId.value = null
            _snackMessage.value = "Trecho de áudio removido."
            autoSave()
            return
        }
    }

    /**
     * Duplicar clipe selecionado.
     */
    fun duplicateSelectedClip() {
        val selectedVid = _selectedVideoClipId.value
        if (selectedVid != null) {
            val idx = _project.value.videoClips.indexOfFirst { it.id == selectedVid }
            if (idx != -1) {
                pushHistory()
                val orig = _project.value.videoClips[idx]
                val copy = orig.copy(
                    id = java.util.UUID.randomUUID().toString(),
                    name = "${orig.name} (Cópia)"
                )
                val newClips = _project.value.videoClips.toMutableList()
                newClips.add(idx + 1, copy)
                _project.value = _project.value.copy(videoClips = newClips)
                _selectedVideoClipId.value = copy.id
                _snackMessage.value = "Clipe duplicado."
                autoSave()
            }
            return
        }
    }

    /**
     * Separar o áudio do vídeo para uma faixa independente.
     */
    fun separateAudioFromVideo() {
        val selectedVid = _selectedVideoClipId.value ?: return
        val clip = _project.value.videoClips.find { it.id == selectedVid } ?: return
        val clipStartTimeline = _project.value.getClipStartOnTimeline(clip.id)

        pushHistory()

        // 1. Muta o vídeo original
        val updatedClips = _project.value.videoClips.map {
            if (it.id == clip.id) it.copy(isMuted = true) else it
        }

        // 2. Cria clipe de áudio independente na faixa de Voz ou Música
        val separatedAudio = AudioClip(
            sourceUri = clip.sourceUri,
            name = "Áudio de ${clip.name}",
            timelineStartMs = clipStartTimeline,
            startTrimMs = clip.startTrimMs,
            endTrimMs = clip.endTrimMs,
            originalDurationMs = clip.metadata.durationMs,
            speed = clip.speed,
            volume = clip.volume,
            waveformPoints = MediaMetadataHelper.generateWaveformPoints(32)
        )

        // Adiciona à faixa de voz por padrão
        val updatedTracks = _project.value.audioTracks.map { track ->
            if (track.type == AudioTrackType.VOICE) {
                track.copy(clips = track.clips + separatedAudio)
            } else track
        }

        _project.value = _project.value.copy(
            videoClips = updatedClips,
            audioTracks = updatedTracks
        )
        _selectedAudioClipId.value = separatedAudio.id
        _snackMessage.value = "Áudio separado em faixa independente com sucesso!"
        autoSave()
    }

    fun toggleMuteVideoClip(clipId: String) {
        pushHistory()
        val updated = _project.value.videoClips.map {
            if (it.id == clipId) it.copy(isMuted = !it.isMuted) else it
        }
        _project.value = _project.value.copy(videoClips = updated)
        autoSave()
    }

    fun updateClipSpeed(clipId: String, speed: Float) {
        pushHistory()
        val updated = _project.value.videoClips.map {
            if (it.id == clipId) it.copy(speed = speed) else it
        }
        _project.value = _project.value.copy(videoClips = updated)
        autoSave()
    }

    fun updateClipVolume(clipId: String, volume: Float) {
        pushHistory()
        val updated = _project.value.videoClips.map {
            if (it.id == clipId) it.copy(volume = volume) else it
        }
        _project.value = _project.value.copy(videoClips = updated)
        autoSave()
    }

    fun updateClipTrim(clipId: String, startTrimMs: Long, endTrimMs: Long) {
        val updated = _project.value.videoClips.map {
            if (it.id == clipId) it.copy(startTrimMs = startTrimMs, endTrimMs = endTrimMs) else it
        }
        _project.value = _project.value.copy(videoClips = updated)
        autoSave()
    }

    fun updateClipTransform(clipId: String, scale: Float, offsetX: Float, offsetY: Float, rotation: Float) {
        val updated = _project.value.videoClips.map {
            if (it.id == clipId) it.copy(scale = scale, offsetX = offsetX, offsetY = offsetY, rotation = rotation) else it
        }
        _project.value = _project.value.copy(videoClips = updated)
        autoSave()
    }

    fun resetClipTransform(clipId: String) {
        pushHistory()
        val updated = _project.value.videoClips.map {
            if (it.id == clipId) it.copy(scale = 1.0f, offsetX = 0.0f, offsetY = 0.0f, rotation = 0.0f) else it
        }
        _project.value = _project.value.copy(videoClips = updated)
        _snackMessage.value = "Posição e escala repostas."
        autoSave()
    }

    fun addKeyframe(clipId: String, scale: Float, offsetX: Float, offsetY: Float, rotation: Float) {
        val clip = _project.value.videoClips.find { it.id == clipId } ?: return
        val clipStart = _project.value.getClipStartOnTimeline(clip.id)
        val clipOffsetMs = (_currentTimeMs.value - clipStart).coerceIn(0L, clip.trimmedDurationMs)

        pushHistory()
        val newKf = Keyframe(
            timeOffsetMs = (clipOffsetMs * clip.speed).toLong(),
            scale = scale,
            offsetX = offsetX,
            offsetY = offsetY,
            rotationDegrees = rotation
        )
        val newKeyframes = (clip.keyframes.filterNot { it.timeOffsetMs == newKf.timeOffsetMs } + newKf).sortedBy { it.timeOffsetMs }
        val updated = _project.value.videoClips.map {
            if (it.id == clipId) it.copy(keyframes = newKeyframes) else it
        }
        _project.value = _project.value.copy(videoClips = updated)
        _snackMessage.value = "Keyframe adicionado no frame atual."
        autoSave()
    }

    fun removeKeyframe(clipId: String, keyframeId: String) {
        pushHistory()
        val updated = _project.value.videoClips.map { clip ->
            if (clip.id == clipId) {
                clip.copy(keyframes = clip.keyframes.filterNot { it.id == keyframeId })
            } else clip
        }
        _project.value = _project.value.copy(videoClips = updated)
        autoSave()
    }

    fun setTransition(clipId: String, transition: ClipTransition) {
        pushHistory()
        val updated = _project.value.videoClips.map {
            if (it.id == clipId) it.copy(transitionIn = transition) else it
        }
        _project.value = _project.value.copy(videoClips = updated)
        _snackMessage.value = "Transição '${transition.type.labelPt}' aplicada."
        autoSave()
    }

    fun updateAudioTrack(trackId: String, volume: Float, isMuted: Boolean, isSolo: Boolean, pan: Float, autoDucking: Boolean) {
        val updated = _project.value.audioTracks.map {
            if (it.id == trackId) {
                it.copy(volume = volume, isMuted = isMuted, isSolo = isSolo, pan = pan, autoDucking = autoDucking)
            } else it
        }
        _project.value = _project.value.copy(audioTracks = updated)
        autoSave()
    }

    fun updateAudioClipEffects(clipId: String, effects: AudioEffects, volume: Float, pan: Float, fadeInMs: Long, fadeOutMs: Long) {
        pushHistory()
        val updatedTracks = _project.value.audioTracks.map { track ->
            val updatedClips = track.clips.map { ac ->
                if (ac.id == clipId) {
                    ac.copy(effects = effects, volume = volume, pan = pan, fadeInMs = fadeInMs, fadeOutMs = fadeOutMs)
                } else ac
            }
            track.copy(clips = updatedClips)
        }
        _project.value = _project.value.copy(audioTracks = updatedTracks)
        autoSave()
    }

    fun moveVideoClip(fromIndex: Int, toIndex: Int) {
        if (fromIndex == toIndex || fromIndex !in _project.value.videoClips.indices || toIndex !in _project.value.videoClips.indices) return
        pushHistory()
        val list = _project.value.videoClips.toMutableList()
        val item = list.removeAt(fromIndex)
        list.add(toIndex, item)
        _project.value = _project.value.copy(videoClips = list)
        autoSave()
    }

    fun startExport(context: Context, config: ExportConfig) {
        VideoExportService.startExport(context, _project.value, config)
    }

    fun cancelExport(context: Context) {
        VideoExportService.cancel(context)
    }

    fun resetExportState() {
        VideoExportService.resetState()
    }

    fun createNewProject(title: String) {
        pause()
        val newProj = Project(
            id = java.util.UUID.randomUUID().toString(),
            title = title.ifBlank { "Projeto CineCorte" }
        )
        _project.value = newProj
        _currentTimeMs.value = 0L
        _selectedVideoClipId.value = null
        _selectedAudioClipId.value = null
        undoRedoManager.clear()
        _canUndo.value = false
        _canRedo.value = false
        autoSave()
        _snackMessage.value = "Novo projeto criado: ${newProj.title}"
    }

    fun loadSavedProject(id: String) {
        pause()
        viewModelScope.launch {
            val loaded = repository.loadProject(id)
            if (loaded != null) {
                _project.value = loaded
                _currentTimeMs.value = 0L
                _selectedVideoClipId.value = loaded.videoClips.firstOrNull()?.id
                undoRedoManager.clear()
                _canUndo.value = false
                _canRedo.value = false
                _snackMessage.value = "Projeto carregado: ${loaded.title}"
            }
        }
    }

    fun deleteSavedProject(id: String) {
        viewModelScope.launch {
            repository.deleteProject(id)
            _snackMessage.value = "Projeto eliminado."
        }
    }

    fun openSilenceRemoval() {
        if (_project.value.videoClips.isEmpty() && _project.value.audioTracks.all { it.clips.isEmpty() }) {
            _snackMessage.value = "Importa um vídeo ou narração primeiro para remover silêncios."
            return
        }
        runSilenceDetection(_silenceConfig.value)
        openSheet(EditorSheet.SILENCE_REMOVAL)
    }

    fun updateSilenceConfig(newConfig: SilenceDetectionConfig) {
        _silenceConfig.value = newConfig
        runSilenceDetection(newConfig)
    }

    fun runSilenceDetection(config: SilenceDetectionConfig) {
        viewModelScope.launch {
            _isDetectingSilences.value = true
            val results = mutableListOf<SilenceInterval>()

            val selectedVid = _selectedVideoClipId.value
            val targetClips = if (selectedVid != null && !config.applyToAllClips) {
                _project.value.videoClips.filter { it.id == selectedVid }
            } else {
                _project.value.videoClips
            }

            var timelineAcc = 0L
            for (clip in _project.value.videoClips) {
                val clipStartTimeline = timelineAcc
                val clipDuration = clip.trimmedDurationMs
                timelineAcc += clipDuration

                if (targetClips.any { it.id == clip.id }) {
                    val intervals = MediaMetadataHelper.detectSilenceIntervals(
                        clipId = clip.id,
                        clipName = clip.name,
                        clipStartMs = clip.startTrimMs,
                        clipEndMs = clip.endTrimMs,
                        clipTimelineStartMs = clipStartTimeline,
                        speed = clip.speed,
                        config = config
                    )
                    results.addAll(intervals)
                }
            }

            _detectedSilences.value = results
            _isDetectingSilences.value = false
        }
    }

    fun toggleSilenceInterval(intervalId: String) {
        _detectedSilences.value = _detectedSilences.value.map {
            if (it.id == intervalId) it.copy(isSelectedForRemoval = !it.isSelectedForRemoval) else it
        }
    }

    fun selectAllSilences(selectAll: Boolean) {
        _detectedSilences.value = _detectedSilences.value.map {
            it.copy(isSelectedForRemoval = selectAll)
        }
    }

    /**
     * Remove todos os silêncios marcados, fatiando os clipes nos trechos de fala,
     * eliminando os vãos e unindo os trechos (ripple delete).
     */
    fun executeSilenceRemoval() {
        val toRemove = _detectedSilences.value.filter { it.isSelectedForRemoval }
        if (toRemove.isEmpty()) {
            _snackMessage.value = "Nenhum silêncio marcado para remoção."
            return
        }

        pushHistory()

        val totalRemovedMs = toRemove.sumOf { it.durationMs }
        val newVideoClips = mutableListOf<VideoClip>()

        for (clip in _project.value.videoClips) {
            val clipSilences = toRemove.filter { it.clipId == clip.id }.sortedBy { it.startOffsetMs }
            if (clipSilences.isEmpty()) {
                newVideoClips.add(clip)
                continue
            }

            // Calcula os trechos de fala entre as pausas eliminadas
            var currentStart = clip.startTrimMs
            for (silence in clipSilences) {
                if (silence.startOffsetMs > currentStart + 120L) {
                    val speechPiece = clip.copy(
                        id = java.util.UUID.randomUUID().toString(),
                        startTrimMs = currentStart,
                        endTrimMs = silence.startOffsetMs
                    )
                    newVideoClips.add(speechPiece)
                }
                currentStart = maxOf(currentStart, silence.endOffsetMs)
            }

            // Último trecho de fala após a última pausa
            if (currentStart < clip.endTrimMs - 120L) {
                val speechPiece = clip.copy(
                    id = java.util.UUID.randomUUID().toString(),
                    startTrimMs = currentStart,
                    endTrimMs = clip.endTrimMs
                )
                newVideoClips.add(speechPiece)
            }
        }

        // Se todos os trechos fossem silêncio, preserva ao menos o primeiro
        val finalClips = if (newVideoClips.isNotEmpty()) newVideoClips else _project.value.videoClips

        _project.value = _project.value.copy(
            videoClips = finalClips,
            lastModified = System.currentTimeMillis()
        )

        _selectedVideoClipId.value = finalClips.firstOrNull()?.id
        _detectedSilences.value = emptyList()
        closeSheet()
        autoSave()

        val secondsSaved = String.format(java.util.Locale.US, "%.1f", totalRemovedMs / 1000f)
        _snackMessage.value = "${toRemove.size} pausas removidas! Poupaste ${secondsSaved}s de silêncio."
    }

    // --- ASSISTENTE DE CORTE AUTOMÁTICO EM 2 ETAPAS (SILÊNCIOS + GROQ WHISPER) ---

    fun openAutoCutWizard() {
        if (_project.value.videoClips.isEmpty() && _project.value.audioTracks.all { it.clips.isEmpty() }) {
            _snackMessage.value = "Importa um vídeo ou áudio antes de usar o Corte Automático."
            return
        }
        _autoCutStep.value = AutoCutStep.STEP_A_SILENCE
        runSilenceDetection(_silenceConfig.value)
        openSheet(EditorSheet.AUTO_CUT_WIZARD)
    }

    fun setAutoCutStep(step: AutoCutStep) {
        _autoCutStep.value = step
    }

    fun openSettings() {
        openSheet(EditorSheet.SETTINGS)
    }

    fun saveGroqApiKey(apiKey: String) {
        securePrefs.saveGroqApiKey(apiKey)
        _groqApiKey.value = apiKey.trim()
        _snackMessage.value = "Chave da Groq API guardada com segurança!"
    }

    fun acceptGroqConsent() {
        securePrefs.setGroqConsentAccepted(true)
        _hasAcceptedGroqConsent.value = true
    }

    fun updateRepetitionConfig(config: RepetitionDetectionConfig) {
        _repetitionConfig.value = config
        // Se já tiver palavras em cache, recalcula as sugestões sem gastar chamadas de rede
        cachedTranscriptionWords?.let { words ->
            _repetitionSuggestions.value = RepetitionDetector.findRepetitions(words, config)
        }
    }

    fun toggleRepetitionSuggestion(id: String) {
        _repetitionSuggestions.value = _repetitionSuggestions.value.map {
            if (it.id == id) it.copy(isSelected = !it.isSelected) else it
        }
    }

    fun selectAllRepetitions(selectAll: Boolean) {
        _repetitionSuggestions.value = _repetitionSuggestions.value.map {
            it.copy(isSelected = selectAll)
        }
    }

    /**
     * Toca 1.2 segundos antes do corte, salta o trecho repetido e toca 1.2 segundos depois
     * para pré-visualização auditiva imediata da transição fluente.
     */
    fun previewCutAudio(startMs: Long, endMs: Long) {
        playbackJob?.cancel()
        val preStart = maxOf(0L, startMs - 1200L)
        seekTo(preStart)
        _isPlaying.value = true

        playbackJob = viewModelScope.launch {
            val jumpDelay = (startMs - preStart).coerceAtLeast(100L)
            delay(jumpDelay)
            seekTo(endMs + 30L)
            delay(1200L)
            _isPlaying.value = false
        }
    }

    /**
     * Inicia a extração de áudio da timeline e a chamada ao modelo Whisper na Groq API.
     */
    fun startTranscriptionAndRepetitionDetection(context: Context) {
        val apiKey = _groqApiKey.value
        if (apiKey.isBlank()) {
            openSheet(EditorSheet.SETTINGS)
            _snackMessage.value = "Insere primeiro a tua chave da Groq API."
            return
        }

        viewModelScope.launch {
            _isTranscribing.value = true
            _transcriptionProgressText.value = "A extrair áudio da timeline..."

            try {
                val audioFile = AudioExtractorHelper.extractTimelineAudio(context, _project.value)
                _transcriptionProgressText.value = "A enviar para a Groq Whisper (large-v3-turbo)..."

                val service = GroqWhisperService()
                val result = service.transcribeAudio(
                    apiKey = apiKey,
                    audioFile = audioFile,
                    language = securePrefs.getLanguage()
                )

                _isTranscribing.value = false

                result.fold(
                    onSuccess = { response ->
                        cachedTranscriptionWords = response.words
                        val suggestions = RepetitionDetector.findRepetitions(response.words, _repetitionConfig.value)
                        _repetitionSuggestions.value = suggestions
                        if (suggestions.isEmpty()) {
                            _snackMessage.value = "Transcrição concluída: nenhuma repetição ou gaguez detetada!"
                        } else {
                            _snackMessage.value = "${suggestions.size} repetições detetadas! Revê os cortes sugeridos."
                        }
                    },
                    onFailure = { err ->
                        _snackMessage.value = err.message ?: "Erro ao contactar a API da Groq."
                    }
                )
            } catch (e: Exception) {
                _isTranscribing.value = false
                _snackMessage.value = e.message ?: "Falha ao processar áudio para transcrição."
            }
        }
    }

    /**
     * Executa o corte atómico de todas as repetições selecionadas pelo utilizador.
     * Preserva as dimensões 100% originais e consolida tudo numa única ação de Desfazer (Undo).
     */
    fun executeRepetitionCuts() {
        val toCut = _repetitionSuggestions.value.filter { it.isSelected }.sortedBy { it.startMs }
        if (toCut.isEmpty()) {
            _snackMessage.value = "Nenhum corte selecionado."
            return
        }

        pushHistory()

        val totalDurationCutMs = toCut.sumOf { it.durationMs }
        val currentVideoClips = _project.value.videoClips.toMutableList()
        val newVideoClips = mutableListOf<VideoClip>()

        var accumulatedTimelineMs = 0L
        for (clip in currentVideoClips) {
            val clipTimelineStart = accumulatedTimelineMs
            val clipTimelineEnd = clipTimelineStart + clip.trimmedDurationMs
            accumulatedTimelineMs = clipTimelineEnd

            val relevantCuts = toCut.filter {
                it.endMs > clipTimelineStart && it.startMs < clipTimelineEnd
            }

            if (relevantCuts.isEmpty()) {
                newVideoClips.add(clip)
                continue
            }

            var currentTimelineCursor = clipTimelineStart
            var currentMediaOffsetMs = clip.startTrimMs

            for (cut in relevantCuts) {
                val cutStartInTimeline = maxOf(currentTimelineCursor, cut.startMs)
                val cutEndInTimeline = minOf(clipTimelineEnd, cut.endMs)

                if (cutStartInTimeline > currentTimelineCursor + 100L) {
                    val pieceDurationTimeline = cutStartInTimeline - currentTimelineCursor
                    val pieceMediaDuration = (pieceDurationTimeline * clip.speed).toLong()
                    val piece = clip.copy(
                        id = java.util.UUID.randomUUID().toString(),
                        startTrimMs = currentMediaOffsetMs,
                        endTrimMs = currentMediaOffsetMs + pieceMediaDuration
                    )
                    newVideoClips.add(piece)
                }

                val skippedDurationTimeline = cutEndInTimeline - cutStartInTimeline
                val skippedMediaDuration = (skippedDurationTimeline * clip.speed).toLong()

                currentTimelineCursor = cutEndInTimeline
                currentMediaOffsetMs += (cutStartInTimeline - (clipTimelineStart + ((currentMediaOffsetMs - clip.startTrimMs) / clip.speed).toLong())) * clip.speed.toLong() + skippedMediaDuration
            }

            if (currentTimelineCursor < clipTimelineEnd - 100L) {
                val remainingTimeline = clipTimelineEnd - currentTimelineCursor
                val remainingMedia = (remainingTimeline * clip.speed).toLong()
                val piece = clip.copy(
                    id = java.util.UUID.randomUUID().toString(),
                    startTrimMs = maxOf(0L, clip.endTrimMs - remainingMedia),
                    endTrimMs = clip.endTrimMs
                )
                newVideoClips.add(piece)
            }
        }

        val finalClips = if (newVideoClips.isNotEmpty()) newVideoClips else _project.value.videoClips

        _project.value = _project.value.copy(
            videoClips = finalClips,
            lastModified = System.currentTimeMillis()
        )

        _selectedVideoClipId.value = finalClips.firstOrNull()?.id
        _repetitionSuggestions.value = emptyList()
        cachedTranscriptionWords = null
        closeSheet()
        autoSave()

        val secondsSaved = String.format(java.util.Locale.US, "%.1f", totalDurationCutMs / 1000f)
        _snackMessage.value = "${toCut.size} repetições cortadas! Poupaste ${secondsSaved}s de áudio redundante."
    }

    private fun autoSave() {
        viewModelScope.launch {
            repository.saveProject(_project.value)
        }
    }
}

