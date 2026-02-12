package com.example.echoverse.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.echoverse.ui.theme.*

@Composable
fun EchoScreen(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(PremiumGradient)
    ) {
        // Floating ambient blobs
        PulsingOrb(
            modifier = Modifier.align(Alignment.TopEnd).offset(x = 50.dp, y = (-50).dp),
            size = 300.dp,
            color = AccentSecondary.copy(alpha = 0.15f)
        )
        PulsingOrb(
            modifier = Modifier.align(Alignment.BottomStart).offset(x = (-50).dp, y = 50.dp),
            size = 300.dp,
            color = AccentPrimary.copy(alpha = 0.15f)
        )
        content()
    }
}

@Composable
fun GlassPanel(
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(24.dp),
    opacity: Float = 0.1f, // Standard glass
    content: @Composable ColumnScope.() -> Unit
) {
    Box(
        modifier = modifier
            .clip(shape)
            .background(Color.White.copy(alpha = opacity)) // Simple alpha for now, proper blur needs API 31+ or renderscript
            .border(1.dp, Brush.linearGradient(listOf(Color.White.copy(alpha = 0.2f), Color.White.copy(alpha = 0.05f))), shape)
            .padding(24.dp)
    ) {
        Column {
            content()
        }
    }
}

@Composable
fun EchoButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    primary: Boolean = true
) {
    val backgroundBrush = if (primary) GlowGradient else Brush.verticalGradient(
        colors = listOf(Color(0x20FFFFFF), Color(0x10FFFFFF))
    )
    
    val textStyle = MaterialTheme.typography.titleMedium

    Box(
        modifier = modifier
            .height(56.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(backgroundBrush)
            .clickable(onClick = onClick)
            .then(
                 if (!primary) Modifier.border(1.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(16.dp)) else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = textStyle,
            color = if (primary) Color.White else TextPrimary
        )
    }
}

@Composable
fun PulsingOrb(
    modifier: Modifier = Modifier,
    size: Dp = 200.dp,
    color: Color = AccentPrimary,
    durationMillis: Int = 4000
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )
    
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Box(
        modifier = modifier
            .size(size)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                alpha = pulseAlpha
            }
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(color, Color.Transparent),
                    center = Offset.Unspecified,
                    radius = Float.POSITIVE_INFINITY
                ),
                shape = CircleShape
            )
    )
}
