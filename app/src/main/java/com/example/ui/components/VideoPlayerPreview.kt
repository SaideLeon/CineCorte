package com.example.ui.components

import android.net.Uri
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.example.model.Project
import com.example.model.TransitionType
import com.example.model.VideoClip
import com.example.ui.theme.*
import com.example.util.TimeFormatter

/**
 * Área de pré-visualização de vídeo com preservação 100% estrita das dimensões originais,
 * suporte a ExoPlayer da Media3, gestos de pinça/arrastar para reposicionamento e VU meter.
 */
@OptIn(UnstableApi::class)
@Composable
fun VideoPlayerPreview(
    project: Project,
    currentTimeMs: Long,
    isPlaying: Boolean,
    selectedClipId: String?,
    onPlayPause: () -> Unit,
    onStepFrame: (Boolean) -> Unit,
    onTransformUpdate: (clipId: String, scale: Float, offsetX: Float, offsetY: Float, rotation: Float) -> Unit,
    onResetTransform: (clipId: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // Determina o clipe ativo na linha do tempo para o tempo atual
    val currentClipPair = remember(project.videoClips, currentTimeMs) {
        project.findVideoClipAt(currentTimeMs)
    }
    val activeClip = currentClipPair?.first
    val clipInternalOffsetMs = currentClipPair?.second ?: 0L

    // Cálculo das transformações interpoladas (keyframes)
    val transform = remember(activeClip, clipInternalOffsetMs) {
        activeClip?.interpolatedTransform(clipInternalOffsetMs)
            ?: Triple(1.0f, Pair(0.0f, 0.0f), 0.0f)
    }
    val currentScale = transform.first
    val currentOffsetX = transform.second.first
    val currentOffsetY = transform.second.second
    val currentRotation = transform.third

    // Proporção estrita original da área de edição
    val aspectRatio = project.canvasAspectRatio

    // Configuração do ExoPlayer para pré-visualização contínua
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            repeatMode = Player.REPEAT_MODE_OFF
            playWhenReady = false
        }
    }

    // Atualiza a fonte de mídia no ExoPlayer quando o clipe ativo muda
    LaunchedEffect(activeClip?.sourceUri) {
        if (activeClip != null && activeClip.sourceUri.isNotBlank()) {
            try {
                val mediaItem = MediaItem.fromUri(Uri.parse(activeClip.sourceUri))
                exoPlayer.setMediaItem(mediaItem)
                exoPlayer.prepare()
                exoPlayer.seekTo(clipInternalOffsetMs)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } else {
            exoPlayer.clearMediaItems()
        }
    }

    // Sincroniza reprodução e seek
    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            exoPlayer.play()
        } else {
            exoPlayer.pause()
        }
    }

    LaunchedEffect(currentTimeMs) {
        if (!isPlaying && activeClip != null) {
            exoPlayer.seekTo(clipInternalOffsetMs)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(StudioDarkBg),
        contentAlignment = Alignment.Center
    ) {
        // Área de edição estrita com a proporção exata original (sem barras pretas forçadas nem cortes)
        Box(
            modifier = Modifier
                .fillMaxHeight(0.92f)
                .aspectRatio(aspectRatio, matchHeightConstraintsFirst = true)
                .clip(RoundedCornerShape(8.dp))
                .background(Color.Black)
                .border(1.dp, StudioBorder.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                .pointerInput(activeClip?.id) {
                    if (activeClip != null) {
                        detectTransformGestures { _, pan, zoom, rotationChange ->
                            val newScale = (currentScale * zoom).coerceIn(0.2f, 4.0f)
                            val newOffsetX = currentOffsetX + pan.x * 0.002f
                            val newOffsetY = currentOffsetY + pan.y * 0.002f
                            val newRot = currentRotation + rotationChange
                            onTransformUpdate(activeClip.id, newScale, newOffsetX, newOffsetY, newRot)
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            if (activeClip != null) {
                // Conteúdo do clipe de vídeo com transformações de escala, pan e rotação
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .offset(x = (currentOffsetX * 200).dp, y = (currentOffsetY * 200).dp)
                        .scale(currentScale)
                        .rotate(currentRotation),
                    contentAlignment = Alignment.Center
                ) {
                    // Player de vídeo Media3 ExoPlayer nativo
                    AndroidView(
                        factory = { ctx ->
                            PlayerView(ctx).apply {
                                player = exoPlayer
                                useController = false
                                setShutterBackgroundColor(android.graphics.Color.TRANSPARENT)
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )

                    // Overlay de transição ativa
                    val transition = activeClip.transitionIn
                    if (transition.type != TransitionType.NONE && clipInternalOffsetMs < transition.durationMs) {
                        val progress = (clipInternalOffsetMs.toFloat() / transition.durationMs.toFloat()).coerceIn(0f, 1f)
                        TransitionVisualOverlay(type = transition.type, progress = progress)
                    }
                }
            } else {
                // Estado vazio quando não há vídeo importado
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MovieCreation,
                        contentDescription = "Sem mídia",
                        tint = StudioBorder,
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Nenhum vídeo na linha do tempo",
                        color = TextSecondary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Importa um vídeo para iniciar a edição",
                        color = TextTertiary,
                        fontSize = 12.sp
                    )
                }
            }

            // Emblema informativo da resolução original preservada no topo esquerdo
            Row(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(StudioSurface.copy(alpha = 0.85f))
                    .padding(horizontal = 6.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(CineCyan)
                )
                Text(
                    text = "${project.canvasWidth}x${project.canvasHeight} (${project.canvasAspectRatioLabel}) • ${project.canvasFps.toInt()}fps",
                    color = TextPrimary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // VU Meter e botão de repor transformações no topo direito
            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (activeClip != null && (currentScale != 1f || currentOffsetX != 0f || currentOffsetY != 0f || currentRotation != 0f)) {
                    IconButton(
                        onClick = { onResetTransform(activeClip.id) },
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(StudioSurface.copy(alpha = 0.85f))
                            .testTag("reset_transform_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Repor posição",
                            tint = CineCyan,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                VuMeter(isPlaying = isPlaying)
            }

            // Barra inferior de controle de transporte dentro da pré-visualização
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(StudioSurface.copy(alpha = 0.75f))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Timecode profissional
                Text(
                    text = TimeFormatter.formatTimecode(currentTimeMs, project.canvasFps),
                    color = CineCyanGlow,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.testTag("timecode_display")
                )

                // Controles de transporte
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Frame anterior (-1 frame)
                    IconButton(
                        onClick = { onStepFrame(false) },
                        modifier = Modifier.size(32.dp).testTag("step_backward_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipPrevious,
                            contentDescription = "Voltar 1 quadro",
                            tint = TextPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Play / Pause principal
                    FilledIconButton(
                        onClick = onPlayPause,
                        colors = IconButtonDefaults.filledIconButtonColors(containerColor = CineCyan),
                        modifier = Modifier.size(40.dp).testTag("play_pause_button")
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pausar" else "Reproduzir",
                            tint = Color.Black,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    // Próximo frame (+1 frame)
                    IconButton(
                        onClick = { onStepFrame(true) },
                        modifier = Modifier.size(32.dp).testTag("step_forward_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipNext,
                            contentDescription = "Avançar 1 quadro",
                            tint = TextPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Duração total
                Text(
                    text = TimeFormatter.formatSimple(project.totalDurationMs),
                    color = TextSecondary,
                    fontSize = 12.sp
                )
            }
        }
    }
}

/**
 * Overlay para simulação imediata de transições no player.
 */
@Composable
private fun TransitionVisualOverlay(
    type: TransitionType,
    progress: Float
) {
    val alpha = (1f - progress).coerceIn(0f, 1f)
    when (type) {
        TransitionType.FADE_BLACK -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = alpha))
            )
        }
        TransitionType.FLASH_WHITE -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White.copy(alpha = alpha * 0.8f))
            )
        }
        TransitionType.DISSOLVE -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(StudioDarkBg.copy(alpha = alpha * 0.6f))
            )
        }
        TransitionType.ZOOM_IN -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .scale(1f + (1f - progress) * 0.3f)
            )
        }
        else -> {}
    }
}
