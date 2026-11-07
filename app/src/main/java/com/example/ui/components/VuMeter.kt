package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

/**
 * Medidor de nível estéreo profissional (VU Meter) com indicação de dB e pico.
 */
@Composable
fun VuMeter(
    isPlaying: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "vu_anim")
    val animLeft by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(240, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "vu_left"
    )
    val animRight by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.90f,
        animationSpec = infiniteRepeatable(
            animation = tween(310, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "vu_right"
    )

    val leftLevel = if (isPlaying) animLeft else 0.05f
    val rightLevel = if (isPlaying) animRight else 0.05f

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(StudioSurfaceVariant.copy(alpha = 0.9f))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = "VU",
            color = CineCyan,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold
        )

        // Canal L
        VuBar(level = leftLevel, label = "L")

        // Canal R
        VuBar(level = rightLevel, label = "R")
    }
}

@Composable
private fun VuBar(
    level: Float,
    label: String,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        modifier = modifier
    ) {
        Text(
            text = label,
            color = TextSecondary,
            fontSize = 8.sp,
            fontWeight = FontWeight.Medium
        )
        Box(
            modifier = Modifier
                .width(42.dp)
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(StudioDarkBg)
        ) {
            val barColor = when {
                level > 0.82f -> CineRose
                level > 0.65f -> CineAmber
                else -> CineEmerald
            }
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(level.coerceIn(0f, 1f))
                    .clip(RoundedCornerShape(3.dp))
                    .background(barColor)
            )
        }
    }
}
