package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.*
import com.example.ui.theme.*
import com.example.util.TimeFormatter

/**
 * Assistente de Corte Automático em 2 Etapas:
 * - Etapa A: Deteção e remoção de pausas/silêncios 100% offline
 * - Etapa B: Transcrição Whisper na Groq e remoção determinística de repetições/gaguez
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutoCutWizardSheet(
    currentStep: AutoCutStep,
    onStepChange: (AutoCutStep) -> Unit,
    // Estado Etapa A (Silêncios)
    silences: List<SilenceInterval>,
    isDetectingSilences: Boolean,
    silenceConfig: SilenceDetectionConfig,
    totalProjectDurationMs: Long,
    onUpdateSilenceConfig: (SilenceDetectionConfig) -> Unit,
    onToggleSilenceInterval: (String) -> Unit,
    onSelectAllSilences: (Boolean) -> Unit,
    onExecuteSilenceRemoval: () -> Unit,
    // Estado Etapa B (Groq Whisper & Repetições)
    hasGroqApiKey: Boolean,
    hasAcceptedConsent: Boolean,
    onAcceptConsent: () -> Unit,
    onOpenSettings: () -> Unit,
    isTranscribing: Boolean,
    transcriptionProgressText: String,
    repetitionSuggestions: List<RepetitionCutSuggestion>,
    repetitionConfig: RepetitionDetectionConfig,
    onUpdateRepetitionConfig: (RepetitionDetectionConfig) -> Unit,
    onStartTranscription: () -> Unit,
    onToggleRepetitionSuggestion: (String) -> Unit,
    onSelectAllRepetitions: (Boolean) -> Unit,
    onPreviewCutAudio: (Long, Long) -> Unit,
    onExecuteRepetitionCuts: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = StudioSurface,
        scrimColor = Color.Black.copy(alpha = 0.65f),
        modifier = modifier.fillMaxHeight(0.92f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 6.dp)
        ) {
            // Cabeçalho Principal do Assistente
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
                            .size(34.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(CineCyan.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoFixHigh,
                            contentDescription = null,
                            tint = CineCyan,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Assistente de Corte Automático",
                            color = TextPrimary,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Otimiza a tua narração eliminando silêncios e gaguez",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                }

                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Fechar", tint = TextSecondary)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Seletor de Etapas (Tabs)
            TabRow(
                selectedTabIndex = if (currentStep == AutoCutStep.STEP_A_SILENCE) 0 else 1,
                containerColor = StudioSurfaceVariant,
                contentColor = CineCyan,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
            ) {
                Tab(
                    selected = currentStep == AutoCutStep.STEP_A_SILENCE,
                    onClick = { onStepChange(AutoCutStep.STEP_A_SILENCE) },
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(imageVector = Icons.Default.GraphicEq, contentDescription = null, modifier = Modifier.size(15.dp))
                            Text("1. Silêncios (Offline)", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                )
                Tab(
                    selected = currentStep == AutoCutStep.STEP_B_REPETITIONS,
                    onClick = { onStepChange(AutoCutStep.STEP_B_REPETITIONS) },
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Psychology, contentDescription = null, modifier = Modifier.size(15.dp))
                            Text("2. Repetições & IA", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Conteúdo específico da etapa selecionada
            when (currentStep) {
                AutoCutStep.STEP_A_SILENCE -> {
                    StepASilenceContent(
                        silences = silences,
                        isDetecting = isDetectingSilences,
                        silenceConfig = silenceConfig,
                        totalProjectDurationMs = totalProjectDurationMs,
                        onUpdateConfig = onUpdateSilenceConfig,
                        onToggleInterval = onToggleSilenceInterval,
                        onSelectAll = onSelectAllSilences,
                        onExecuteRemoval = onExecuteSilenceRemoval,
                        onNextStep = { onStepChange(AutoCutStep.STEP_B_REPETITIONS) }
                    )
                }
                AutoCutStep.STEP_B_REPETITIONS -> {
                    StepBRepetitionsContent(
                        hasGroqApiKey = hasGroqApiKey,
                        hasAcceptedConsent = hasAcceptedConsent,
                        onAcceptConsent = onAcceptConsent,
                        onOpenSettings = onOpenSettings,
                        isTranscribing = isTranscribing,
                        progressText = transcriptionProgressText,
                        suggestions = repetitionSuggestions,
                        config = repetitionConfig,
                        onUpdateConfig = onUpdateRepetitionConfig,
                        onStartTranscription = onStartTranscription,
                        onToggleSuggestion = onToggleRepetitionSuggestion,
                        onSelectAll = onSelectAllRepetitions,
                        onPreviewCutAudio = onPreviewCutAudio,
                        onExecuteCuts = onExecuteRepetitionCuts
                    )
                }
            }
        }
    }
}

/**
 * Conteúdo da Etapa A: Silêncios e Pausas
 */
@Composable
private fun StepASilenceContent(
    silences: List<SilenceInterval>,
    isDetecting: Boolean,
    silenceConfig: SilenceDetectionConfig,
    totalProjectDurationMs: Long,
    onUpdateConfig: (SilenceDetectionConfig) -> Unit,
    onToggleInterval: (String) -> Unit,
    onSelectAll: (Boolean) -> Unit,
    onExecuteRemoval: () -> Unit,
    onNextStep: () -> Unit
) {
    val selectedToRemove = silences.filter { it.isSelectedForRemoval }
    val totalTimeSavedMs = selectedToRemove.sumOf { it.durationMs }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Controles de Sensibilidade (Sliders compactos)
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = StudioSurfaceVariant.copy(alpha = 0.6f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Limiar de Ruído: ${silenceConfig.thresholdDb.toInt()} dB", fontSize = 12.sp, color = TextPrimary)
                    Text("Padrão: -40 dB", fontSize = 11.sp, color = TextSecondary)
                }
                Slider(
                    value = silenceConfig.thresholdDb,
                    onValueChange = { onUpdateConfig(silenceConfig.copy(thresholdDb = it)) },
                    valueRange = -55f..-20f,
                    steps = 34,
                    colors = SliderDefaults.colors(thumbColor = CineCyan, activeTrackColor = CineCyan)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Duração mínima: ${silenceConfig.minDurationMs} ms", fontSize = 12.sp, color = TextPrimary)
                    Text("Margem segurança: ${silenceConfig.safetyPaddingMs} ms", fontSize = 11.sp, color = TextSecondary)
                }
            }
        }

        // Resumo das Pausas Detetadas
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val secondsSaved = String.format(java.util.Locale.US, "%.1f", totalTimeSavedMs / 1000f)
            Text(
                text = "${selectedToRemove.size} de ${silences.size} pausas marcadas (~${secondsSaved}s a poupar)",
                fontSize = 12.sp,
                color = CineCyan,
                fontWeight = FontWeight.SemiBold
            )

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                TextButton(
                    onClick = { onSelectAll(true) },
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text("Todas", fontSize = 11.sp, color = CineCyan)
                }
                TextButton(
                    onClick = { onSelectAll(false) },
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text("Nenhuma", fontSize = 11.sp, color = TextSecondary)
                }
            }
        }

        // Lista de Intervalos de Silêncio
        if (silences.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                if (isDetecting) {
                    CircularProgressIndicator(color = CineCyan)
                } else {
                    Text("Nenhuma pausa significativa encontrada.", color = TextSecondary, fontSize = 12.sp)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(silences, key = { it.id }) { interval ->
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (interval.isSelectedForRemoval) StudioDarkBg else StudioSurfaceVariant.copy(alpha = 0.4f),
                        border = if (interval.isSelectedForRemoval) borderStroke(1.dp, CineCyan.copy(alpha = 0.5f)) else null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onToggleInterval(interval.id) }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Checkbox(
                                checked = interval.isSelectedForRemoval,
                                onCheckedChange = { onToggleInterval(interval.id) },
                                colors = CheckboxDefaults.colors(checkedColor = CineCyan)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Pausa em ${TimeFormatter.formatMsToTimecode(interval.timelineStartMs)}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "Duração: ${interval.durationMs}ms (até ${TimeFormatter.formatMsToTimecode(interval.timelineEndMs)})",
                                    fontSize = 11.sp,
                                    color = TextSecondary
                                )
                            }
                            Text(
                                text = "-${interval.durationMs / 1000f}s",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = CineRose
                            )
                        }
                    }
                }
            }
        }

        // Barra inferior de Ações da Etapa A
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = onExecuteRemoval,
                enabled = selectedToRemove.isNotEmpty(),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text("Remover Silêncios", fontSize = 12.sp)
            }

            Button(
                onClick = onNextStep,
                colors = ButtonDefaults.buttonColors(containerColor = CineCyan),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text("Avançar para Etapa B →", fontSize = 12.sp, color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }
    }
}

/**
 * Conteúdo da Etapa B: Transcrição Whisper e Deteção de Repetições / Gaguez
 */
@Composable
private fun StepBRepetitionsContent(
    hasGroqApiKey: Boolean,
    hasAcceptedConsent: Boolean,
    onAcceptConsent: () -> Unit,
    onOpenSettings: () -> Unit,
    isTranscribing: Boolean,
    progressText: String,
    suggestions: List<RepetitionCutSuggestion>,
    config: RepetitionDetectionConfig,
    onUpdateConfig: (RepetitionDetectionConfig) -> Unit,
    onStartTranscription: () -> Unit,
    onToggleSuggestion: (String) -> Unit,
    onSelectAll: (Boolean) -> Unit,
    onPreviewCutAudio: (Long, Long) -> Unit,
    onExecuteCuts: () -> Unit
) {
    val selectedCuts = suggestions.filter { it.isSelected }
    val totalSavedMs = selectedCuts.sumOf { it.durationMs }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // 1. Alerta se chave de API estiver ausente
        if (!hasGroqApiKey) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = CineAmber.copy(alpha = 0.15f),
                border = borderStroke(1.dp, CineAmber.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(imageVector = Icons.Default.VpnKey, contentDescription = null, tint = CineAmber)
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Chave da Groq API Necessária", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextPrimary)
                        Text("Configura a tua chave gratuita para desbloquear a transcrição e remoção de gaguez.", fontSize = 11.sp, color = TextSecondary)
                    }
                    Button(
                        onClick = onOpenSettings,
                        colors = ButtonDefaults.buttonColors(containerColor = CineAmber),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text("Definições", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // 2. Consentimento de Privacidade Inicial
        if (hasGroqApiKey && !hasAcceptedConsent) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = CineCyan.copy(alpha = 0.12f),
                border = borderStroke(1.dp, CineCyan.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Lock, contentDescription = null, tint = CineCyan, modifier = Modifier.size(18.dp))
                        Text("Consentimento de Transcrição", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextPrimary)
                    }
                    Text(
                        text = "O áudio da timeline será enviado de forma segura à API Groq (modelo Whisper) para detetar as palavras faladas e localizar repetições. Não será guardado para treino.",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                    Button(
                        onClick = onAcceptConsent,
                        colors = ButtonDefaults.buttonColors(containerColor = CineCyan),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Compreendi e Concordo", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // 3. Barra de Configurações de Deteção
        if (hasGroqApiKey && hasAcceptedConsent) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = StudioSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Sensibilidade:", fontSize = 12.sp, color = TextPrimary, fontWeight = FontWeight.Medium)
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            RepetitionSensitivity.values().forEach { s ->
                                FilterChip(
                                    selected = config.sensitivity == s,
                                    onClick = { onUpdateConfig(config.copy(sensitivity = s)) },
                                    label = { Text(s.name.lowercase().replaceFirstChar { it.uppercase() }, fontSize = 10.sp) }
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Ignorar palavras curtas (a, o, de...)", fontSize = 11.sp, color = TextSecondary)
                        Switch(
                            checked = config.ignoreShortWords,
                            onCheckedChange = { onUpdateConfig(config.copy(ignoreShortWords = it)) },
                            colors = SwitchDefaults.colors(checkedThumbColor = CineCyan, checkedTrackColor = CineCyan.copy(alpha = 0.5f))
                        )
                    }
                }
            }
        }

        // 4. Botão de Iniciar Transcrição ou Estado de Processamento
        if (hasGroqApiKey && hasAcceptedConsent && suggestions.isEmpty() && !isTranscribing) {
            Button(
                onClick = onStartTranscription,
                colors = ButtonDefaults.buttonColors(containerColor = CineCyan),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("start_transcription_button")
            ) {
                Icon(imageVector = Icons.Default.Bolt, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Transcrever com Whisper e Detetar Repetições", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }

        // Indicador de Carregamento
        if (isTranscribing) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = StudioSurfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CircularProgressIndicator(color = CineCyan, modifier = Modifier.size(24.dp), strokeWidth = 2.5.dp)
                    Column {
                        Text("A processar áudio com IA...", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = TextPrimary)
                        Text(progressText.ifBlank { "A enviar para a Groq Whisper..." }, fontSize = 11.sp, color = CineCyan)
                    }
                }
            }
        }

        // 5. Ecrã de Revisão das Sugestões Detetadas
        if (suggestions.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val secondsSaved = String.format(java.util.Locale.US, "%.1f", totalSavedMs / 1000f)
                Text(
                    text = "${selectedCuts.size} de ${suggestions.size} repetições (~${secondsSaved}s a poupar)",
                    fontSize = 12.sp,
                    color = CineCyan,
                    fontWeight = FontWeight.SemiBold
                )

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    TextButton(
                        onClick = { onSelectAll(true) },
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text("Todas", fontSize = 11.sp, color = CineCyan)
                    }
                    TextButton(
                        onClick = { onSelectAll(false) },
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text("Nenhuma", fontSize = 11.sp, color = TextSecondary)
                    }
                }
            }

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(suggestions, key = { it.id }) { item ->
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (item.isSelected) StudioDarkBg else StudioSurfaceVariant.copy(alpha = 0.4f),
                        border = if (item.isSelected) borderStroke(1.dp, CineCyan.copy(alpha = 0.5f)) else null,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Checkbox(
                                checked = item.isSelected,
                                onCheckedChange = { onToggleSuggestion(item.id) },
                                colors = CheckboxDefaults.colors(checkedColor = CineCyan)
                            )

                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = CineRose.copy(alpha = 0.2f)
                                    ) {
                                        Text(
                                            text = item.reason,
                                            color = CineRose,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                        )
                                    }
                                    Text(
                                        text = "${TimeFormatter.formatMsToTimecode(item.startMs)} (-${item.durationMs / 1000f}s)",
                                        fontSize = 10.sp,
                                        color = TextSecondary
                                    )
                                }

                                Spacer(modifier = Modifier.height(2.dp))

                                // Texto repetido em destaque com contexto
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (item.contextBefore.isNotBlank()) {
                                        Text(
                                            text = "${item.contextBefore} ",
                                            fontSize = 11.sp,
                                            color = TextSecondary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(3.dp),
                                        color = CineRose.copy(alpha = 0.25f)
                                    ) {
                                        Text(
                                            text = item.repeatedText,
                                            color = TextPrimary,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            modifier = Modifier.padding(horizontal = 3.dp)
                                        )
                                    }
                                    if (item.contextAfter.isNotBlank()) {
                                        Text(
                                            text = " ${item.contextAfter}",
                                            fontSize = 11.sp,
                                            color = TextSecondary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }

                            // Botão Ouvir Prévia (1s antes + 1s depois)
                            IconButton(
                                onClick = { onPreviewCutAudio(item.startMs, item.endMs) },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayCircle,
                                    contentDescription = "Ouvir corte",
                                    tint = CineCyan,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Botão Aplicar Cortes Selecionados
            Button(
                onClick = onExecuteCuts,
                enabled = selectedCuts.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(containerColor = CineCyan),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("apply_repetition_cuts_button")
            ) {
                Icon(imageVector = Icons.Default.ContentCut, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Aplicar ${selectedCuts.size} Cortes Selecionados",
                    color = Color.Black,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }
        }
    }
}

private fun borderStroke(width: androidx.compose.ui.unit.Dp, color: Color) = androidx.compose.foundation.BorderStroke(width, color)
