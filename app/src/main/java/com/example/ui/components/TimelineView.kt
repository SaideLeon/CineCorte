package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.*
import com.example.ui.theme.*
import com.example.util.TimeFormatter

/**
 * Linha do tempo não linear profissional com múltiplas faixas, zoom por pinça até ao nível do frame,
 * régua de tempo SMPTE, formas de onda em cada clipe de áudio e playhead interativo.
 */
@Composable
fun TimelineView(
    project: Project,
    currentTimeMs: Long,
    zoomFactor: Float,
    selectedVideoClipId: String?,
    selectedAudioClipId: String?,
    onSeek: (Long) -> Unit,
    onZoomChange: (Float) -> Unit,
    onSelectVideoClip: (String?) -> Unit,
    onSelectAudioClip: (String?, String?) -> Unit,
    onTrimVideoClip: (clipId: String, startTrimMs: Long, endTrimMs: Long) -> Unit,
    onSplit: () -> Unit,
    onOpenTransitions: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    // 1 segundo = baseWidth * zoomFactor em dp
    val dpPerSecond = (40f * zoomFactor).coerceIn(15f, 350f)
    val msToDp: (Long) -> Float = { ms -> (ms / 1000f) * dpPerSecond }
    val dpToMs: (Float) -> Long = { dp -> ((dp / dpPerSecond) * 1000f).toLong() }

    val totalDurationMs = project.totalDurationMs.coerceAtLeast(15000L)
    val totalTimelineWidthDp = (msToDp(totalDurationMs) + 300f).dp

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(StudioSurface)
            .border(width = 1.dp, color = StudioBorder.copy(alpha = 0.4f))
            .pointerInput(zoomFactor) {
                detectTransformGestures { _, _, zoom, _ ->
                    if (zoom != 1f) {
                        onZoomChange(zoomFactor * zoom)
                    }
                }
            }
    ) {
        // Régua superior de tempo e cabeçalho da timeline
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(28.dp)
                .background(StudioSurfaceVariant)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .horizontalScroll(scrollState)
            ) {
                TimelineRuler(
                    totalDurationMs = totalDurationMs,
                    dpPerSecond = dpPerSecond,
                    widthDp = totalTimelineWidthDp,
                    fps = project.canvasFps,
                    onSeek = onSeek
                )
            }
        }

        // Área rolável de faixas (Vídeo + Múltiplos Áudios)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = false)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(scrollState)
                    .pointerInput(Unit) {
                        detectTapGestures { offset ->
                            val clickedMs = dpToMs(offset.x)
                            onSeek(clickedMs)
                        }
                    }
            ) {
                Box(
                    modifier = Modifier
                        .width(totalTimelineWidthDp)
                        .padding(vertical = 6.dp)
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // FAIXA DE VÍDEO PRINCIPAL
                        VideoTrackRow(
                            clips = project.videoClips,
                            selectedClipId = selectedVideoClipId,
                            msToDp = msToDp,
                            onSelectClip = onSelectVideoClip,
                            onOpenTransitions = onOpenTransitions
                        )

                        // FAIXAS DE ÁUDIO INDEPENDENTES
                        project.audioTracks.forEach { track ->
                            AudioTrackRow(
                                track = track,
                                selectedClipId = selectedAudioClipId,
                                msToDp = msToDp,
                                onSelectClip = { clipId -> onSelectAudioClip(clipId, track.id) }
                            )
                        }
                    }

                    // PLAYHEAD (Cursor vermelho vertical em tempo real)
                    val playheadOffsetDp = msToDp(currentTimeMs).dp
                    PlayheadIndicator(
                        offsetDp = playheadOffsetDp,
                        modifier = Modifier
                            .fillMaxHeight()
                            .pointerInput(dpPerSecond) {
                                detectDragGestures { change, dragAmount ->
                                    change.consume()
                                    val deltaMs = dpToMs(dragAmount.x)
                                    onSeek(currentTimeMs + deltaMs)
                                }
                            }
                    )
                }
            }
        }
    }
}

/**
 * Régua com marcas de tempo (segundos e quadros).
 */
@Composable
private fun TimelineRuler(
    totalDurationMs: Long,
    dpPerSecond: Float,
    widthDp: androidx.compose.ui.unit.Dp,
    fps: Float,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier
            .width(widthDp)
            .fillMaxHeight()
            .pointerInput(dpPerSecond) {
                detectTapGestures { offset ->
                    val clickedMs = ((offset.x / dpPerSecond) * 1000f).toLong()
                    onSeek(clickedMs)
                }
            }
    ) {
        val totalSeconds = (totalDurationMs / 1000).toInt() + 5
        val stepSec = when {
            dpPerSecond > 180f -> 1 // zoom detalhado ao frame
            dpPerSecond > 60f -> 2
            dpPerSecond > 30f -> 5
            else -> 10
        }

        for (sec in 0..totalSeconds step stepSec) {
            val x = (sec * dpPerSecond)
            val isMajor = sec % (stepSec * 2) == 0
            val lineH = if (isMajor) size.height * 0.65f else size.height * 0.35f

            drawLine(
                color = if (isMajor) CineCyan else StudioBorder,
                start = Offset(x, size.height - lineH),
                end = Offset(x, size.height),
                strokeWidth = if (isMajor) 2f else 1f
            )
        }
    }
}

/**
 * Faixa de vídeo contendo os clipes com bordas de seleção e marcadores de transição.
 */
@Composable
private fun VideoTrackRow(
    clips: List<VideoClip>,
    selectedClipId: String?,
    msToDp: (Long) -> Float,
    onSelectClip: (String?) -> Unit,
    onOpenTransitions: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp)
            .background(StudioCard, RoundedCornerShape(6.dp))
            .border(1.dp, StudioBorder.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
            .padding(2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (clips.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    text = "Faixa de Vídeo Principal (V1) - Vazia",
                    color = TextTertiary,
                    fontSize = 11.sp
                )
            }
        } else {
            clips.forEachIndexed { index, clip ->
                val clipWidth = msToDp(clip.trimmedDurationMs).coerceAtLeast(32f).dp
                val isSelected = clip.id == selectedClipId

                Box(
                    modifier = Modifier
                        .width(clipWidth)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (isSelected) CineCyan.copy(alpha = 0.35f) else TrackVideoColor.copy(alpha = 0.7f))
                        .border(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) CineCyan else TrackVideoColor,
                            shape = RoundedCornerShape(4.dp)
                        )
                        .clickable { onSelectClip(clip.id) }
                        .padding(horizontal = 6.dp, vertical = 4.dp)
                        .testTag("video_clip_${clip.id}")
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Videocam,
                                contentDescription = null,
                                tint = TextPrimary,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = clip.name,
                                color = TextPrimary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${TimeFormatter.formatSimple(clip.trimmedDurationMs)} (${clip.speed}x)",
                                color = TextSecondary,
                                fontSize = 9.sp
                            )
                            if (clip.isMuted) {
                                Icon(
                                    imageVector = Icons.Default.VolumeOff,
                                    contentDescription = "Silenciado",
                                    tint = CineRose,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }
                    }

                    // Botãozinho de transição entre clipes
                    if (index < clips.size - 1) {
                        IconButton(
                            onClick = onOpenTransitions,
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .size(20.dp)
                                .background(StudioSurface, RoundedCornerShape(10.dp))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Transform,
                                contentDescription = "Transição",
                                tint = CineCyan,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(2.dp))
            }
        }
    }
}

/**
 * Faixa de áudio independente com visualização de waveform e etiqueta de tipo.
 */
@Composable
private fun AudioTrackRow(
    track: AudioTrack,
    selectedClipId: String?,
    msToDp: (Long) -> Float,
    onSelectClip: (String) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .background(StudioCard, RoundedCornerShape(6.dp))
            .border(1.dp, StudioBorder.copy(alpha = 0.35f), RoundedCornerShape(6.dp))
            .padding(2.dp)
    ) {
        // Clipes na faixa
        track.clips.forEach { clip ->
            val startDp = msToDp(clip.timelineStartMs).dp
            val clipWidth = msToDp(clip.durationMs).coerceAtLeast(30f).dp
            val isSelected = clip.id == selectedClipId

            Box(
                modifier = Modifier
                    .offset(x = startDp)
                    .width(clipWidth)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(4.dp))
                    .background(track.type.composeColor.copy(alpha = if (isSelected) 0.5f else 0.25f))
                    .border(
                        width = if (isSelected) 2.dp else 1.dp,
                        color = if (isSelected) Color.White else track.type.composeColor,
                        shape = RoundedCornerShape(4.dp)
                    )
                    .clickable { onSelectClip(clip.id) }
                    .padding(horizontal = 4.dp, vertical = 2.dp)
                    .testTag("audio_clip_${clip.id}")
            ) {
                // Desenha a forma de onda do áudio
                AudioWaveformCanvas(
                    points = clip.waveformPoints,
                    color = track.type.composeColor,
                    modifier = Modifier.fillMaxSize()
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.align(Alignment.TopStart)
                ) {
                    Text(
                        text = clip.name,
                        color = TextPrimary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1
                    )
                }

                Text(
                    text = "${(clip.volume * 100).toInt()}%",
                    color = TextSecondary,
                    fontSize = 8.sp,
                    modifier = Modifier.align(Alignment.BottomEnd)
                )
            }
        }
    }
}

/**
 * Desenho de forma de onda estéreo/mono normalizada.
 */
@Composable
fun AudioWaveformCanvas(
    points: List<Float>,
    color: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        if (points.isEmpty()) return@Canvas
        val midY = size.height / 2f
        val stepX = size.width / points.size.toFloat().coerceAtLeast(1f)

        points.forEachIndexed { i, amp ->
            val x = i * stepX
            val barH = (midY * amp * 0.85f).coerceAtLeast(2f)
            drawLine(
                color = color.copy(alpha = 0.85f),
                start = Offset(x, midY - barH),
                end = Offset(x, midY + barH),
                strokeWidth = (stepX * 0.6f).coerceIn(1.5f, 4f)
            )
        }
    }
}

/**
 * Playhead (cursor de reprodução vertical).
 */
@Composable
private fun PlayheadIndicator(
    offsetDp: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.offset(x = offsetDp)
    ) {
        // Cabeça triangular do playhead no topo
        Canvas(
            modifier = Modifier
                .size(14.dp, 12.dp)
                .align(Alignment.TopCenter)
        ) {
            val path = Path().apply {
                moveTo(0f, 0f)
                lineTo(size.width, 0f)
                lineTo(size.width / 2f, size.height)
                close()
            }
            drawPath(path, CineRose)
        }

        // Linha vertical do playhead cruzando todas as faixas
        Box(
            modifier = Modifier
                .width(2.dp)
                .fillMaxHeight()
                .background(CineRose)
                .align(Alignment.TopCenter)
        )
    }
}
