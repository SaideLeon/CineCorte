package com.example

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.*
import com.example.ui.EditorSheet
import com.example.ui.EditorViewModel
import com.example.ui.components.*
import com.example.ui.theme.*

class MainActivity : ComponentActivity() {

    private val viewModel: EditorViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            CineCorteTheme {
                val context = LocalContext.current

                // Estados reativos da arquitetura MVVM
                val project by viewModel.project.collectAsStateWithLifecycle()
                val currentTimeMs by viewModel.currentTimeMs.collectAsStateWithLifecycle()
                val isPlaying by viewModel.isPlaying.collectAsStateWithLifecycle()
                val selectedVideoClipId by viewModel.selectedVideoClipId.collectAsStateWithLifecycle()
                val selectedAudioClipId by viewModel.selectedAudioClipId.collectAsStateWithLifecycle()
                val zoomFactor by viewModel.timelineZoom.collectAsStateWithLifecycle()
                val activeSheet by viewModel.activeSheet.collectAsStateWithLifecycle()
                val canUndo by viewModel.canUndo.collectAsStateWithLifecycle()
                val canRedo by viewModel.canRedo.collectAsStateWithLifecycle()
                val snackMessage by viewModel.snackMessage.collectAsStateWithLifecycle()
                val savedProjects by viewModel.savedProjects.collectAsStateWithLifecycle()
                val exportState by viewModel.exportState.collectAsStateWithLifecycle()
                val detectedSilences by viewModel.detectedSilences.collectAsStateWithLifecycle()
                val silenceConfig by viewModel.silenceConfig.collectAsStateWithLifecycle()
                val isDetectingSilences by viewModel.isDetectingSilences.collectAsStateWithLifecycle()
                val groqApiKey by viewModel.groqApiKey.collectAsStateWithLifecycle()
                val hasAcceptedGroqConsent by viewModel.hasAcceptedGroqConsent.collectAsStateWithLifecycle()
                val autoCutStep by viewModel.autoCutStep.collectAsStateWithLifecycle()
                val repetitionConfig by viewModel.repetitionConfig.collectAsStateWithLifecycle()
                val repetitionSuggestions by viewModel.repetitionSuggestions.collectAsStateWithLifecycle()
                val isTranscribing by viewModel.isTranscribing.collectAsStateWithLifecycle()
                val transcriptionProgressText by viewModel.transcriptionProgressText.collectAsStateWithLifecycle()

                val snackbarHostState = remember { SnackbarHostState() }

                // Exibe mensagens informativas
                LaunchedEffect(snackMessage) {
                    snackMessage?.let { msg ->
                        snackbarHostState.showSnackbar(msg)
                        viewModel.clearSnack()
                    }
                }

                // Photo Picker oficial para importação de vídeos (Zero permissões invasivas)
                val videoPickerLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.PickVisualMedia()
                ) { uri: Uri? ->
                    uri?.let {
                        try {
                            contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        } catch (ignored: Exception) {}
                        viewModel.importVideo(context, it)
                    }
                }

                // Storage Access Framework (SAF) para seleção de áudios (MP3, WAV, AAC, M4A)
                var pendingAudioTrackType by remember { mutableStateOf(AudioTrackType.MUSIC) }
                val audioPickerLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.OpenDocument()
                ) { uri: Uri? ->
                    uri?.let {
                        try {
                            contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        } catch (ignored: Exception) {}
                        viewModel.importAudio(context, it, pendingAudioTrackType)
                    }
                }

                Scaffold(
                    snackbarHost = { SnackbarHost(snackbarHostState) },
                    containerColor = StudioDarkBg,
                    contentWindowInsets = WindowInsets.safeDrawing,
                    topBar = {
                        EditorTopBar(
                            projectTitle = project.title,
                            canUndo = canUndo,
                            canRedo = canRedo,
                            onUndo = viewModel::undo,
                            onRedo = viewModel::redo,
                            onOpenProjects = { viewModel.openSheet(EditorSheet.PROJECTS) },
                            onOpenSettings = viewModel::openSettings,
                            onOpenExport = { viewModel.openSheet(EditorSheet.EXPORT) }
                        )
                    }
                ) { innerPadding ->
                    BoxWithConstraints(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        val isTabletOrLandscape = maxWidth > 680.dp

                        if (isTabletOrLandscape) {
                            // Layout Adaptativo Horizontal / Tablet (Pré-visualização ao lado da Timeline)
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // Painel Esquerdo: Pré-visualização
                                VideoPlayerPreview(
                                    project = project,
                                    currentTimeMs = currentTimeMs,
                                    isPlaying = isPlaying,
                                    selectedClipId = selectedVideoClipId,
                                    onPlayPause = viewModel::togglePlayPause,
                                    onStepFrame = viewModel::stepFrame,
                                    onTransformUpdate = viewModel::updateClipTransform,
                                    onResetTransform = viewModel::resetClipTransform,
                                    modifier = Modifier
                                        .weight(1.1f)
                                        .fillMaxHeight()
                                )

                                // Painel Direito: Ações e Linha do Tempo
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight(),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    EditorActionToolbar(
                                        onImportVideo = {
                                            videoPickerLauncher.launch(
                                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                                            )
                                        },
                                        onImportAudio = { type ->
                                            pendingAudioTrackType = type
                                            audioPickerLauncher.launch(arrayOf("audio/*"))
                                        },
                                        onSplit = viewModel::splitClipAtPlayhead,
                                        onDelete = viewModel::deleteSelectedClip,
                                        onSeparateAudio = viewModel::separateAudioFromVideo,
                                        onOpenProperties = { viewModel.openSheet(EditorSheet.PROPERTIES) },
                                        onOpenAudioEffects = { viewModel.openSheet(EditorSheet.AUDIO_EFFECTS) },
                                        onOpenTransitions = { viewModel.openSheet(EditorSheet.TRANSITIONS) },
                                        onOpenKeyframes = { viewModel.openSheet(EditorSheet.KEYFRAMES) },
                                        onOpenSilenceRemoval = viewModel::openSilenceRemoval,
                                        onOpenAutoCut = viewModel::openAutoCutWizard,
                                        hasSelectedVideoClip = selectedVideoClipId != null,
                                        hasSelectedAudioClip = selectedAudioClipId != null
                                    )

                                    TimelineView(
                                        project = project,
                                        currentTimeMs = currentTimeMs,
                                        zoomFactor = zoomFactor,
                                        selectedVideoClipId = selectedVideoClipId,
                                        selectedAudioClipId = selectedAudioClipId,
                                        onSeek = viewModel::seekTo,
                                        onZoomChange = viewModel::setZoom,
                                        onSelectVideoClip = viewModel::selectVideoClip,
                                        onSelectAudioClip = viewModel::selectAudioClip,
                                        onTrimVideoClip = viewModel::updateClipTrim,
                                        onSplit = viewModel::splitClipAtPlayhead,
                                        onOpenTransitions = { viewModel.openSheet(EditorSheet.TRANSITIONS) },
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }
                        } else {
                            // Layout Vertical padrão para Smartphones
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(vertical = 4.dp),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                // 1. Pré-visualização do vídeo (dimensões exatas e proporção original)
                                VideoPlayerPreview(
                                    project = project,
                                    currentTimeMs = currentTimeMs,
                                    isPlaying = isPlaying,
                                    selectedClipId = selectedVideoClipId,
                                    onPlayPause = viewModel::togglePlayPause,
                                    onStepFrame = viewModel::stepFrame,
                                    onTransformUpdate = viewModel::updateClipTransform,
                                    onResetTransform = viewModel::resetClipTransform,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1.15f)
                                )

                                // 2. Barra de Ferramentas de Edição rápida
                                EditorActionToolbar(
                                    onImportVideo = {
                                        videoPickerLauncher.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                                        )
                                    },
                                    onImportAudio = { type ->
                                        pendingAudioTrackType = type
                                        audioPickerLauncher.launch(arrayOf("audio/*"))
                                    },
                                    onSplit = viewModel::splitClipAtPlayhead,
                                    onDelete = viewModel::deleteSelectedClip,
                                    onSeparateAudio = viewModel::separateAudioFromVideo,
                                    onOpenProperties = { viewModel.openSheet(EditorSheet.PROPERTIES) },
                                    onOpenAudioEffects = { viewModel.openSheet(EditorSheet.AUDIO_EFFECTS) },
                                    onOpenTransitions = { viewModel.openSheet(EditorSheet.TRANSITIONS) },
                                    onOpenKeyframes = { viewModel.openSheet(EditorSheet.KEYFRAMES) },
                                    onOpenSilenceRemoval = viewModel::openSilenceRemoval,
                                    onOpenAutoCut = viewModel::openAutoCutWizard,
                                    hasSelectedVideoClip = selectedVideoClipId != null,
                                    hasSelectedAudioClip = selectedAudioClipId != null,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )

                                // 3. Linha do Tempo Não Linear Multi-Faixas
                                TimelineView(
                                    project = project,
                                    currentTimeMs = currentTimeMs,
                                    zoomFactor = zoomFactor,
                                    selectedVideoClipId = selectedVideoClipId,
                                    selectedAudioClipId = selectedAudioClipId,
                                    onSeek = viewModel::seekTo,
                                    onZoomChange = viewModel::setZoom,
                                    onSelectVideoClip = viewModel::selectVideoClip,
                                    onSelectAudioClip = viewModel::selectAudioClip,
                                    onTrimVideoClip = viewModel::updateClipTrim,
                                    onSplit = viewModel::splitClipAtPlayhead,
                                    onOpenTransitions = { viewModel.openSheet(EditorSheet.TRANSITIONS) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f)
                                )
                            }
                        }
                    }

                    // MODAL SHEETS & DIÁLOGOS
                    when (activeSheet) {
                        EditorSheet.PROPERTIES -> {
                            val selectedClip = project.videoClips.find { it.id == selectedVideoClipId }
                            if (selectedClip != null) {
                                ClipPropertiesSheet(
                                    clip = selectedClip,
                                    onDismiss = viewModel::closeSheet,
                                    onUpdateSpeed = { speed -> viewModel.updateClipSpeed(selectedClip.id, speed) },
                                    onUpdateVolume = { vol -> viewModel.updateClipVolume(selectedClip.id, vol) },
                                    onToggleMute = { viewModel.toggleMuteVideoClip(selectedClip.id) },
                                    onSeparateAudio = viewModel::separateAudioFromVideo,
                                    onDuplicate = viewModel::duplicateSelectedClip,
                                    onDelete = viewModel::deleteSelectedClip,
                                    onResetTransform = { viewModel.resetClipTransform(selectedClip.id) },
                                    onOpenKeyframes = { viewModel.openSheet(EditorSheet.KEYFRAMES) },
                                    onOpenTransitions = { viewModel.openSheet(EditorSheet.TRANSITIONS) },
                                    onOpenSilenceRemoval = viewModel::openSilenceRemoval
                                )
                            }
                        }

                        EditorSheet.TRANSITIONS -> {
                            val selectedClip = project.videoClips.find { it.id == selectedVideoClipId }
                                ?: project.videoClips.firstOrNull()
                            if (selectedClip != null) {
                                TransitionPickerSheet(
                                    currentTransition = selectedClip.transitionIn,
                                    onDismiss = viewModel::closeSheet,
                                    onApplyTransition = { tr -> viewModel.setTransition(selectedClip.id, tr) }
                                )
                            }
                        }

                        EditorSheet.AUDIO_EFFECTS -> {
                            val activeAudioClip = project.audioTracks.flatMap { it.clips }.find { it.id == selectedAudioClipId }
                                ?: project.audioTracks.flatMap { it.clips }.firstOrNull()

                            if (activeAudioClip != null) {
                                AudioEffectsSheet(
                                    clip = activeAudioClip,
                                    onDismiss = viewModel::closeSheet,
                                    onSaveEffects = { ef, vol, pan, fIn, fOut ->
                                        viewModel.updateAudioClipEffects(activeAudioClip.id, ef, vol, pan, fIn, fOut)
                                    },
                                    onExportAudioOnly = { viewModel.openSheet(EditorSheet.EXPORT) }
                                )
                            }
                        }

                        EditorSheet.KEYFRAMES -> {
                            val selectedClip = project.videoClips.find { it.id == selectedVideoClipId }
                            if (selectedClip != null) {
                                KeyframeEditorSheet(
                                    clip = selectedClip,
                                    currentTimeMs = currentTimeMs,
                                    onDismiss = viewModel::closeSheet,
                                    onAddKeyframe = { s, x, y, r -> viewModel.addKeyframe(selectedClip.id, s, x, y, r) },
                                    onRemoveKeyframe = { kId -> viewModel.removeKeyframe(selectedClip.id, kId) },
                                    onResetTransform = { viewModel.resetClipTransform(selectedClip.id) }
                                )
                            }
                        }

                        EditorSheet.EXPORT -> {
                            ExportDialog(
                                project = project,
                                exportState = exportState,
                                onDismiss = viewModel::closeSheet,
                                onStartExport = { cfg -> viewModel.startExport(context, cfg) },
                                onCancelExport = { viewModel.cancelExport(context) },
                                onResetExportState = viewModel::resetExportState
                            )
                        }

                        EditorSheet.PROJECTS -> {
                            ProjectListDialog(
                                projects = savedProjects,
                                currentProjectId = project.id,
                                onDismiss = viewModel::closeSheet,
                                onSelectProject = viewModel::loadSavedProject,
                                onCreateNewProject = viewModel::createNewProject,
                                onDeleteProject = viewModel::deleteSavedProject
                            )
                        }

                        EditorSheet.SILENCE_REMOVAL -> {
                            SilenceRemovalSheet(
                                silences = detectedSilences,
                                isDetecting = isDetectingSilences,
                                currentConfig = silenceConfig,
                                totalProjectDurationMs = project.totalDurationMs,
                                onDismiss = viewModel::closeSheet,
                                onUpdateConfig = viewModel::updateSilenceConfig,
                                onToggleInterval = viewModel::toggleSilenceInterval,
                                onSelectAll = viewModel::selectAllSilences,
                                onExecuteRemoval = viewModel::executeSilenceRemoval
                            )
                        }

                        EditorSheet.AUTO_CUT_WIZARD -> {
                            AutoCutWizardSheet(
                                currentStep = autoCutStep,
                                onStepChange = viewModel::setAutoCutStep,
                                silences = detectedSilences,
                                isDetectingSilences = isDetectingSilences,
                                silenceConfig = silenceConfig,
                                totalProjectDurationMs = project.totalDurationMs,
                                onUpdateSilenceConfig = viewModel::updateSilenceConfig,
                                onToggleSilenceInterval = viewModel::toggleSilenceInterval,
                                onSelectAllSilences = viewModel::selectAllSilences,
                                onExecuteSilenceRemoval = viewModel::executeSilenceRemoval,
                                hasGroqApiKey = groqApiKey.isNotBlank(),
                                hasAcceptedConsent = hasAcceptedGroqConsent,
                                onAcceptConsent = viewModel::acceptGroqConsent,
                                onOpenSettings = viewModel::openSettings,
                                isTranscribing = isTranscribing,
                                transcriptionProgressText = transcriptionProgressText,
                                repetitionSuggestions = repetitionSuggestions,
                                repetitionConfig = repetitionConfig,
                                onUpdateRepetitionConfig = viewModel::updateRepetitionConfig,
                                onStartTranscription = { viewModel.startTranscriptionAndRepetitionDetection(context) },
                                onToggleRepetitionSuggestion = viewModel::toggleRepetitionSuggestion,
                                onSelectAllRepetitions = viewModel::selectAllRepetitions,
                                onPreviewCutAudio = viewModel::previewCutAudio,
                                onExecuteRepetitionCuts = viewModel::executeRepetitionCuts,
                                onDismiss = viewModel::closeSheet
                            )
                        }

                        EditorSheet.SETTINGS -> {
                            SettingsDialog(
                                initialApiKey = groqApiKey,
                                onSaveApiKey = viewModel::saveGroqApiKey,
                                onDismiss = viewModel::closeSheet
                            )
                        }

                        EditorSheet.NONE -> {}
                    }
                }
            }
        }
    }
}

/**
 * Barra superior da aplicação com identificação, Desfazer/Refazer e Exportar.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditorTopBar(
    projectTitle: String,
    canUndo: Boolean,
    canRedo: Boolean,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onOpenProjects: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenExport: () -> Unit
) {
    TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = StudioSurface,
            titleContentColor = TextPrimary
        ),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(CineCyan),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCut,
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Column {
                    Text(
                        text = "CineCorte",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = projectTitle,
                        fontSize = 11.sp,
                        color = CineCyanGlow
                    )
                }
            }
        },
        actions = {
            // Desfazer (Undo)
            IconButton(
                onClick = onUndo,
                enabled = canUndo,
                modifier = Modifier.testTag("undo_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Undo,
                    contentDescription = "Desfazer",
                    tint = if (canUndo) TextPrimary else TextTertiary
                )
            }

            // Refazer (Redo)
            IconButton(
                onClick = onRedo,
                enabled = canRedo,
                modifier = Modifier.testTag("redo_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Redo,
                    contentDescription = "Refazer",
                    tint = if (canRedo) TextPrimary else TextTertiary
                )
            }

            // Projetos guardados (Room)
            IconButton(
                onClick = onOpenProjects,
                modifier = Modifier.testTag("projects_button")
            ) {
                Icon(
                    imageVector = Icons.Default.FolderOpen,
                    contentDescription = "Projetos",
                    tint = CineViolet
                )
            }

            // Definições (Chave API Groq)
            IconButton(
                onClick = onOpenSettings,
                modifier = Modifier.testTag("settings_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Definições",
                    tint = TextSecondary
                )
            }

            // Botão Exportar destacado
            FilledTonalButton(
                onClick = onOpenExport,
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = CineCyan,
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                modifier = Modifier.padding(end = 8.dp).testTag("export_button")
            ) {
                Icon(
                    imageVector = Icons.Default.FileUpload,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Exportar", fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }
    )
}

/**
 * Barra de ferramentas com botões de ação rápidos (Cortar, Separar Áudio, Importar, etc.).
 */
@Composable
private fun EditorActionToolbar(
    onImportVideo: () -> Unit,
    onImportAudio: (AudioTrackType) -> Unit,
    onSplit: () -> Unit,
    onDelete: () -> Unit,
    onSeparateAudio: () -> Unit,
    onOpenProperties: () -> Unit,
    onOpenAudioEffects: () -> Unit,
    onOpenTransitions: () -> Unit,
    onOpenKeyframes: () -> Unit,
    onOpenSilenceRemoval: () -> Unit,
    onOpenAutoCut: () -> Unit,
    hasSelectedVideoClip: Boolean,
    hasSelectedAudioClip: Boolean,
    modifier: Modifier = Modifier
) {
    var showAudioMenu by remember { mutableStateOf(false) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // IMPORTAR VÍDEO
        FilledTonalButton(
            onClick = onImportVideo,
            colors = ButtonDefaults.filledTonalButtonColors(containerColor = TrackVideoColor.copy(alpha = 0.3f)),
            shape = RoundedCornerShape(8.dp),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
            modifier = Modifier.testTag("import_video_button")
        ) {
            Icon(imageVector = Icons.Default.VideoCall, contentDescription = null, tint = CineCyan, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("+ Vídeo", fontSize = 11.sp, color = TextPrimary, fontWeight = FontWeight.Medium)
        }

        // IMPORTAR ÁUDIO
        Box {
            FilledTonalButton(
                onClick = { showAudioMenu = true },
                colors = ButtonDefaults.filledTonalButtonColors(containerColor = TrackMusicColor.copy(alpha = 0.3f)),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                modifier = Modifier.testTag("import_audio_button")
            ) {
                Icon(imageVector = Icons.Default.AudioFile, contentDescription = null, tint = CineViolet, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("+ Áudio", fontSize = 11.sp, color = TextPrimary, fontWeight = FontWeight.Medium)
            }

            DropdownMenu(
                expanded = showAudioMenu,
                onDismissRequest = { showAudioMenu = false },
                modifier = Modifier.background(StudioSurface)
            ) {
                AudioTrackType.values().forEach { trackType ->
                    DropdownMenuItem(
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(RoundedCornerShape(5.dp))
                                        .background(trackType.composeColor)
                                )
                                Text(trackType.labelPt, color = TextPrimary, fontSize = 12.sp)
                            }
                        },
                        onClick = {
                            showAudioMenu = false
                            onImportAudio(trackType)
                        }
                    )
                }
            }
        }

        // CORTAR (SPLIT)
        Button(
            onClick = onSplit,
            colors = ButtonDefaults.buttonColors(containerColor = StudioSurfaceVariant),
            shape = RoundedCornerShape(8.dp),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
            modifier = Modifier.testTag("split_button")
        ) {
            Icon(imageVector = Icons.Default.ContentCut, contentDescription = null, tint = CineCyan, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Cortar", fontSize = 11.sp, color = TextPrimary)
        }

        // CORTE AUTOMÁTICO COMPLETO (2 ETAPAS: SILÊNCIOS + GROQ WHISPER)
        Button(
            onClick = onOpenAutoCut,
            colors = ButtonDefaults.buttonColors(containerColor = CineCyan.copy(alpha = 0.28f)),
            shape = RoundedCornerShape(8.dp),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
            modifier = Modifier.testTag("toolbar_auto_cut_button")
        ) {
            Icon(imageVector = Icons.Default.AutoFixHigh, contentDescription = null, tint = CineCyan, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Corte Auto (IA)", fontSize = 11.sp, color = CineCyanGlow, fontWeight = FontWeight.Bold)
        }

        // AUTO-REMOÇÃO DE PAUSAS / SILÊNCIOS (OFFLINE)
        Button(
            onClick = onOpenSilenceRemoval,
            colors = ButtonDefaults.buttonColors(containerColor = StudioSurfaceVariant),
            shape = RoundedCornerShape(8.dp),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
            modifier = Modifier.testTag("toolbar_silence_removal_button")
        ) {
            Icon(imageVector = Icons.Default.GraphicEq, contentDescription = null, tint = CineCyan, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Pausas", fontSize = 11.sp, color = TextPrimary)
        }

        // SEPARAR ÁUDIO (ATIVO QUANDO HÁ VÍDEO SELECIONADO)
        if (hasSelectedVideoClip) {
            Button(
                onClick = onSeparateAudio,
                colors = ButtonDefaults.buttonColors(containerColor = CineViolet.copy(alpha = 0.35f)),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                modifier = Modifier.testTag("toolbar_separate_audio_button")
            ) {
                Icon(imageVector = Icons.Default.CallSplit, contentDescription = null, tint = CineViolet, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Separar Áudio", fontSize = 11.sp, color = TextPrimary)
            }
        }

        // PROPRIEDADES / AJUSTES
        if (hasSelectedVideoClip) {
            Button(
                onClick = onOpenProperties,
                colors = ButtonDefaults.buttonColors(containerColor = StudioSurfaceVariant),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Icon(imageVector = Icons.Default.Tune, contentDescription = null, tint = CineCyan, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Ajustes", fontSize = 11.sp, color = TextPrimary)
            }
        }

        // TRANSIÇÕES
        Button(
            onClick = onOpenTransitions,
            colors = ButtonDefaults.buttonColors(containerColor = StudioSurfaceVariant),
            shape = RoundedCornerShape(8.dp),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, tint = CineCyan, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Transições", fontSize = 11.sp, color = TextPrimary)
        }

        // EFEITOS DE ÁUDIO
        if (hasSelectedAudioClip) {
            Button(
                onClick = onOpenAudioEffects,
                colors = ButtonDefaults.buttonColors(containerColor = CineEmerald.copy(alpha = 0.25f)),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Icon(imageVector = Icons.Default.GraphicEq, contentDescription = null, tint = CineEmerald, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("EQ & Efeitos", fontSize = 11.sp, color = TextPrimary)
            }
        }

        // KEYFRAMES
        if (hasSelectedVideoClip) {
            Button(
                onClick = onOpenKeyframes,
                colors = ButtonDefaults.buttonColors(containerColor = StudioSurfaceVariant),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Icon(imageVector = Icons.Default.Animation, contentDescription = null, tint = CineCyan, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Keyframes", fontSize = 11.sp, color = TextPrimary)
            }
        }

        // ELIMINAR (RIPPLE DELETE)
        if (hasSelectedVideoClip || hasSelectedAudioClip) {
            Button(
                onClick = onDelete,
                colors = ButtonDefaults.buttonColors(containerColor = CineRose.copy(alpha = 0.25f)),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                modifier = Modifier.testTag("toolbar_delete_button")
            ) {
                Icon(imageVector = Icons.Default.DeleteOutline, contentDescription = null, tint = CineRose, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Eliminar", fontSize = 11.sp, color = CineRose)
            }
        }
    }
}
