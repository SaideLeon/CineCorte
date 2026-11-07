package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ClipTransition
import com.example.model.TransitionType
import com.example.ui.theme.*

/**
 * Seletor de transições cinematográficas com ajuste de duração e easing.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransitionPickerSheet(
    currentTransition: ClipTransition,
    onDismiss: () -> Unit,
    onApplyTransition: (ClipTransition) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedType by remember { mutableStateOf(currentTransition.type) }
    var durationMs by remember { mutableStateOf(currentTransition.durationMs.toFloat()) }
    var selectedEasing by remember { mutableStateOf(currentTransition.easing) }

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
                        text = "Transições Cinematográficas",
                        color = TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Efeitos visuais renderizados na placa gráfica",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Fechar", tint = TextSecondary)
                }
            }

            // Grid de opções de transição
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.height(210.dp)
            ) {
                items(TransitionType.values()) { type ->
                    val isSelected = type == selectedType
                    val icon = getTransitionIcon(type)

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) CineCyan.copy(alpha = 0.25f) else StudioCard)
                            .border(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) CineCyan else StudioBorder,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable { selectedType = type }
                            .padding(10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = if (isSelected) CineCyanGlow else TextSecondary,
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                text = type.labelPt,
                                color = if (isSelected) TextPrimary else TextSecondary,
                                fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            // Slider de duração da transição (200ms a 2000ms)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Duração da Transição", color = TextPrimary, fontSize = 13.sp)
                    Text(text = "${(durationMs / 1000f).let { String.format("%.1fs", it) }}", color = CineCyan, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
                Slider(
                    value = durationMs,
                    onValueChange = { durationMs = it },
                    valueRange = 200f..2000f,
                    steps = 8,
                    colors = SliderDefaults.colors(thumbColor = CineCyan, activeTrackColor = CineCyan)
                )
            }

            // Easing / Suavização
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf("EaseInOut", "EaseIn", "EaseOut", "Linear").forEach { ease ->
                    FilterChip(
                        selected = selectedEasing == ease,
                        onClick = { selectedEasing = ease },
                        label = { Text(ease, fontSize = 10.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = CineCyan.copy(alpha = 0.25f),
                            selectedLabelColor = CineCyanGlow
                        )
                    )
                }
            }

            // Botão Aplicar
            Button(
                onClick = {
                    onApplyTransition(
                        ClipTransition(
                            type = selectedType,
                            durationMs = durationMs.toLong(),
                            easing = selectedEasing
                        )
                    )
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = CineCyan),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth().testTag("apply_transition_button")
            ) {
                Text(text = "Aplicar Transição", color = Color.Black, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

private fun getTransitionIcon(type: TransitionType): ImageVector {
    return when (type) {
        TransitionType.NONE -> Icons.Default.Block
        TransitionType.FADE_BLACK -> Icons.Default.BrightnessMedium
        TransitionType.DISSOLVE -> Icons.Default.BlurOn
        TransitionType.SLIDE_LEFT -> Icons.Default.ArrowBack
        TransitionType.SLIDE_RIGHT -> Icons.Default.ArrowForward
        TransitionType.SLIDE_UP -> Icons.Default.ArrowUpward
        TransitionType.SLIDE_DOWN -> Icons.Default.ArrowDownward
        TransitionType.ZOOM_IN -> Icons.Default.ZoomIn
        TransitionType.ZOOM_OUT -> Icons.Default.ZoomOut
        TransitionType.WIPE -> Icons.Default.ViewCarousel
        TransitionType.FLASH_WHITE -> Icons.Default.FlashOn
    }
}
