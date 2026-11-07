package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import com.example.model.SilenceDetectionConfig
import com.example.model.SilenceInterval
import com.example.ui.theme.*
import com.example.util.TimeFormatter

/**
 * Painel completo para Auto-Remoção de Silêncios e Pausas Longas na narração.
 * Permite ajustar a sensibilidade, calibrar a margem de segurança e selecionar
 * exatamente quais as pausas a eliminar.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SilenceRemovalSheet(
    silences: List<SilenceInterval>,
    isDetecting: Boolean,
    currentConfig: SilenceDetectionConfig,
    totalProjectDurationMs: Long,
    onDismiss: () -> Unit,
    onUpdateConfig: (SilenceDetectionConfig) -> Unit,
    onToggleInterval: (String) -> Unit,
    onSelectAll: (Boolean) -> Unit,
    onExecuteRemoval: () -> Unit,
    modifier: Modifier = Modifier
) {
    var thresholdDb by remember { mutableStateOf(currentConfig.thresholdDb) }
    var minDurationMs by remember { mutableStateOf(currentConfig.minDurationMs.toFloat()) }
    var safetyPaddingMs by remember { mutableStateOf(currentConfig.safetyPaddingMs.toFloat()) }
    var applyToAll by remember { mutableStateOf(currentConfig.applyToAllClips) }

    val selectedToRemove = silences.filter { it.isSelectedForRemoval }
    val totalTimeSavedMs = selectedToRemove.sumOf { it.durationMs }
    val newDurationMs = (totalProjectDurationMs - totalTimeSavedMs).coerceAtLeast(0L)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = StudioSurface,
        scrimColor = Color.Black.copy(alpha = 0.65f),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Título e Fechar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(CineCyan.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.GraphicEq,
                            contentDescription = null,
                            tint = CineCyan,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Auto-Remoção de Pausas",
                            color = TextPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Corta silêncios mortos na narração em segundos",
                            color = CineCyanGlow,
                            fontSize = 11.sp
                        )
                    }
                }

                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Fechar", tint = TextSecondary)
                }
            }

            // CARTÃO DE IMPACTO / RESUMO DO TEMPO A POUPAR
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(StudioCard)
                    .border(1.dp, CineCyan.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                    .padding(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "${selectedToRemove.size} de ${silences.size} pausas selecionadas",
                            color = TextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Duração: ${TimeFormatter.formatSimple(totalProjectDurationMs)} ➔ ${TimeFormatter.formatSimple(newDurationMs)}",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }

                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = "-${String.format(java.util.Locale.US, "%.1f", totalTimeSavedMs / 1000f)}s",
                            color = CineEmerald,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = "tempo economizado",
                            color = TextSecondary,
                            fontSize = 10.sp
                        )
                    }
                }
            }

            // CONTROLES DE CALIBRAÇÃO (Sensibilidade, Duração Mínima e Padding)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(StudioSurfaceVariant.copy(alpha = 0.5f))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Limiar / Sensibilidade (dB)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Sensibilidade do Silêncio", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    Text("${thresholdDb.toInt()} dB", color = CineCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Slider(
                    value = thresholdDb,
                    onValueChange = {
                        thresholdDb = it
                        onUpdateConfig(
                            currentConfig.copy(thresholdDb = it, minDurationMs = minDurationMs.toLong(), safetyPaddingMs = safetyPaddingMs.toLong(), applyToAllClips = applyToAll)
                        )
                    },
                    valueRange = -50f..-20f,
                    steps = 5,
                    colors = SliderDefaults.colors(thumbColor = CineCyan, activeTrackColor = CineCyan),
                    modifier = Modifier.testTag("silence_threshold_slider")
                )

                // Duração mínima da pausa
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Duração Mínima da Pausa", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    Text("${minDurationMs.toInt()} ms", color = CineViolet, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Slider(
                    value = minDurationMs,
                    onValueChange = {
                        minDurationMs = it
                        onUpdateConfig(
                            currentConfig.copy(thresholdDb = thresholdDb, minDurationMs = it.toLong(), safetyPaddingMs = safetyPaddingMs.toLong(), applyToAllClips = applyToAll)
                        )
                    },
                    valueRange = 250f..1200f,
                    steps = 8,
                    colors = SliderDefaults.colors(thumbColor = CineViolet, activeTrackColor = CineViolet),
                    modifier = Modifier.testTag("silence_min_duration_slider")
                )

                // Margem de segurança (Padding)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Margem de Segurança (Evita voz decepada)", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    Text("${safetyPaddingMs.toInt()} ms", color = CineAmber, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Slider(
                    value = safetyPaddingMs,
                    onValueChange = {
                        safetyPaddingMs = it
                        onUpdateConfig(
                            currentConfig.copy(thresholdDb = thresholdDb, minDurationMs = minDurationMs.toLong(), safetyPaddingMs = it.toLong(), applyToAllClips = applyToAll)
                        )
                    },
                    valueRange = 20f..180f,
                    steps = 7,
                    colors = SliderDefaults.colors(thumbColor = CineAmber, activeTrackColor = CineAmber)
                )

                // Opção: Todos os clipes vs Selecionado
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Aplicar a toda a Linha do Tempo", color = TextSecondary, fontSize = 12.sp)
                    Switch(
                        checked = applyToAll,
                        onCheckedChange = {
                            applyToAll = it
                            onUpdateConfig(
                                currentConfig.copy(applyToAllClips = it)
                            )
                        }
                    )
                }
            }

            // LISTA DE PAUSAS DETETADAS
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Pausas Detetadas:",
                    color = TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(onClick = { onSelectAll(true) }) {
                        Text("Todas", fontSize = 11.sp, color = CineCyan)
                    }
                    TextButton(onClick = { onSelectAll(false) }) {
                        Text("Nenhuma", fontSize = 11.sp, color = TextSecondary)
                    }
                }
            }

            if (isDetecting) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(90.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = CineCyan, modifier = Modifier.size(28.dp))
                }
            } else if (silences.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(90.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Nenhuma pausa longa detetada com a sensibilidade atual.",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.height(130.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(silences) { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (item.isSelectedForRemoval) StudioCard else StudioSurfaceVariant.copy(alpha = 0.3f))
                                .border(
                                    width = 1.dp,
                                    color = if (item.isSelectedForRemoval) CineCyan.copy(alpha = 0.5f) else StudioBorder.copy(alpha = 0.25f),
                                    shape = RoundedCornerShape(6.dp)
                                )
                                .clickable { onToggleInterval(item.id) }
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Checkbox(
                                    checked = item.isSelectedForRemoval,
                                    onCheckedChange = { onToggleInterval(item.id) },
                                    colors = CheckboxDefaults.colors(checkedColor = CineCyan)
                                )
                                Column {
                                    Text(
                                        text = "${TimeFormatter.formatSimple(item.timelineStartMs)} ➔ ${TimeFormatter.formatSimple(item.timelineEndMs)}",
                                        color = TextPrimary,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = "${item.clipName} • Duração da pausa: ${String.format(java.util.Locale.US, "%.2f", item.durationMs / 1000f)}s",
                                        color = TextSecondary,
                                        fontSize = 10.sp
                                    )
                                }
                            }

                            Icon(
                                imageVector = Icons.Default.ContentCut,
                                contentDescription = null,
                                tint = if (item.isSelectedForRemoval) CineRose else TextTertiary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            // BOTÃO DE EXECUÇÃO PRINCIPAL
            Button(
                onClick = onExecuteRemoval,
                enabled = selectedToRemove.isNotEmpty() && !isDetecting,
                colors = ButtonDefaults.buttonColors(containerColor = CineCyan),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("execute_silence_removal_button")
            ) {
                Icon(
                    imageVector = Icons.Default.AutoFixHigh,
                    contentDescription = null,
                    tint = Color.Black,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Remover Silêncios Marcados (-${String.format(java.util.Locale.US, "%.1f", totalTimeSavedMs / 1000f)}s)",
                    color = Color.Black,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
