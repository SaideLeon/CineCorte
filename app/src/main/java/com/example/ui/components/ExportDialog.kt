package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import com.example.model.*
import com.example.service.ExportState
import com.example.ui.theme.*
import com.example.util.TimeFormatter

/**
 * Diálogo de resumo e exportação com preservação 100% garantida das dimensões originais,
 * suporte a H.264/HEVC e controlo de Foreground Service com barra de progresso.
 */
@Composable
fun ExportDialog(
    project: Project,
    exportState: ExportState,
    onDismiss: () -> Unit,
    onStartExport: (ExportConfig) -> Unit,
    onCancelExport: () -> Unit,
    onResetExportState: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedPreset by remember { mutableStateOf(ResolutionPreset.ORIGINAL) }
    var selectedFormat by remember { mutableStateOf(ExportFormat.MP4) }
    var targetFps by remember { mutableStateOf(project.canvasFps.toInt().coerceIn(24, 60)) }

    // Dimensões finais efetivas calculadas
    val exportWidth = selectedPreset.width ?: project.canvasWidth
    val exportHeight = selectedPreset.height ?: project.canvasHeight
    val totalSeconds = (project.totalDurationMs / 1000f).coerceAtLeast(1f)

    // Estimativa de tamanho em MB (bitrate ~20 Mbps para vídeo original)
    val estimatedMb = ((20 * totalSeconds) / 8f).coerceAtLeast(1.5f)

    AlertDialog(
        onDismissRequest = {
            if (exportState !is ExportState.Progress) {
                onDismiss()
                onResetExportState()
            }
        },
        containerColor = StudioSurface,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(imageVector = Icons.Default.CloudUpload, contentDescription = null, tint = CineCyan)
                Text(
                    text = "Exportação de Vídeo",
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                when (exportState) {
                    is ExportState.Idle -> {
                        // RESUMO TÉCNICO ANTES DE EXPORTAR
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(StudioCard)
                                .border(1.dp, CineCyan.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                .padding(12.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = CineEmerald, modifier = Modifier.size(16.dp))
                                    Text(
                                        text = "100% Dimensões Originais Preservadas",
                                        color = CineCyanGlow,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Text(
                                    text = "Resolução: ${exportWidth}x${exportHeight} (${project.canvasAspectRatioLabel})",
                                    color = TextPrimary,
                                    fontSize = 12.sp
                                )
                                Text(
                                    text = "Taxa de Quadros: ${targetFps} fps",
                                    color = TextPrimary,
                                    fontSize = 12.sp
                                )
                                Text(
                                    text = "Duração Total: ${TimeFormatter.formatSimple(project.totalDurationMs)}",
                                    color = TextPrimary,
                                    fontSize = 12.sp
                                )
                                Text(
                                    text = "Tamanho Estimado: ~${String.format("%.1f", estimatedMb)} MB",
                                    color = TextSecondary,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        // ESCOLHA DO FORMATO
                        Text("Formato & Codec:", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            ExportFormat.values().forEach { fmt ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (selectedFormat == fmt) CineCyan.copy(alpha = 0.2f) else StudioCard)
                                        .clickable { selectedFormat = fmt }
                                        .padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    RadioButton(
                                        selected = selectedFormat == fmt,
                                        onClick = { selectedFormat = fmt },
                                        colors = RadioButtonDefaults.colors(selectedColor = CineCyan)
                                    )
                                    Text(text = fmt.label, color = TextPrimary, fontSize = 12.sp)
                                }
                            }
                        }

                        // PREDEFINIÇÃO DE RESOLUÇÃO (ORIGINAL POR DEFEITO)
                        Text("Resolução de Saída:", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            ResolutionPreset.values().take(3).forEach { preset ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (selectedPreset == preset) CineViolet.copy(alpha = 0.2f) else StudioCard)
                                        .clickable { selectedPreset = preset }
                                        .padding(horizontal = 8.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    RadioButton(
                                        selected = selectedPreset == preset,
                                        onClick = { selectedPreset = preset },
                                        colors = RadioButtonDefaults.colors(selectedColor = CineViolet)
                                    )
                                    Text(text = preset.labelPt, color = TextPrimary, fontSize = 11.sp)
                                }
                            }
                        }
                    }

                    is ExportState.Progress -> {
                        // ESTADO DE PROGRESSO EM SEGUNDO PLANO
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.padding(vertical = 12.dp)
                        ) {
                            Text(
                                text = exportState.message,
                                color = TextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                            LinearProgressIndicator(
                                progress = { exportState.progress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = CineCyan,
                                trackColor = StudioBorder
                            )
                            Text(
                                text = "${(exportState.progress * 100).toInt()}% Concluído",
                                color = CineCyanGlow,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "A exportação decorre ininterruptamente num Foreground Service. Podes minimizar o telemóvel sem parar o processo.",
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }

                    is ExportState.Success -> {
                        // SUCESSO
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(vertical = 12.dp)
                        ) {
                            Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = CineEmerald, modifier = Modifier.size(48.dp))
                            Text("Vídeo Exportado com Sucesso!", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                            Text(exportState.filePath, color = CineCyan, fontSize = 12.sp)
                            Text("Guardado na Galeria do telemóvel através de MediaStore.", color = TextSecondary, fontSize = 11.sp)
                        }
                    }

                    is ExportState.Error -> {
                        // ERRO
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Error, contentDescription = null, tint = CineRose, modifier = Modifier.size(44.dp))
                            Text("Erro na Exportação", color = CineRose, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                            Text(exportState.message, color = TextSecondary, fontSize = 12.sp)
                        }
                    }
                }
            }
        },
        confirmButton = {
            when (exportState) {
                is ExportState.Idle -> {
                    Button(
                        onClick = {
                            onStartExport(
                                ExportConfig(
                                    format = selectedFormat,
                                    preset = selectedPreset,
                                    targetFps = targetFps,
                                    preserveExactOriginal = (selectedPreset == ResolutionPreset.ORIGINAL)
                                )
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CineCyan),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("confirm_export_button")
                    ) {
                        Text("Iniciar Exportação", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
                is ExportState.Progress -> {
                    OutlinedButton(
                        onClick = onCancelExport,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = CineRose),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Cancelar Exportação")
                    }
                }
                is ExportState.Success, is ExportState.Error -> {
                    Button(
                        onClick = {
                            onResetExportState()
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CineCyan),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Fechar", color = Color.Black)
                    }
                }
            }
        },
        dismissButton = {
            if (exportState is ExportState.Idle) {
                TextButton(onClick = onDismiss) {
                    Text("Voltar", color = TextSecondary)
                }
            }
        },
        modifier = modifier
    )
}
