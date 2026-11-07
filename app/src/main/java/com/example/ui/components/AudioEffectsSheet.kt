package com.example.ui.components

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
import com.example.model.AudioClip
import com.example.model.AudioEffects
import com.example.ui.theme.*

/**
 * Painel completo de edição e mistura de áudio: Equalizador de 3 bandas,
 * Normalização, Redução de Ruído, Panorâmica (Pan) e Fade In/Out.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioEffectsSheet(
    clip: AudioClip,
    onDismiss: () -> Unit,
    onSaveEffects: (effects: AudioEffects, volume: Float, pan: Float, fadeInMs: Long, fadeOutMs: Long) -> Unit,
    onExportAudioOnly: () -> Unit,
    modifier: Modifier = Modifier
) {
    var volume by remember { mutableStateOf(clip.volume) }
    var pan by remember { mutableStateOf(clip.pan) }
    var fadeInMs by remember { mutableStateOf(clip.fadeInMs.toFloat()) }
    var fadeOutMs by remember { mutableStateOf(clip.fadeOutMs.toFloat()) }

    var normalize by remember { mutableStateOf(clip.effects.normalizeVolume) }
    var noiseReduction by remember { mutableStateOf(clip.effects.noiseReduction) }
    var bassDb by remember { mutableStateOf(clip.effects.bassBoostDb) }
    var midDb by remember { mutableStateOf(clip.effects.midToneDb) }
    var trebleDb by remember { mutableStateOf(clip.effects.trebleDb) }

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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Edição & Mistura de Áudio",
                        color = TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = clip.name,
                        color = CineViolet,
                        fontSize = 12.sp
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Fechar", tint = TextSecondary)
                }
            }

            // VOLUME & PANORÂMICA (PAN)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Volume da Faixa", color = TextPrimary, fontSize = 13.sp)
                    Text("${(volume * 100).toInt()}%", color = CineEmerald, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
                Slider(
                    value = volume,
                    onValueChange = { volume = it },
                    valueRange = 0f..2f,
                    colors = SliderDefaults.colors(thumbColor = CineEmerald, activeTrackColor = CineEmerald)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Panorâmica (Equilíbrio E/D)", color = TextPrimary, fontSize = 13.sp)
                    val panLabel = when {
                        pan < -0.1f -> "Esq ${(pan * -100).toInt()}%"
                        pan > 0.1f -> "Dir ${(pan * 100).toInt()}%"
                        else -> "Centro"
                    }
                    Text(panLabel, color = CineCyan, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
                Slider(
                    value = pan,
                    onValueChange = { pan = it },
                    valueRange = -1f..1f,
                    colors = SliderDefaults.colors(thumbColor = CineCyan, activeTrackColor = CineCyan)
                )
            }

            HorizontalDivider(color = StudioBorder.copy(alpha = 0.5f))

            // FADE IN & FADE OUT (Subida e descida gradual do volume)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Transições de Volume (Fade)", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Fade In (Entrada suave)", color = TextSecondary, fontSize = 12.sp)
                    Text("${(fadeInMs / 1000f).let { String.format("%.1fs", it) }}", color = CineViolet, fontSize = 12.sp)
                }
                Slider(
                    value = fadeInMs,
                    onValueChange = { fadeInMs = it },
                    valueRange = 0f..3000f,
                    colors = SliderDefaults.colors(thumbColor = CineViolet, activeTrackColor = CineViolet)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Fade Out (Saída suave)", color = TextSecondary, fontSize = 12.sp)
                    Text("${(fadeOutMs / 1000f).let { String.format("%.1fs", it) }}", color = CineViolet, fontSize = 12.sp)
                }
                Slider(
                    value = fadeOutMs,
                    onValueChange = { fadeOutMs = it },
                    valueRange = 0f..3000f,
                    colors = SliderDefaults.colors(thumbColor = CineViolet, activeTrackColor = CineViolet)
                )
            }

            HorizontalDivider(color = StudioBorder.copy(alpha = 0.5f))

            // EQUALIZADOR DE 3 BANDAS (GRAVES, MÉDIOS E AGUDOS)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Equalizador Gráfico (EQ)", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)

                // Graves
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Graves (Bass 100Hz)", color = TextSecondary, fontSize = 12.sp)
                    Text("${bassDb.toInt()} dB", color = CineAmber, fontSize = 12.sp)
                }
                Slider(
                    value = bassDb,
                    onValueChange = { bassDb = it },
                    valueRange = -12f..12f,
                    colors = SliderDefaults.colors(thumbColor = CineAmber, activeTrackColor = CineAmber)
                )

                // Médios
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Médios (Mid 1kHz)", color = TextSecondary, fontSize = 12.sp)
                    Text("${midDb.toInt()} dB", color = CineCyan, fontSize = 12.sp)
                }
                Slider(
                    value = midDb,
                    onValueChange = { midDb = it },
                    valueRange = -12f..12f,
                    colors = SliderDefaults.colors(thumbColor = CineCyan, activeTrackColor = CineCyan)
                )

                // Agudos
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Agudos (Treble 8kHz)", color = TextSecondary, fontSize = 12.sp)
                    Text("${trebleDb.toInt()} dB", color = CineEmerald, fontSize = 12.sp)
                }
                Slider(
                    value = trebleDb,
                    onValueChange = { trebleDb = it },
                    valueRange = -12f..12f,
                    colors = SliderDefaults.colors(thumbColor = CineEmerald, activeTrackColor = CineEmerald)
                )
            }

            // PROCESSAMENTO INTELIGENTE (NORMALIZAÇÃO E REDUÇÃO DE RUÍDO)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Normalização de Volume", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    Text("Nivela o volume para o padrão padrão da indústria", color = TextSecondary, fontSize = 11.sp)
                }
                Switch(checked = normalize, onCheckedChange = { normalize = it })
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Redução de Ruído", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    Text("Filtra zumbidos e ruído de fundo estático", color = TextSecondary, fontSize = 11.sp)
                }
                Switch(checked = noiseReduction, onCheckedChange = { noiseReduction = it })
            }

            // BOTÃO EXPORTAR APENAS ÁUDIO
            OutlinedButton(
                onClick = {
                    onDismiss()
                    onExportAudioOnly()
                },
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(imageVector = Icons.Default.Audiotrack, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Exportar Apenas Este Áudio (MP3 / WAV)", fontSize = 12.sp)
            }

            // BOTÃO APLICAR ALTERAÇÕES
            Button(
                onClick = {
                    onSaveEffects(
                        AudioEffects(
                            normalizeVolume = normalize,
                            bassBoostDb = bassDb,
                            midToneDb = midDb,
                            trebleDb = trebleDb,
                            noiseReduction = noiseReduction
                        ),
                        volume,
                        pan,
                        fadeInMs.toLong(),
                        fadeOutMs.toLong()
                    )
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = CineCyan),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth().testTag("save_audio_effects_button")
            ) {
                Text("Guardar Alterações de Áudio", color = Color.Black, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
