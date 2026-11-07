package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.VideoClip
import com.example.ui.theme.*
import com.example.util.TimeFormatter

/**
 * Editor de pontos-chave (Keyframes) para interpolação dinâmica de escala, panorâmica e rotação.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KeyframeEditorSheet(
    clip: VideoClip,
    currentTimeMs: Long,
    onDismiss: () -> Unit,
    onAddKeyframe: (scale: Float, offsetX: Float, offsetY: Float, rotation: Float) -> Unit,
    onRemoveKeyframe: (keyframeId: String) -> Unit,
    onResetTransform: () -> Unit,
    modifier: Modifier = Modifier
) {
    var scale by remember { mutableStateOf(clip.scale) }
    var offsetX by remember { mutableStateOf(clip.offsetX) }
    var offsetY by remember { mutableStateOf(clip.offsetY) }
    var rotation by remember { mutableStateOf(clip.rotation) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = StudioSurface,
        scrimColor = Color.Black.copy(alpha = 0.6f),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Animação por Keyframes",
                        color = TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Zoom progressivo e movimentos sem alterar a resolução final",
                        color = CineCyan,
                        fontSize = 12.sp
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Fechar", tint = TextSecondary)
                }
            }

            // CONTROLES DE ESCALA, DESLOCAMENTO E ROTAÇÃO
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // Escala / Zoom (0.5x a 3.0x)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Zoom / Escala", color = TextSecondary, fontSize = 12.sp)
                    Text("${String.format("%.2f", scale)}x", color = CineCyanGlow, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Slider(
                    value = scale,
                    onValueChange = { scale = it },
                    valueRange = 0.5f..3.0f,
                    colors = SliderDefaults.colors(thumbColor = CineCyan, activeTrackColor = CineCyan)
                )

                // Posição X
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Posição Horizontal (X)", color = TextSecondary, fontSize = 12.sp)
                    Text("${(offsetX * 100).toInt()}%", color = TextPrimary, fontSize = 12.sp)
                }
                Slider(
                    value = offsetX,
                    onValueChange = { offsetX = it },
                    valueRange = -1.0f..1.0f,
                    colors = SliderDefaults.colors(thumbColor = CineCyan, activeTrackColor = CineCyan)
                )

                // Posição Y
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Posição Vertical (Y)", color = TextSecondary, fontSize = 12.sp)
                    Text("${(offsetY * 100).toInt()}%", color = TextPrimary, fontSize = 12.sp)
                }
                Slider(
                    value = offsetY,
                    onValueChange = { offsetY = it },
                    valueRange = -1.0f..1.0f,
                    colors = SliderDefaults.colors(thumbColor = CineCyan, activeTrackColor = CineCyan)
                )

                // Rotação
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Rotação", color = TextSecondary, fontSize = 12.sp)
                    Text("${rotation.toInt()}°", color = CineAmber, fontSize = 12.sp)
                }
                Slider(
                    value = rotation,
                    onValueChange = { rotation = it },
                    valueRange = -180f..180f,
                    colors = SliderDefaults.colors(thumbColor = CineAmber, activeTrackColor = CineAmber)
                )
            }

            // BOTÕES: ADICIONAR KEYFRAME E REPOR
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        scale = 1.0f
                        offsetX = 0.0f
                        offsetY = 0.0f
                        rotation = 0.0f
                        onResetTransform()
                    },
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Repor", fontSize = 12.sp)
                }

                Button(
                    onClick = {
                        onAddKeyframe(scale, offsetX, offsetY, rotation)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CineCyan),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(2f).testTag("add_keyframe_button")
                ) {
                    Icon(imageVector = Icons.Default.AddLocation, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Marcar Ponto-Chave Aqui", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }

            // LISTA DE KEYFRAMES EXISTENTES
            Text("Pontos-Chave Registados (${clip.keyframes.size}):", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)

            LazyColumn(
                modifier = Modifier.height(130.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (clip.keyframes.isEmpty()) {
                    item {
                        Text(
                            text = "Nenhum ponto-chave marcado. Ajusta a escala/posição e clica no botão acima.",
                            color = TextTertiary,
                            fontSize = 11.sp
                        )
                    }
                } else {
                    items(clip.keyframes.sortedBy { it.timeOffsetMs }) { kf ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(StudioCard)
                                .border(1.dp, StudioBorder, RoundedCornerShape(6.dp))
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "Offset: ${TimeFormatter.formatSimple(kf.timeOffsetMs)}",
                                    color = CineCyan,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Zoom: ${String.format("%.2f", kf.scale)}x | Rot: ${kf.rotationDegrees.toInt()}° | X:${(kf.offsetX * 100).toInt()}% Y:${(kf.offsetY * 100).toInt()}%",
                                    color = TextSecondary,
                                    fontSize = 10.sp
                                )
                            }
                            IconButton(onClick = { onRemoveKeyframe(kf.id) }) {
                                Icon(imageVector = Icons.Default.Delete, contentDescription = "Remover", tint = CineRose, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
