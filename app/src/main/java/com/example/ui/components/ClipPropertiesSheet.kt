package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.VideoClip
import com.example.ui.theme.*
import com.example.util.TimeFormatter

/**
 * Painel de propriedades e ações do clipe selecionado (velocidade, aparar, separar áudio, silenciar).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClipPropertiesSheet(
    clip: VideoClip,
    onDismiss: () -> Unit,
    onUpdateSpeed: (Float) -> Unit,
    onUpdateVolume: (Float) -> Unit,
    onToggleMute: () -> Unit,
    onSeparateAudio: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
    onResetTransform: () -> Unit,
    onOpenKeyframes: () -> Unit,
    onOpenTransitions: () -> Unit,
    onOpenSilenceRemoval: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = StudioSurface,
        scrimColor = Color.Black.copy(alpha = 0.6f),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Título e identificação do clipe
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = clip.name,
                        color = TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Original: ${clip.metadata.displayWidth}x${clip.metadata.displayHeight} • ${clip.metadata.aspectRatioLabel}",
                        color = CineCyan,
                        fontSize = 12.sp
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Fechar", tint = TextSecondary)
                }
            }

            HorizontalDivider(color = StudioBorder.copy(alpha = 0.5f))

            // Ações rápidas principais: Separar Áudio, Duplicar, Silenciar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Separar Áudio
                Button(
                    onClick = {
                        onSeparateAudio()
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CineViolet),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f).testTag("separate_audio_button")
                ) {
                    Icon(imageVector = Icons.Default.CallSplit, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "Separar Áudio", fontSize = 12.sp)
                }

                // Silenciar / Ativar Som
                OutlinedButton(
                    onClick = onToggleMute,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = if (clip.isMuted) CineRose else CineCyan
                    ),
                    modifier = Modifier.weight(1f).testTag("toggle_mute_button")
                ) {
                    Icon(
                        imageVector = if (clip.isMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = if (clip.isMuted) "Silenciado" else "Com Som", fontSize = 12.sp)
                }
            }

            // Auto-Remover Pausas da Narração
            OutlinedButton(
                onClick = {
                    onDismiss()
                    onOpenSilenceRemoval()
                },
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = CineCyan),
                modifier = Modifier.fillMaxWidth().testTag("clip_silence_removal_button")
            ) {
                Icon(imageVector = Icons.Default.GraphicEq, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = "Auto-Remover Pausas & Silêncios", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }

            // CONTROLE DE VELOCIDADE (0.25x a 4.0x)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Velocidade de Reprodução", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    Text(text = "${String.format("%.2f", clip.speed)}x", color = CineCyanGlow, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
                Slider(
                    value = clip.speed,
                    onValueChange = onUpdateSpeed,
                    valueRange = 0.25f..4.0f,
                    steps = 14,
                    colors = SliderDefaults.colors(
                        thumbColor = CineCyan,
                        activeTrackColor = CineCyan,
                        inactiveTrackColor = StudioBorder
                    ),
                    modifier = Modifier.testTag("speed_slider")
                )
                // Chips de velocidade rápida
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    listOf(0.5f, 1.0f, 1.5f, 2.0f).forEach { spd ->
                        FilterChip(
                            selected = clip.speed == spd,
                            onClick = { onUpdateSpeed(spd) },
                            label = { Text("${spd}x", fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = CineCyan.copy(alpha = 0.25f),
                                selectedLabelColor = CineCyanGlow
                            )
                        )
                    }
                }
            }

            // CONTROLE DE VOLUME DO CLIPE (0% a 200%)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Volume do Clipe", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    Text(text = "${(clip.volume * 100).toInt()}%", color = CineEmerald, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
                Slider(
                    value = clip.volume,
                    onValueChange = onUpdateVolume,
                    valueRange = 0.0f..2.0f,
                    colors = SliderDefaults.colors(
                        thumbColor = CineEmerald,
                        activeTrackColor = CineEmerald,
                        inactiveTrackColor = StudioBorder
                    )
                )
            }

            // REPOSICIONAMENTO, KEYFRAMES E TRANSIÇÕES
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        onDismiss()
                        onOpenKeyframes()
                    },
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(imageVector = Icons.Default.Animation, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "Keyframes (${clip.keyframes.size})", fontSize = 12.sp)
                }

                OutlinedButton(
                    onClick = {
                        onDismiss()
                        onOpenTransitions()
                    },
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "Transição", fontSize = 12.sp)
                }
            }

            // DUPLICAR, REPOR E ELIMINAR
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onResetTransform,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Repor", fontSize = 12.sp)
                }

                OutlinedButton(
                    onClick = {
                        onDuplicate()
                        onDismiss()
                    },
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Duplicar", fontSize = 12.sp)
                }

                Button(
                    onClick = {
                        onDelete()
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CineRose),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f).testTag("delete_clip_button")
                ) {
                    Icon(imageVector = Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Eliminar", fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
